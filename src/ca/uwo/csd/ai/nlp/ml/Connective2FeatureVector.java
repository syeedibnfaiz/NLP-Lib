/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import edu.stanford.nlp.trees.Tree;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Connective2FeatureVector extends Pipe {

    private static Pattern pat = Pattern.compile("CC|TO|IN|PDT|[,.;:?!-+()]");

    public Connective2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
    }


    @Override
    public Instance pipe(Instance carrier) {
        ConnectiveInstance instance = (ConnectiveInstance) carrier;
        Sentence sentence = (Sentence) instance.getData();
        int start = instance.getS();
        int end = instance.getE();

        PropertyList pl = null;        
        pl = PropertyList.add(sentence.toString(start, end).toLowerCase(), 1.0, pl);
        /*pl = addPOSChunkFeatures(pl, sentence, start, end);
        pl = addBoundaryFeatures(pl, sentence, start, end);
        pl = addPositionFeature(pl, sentence, start, end);
        pl = addSentencePatterns(pl, sentence, start, end);*/
        pl = addSyntaxTreeFeatures(pl, sentence, start, end);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
        //boolean connective = instance.isConnective();
        //carrier.setTarget(ldict.lookupLabel(String.valueOf(connective)));
        carrier.setTarget(ldict.lookupLabel(instance.getLabel().toString()));
        //System.out.println(connective+" : " + sentence.toString(start, end));
        carrier.setData(fv);
        return carrier;
    }

    private PropertyList addSyntaxTreeFeatures(PropertyList pl, Sentence sentence, int start, int end) {
        Tree root = sentence.getParseTree();
        if (root == null) return pl;
        List<Tree> leaves = root.getLeaves();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        
        Tree lca = analyzer.getLCA(root, leaves.subList(start, end+1));
        if (lca != null) {
            pl = PropertyList.add("SELF="+lca.value(), 1.0, pl);
        }

        Tree parent = lca.parent(root);
        if (parent != null) {
            pl = PropertyList.add("PARENT="+parent.value(), 1.0, pl);
            pl = PropertyList.add("SELF="+lca.value()+"&"+"PARENT="+parent.value(), 1.0, pl);
        }

        Tree left = analyzer.getSibling(root, lca, -1);
        if (left != null) {
            pl = PropertyList.add("LEFT@-1-"+left.value(), 1.0, pl);            
        } else {
            pl = PropertyList.add("LEFT@-1-<NONE>", 1.0, pl);
        }

        left = analyzer.getSibling(root, lca, -2);
        if (left != null) {
            pl = PropertyList.add("LEFT@-2-"+left.value(), 1.0, pl);
        } else {
            pl = PropertyList.add("LEFT@-2-<NONE>", 1.0, pl);
        }

        Tree right = analyzer.getSibling(root, lca, 1);
        if (right != null) {
            pl = PropertyList.add("RIGHT@+1-"+right.value(), 1.0, pl);
        } else {
            pl = PropertyList.add("RIGHT@+1-<NONE>", 1.0, pl);
        }

        right = analyzer.getSibling(root, lca, 2);
        if (right != null) {
            pl = PropertyList.add("RIGHT@+2-"+right.value(), 1.0, pl);
        } else {
            pl = PropertyList.add("RIGHT@+2-<NONE>", 1.0, pl);
        }
        
        return pl;
    }
    private PropertyList addPOSChunkFeatures(PropertyList pl, Sentence sentence, int start, int end) {
        String connective = sentence.toString(start, end);
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        ArrayList<String> words = sentence.getWords();
        
        pl = PropertyList.add(chunkTags[start], 1.0, pl);
        pl = PropertyList.add(posTags[start]+":"+chunkTags[start], 1.0, pl);
        //pl = PropertyList.add(sentence.toString(start, end).toLowerCase()+":"+chunkTags[start], 1.0, pl);
        /*if (start > 0 && pat.matcher(posTags[start-1]).matches()) {
            pl = PropertyList.add(posTags[start-1]+"-"+sentence.toString(start, end).toLowerCase()+":"+chunkTags[start], 1.0, pl);
        }*/
        //pos seq
        String chunk = "";
        for (int i = start; i <= end; i++) {            
            chunk += chunkTags[i];
        }        
        pl = PropertyList.add(sentence.toString(start, end).toLowerCase()+":"+chunk, 1.0, pl);

        //left chunk window
        String prevChunk = "";
        for (int i = start-1, count = 1; i >=0 && count <= 2; i--) {
            if (chunkTags[i].startsWith("B")) {
                pl = PropertyList.add(chunkTags[i]+"@-"+count, 1.0, pl);
                count++;
                if (prevChunk.equals("")) prevChunk = chunkTags[i];
            }
        }
        //right chunk window
        String nextChunk = "";
        for (int i = end+1, count = 1; i < chunkTags.length && count <= 2; i++) {
            if (chunkTags[i].startsWith("B")) {
                pl = PropertyList.add(chunkTags[i]+"@+"+count, 1.0, pl);
                count++;
                if (nextChunk.equals("")) nextChunk = chunkTags[i];
            }
        }
        if (!prevChunk.equals("") && !nextChunk.equals("")) {
            pl = PropertyList.add(prevChunk+"&&"+nextChunk, 1.0, pl);
            pl = PropertyList.add(prevChunk+"&"+sentence.toString(start, end).toLowerCase()+"&"+nextChunk, 1.0, pl);
        }
        //left pos window
        for (int i = start-1, count = 1; i >=0 && count <= 2; i--, count++) {            
            pl = PropertyList.add(posTags[start]+":"+posTags[i] + "@-" + count, 1.0, pl);
        }
        //right pos window
        for (int i = end+1, count = 1; i < posTags.length && count <= 2; i++) {
            pl = PropertyList.add(posTags[end]+":"+posTags[i] + "@+" + count, 1.0, pl);
        }
        return pl;
    }

    private PropertyList addBoundaryFeatures(PropertyList pl, Sentence sentence, int start, int end) {
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        String[] sTags = sentence.getTags("CLS_BN_S");
        String[] eTags = sentence.getTags("CLS_BN_E");
        String word = sentence.toString(start, end);
        
        for (int i = start - 1, count = 1; i >= 0 && count <= 2; i--, count++) {            
            if (eTags[i].equals("E")) {
                pl = PropertyList.add(eTags[i] + "@-" + count, 1.0, pl);
                pl = PropertyList.add(word+":"+eTags[i] + "@-" + count, 1.0, pl);
            }
        }
        for (int i = end+1, count = 1; i < sTags.length && count <= 2; i++, count++) {
            if (sTags[i].equals("S")) {
                pl = PropertyList.add(sTags[i] + "@+" + count, 1.0, pl);
                pl = PropertyList.add(word+":"+sTags[i] + "@+" + count, 1.0, pl);
            }            
        }
        if (sTags[start].equals("S")) {
            pl = PropertyList.add("^^^"+sentence.get(start).word().toLowerCase()+"-"+sTags[start], 1.0, pl);
            pl = PropertyList.add("^^^"+sTags[start], 1.0, pl);
        }

        boolean vpLeft = false;
        for (int i = start-1; i >= 0; i--) {
            if (sTags[i].equals("S")) break;
            if (chunkTags[i].equals("B-VP")) {
                vpLeft = true;
                break;
            }            
        }
        boolean vpRight = false;
        for (int i = end+1; i < chunkTags.length; i++) {
            if (eTags[i].equals("E")) break;
            if (chunkTags[i].equals("B-VP")) {
                vpRight = true;
                break;
            }            
        }
        pl = PropertyList.add(word+"-VP-"+vpLeft+","+vpRight, 1.0, pl);
        return pl;
    }
    private PropertyList addPositionFeature(PropertyList pl, Sentence sentence, int start, int end) {
        if (start == 0) {
            pl = PropertyList.add("First", 1.0, pl);
        }        
        return pl;
    }
    private PropertyList addSentencePatterns(PropertyList pl, Sentence sentence, int start, int end) {
        String[] posTags = sentence.getTags("POS");
        String[] chunkTags = sentence.getTags("CHUNK");
        String[] clauseSTags = sentence.getTags("CLS_BN_S");
        String[] clauseETags = sentence.getTags("CLS_BN_E");
        ArrayList<String> words = sentence.getWords();

        String leftPat = getPattern(0, start-1, words, posTags, chunkTags, clauseETags, clauseSTags);
        String rightPat = getPattern(end+1, sentence.size()-1, words, posTags, chunkTags, clauseETags, clauseSTags);
        pl = PropertyList.add(leftPat+"---"+rightPat, 1.0, pl);
        
        return pl;
    }
    private String getPattern(int start, int end, ArrayList<String> words, String[] posTags, String[] chunkTags, String[] clauseETags, String[] clauseSTags) {
        String pattern = "";
        String prev = "";
        for (int i = start; i <= end; i++) {
            if (clauseSTags[i].equals("S")&& !prev.equals("::S")) {
                pattern += "::S";
                prev = "::S";
            }

            /*if (posTags[i].matches("CC") && chunkTags[i].equals("O") && !prev.equals("::CC")) {
                pattern += "::CC";
                prev = "::CC";
            } else*/ if (chunkTags[i].matches("B-VP") && !prev.equals("::VP")) {
               pattern += "::VP";
               prev = "::VP";
            } /*else if (chunkTags[i].matches("B-SBAR") && !prev.equals("::SBAR")) {
               pattern += "::SBAR";
               prev = "::SBAR";
            }*/

            if (clauseETags[i].equals("E")&& !prev.equals("::E")) {
                pattern += "::E";
                prev = "::E";
            }
        }
        return pattern;
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
