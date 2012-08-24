/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * A <code>Chunk2FeatureVector</code> object is a pipe that converts a
 * <code>ChunkInstance</code> to a <code>Featurevector</code>. This pipe
 * is used for open brackets counting/classifying task.
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class ChunkClauseS2FeatureVector extends Pipe implements Serializable {    
    private String elements[];          //sentence feature elements
    private Pattern patterns[];    

    public ChunkClauseS2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
        elements  = new String[]{"VP", "W*", "PUNC"};
        patterns  = new Pattern[4];
        patterns[0] = Pattern.compile("VB.*");
        patterns[1] = Pattern.compile("W.*");
        patterns[2] = Pattern.compile("[,.;:?!-+`]");
    }

    @Override
    public Instance pipe(Instance carrier) {
        Chunk chunk = (Chunk) carrier.getData();
        Sentence sentence = chunk.getSentence();

        int pos;    //position of candidate word
        pos = chunk.getStart();        

        TokWord tokWord = sentence.get(pos);

        String word = tokWord.word();
        String posTag = tokWord.getTag("POS");
        String chunkTag = tokWord.getTag("CHUNK");
        //String discTag = tokWord.getTag("DIS_CON");

        PropertyList pl = null;
        //pl = PropertyList.add(word.toLowerCase(), 1.0, pl);
        pl = PropertyList.add(posTag, 1.0, pl);
        pl = PropertyList.add(chunkTag, 1.0, pl);

        //removing this feature improved performance, yet to decide about it
        //if (discTag.startsWith("B")) pl = PropertyList.add("DIS_CON=Y", 1.0, pl);
        //else pl = PropertyList.add("DIS_CON=N", 1.0, pl);

        pl = addPOSWindowFeature(pl, sentence, chunk);
        pl = addChunkWindowFeature(pl, sentence, chunk);
        pl = addPositionFeature(pl, sentence, chunk);
        pl = addSentenceFeature(pl, sentence, chunk);
        pl = addBoundaryFeature(pl, sentence, chunk);
        pl = addSentencePatterns(pl, sentence, chunk);
        pl = addSpecialFeatures(pl, sentence, chunk);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
        if (tokWord.getTag("CLS_ANN") != null) {
            String tag = tokWord.getTag("CLS_ANN");
            int openBracketCount = 0;
            for (int i = 0; i < tag.length(); i++) {
                if (tag.charAt(i) == '(') openBracketCount++;                
            }
            if (openBracketCount > 2) openBracketCount = 2;
            carrier.setTarget(ldict.lookupLabel(String.valueOf(openBracketCount)));
        }
        else carrier.setTarget(ldict.lookupLabel("1"));

        carrier.setData(fv);
        return carrier;
    }

    private PropertyList addPOSWindowFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        String[] posTags = sentence.getTags("POS");
        //Pattern pat = Pattern.compile("CC|DT|EX|TO|IN|MD|PDT|V.*|W.*|[,.;:?!-+()]");
        Pattern pat = Pattern.compile("PRP|CC|DT|EX|TO|IN|MD|PDT|W.*|[,.;:?!-+()]");
        //pos left window
        for (int i = chunk.getStart() - 1, count = 1; i >= 0 && count <= 3; i--, count++) {
            pl = PropertyList.add(posTags[i]+"@-"+count, 1.0, pl);
            if (pat.matcher(posTags[i]).matches()) {
                pl = PropertyList.add(sentence.get(i).word()+"@-"+count, 1.0, pl);
            }
        }

        //pos right window
        for (int i = chunk.getStart() + 1, count = 1; i < sentence.size() && count <= 3; i++, count++) {
            pl = PropertyList.add(posTags[i]+"@+"+count, 1.0, pl);
            if (pat.matcher(posTags[i]).matches()) {
                pl = PropertyList.add(sentence.get(i).word()+"@+"+count, 1.0, pl);
            }
        }

        return pl;
    }

    private PropertyList addChunkWindowFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        String[] chunkTags = sentence.getTags("CHUNK");
        //chunk left window feature
        for (int i = chunk.getStart() - 1, count = 0; i >= 0 && count < 3; i--) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                pl = PropertyList.add(chunkTags[i]+"@-"+count, 1.0, pl);
            }
        }

        //chunk right window feature
        for (int i = chunk.getEnd() + 1, count = 0; i < sentence.size() && count < 3; i++) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                pl = PropertyList.add(chunkTags[i]+"@+"+count, 1.0, pl);
            }
        }
        return pl;
    }

    private PropertyList addPositionFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos;
        pos = chunk.getStart();        

        if (pos == 0) {
            pl = PropertyList.add("First", 1.0, pl);
        } else if (pos == (sentence.size() - 1)) {
            pl = PropertyList.add("Last", 1.0, pl);
        }
        return pl;
    }

    private PropertyList addSentenceFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        //elements VP, WP, WP$, PUNC

        int leftCount[] = new int[elements.length];
        int rightCount[] = new int[elements.length];

        String[] posTags = sentence.getTags("POS");

        int size = posTags.length;
        int pos;
        pos = chunk.getStart();        


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

            pl = PropertyList.add(elements[i]+"--"+leftCount[i], 1.0, pl);
            pl = PropertyList.add(elements[i]+"++"+rightCount[i], 1.0, pl);
        }
        return pl;
    }

    /**
     * Add boundary features
     * @param pl
     * @param sentence
     * @param chunk
     * @return
     */
    private PropertyList addBoundaryFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos = chunk.getStart();
        int leftCount = 0;
        int rightCount = 0;
        String[] tags = sentence.getTags("CLS_BN_S");

        for (int i = pos - 1; i >= 0; i--) {
            if (tags[i].equals("S")) {
                leftCount++;
            }
        }

        for (int i = pos + 1; i < tags.length; i++) {
            if (tags[i].equals("S")) rightCount++;
        }

        if (leftCount > 5) leftCount = 5;
        if (rightCount > 5) rightCount = 5;

        pl = PropertyList.add("SBNDRY-"+leftCount, 1.0, pl);
        pl = PropertyList.add("SBNDRY+"+rightCount, 1.0, pl);

        //E-boundary
        tags = sentence.getTags("CLS_BN_E");
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

        if (leftCount > 5) leftCount = 5;
        if (rightCount > 5) rightCount = 5;
        
        pl = PropertyList.add("EBNDRY-"+leftCount, 1.0, pl);
        pl = PropertyList.add("EBNDRY+"+rightCount, 1.0, pl);

        return pl;
    }

    private PropertyList addSentencePatterns(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos = chunk.getStart();
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        String[] clauseETags = sentence.getTags("CLS_BN_E");
        
        ArrayList<String> words = sentence.getWords();        
        
        pl = PropertyList.add("^^" + getPattern(0, pos, words, posTags, chunkTags, clauseETags), 1.0, pl);
        pl = PropertyList.add("$$" + getPattern(pos, sentence.size()-1, words, posTags, chunkTags, clauseETags), 1.0, pl);


        for (int i = pos, count = 0; i < clauseETags.length; i++) {
            if (clauseETags[i].equals("E")) {
                pl = PropertyList.add("E"+ count + "-" + getPattern(pos, i, words, posTags, chunkTags, clauseETags), 1.0, pl);
                count++;
            }
        }
        return pl;
    }
    private String getPattern(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags, String[] clauseETags) {
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
            if (clauseETags[i].equals("E")) pattern += "::E";
        }
        return pattern;
    }
    private PropertyList addSpecialFeatures(PropertyList pl, Sentence sentence, Chunk chunk) {
        String[] sTags = sentence.getTags("CLS_BN_S");
        String[] eTags = sentence.getTags("CLS_BN_E");
        int pos = chunk.getStart();
        for (int i = pos-1, count = 1; i >= 0 && count < 2; i--, count++) {
            if (sTags[i].equals("S")) pl = PropertyList.add("^SS-"+count, 1.0, pl);
            if (eTags[i].equals("E")) pl = PropertyList.add("EE$-"+count, 1.0, pl);
        }

        for (int i = pos+1, count = 1; i < sTags.length && count < 2; i++, count++) {
            if (sTags[i].equals("S")) pl = PropertyList.add("^SS+"+count, 1.0, pl);
            if (eTags[i].equals("E")) pl = PropertyList.add("EE$+"+count, 1.0, pl);
        }

        int leftOpenCount = 0;
        int rightCloseCount = 0;
        for (int i = pos; i >= 0; i--) {
            if (sTags[i].equals("S")) leftOpenCount++;
        }
        for (int i = pos; i < eTags.length; i++) {
            if (eTags[i].equals("E")) rightCloseCount++;
        }
        if (leftOpenCount == rightCloseCount) pl = PropertyList.add("[=]", 1.0, pl);
        else if (leftOpenCount > rightCloseCount) pl = PropertyList.add("[>]", 1.0, pl);
        else pl = PropertyList.add("[<]", 1.0, pl);

        if (sentence.get(pos).word().matches("[A-Z]+")) pl = PropertyList.add("INITCAP", 1.0, pl);
        return pl;
    }
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);        
        out.writeObject(elements);
        out.writeObject(patterns);

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();        
        elements = (String[]) in.readObject();
        patterns = (Pattern[]) in.readObject();
    }
}
