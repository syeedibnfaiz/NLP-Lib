package ca.uwo.csd.ai.nlp.ml;


import ca.uwo.csd.ai.nlp.ling.ann.ClauseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * A <code>SeqTaggedSentence2TokenSequence</code> object is pipe which takes a
 * <code>SentenceInstance</code> ands transforms it into a <code>TokenSequence</code>.
 * Tokens inside that sentence should have its tag associated with it 'word', attached by
 * a '|'. For example: However|B-conn , we are sure that ... as|B-conn a|I-conn result|I-conn ...
 * @author Syeed Ibn Faiz
 */
public class SeqTaggedSentence2TokenSequence extends Pipe implements Serializable {

    private SerialAnnotator annotator;
    private static final Pattern pat = Pattern.compile("CC|TO|IN|PDT|WRB|[,.;:?!-+()]");
    public SeqTaggedSentence2TokenSequence() {
        super(null, new LabelAlphabet());
        annotator = new SerialAnnotator();
        annotator.add(new GeniaTagger());
        annotator.add(new ClauseBoundaryAnnotator(false));
        //annotator.add(new ClauseAnnotator());
    }


    @Override
    public Instance pipe(Instance carrier) {
        Sentence sentence = (Sentence) carrier.getData();
        TokenSequence data = new TokenSequence(sentence.size());
        LabelSequence target = new LabelSequence ((LabelAlphabet)getTargetAlphabet(), sentence.size());

        for (int i = 0; i < sentence.size(); i++) {
            TokWord tokWord = sentence.get(i);
            String s = tokWord.word();
            int where = s.lastIndexOf('|');
            String word;
            String tag;
            if (where == -1 || where == 0 || where == (s.length() - 1) ) {
                word = s;
                tag = "O";
            } else {
                word = s.substring(0, where);
                tag = s.substring(where + 1);
            }

            tokWord.setWord(word);
            //word = word.toLowerCase();
            
            Token token = new Token(word);
            
            data.add(token);
            target.add(tag);
        }

        if (!sentence.isAnnotatedBy("CLS_ANN")) {
            sentence = annotator.annotate(sentence);
        }
        for (int i = 0; i < sentence.size(); i++) {
            TokWord tokWord = sentence.get(i);
            Token token = data.get(i);
            String word = tokWord.word().toLowerCase();
            
            token.setFeatureValue("W="+word.toLowerCase(), 1.0);
            
            token.setFeatureValue(tokWord.getTag("POS"), 1.0);
            token.setFeatureValue(tokWord.getTag("CHUNK"), 1.0);
            if (tokWord.getTag("POS").matches("CC|IN|TO|WRB")) {
                //token.setFeatureValue(word.toLowerCase()+"-"+tokWord.getTag("POS"), 1.0);
                //token.setFeatureValue(tokWord.getTag("POS")+"&"+tokWord.getTag("CHUNK"), 1.0);
                token.setFeatureValue(word+"&"+tokWord.getTag("CHUNK"), 1.0);
            }
            
            
            /*
            if (tokWord.getTag("CLS_BN_S").equals("S")) {
                token.setFeatureValue("<"+tokWord.getTag("CLS_BN_S"), 1.0);
                token.setFeatureValue("<"+tokWord.getTag("POS")+"-"+tokWord.getTag("CLS_BN_S"), 1.0);
            }
            if (tokWord.getTag("CLS_BN_E").equals("E")) {
                token.setFeatureValue(">"+tokWord.getTag("CLS_BN_E"), 1.0);
            }
            */
            addPOSWindowFeatures(token, i, sentence);
            addChunkWindowFeature(token, i, sentence);
            addBoundaryWindowFeature(token, i, sentence);
            //addClauseFeature(token, i, sentence);
            addSentencePatterns(token, i, sentence);
            
        }
        
        carrier.setData(data);
        carrier.setTarget(target);
        carrier.setSource(sentence.toString());
        return carrier;
    }
    
