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
 * is likely to be used for clause boundary learning tasks.
 * @author Syeed Ibn Faiz
 */
public class Chunk2FeatureVector extends Pipe implements Serializable {
    private boolean startingBoundary;   // indicates whether to learn start or end
    private String elements[];          //sentence feature elements
    private Pattern patterns[];

    /**
     * Create a <code>Chunk2FeatureVector</code> object that can be
     * used to learn clause START boundary.
     */
    public Chunk2FeatureVector() {
        this(true);
    }

    public Chunk2FeatureVector(boolean startingBoundary) {
        super(new Alphabet(), new LabelAlphabet());
        elements  = new String[]{"VP", "W*", "PUNC"};
        patterns  = new Pattern[4];
        patterns[0] = Pattern.compile("VB.*");
        patterns[1] = Pattern.compile("W.*");
        patterns[2] = Pattern.compile("[,.;:?!-+]");
        this.startingBoundary = startingBoundary;
    }

    @Override
    public Instance pipe(Instance carrier) {
        Chunk chunk = (Chunk) carrier.getData();
        Sentence sentence = chunk.getSentence();
        
        int pos;    //position of candidate word
        if (this.startingBoundary) pos = chunk.getStart();
        else pos = chunk.getEnd();       
        
        TokWord tokWord = sentence.get(pos);

        String word = tokWord.word();
        String posTag = tokWord.getTag("POS");
        String chunkTag = tokWord.getTag("CHUNK");
        String discTag = tokWord.getTag("DIS_CON");

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

        //when classifying E-boundaries, use S-boundaries as features
        if (!startingBoundary) {
            pl = addBoundaryFeature(pl, sentence, chunk);
            pl = addSentencePatterns4E(pl, sentence, chunk);
        } /*else {
            pl = addSentencePatterns4S(pl, sentence, chunk);
        }*/
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
        if (tokWord.getTag("CLS_BN") != null) carrier.setTarget(ldict.lookupLabel(tokWord.getTag("CLS_BN")));
        else carrier.setTarget(ldict.lookupLabel("X"));

        carrier.setData(fv);       
        return carrier;
    }

    private PropertyList addPOSWindowFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        String[] posTags = sentence.getTags("POS");        
        //Pattern pat = Pattern.compile("CC|DT|TO|EX|IN|MD|PDT|V.*|W.*|[,.;:?!-+]");
        Pattern pat = Pattern.compile("CC|DT|TO|EX|IN|MD|PDT|W.*|[,.;:?!-+]");
        //pos left window
        for (int i = chunk.getStart() - 1, count = 1; i >= 0 && count <= 3; i--, count++) {
            pl = PropertyList.add(posTags[i]+"@-"+count, 1.0, pl);
            if (pat.matcher(posTags[i]).matches()) {
                pl = PropertyList.add(sentence.get(i).word()+"@-"+count, 1.0, pl);
            }
        }

        //pos right window
        for (int i = chunk.getEnd() + 1, count = 1; i < sentence.size() && count <= 3; i++, count++) {
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
        for (int i = chunk.getStart() - 1, count = 0; i >= 0 && count < 2; i--) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                pl = PropertyList.add(chunkTags[i]+"@-"+count, 1.0, pl);
            }
        }

        //chunk right window feature
        for (int i = chunk.getEnd() + 1, count = 0; i < sentence.size() && count < 2; i++) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                pl = PropertyList.add(chunkTags[i]+"@+"+count, 1.0, pl);
            }
        }
        return pl;
    }

    private PropertyList addPositionFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos;
        if (this.startingBoundary) pos = chunk.getStart();
        else pos = chunk.getEnd();
        
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
        if (this.startingBoundary) pos = chunk.getStart();
        else pos = chunk.getEnd();
        
        
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
     * Add S-boundary features
     * @param pl
     * @param sentence
     * @param chunk
     * @return
     */
    private PropertyList addBoundaryFeature(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos = chunk.getEnd();
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

        //pl = PropertyList.add("SBNDRY-"+leftCount+"&"+"SBNDRY+"+rightCount, 1.0, pl);
        return pl;
    }
    private PropertyList addSentencePatterns4E(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos = chunk.getStart();
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        String[] clauseSTags = sentence.getTags("CLS_BN_S");        
        ArrayList<String> words = sentence.getWords();
        for (int i = 0; i < pos; i++) {
            if (clauseSTags[i].equals("S")) {
                pl = PropertyList.add(getPattern4E(i, pos, words, posTags, chunkTags, clauseSTags), 1.0, pl);
            }
        }
        return pl;
    }
    private String getPattern4E(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags, String[] clauseSTags) {
        String pattern = "";
        for (int i = start; i <= end; i++) {
            if (clauseSTags[i].equals("S")) pattern += "::S";

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
            } else if (chunkTags[i].matches("B-SBAR")) {
               pattern += "::SBAR";
            }

        }
        return pattern;
    }
    private PropertyList addSentencePatterns4S(PropertyList pl, Sentence sentence, Chunk chunk) {
        int pos = chunk.getStart();
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");

        ArrayList<String> words = sentence.getWords();

        pl = PropertyList.add("^^"+getPattern4S(0, pos, words, posTags, chunkTags), 1.0, pl);
        pl = PropertyList.add("$$"+getPattern4S(pos, sentence.size()-1, words, posTags, chunkTags), 1.0, pl);
        return pl;
    }
    private String getPattern4S(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags) {
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
            } else if (chunkTags[i].matches("B-SBAR")) {
               pattern += "::SBAR";
            }
        }
        return pattern;
    }
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeBoolean(startingBoundary);
        out.writeObject(elements);
        out.writeObject(patterns);

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        startingBoundary = in.readBoolean();
        elements = (String[]) in.readObject();
        patterns = (Pattern[]) in.readObject();
    }
}
