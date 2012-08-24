package ca.uwo.csd.ai.nlp.ml;


import ca.uwo.csd.ai.nlp.ling.Sentence;
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
 * Tokens inside that sentence should have POS, CHUNK, CLS_BN_S&E and CLS_ANN tags. This pipe is
 * used for tagging clause open boundaries with wither 1, 2 or 3.
 * @author Syeed Ibn Faiz
 */
public class ClauseTaggedSentence2TokenSequence extends Pipe implements Serializable {

    static Pattern pat = Pattern.compile("CC|DT|EX|TO|IN|MD|PDT|V.*|W.*|[,.;:?!-+()]");
    private static final String elements[] = new String[]{"VP", "W*", "PUNC"};          //sentence feature element
    private static final Pattern patterns[] = new Pattern[]{Pattern.compile("VB.*"),Pattern.compile("W.*"),Pattern.compile("[,.;:?!-+]")};
    public ClauseTaggedSentence2TokenSequence() {
        super(null, new LabelAlphabet());
    }


    @Override
    public Instance pipe(Instance carrier) {
        Sentence sentence = (Sentence) carrier.getData();
        TokenSequence data = new TokenSequence(sentence.size());
        LabelSequence target = new LabelSequence ((LabelAlphabet)getTargetAlphabet(), sentence.size());
                
        for (int i = 0; i < sentence.size(); i++) {
            TokWord tokWord = sentence.get(i);
            String word = tokWord.word();            
            Token token = new Token(word);

            //token.setFeatureValue("W="+word.toLowerCase(), 1.0);
            String posTag = tokWord.getTag("POS");
            String chunkTag = tokWord.getTag("CHUNK");
            token.setFeatureValue(posTag, 1.0);
            token.setFeatureValue(chunkTag, 1.0);

            if (pat.matcher(posTag).matches()) {
                token.setFeatureValue(word.toLowerCase(), 1.0);
            }
            if (tokWord.getTag("CLS_BN_S").equals("S")) {
                token.setFeatureValue("^SS", 1.0);                
            }
            if (tokWord.getTag("CLS_BN_E").equals("E")) {
                token.setFeatureValue("EE$", 1.0);                
            }

            if (i == 0 || chunkTag.startsWith("B")) {
                addPOSWindowFeatures(token, i, sentence);
                addChunkWindowFeature(token, i, sentence);
                addBoundaryFeature(token, i, sentence);
                addSentenceFeature(token, i, sentence);
                addSentencePatterns(token, i, sentence);
                addSpecialFeatures(token, i, sentence);
            }
            data.add(token);

            //set target
            if (tokWord.getTag("CLS_ANN") != null) {
                String tag = tokWord.getTag("CLS_ANN");
                int openBracketCount = 0;
                for (int j = 0; j < tag.length(); j++) {
                    if (tag.charAt(j) == '(') {
                        openBracketCount++;
                    }
                }
                if (openBracketCount > 2) {
                    openBracketCount = 2;
                }
                target.add(String.valueOf(openBracketCount));
            } else {
                target.add("0");
            }
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
            if (pat.matcher(posTags[i]).matches()) {
                t.setFeatureValue(s.get(i).word().toLowerCase()+"@-"+count, 1.0);
            }
        }
        //pos right window
        for (int i = pos + 1, count = 1; i < s.size() && count <= 3; i++, count++) {
            t.setFeatureValue(posTags[i]+"@+"+count, 1.0);
            if (pat.matcher(posTags[i]).matches()) {
                t.setFeatureValue(s.get(i).word().toLowerCase()+"@+"+count, 1.0);
            }
        }
    }
    private void addChunkWindowFeature(Token t, int pos, Sentence s) {
        String[] chunkTags = s.getTags("CHUNK");
        //chunk left window feature
        for (int i = pos - 1, count = 0; i >= 0 && count < 3; i--) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                t.setFeatureValue(chunkTags[i]+"@-"+count, 1.0);
            }
        }

        //chunk right window feature
        for (int i = pos + 1, count = 0; i < s.size() && count < 3; i++) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                t.setFeatureValue(chunkTags[i]+"@+"+count, 1.0);
            }
        }        
    }
    private void addSentenceFeature(Token t, int pos, Sentence s) {
        //elements VP, WP, WP$, PUNC

        int leftCount[] = new int[elements.length];
        int rightCount[] = new int[elements.length];

        String[] posTags = s.getTags("POS");

        int size = posTags.length;        

        for (int i = 0; i < size; i++) {
            if (i < pos) {
                for (int j = 0; j < elements.length; j++) {
                    if (patterns[j].matcher(posTags[i]).matches()) leftCount[j]++;
                }
            } else if (i > pos) {
                for (int j = 0; j < elements.length; j++) {
                    if (patterns[j].matcher(posTags[i]).matches()) rightCount[j]++;
                }
            }
        }
        for (int i = 0; i < elements.length; i++) {
            if (leftCount[i] > 5) leftCount[i] = 5;
            if (rightCount[i] > 5) rightCount[i] = 5;

            t.setFeatureValue(elements[i]+"--"+leftCount[i], 1.0);
            t.setFeatureValue(elements[i]+"++"+rightCount[i], 1.0);
        }        
    }
    private void addBoundaryFeature(Token t, int pos, Sentence s) {        
        int leftCount = 0;
        int rightCount = 0;        
        String[] tags = s.getTags("CLS_BN_S");

        for (int i = pos - 1; i >= 0; i--) {
            if (tags[i].equals("S")) {
                leftCount++;
            }
        }

        for (int i = pos + 1; i < tags.length; i++) {
            if (tags[i].equals("S")) rightCount++;
        }

        t.setFeatureValue("SBNDRY-"+leftCount, 1.0);
        t.setFeatureValue("SBNDRY+"+rightCount, 1.0);

        //E-boundary
        tags = s.getTags("CLS_BN_E");
        leftCount = 0;
        rightCount = 0;

        for (int i = pos - 1; i >= 0; i--) {
            if (tags[i].equals("E")) {
                leftCount++;
            }
        }

        for (int i = pos + 1; i < tags.length; i++) {
            if (tags[i].equals("E")) rightCount++;
        }

        t.setFeatureValue("EBNDRY-"+leftCount, 1.0);
        t.setFeatureValue("EBNDRY+"+rightCount, 1.0);
        
    }
    private void addSentencePatterns(Token t, int pos, Sentence s) {

        String[] posTags = s.getTags("POS");
        String[] chunkTags = s.getTags("CHUNK");
        //String[] clauseETags = s.getTags("CLS_BN_E");

        ArrayList<String> words = s.getWords();
        
        t.setFeatureValue("^^" + getPattern(0, pos, words, posTags, chunkTags), 1.0);
        t.setFeatureValue("$$" + getPattern(pos, s.size() - 1, words, posTags, chunkTags), 1.0);

        /*for (int i = pos, count = 0; i < clauseETags.length; i++) {
            if (clauseETags[i].equals("E")) {
                t.setFeatureValue("E"+ count + "-" + getPattern(pos, i, words, posTags, chunkTags), 1.0);
                count++;
            }
        }*/
    }
    private String getPattern(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags) {
        String pattern = "";
        for (int i = start; i <= end; i++) {
            if (posTags[i].matches("CC")) {
                pattern += "::" + posTags[i];
            } else if (words.get(i).matches("(T|t)hat")) {
                pattern += "::that-" + posTags[i];
            } else if (posTags[i].matches("W.*")) {
                pattern += "::" + "W";
            } else if (posTags[i].matches("[,.;:?!-+()]")) {
                pattern += "::" + "PUNC";
            } else if (chunkTags[i].matches("B-VP")) {
               pattern += "::VP";
            }
        }
        return pattern;
    }

    private void addSpecialFeatures(Token t, int pos, Sentence s) {
        String[] sTags = s.getTags("CLS_BN_S");
        String[] eTags = s.getTags("CLS_BN_E");
        
        for (int i = pos-1, count = 1; i >= 0 && count < 2; i--, count++) {
            if (sTags[i].equals("S")) t.setFeatureValue("^SS-"+count, 1.0);
            if (eTags[i].equals("E")) t.setFeatureValue("EE$-"+count, 1.0);
        }

        for (int i = pos+1, count = 1; i < sTags.length && count < 2; i++, count++) {
            if (sTags[i].equals("S")) t.setFeatureValue("^SS+"+count, 1.0);
            if (eTags[i].equals("E")) t.setFeatureValue("EE$+"+count, 1.0);
        }
    }
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
    }
}
