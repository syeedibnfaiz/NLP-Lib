/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.BLLIPClient;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class PTBGS2PTB2 {
        
    String ptbGSRoot;
    String ptb2Root;

    public PTBGS2PTB2() {
        this("./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/psptb");
    }
    
    public PTBGS2PTB2(String ptbGSRoot, String ptb2Root) {
        this.ptbGSRoot = ptbGSRoot;
        this.ptb2Root = ptb2Root;
    }
    
    public void convert() throws IOException {
        File gsRootDir = new File(ptbGSRoot);
        File psRootDir = new File(ptb2Root);
        File[] gsSections = gsRootDir.listFiles(new DirFilter());
        PTBFileReader reader = new PTBFileReader();
        BLLIPClient bLLIPClient = new BLLIPClient();
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        TreeFactory tf = new LabeledScoredTreeFactory();
        
        for (File gsSection : gsSections) {
            File psSection = new File(psRootDir, gsSection.getName());
            File[] gsFiles = gsSection.listFiles(new MRGFileNameFilter());
            
            for (File gsFile : gsFiles) {
                File psFile = new File(psSection, gsFile.getName());
                BufferedWriter writer = new BufferedWriter(new FileWriter(psFile));
                //write ps file
                List<Tree> trees = reader.read(gsFile);
                writer.write("\n");
                for (Tree t : trees) {
                    String sentence = getSentence(t);
                    String parse = bLLIPClient.parse(sentence);
                    TreeReader tr = new PennTreeReader(new StringReader(parse));
                    Tree newTree = tr.readTree();
                    newTree.setValue("");
                    writer.write(treeAnalyzer.getPennOutput(t) + "\n");
                }
                writer.close();
            }
        }
    }
    String getSentence(Tree root) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        for (Tree leaf : leaves) {
            Tree parent = leaf.parent(root);
            if (!parent.value().equals("-NONE-")) {
                sb.append(leaf.value()).append(" ");
            }
        }
        return sb.toString();
    }
    
    public static void main(String args[]) throws IOException {
        PTBGS2PTB2 converter = new PTBGS2PTB2();
        
    }
    
    private static class DirFilter implements FileFilter {        
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }
    
    private static class MRGFileNameFilter implements FilenameFilter{

        public MRGFileNameFilter() {
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".mrg");
        }
    }
}