    private void addPOSWindowFeatures(Token t, int pos, Sentence s) {
        String[] posTags = s.getTags("POS");
        
        for (int i = pos - 1, count = 1; i >= 0 && count <= 3; i--, count++) {
            t.setFeatureValue(posTags[i]+"@-"+count, 1.0);
            //t.setFeatureValue(posTags[pos]+":"+posTags[i]+"@-"+count, 1.0);
            if (pat.matcher(posTags[i]).matches()) {
                t.setFeatureValue(s.get(i).word().toLowerCase()+"@-"+count, 1.0);
            }            
        }
        //pos right window
        
        for (int i = pos + 1, count = 1; i < s.size() && count <= 3; i++, count++) {
            t.setFeatureValue(posTags[i]+"@+"+count, 1.0);
            //t.setFeatureValue(posTags[pos]+":"+posTags[i]+"@+"+count, 1.0);
            if (pat.matcher(posTags[i]).matches()) {
                t.setFeatureValue(s.get(i).word().toLowerCase()+"@+"+count, 1.0);
            }        
        }
    }

    private void addChunkWindowFeature(Token t, int pos, Sentence s) {
        String[] chunkTags = s.getTags("CHUNK");
        String[] posTags = s.getTags("POS");
        //chunk left window feature        
        for (int i = pos - 1, count = 0; i >= 0 && count < 3; i--) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                t.setFeatureValue(chunkTags[i]+"@-"+count, 1.0);
                //t.setFeatureValue(chunkTags[pos]+":"+chunkTags[i]+"@-"+count, 1.0);                
            } else if (chunkTags[i].startsWith("O")) {
                count++;
                t.setFeatureValue("O-"+s.get(i).word().toLowerCase()+"@-"+count, 1.0);
            }
        }

        //chunk right window feature        
        for (int i = pos + 1, count = 0; i < s.size() && count < 3; i++) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                t.setFeatureValue(chunkTags[i]+"@+"+count, 1.0);
                //t.setFeatureValue(chunkTags[pos]+":"+chunkTags[i]+"@+"+count, 1.0);                
            } else if (chunkTags[i].startsWith("O")) {
                count++;
                t.setFeatureValue("O-"+s.get(i).word().toLowerCase()+"@-"+count, 1.0);
            }
        }
        String leftChunk = "#";
        for (int i = pos - 1; i >= 0; i--) {
            if (chunkTags[i].startsWith("B")) {
                leftChunk = chunkTags[i];
                break;
            }
        }
        String rightChunk = "%";
        for (int i = pos + 1; i < chunkTags.length; i++) {
            if (chunkTags[i].startsWith("B")) {
                rightChunk = chunkTags[i];
                break;
            }
        }
        if (posTags[pos].matches("CC|IN|TO|WRB")) {
            t.setFeatureValue(leftChunk+"&"+s.get(pos).word().toLowerCase()+"&"+rightChunk, 1.0);
        }
    }
    private void addBoundaryWindowFeature(Token t, int pos, Sentence s) {
        String[] sTags = s.getTags("CLS_BN_S");
        String[] eTags = s.getTags("CLS_BN_E");
        String posTag = s.get(pos).getTag("POS");
        String[] chunkTags = s.getTags("CHUNK");
        for (int i = pos - 1, count = 0; i >= 0 && count < 4; i--, count++) {
            /*if (sTags[i].equals("S")) {
                t.setFeatureValue(sTags[i]+"@-"+count, 1.0);
                //t.setFeatureValue(posTag+":"+sTags[i]+"@-"+count, 1.0);
            }*/
            if (eTags[i].equals("E")) {
                t.setFeatureValue(eTags[i]+"@-"+count, 1.0);
                //t.setFeatureValue(posTag+":"+eTags[i]+"@-"+count, 1.0);
            }
        }        
        for (int i = pos + 1, count = 0; i < s.size() && count < 4; i++, count++) {
            if (sTags[i].equals("S")) {                
                t.setFeatureValue(sTags[i]+"@+"+count, 1.0);
                //t.setFeatureValue(posTag+":"+sTags[i]+"@+"+count, 1.0);
            }
            /*if (eTags[i].equals("E")) {
                t.setFeatureValue(eTags[i]+"@+"+count, 1.0);
                //t.setFeatureValue(posTag+":"+eTags[i]+"@+"+count, 1.0);
            }*/
        }
        boolean leftVP = false;
        for (int i = pos-1; i >= 0; i--) {
            if (sTags[i].equals("S")) break;
            if (chunkTags[i].equals("B-VP")) {
                leftVP = true;
                break;
            }
        }
        boolean rightVP = false;
        for (int i = pos+1; i < chunkTags.length; i++) {
            if (eTags[i].equals("E")) break;
            if (chunkTags[i].equals("B-VP")) {
                rightVP = true;
                break;
            }
        }
        t.setFeatureValue("VP-"+leftVP+","+rightVP, 1.0);
        if (posTag.matches("CC|IN|TO|WRB")) {
            t.setFeatureValue(s.get(pos).word()+"-VP-"+leftVP+","+rightVP, 1.0);
        }
    }
    private void addClauseFeature(Token t, int pos, Sentence s) {
        String[] clsAnn = s.getTags("CLS_ANN");
        String[] clsStart = s.getTags("CLS_S#");
        String[] clsEnd = s.getTags("CLS_E#");
        //determine the clause depth for this token,i.e how deep it is buried
        int openDepth = 0;
        int closeDepth = 0;
        for (int i = 0; i <= pos; i++) {
            if (!clsStart[i].equals("0")) {
                String tokens[] = clsStart[i].split(":");
                for (String token : tokens) {
                    int tmp = Integer.parseInt(token);
                    if (tmp > openDepth) openDepth = tmp;
                }
            }
            if (!clsEnd[i].equals("0")) {
                String tokens[] = clsEnd[i].split(":");
                for (String token : tokens) {
                    int tmp = Integer.parseInt(token);
                    if (tmp > closeDepth) closeDepth = tmp;
                }
            }
        }
        int clsDepth = (openDepth - closeDepth);
        t.setFeatureValue("CLS_DEPTH=" + clsDepth, 1.0);
        
    }
    private void addSentencePatterns(Token t, int pos, Sentence s) {

        String[] posTags = s.getTags("POS");
        String[] chunkTags = s.getTags("CHUNK");
        String[] clauseSTags = s.getTags("CLS_BN_S");
        String[] clauseETags = s.getTags("CLS_BN_E");

        ArrayList<String> words = s.getWords();

        t.setFeatureValue("^^" + getPattern(0, pos, words, posTags, chunkTags, clauseSTags, clauseETags), 1.0);
        t.setFeatureValue("$$" + getPattern(pos, s.size() - 1, words, posTags, chunkTags, clauseSTags, clauseETags), 1.0);
        
    }
    private String getPattern(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags, String[] clauseSTags, String[] clauseETags) {
        String pattern = "";
        for (int i = start; i <= end; i++) {
            if (clauseSTags[i].equals("S")) pattern += "::S";
            /*if (posTags[i].matches("CC")) {
                pattern += "::" + posTags[i];
            } else if (chunkTags[i].matches("B-SBAR")) {
                pattern += "::SBAR";;
            } else if (posTags[i].matches("[,.;:?!-+()]")) {
                pattern += "::" + "PUNC";
            } else*/ if (chunkTags[i].matches("B-VP")) {
               pattern += "::VP";
            }
            if (clauseSTags[i].equals("E")) pattern += "::E";
        }
        return pattern;
    }
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeObject(annotator);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        this.annotator = (SerialAnnotator) in.readObject();
    }
}
