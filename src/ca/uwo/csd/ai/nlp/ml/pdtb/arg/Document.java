/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>Document</code> represents a single PTB document.
 * @author Syeed Ibn Faiz
 */
public class Document {
    final static PTBFileReader PTB_READER = new PTBFileReader();
    final static SimpleDepFileReader DEP_FILE_READER = new SimpleDepFileReader();
    final static TreeFactory TREE_FACTORY = new LabeledScoredTreeFactory();
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    List<String> trees;
    List<SimpleDepGraph> depGraphs;
    String baseName;
    
    public Document(String ptbRoot, String depRoot, String section, String baseName) {
        File ptbFile = new File(new File(ptbRoot, section), baseName + ".mrg");
        File depFile = new File(new File(depRoot, section), baseName + ".dep");
        trees = PTB_READER.readStrings(ptbFile);
        depGraphs = DEP_FILE_READER.read(depFile);
        this.baseName = baseName;
    }
    public Document(File ptbFile, File depFile) {
        trees = PTB_READER.readStrings(ptbFile);
        depGraphs = DEP_FILE_READER.read(depFile);
        this.baseName = ptbFile.getName().replace(".mrg", "");
    }
    
    public Document(Tree root, SimpleDepGraph depGraph) {
        trees = new ArrayList<String>();
        depGraphs = new ArrayList<SimpleDepGraph>();
        trees.add(TREE_ANALYZER.getPennOutput(root));
        depGraphs.add(depGraph);
    }
    public Document(List<Tree> trees, List<SimpleDepGraph> depGraphs) {
        this.trees = new ArrayList<String>();
        for (Tree t : trees) {
            this.trees.add(TREE_ANALYZER.getPennOutput(t));
        }
        this.depGraphs = depGraphs;
    }
    
    public String getTreeString(int i) {
        return trees.get(i);
    }
    
    public Tree getTree(int i) {
        String treeStr = trees.get(i);
        TreeReader tr = new PennTreeReader(new StringReader(treeStr), TREE_FACTORY);
        try {
            return tr.readTree();
        } catch (IOException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    /**
     * Returns ith parsed <code>Sentence</code> in the document
     * @param i
     * @return 
     */
    public Sentence getSentence(int i) {
        return new Sentence(getTree(i));
    }
    public SimpleDepGraph getDepGraph(int i) {
        return depGraphs.get(i);
    }
    
    public String getFileName() {
        return baseName;
    }
    public void show() {
        for (int i = 0; i < trees.size(); i++) {
            System.out.println(i+": " + getTreeString(i));
            System.out.println(getDepGraph(i));
        }
    }
    
    public int size() {
        return trees.size();
    }
    
    public static void main(String args[]) {
        Document document = new Document("./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep", "00", "wsj_0009");
        document.show();
    }
}
