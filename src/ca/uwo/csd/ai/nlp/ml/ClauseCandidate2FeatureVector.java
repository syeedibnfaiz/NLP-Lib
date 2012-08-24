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
 */
public class ClauseCandidate2FeatureVector extends Pipe implements Serializable {
    private String elements[];          //sentence feature elements
    private Pattern patterns[];

    public ClauseCandidate2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
        elements  = new String[]{"VP", "W*", "PUNC"};
        patterns  = new Pattern[4];
        patterns[0] = Pattern.compile("VB.*");
        patterns[1] = Pattern.compile("W.*");
        patterns[2] = Pattern.compile("[,.;:?!-+`]");
    }

    @Override
    public Instance pipe(Instance carrier) {
        ClauseCandidateInstance instance = (ClauseCandidateInstance) carrier;
        Sentence sentence = (Sentence) instance.getData();
        int start = instance.getS();
        int end = instance.getE();        

        PropertyList pl = null;
        pl = addPOSWindowFeature(pl, sentence, start, end);
        pl = addChunkFeature(pl, sentence, start, end);
        pl = addPositionFeature(pl, sentence, start, end);
        pl = addSentenceFeature(pl, sentence, start, end);
        pl = addSentencePatterns(pl, sentence, start, end);
        pl = addBoundaryFeature(pl, sentence, start, end);

        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
        boolean clause = instance.isClause();
        carrier.setTarget(ldict.lookupLabel(String.valueOf(clause)));

        carrier.setData(fv);
        return carrier;
    }

    private PropertyList addPOSWindowFeature(PropertyList pl, Sentence sentence, int start, int end) {
        String[] posTags = sentence.getTags("POS");
        //Pattern pat = Pattern.compile("CC|DT|EX|TO|IN|MD|PDT|V.*|W.*|[,.;:?!-+()]");
        Pattern pat = Pattern.compile("CC|DT|EX|TO|IN|MD|PDT|W.*|[,.;:?!-+()]");

        ArrayList<String> words = new ArrayList<String>();
        for (int i = start; i <= end; i++) {            
            if (pat.matcher(posTags[i]).matches()) {
                words.add(sentence.get(i).word());
            }
        }
        for (int i = 1; i < words.size(); i++) {
            pl = PropertyList.add(words.get(i-1)+"&p&"+words.get(i), 1.0, pl);
        }

        return pl;
    }

    private PropertyList addChunkFeature(PropertyList pl, Sentence sentence, int start, int end) {
        String[] chunkTags = sentence.getTags("CHUNK");
        int count = 0;
        for (int i = start; i <= end; i++) {
            if (chunkTags[i].startsWith("B")) {
                count++;
                pl = PropertyList.add(chunkTags[i]+"@+"+count, 1.0, pl);
            }
        }
        pl = PropertyList.add("#chunk="+(2*(count/2)), 1.0, pl);
        pl = PropertyList.add(chunkTags[start]+"&c&"+chunkTags[end], 1.0, pl);
        return pl;
    }

    private PropertyList addPositionFeature(PropertyList pl, Sentence sentence, int start, int end) {

        if (start == 0) {
            pl = PropertyList.add("First", 1.0, pl);
            if (end == (sentence.size() - 1)) {
                pl = PropertyList.add("First&Last", 1.0, pl);
            }

        }
        if (end == (sentence.size() - 1)) {
            pl = PropertyList.add("Last", 1.0, pl);
        }
        return pl;
    }

    private PropertyList addSentenceFeature(PropertyList pl, Sentence sentence, int start, int end) {
        //elements VP, WP, WP$, PUNC

        int count[] = new int[elements.length];        
        String[] posTags = sentence.getTags("POS");

        int size = posTags.length;        

        for (int i = start; i <= end; i++) {
            for (int j = 0; j < elements.length; j++) {
                if (patterns[j].matcher(posTags[i]).matches()) {
                    count[j]++;
                }
            }
        }
        for (int i = 0; i < elements.length; i++) {
            if (count[i] > 5) count[i] = 5;            
            pl = PropertyList.add(elements[i]+"--"+count[i], 1.0, pl);
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
    private PropertyList addBoundaryFeature(PropertyList pl, Sentence sentence, int start, int end) {
        int sCount = 0;
        int eCount = 0;
        String[] sTags = sentence.getTags("CLS_BN_S");
        String[] eTags = sentence.getTags("CLS_BN_E");

        for (int i = start; i <= end; i++) {
            if (sTags[i].equals("S")) {
                sCount++;
            }
            if (eTags[i].equals("E")) {
                eCount++;
            }
        }        

        if (sCount > 5) sCount = 5;
        if (eCount > 5) eCount = 5;

        pl = PropertyList.add("SBNDRY<->"+sCount, 1.0, pl);
        pl = PropertyList.add("EBNDRY<->"+eCount, 1.0, pl);
        pl = PropertyList.add("SBNDRY<->"+sCount+"&"+"EBNDRY<->"+eCount, 1.0, pl);

        sCount = 0;
        for (int i = start - 1; i >= 0; i--) {
            if (sTags[i].equals("S")) {
                sCount++;
            }
        }
        if (sCount > 5) sCount = 5;
        pl = PropertyList.add("SBNDRY<-"+sCount, 1.0, pl);
        
        eCount = 0;
        for (int i = end+1; i < sentence.size(); i++) {
            if (eTags[i].equals("E")) {
                eCount++;
            }
        }
        if (eCount > 5) eCount = 5;
        pl = PropertyList.add("EBNDRY->"+eCount, 1.0, pl);

        pl = PropertyList.add("SBNDRY<-"+sCount+"&"+"EBNDRY->"+eCount, 1.0, pl);

        return pl;
    }

    private PropertyList addSentencePatterns(PropertyList pl, Sentence sentence, int start, int end) {
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        String[] clauseSTags = sentence.getTags("CLS_BN_S");
        String[] clauseETags = sentence.getTags("CLS_BN_E");
        ArrayList<String> words = sentence.getWords();

        pl = PropertyList.add(getPattern(start, end, words, posTags, chunkTags, clauseETags, clauseSTags), 1.0, pl);
        return pl;
    }
    private String getPattern(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags, String[] clauseETags, String[] clauseSTags) {
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
