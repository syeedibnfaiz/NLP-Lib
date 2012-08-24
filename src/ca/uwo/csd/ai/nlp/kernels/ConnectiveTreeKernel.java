
package ca.uwo.csd.ai.nlp.kernels;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import kernel.ds.SparseVector;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ConnectiveTreeKernel {
    HashSet<String> connSet;
    GenericTextReader textReader;
    final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    final SnowballStemmer stemmer = new englishStemmer();
    
    HashMap<String, Integer> featureMap;
    int wordCount;
    
    public ConnectiveTreeKernel() {
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});        
        connSet = new HashSet<String>();
        fillConnSet();
        //connSet.add("and");
        featureMap = new HashMap<String, Integer>();
        wordCount = 1;
    }
    
    private void fillConnSet() {
        try {
            ////BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/all_conn_lexicon.txt"));
            
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/explicit_conn_types"));
            //changed for experimenting on BioDRB
            //BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_explicit_conn_types.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                connSet.add(line);
                System.out.println(line);
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectiveTreeKernel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void addInstances(Sentence s, SimpleDepGraph depGraph, BufferedWriter writer) throws IOException {        
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (!s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                    if (connSet.contains(s.toString(i, i + k).toLowerCase())) {
                        j = i + k;
                    }
                }
            } else {
                j = i;
                for (int k = i + 1; k < words.size(); k++) {
                    if (s.get(k).getTag("CONN").startsWith("I")) {
                        j = k;
                    } else {
                        break;
                    }
                }
                
            }
            if (j != -1) {
                if (s.get(i).getTag("CONN").startsWith("B")) {
                    write(s, i, j, true, writer);
                    //instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, true, depGraph));
                } else if (s.get(i).getTag("CONN").startsWith("DB")) {                 //if..then, either..or
                    write(s, i, j, true, writer);
                    //instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, true, depGraph));
                } else if (!s.get(i).getTag("CONN").startsWith("DI")) {                 //don't take then of if..then                           
                    write(s, i, j, false, writer);
                    //instanceList.addThruPipe(new PDTBConnectiveInstance(s, i, j, false, depGraph));                    
                }
                i = j;
            }
        }
    }
    
    private void write(Sentence s, int connStart, int connEnd, boolean b, BufferedWriter writer) throws IOException {
        Tree root = treeAnalyzer.getPennTree(s.getProperty("parse_tree"));
        treeAnalyzer.assignPOS(s, root);
        List<Tree> leaves = root.getLeaves();
        for (Tree leaf : leaves) {
            leaf.setValue(leaf.value().toLowerCase());
        }
        Tree lca = treeAnalyzer.getLCA(root, connStart, connEnd);
        Tree subTree = null;
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }
            subTree = createSubTree(root, parent, lca);            
        }
        
        SparseVector featureVector = getFeatureVector(s, root, connStart, connEnd);
        featureVector.sortByIndices();
        if (subTree != null) {
            removeFunctionTag(subTree);
            //String pennOutput = treeAnalyzer.getPennOutput(subTree);
            String mainTree = treeToString(subTree);
            Tree leftSibling = subTree.removeChild(0);
            String rightTree = treeToString(subTree);
            subTree.addChild(0, leftSibling);
            subTree.removeChild(2);
            String leftTree = treeToString(subTree);
            //pennOutput = pennOutput.replaceAll("\n", "");
            if (b) {
                writer.write("+1 \t");
            } else {
                writer.write("-1 \t");
            }
            writer.write("|BT| " + mainTree);
            writer.write(" |BT| " + rightTree);
            writer.write(" |BT| " + leftTree);
            writer.write(" |ET| "); //end of forest
            writer.write("1:1 2:0.7 3:0.6 |BV| 1:1 2:1 3:1 |BV| "); //tree parameters, weight, kernel (1 = SST, 0 = ST)
            writer.write("1:1 |BV| 1:0 |BV| "); //vector parameters, weight, kernel (0 = linear, 1 = polynomial)           
            writer.write(featureVector.toString());
            writer.write("|EV|\n");
            //writer.write("\t" + pennOutput2);
            //writer.write(" |ET|\n");
        } else {
            System.out.println("null");
        }
    }
    
    private SparseVector getFeatureVector(Sentence s, Tree root, int connStart, int connEnd) {
        SparseVector vector = new SparseVector();        
        int windowSize = 1;
        String conn = s.toString(connStart, connEnd);
        for (int i = 1; i <= windowSize && (connStart - i) >= 0; i++) {
            String pos = s.get(connStart - i).getTag("POS");
            pos = getGeneralPOS(pos);
            String feature = pos + "@-" + String.valueOf(i);
            vector.add(getIndex(feature), 1.0);
            feature = conn + pos + "@p-" + String.valueOf(i);
            vector.add(getIndex(feature), 1.0);
            String prevWord = s.get(connStart - i).word();
            prevWord = getStem(prevWord);
            feature = conn + prevWord + "@w-" + String.valueOf(i);
            //vector.add(getIndex(feature), 1.0);
        }
        if (connStart == 0) { //first token in sentence
            String feature = "NONE" + "@-" + String.valueOf(1);
            vector.add(getIndex(feature), 1.0);
            feature = conn + "NONE" + "@w-" + String.valueOf(1);
            vector.add(getIndex(feature), 1.0);
        }
        for (int i = 1; i <= windowSize && (connEnd + i) < s.size(); i++) {
            String pos = s.get(connEnd + i).getTag("POS");
            pos = getGeneralPOS(pos);
            String feature = pos + "@+" + String.valueOf(i);
            vector.add(getIndex(feature), 1.0);
            feature = conn + pos + "@p+" + String.valueOf(i);
            vector.add(getIndex(feature), 1.0);
            String nextWord = s.get(connEnd + i).word();
            nextWord = getStem(nextWord);
            feature = conn + nextWord + "@w+" + String.valueOf(i);
            //vector.add(getIndex(feature), 1.0);
        }
        //add connective as feature
        vector.add(getIndex(conn), 1.0);
        
        return vector;
    }
    private String getStem(String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
    }
    private String getGeneralPOS(String pos) {
        if (pos.length() < 2) return pos;
        return pos.substring(0, 2);
    }
    private int getIndex(String word) {
        Integer value = featureMap.get(word);
        if (value == null) {
            featureMap.put(word, wordCount++);
            if (featureMap.size() % 1000 == 0) {
                System.out.println("map_size: "+featureMap.size());
            }
            return wordCount - 1;
        }
        return value;
    }
    
    private SparseVector getVector(Sentence s, int start, int end) {
        SparseVector vector = new SparseVector();
        for (int i = start; i <= end; i++) {
            vector.add(getIndex(s.get(i).word()), 1.0);            
        }
        vector.sortByIndices();
        return vector;
    }
    private Tree createSubTree(Tree root, Tree parent, Tree self) {        
        if (parent == null) {
            return null;
        }
        /*List<Tree> leaves = root.getLeaves();
        for (Tree leaf : leaves) {
            leaf.setValue(leaf.value().toLowerCase());
        }*/
        Tree subTree = new LabeledScoredTreeNode(parent.label());
        Tree lefTree = treeAnalyzer.getSibling(parent, self, -1);
        if (lefTree != null) {
            lefTree = simplifyTree(lefTree, true);
            subTree.addChild(lefTree);            
        } else {
            Tree none = new LabeledScoredTreeNode(new LabeledWord("NONEL"));
            none.addChild(new LabeledScoredTreeNode(new LabeledWord("XL")));
            subTree.addChild(none);
        }
        subTree.addChild(self);
        Tree rightTree = treeAnalyzer.getSibling(parent, self, 1);
        if (rightTree != null) {
            rightTree = simplifyTree(rightTree, false);
            subTree.addChild(rightTree);            
        } else {
            Tree none = new LabeledScoredTreeNode(new LabeledWord("NONER"));
            none.addChild(new LabeledScoredTreeNode(new LabeledWord("XR")));
            subTree.addChild(none);
        }
        
        return subTree;
    }
    
    private Tree simplifyTree(Tree t, boolean left) {
        Tree newTree = new LabeledScoredTreeNode(t.label());
        List<Tree> children = t.getChildrenAsList();                
        /*int count = 0;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).isPhrasal()) {
                //t.setChild(pos++, new LabeledScoredTreeNode(children.get(i).label()));
                newTree.addChild(new LabeledScoredTreeNode(children.get(i).label()));
                count++;
            }
        }
        if (count == 0) {
            newTree.addChild(new LabeledScoredTreeNode(t.label()));
        }*/
        if (left) {
            newTree.addChild(new LabeledScoredTreeNode(children.get(children.size() - 1).label()));
        } else {
            boolean found = false;
            for (Tree child : children) {
                if (child.value().startsWith("V")) {
                    found = true;
                    newTree.addChild(new LabeledScoredTreeNode(child.label()));
                }
            }
            if (!found) {
                newTree.addChild(new LabeledScoredTreeNode(children.get(0).label()));
            }
        }
        return newTree;
    }
    public void generate(String trainingFile, String parsedTrainingFile, String trainingDepFile, String outputFile) throws IOException {
        Text trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);
                
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        String line;        
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        //SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        //List<SimpleDepGraph> depGraphs = depFileReader.read(new File(trainingDepFile));
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence sentence = trainingText.get(i);
            //SimpleDepGraph depGraph = depGraphs.get(i);
            SimpleDepGraph depGraph = null;
            //sentence = annotator.annotate(sentence);            
            line = reader.readLine();            
            if (line.equals("")) continue;
            //to save space
            sentence.setProperty("parse_tree", line);
            addInstances(sentence, depGraph, writer);
            
        }
        writer.close();
     }
    
    private static void create10FoldData(String fileName, String outputDir) throws IOException {
        List<String> instances = Util.readLines(fileName);
        Collections.shuffle(instances);
        Collections.shuffle(instances);
        Collections.shuffle(instances);
        int size = instances.size();
        int chunkSize = size / 10;
        int begin = 0;
        int end = chunkSize - 1;
        new File(outputDir).mkdir();
        
        for (int i = 0; i < 10; i++) {
            File subdir = new File(outputDir, String.valueOf(i));
            subdir.mkdir();
            File trainingFile = new File(subdir, i + ".train");
            File testingFile = new File(subdir, i + ".test");
            BufferedWriter trainWriter = new BufferedWriter(new FileWriter(trainingFile));
            BufferedWriter testWriter = new BufferedWriter(new FileWriter(testingFile));
            
            for (int j = 0; j < size; j++) {
                if (j >= begin && j <= end) {
                    testWriter.write(instances.get(j) + "\n");
                } else {
                    trainWriter.write(instances.get(j) + "\n");
                }
            }
            trainWriter.close();
            testWriter.close();
            begin = end + 1;
            end = begin + chunkSize - 1;
            if (end >= size) {
                end = size - 1;
            }
        }
    }
    private static String treeToString(Tree t) {
        StringBuilder sb = new StringBuilder();
        traverse(t, sb, 1);
        return sb.toString();
    }
    private static void traverse(Tree t, StringBuilder sb, int level) {        
        List<Tree> children = t.getChildrenAsList();
        if (!children.isEmpty()) {
            sb.append("(");
        }
        sb.append(t.value());
        
        for (Tree child : children) {
            sb.append(" ");
            traverse(child, sb, level + 1);
        }
        if (!children.isEmpty()) {
            sb.append(")");
        }
    }
    
    private void removeFunctionTag(Tree t) {        
        String value = t.value();
        if (value.contains("-")) {
            t.setValue(value.substring(0, value.indexOf('-')));
        }
        for (Tree child : t.getChildrenAsList()) {
            removeFunctionTag(child);
        }
    }
    
    public static void main(String[] args) throws IOException {
        ConnectiveTreeKernel treeKernel = new ConnectiveTreeKernel();
        //treeKernel.generate("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_dep_2_22", "connTreeKernel.train");
        treeKernel.generate("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22", "connTreeKernel.train");
        create10FoldData("connTreeKernel.train", "G:/UWO/thesis/Software/svm-light-TK-1.2/connTreeKernel10Fold");        
    }
}
