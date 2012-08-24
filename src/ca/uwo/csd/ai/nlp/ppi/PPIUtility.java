/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.utils.DependencyViewer;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class PPIUtility {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    static final private DependencyViewer DEP_VIEWER = new DependencyViewer();
    
    private String root;

    public PPIUtility(String root) {
        this.root = root;
    }

    public PPIUtility() {
        this("./resource/relation/PPI2");
    }
    
    public void showSentence(String corpusName, String fileName, int lineNum, boolean showTree, boolean showDependency) {
        File corpusRoot = new File(root, corpusName);
        File iobRoot = new File(corpusRoot, "iob");
        File iobFile = new File(iobRoot, fileName + ".txt");
        Text text = TEXT_READER.read(iobFile);
        Sentence s = text.get(lineNum);
        System.out.println(s);
        
        File treeRoot = new File(corpusRoot, "trees");
        File treeFile = new File(treeRoot, fileName + ".mrg");
        List<Tree> trees = TREE__READER.read(treeFile);
        Tree root = trees.get(lineNum);
        s.setParseTree(root);
        
        if (showDependency) {
            DEP_VIEWER.showDependencyTree(s, null);
        }
    }
    public static void main(String args[]) {
        PPIUtility utility = new PPIUtility();
        utility.showSentence("AIMed", "AIMed_d84", 4, true, true);
    }
}
