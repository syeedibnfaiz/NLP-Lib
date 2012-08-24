/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.cmd;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.utils.DependencyViewer;
import ca.uwo.csd.ai.nlp.utils.TreeViewDialog;
import edu.stanford.nlp.trees.Tree;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFrame;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Presentation{
    //final static CharniakParser PARSER_ANNOTATOR = new CharniakParser();//= new ParserAnnotator();
    final static ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator();
    final static HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    final static DependencyViewer DEPENDENCY_VIEWER = new DependencyViewer();
    
    public static Tree fixLeaves(Tree root) {
        List<Tree> leaves = root.getLeaves();
        int i = 1;
        for (Tree leaf : leaves) {
            leaf.setValue(leaf.value() + "-(" + i + ")");
            i++;
        }
        return root;
    }
    public static void attachHeads(Tree root) {
        if (root.isLeaf()) return;
        Tree syntacticHead = HEAD_ANALYZER.getSyntacticHead(root);
        if (syntacticHead != null) {
            String value = syntacticHead.value();
            value = value.substring(value.indexOf('('));
            root.setValue(root.value() + "-" + value);
        }
        List<Tree> children = root.getChildrenAsList();
        for (Tree child : children) {
            attachHeads(child);
        }
    }
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        SimpleSentReader sentReader = new SimpleSentReader();
        String line;
        JFrame frame = new JFrame("Empty Frame");
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            s = PARSER_ANNOTATOR.annotate(s);
            TreeViewDialog treeViewDialog = new TreeViewDialog(frame);
            treeViewDialog.setTree(s.getParseTree());
            treeViewDialog.setVisible(true);
            s.getParseTree().pennPrint();
            DEPENDENCY_VIEWER.showDependencyTree(s, "CCProcessed");
            /*Tree root = s.getParseTree();
            root = fixLeaves(root);
            attachHeads(root);
            treeViewDialog = new TreeViewDialog(frame);
            treeViewDialog.setTree(root);
            treeViewDialog.setVisible(true);*/
            
        }
    }
}
