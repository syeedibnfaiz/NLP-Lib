/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.types.Instance;
import edu.stanford.nlp.trees.Tree;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Arg1ConnInstance extends Instance{
    int connStart;
    int connEnd;
    int arg1HeadPos;
    int arg2HeadPos;
    Object label;
    String conn;
    SimpleDepGraph depGraph;
    String tree;
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    PDTBRelation reln;
    
    public Arg1ConnInstance(String tree, SimpleDepGraph depGraph, int connStart, int connEnd, int arg1HeadPos, int arg2HeadPos, Object target, PDTBRelation reln) {
        super(tree, null, null, null);
        this.depGraph = depGraph;
        this.connStart = connStart;
        this.connEnd = connEnd;
        this.arg1HeadPos = arg1HeadPos;
        this.arg2HeadPos = arg2HeadPos;
        this.label = target;
        this.tree = tree;
        this.setSource(this);
        this.reln = reln;
        try {
            Tree root = treeAnalyzer.getPennTree(tree);
            List<Tree> leaves = root.getLeaves();
            StringBuilder sb = new StringBuilder();
            for (int i = connStart; i <= connEnd; i++) {
                Tree leaf = leaves.get(i);
                if (sb.length() != 0) {
                    sb.append(" ");
                }
                sb.append(leaf.value());
            }
            Tree head = leaves.get(arg1HeadPos);
            Tree headParent = head.parent(root);
            conn = sb.toString() + "-" + headParent.value();
        } catch (IOException ex) {
            Logger.getLogger(Arg2ConnInstance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getConnEnd() {
        return connEnd;
    }

    public int getConnStart() {
        return connStart;
    }    

    public Object getLabel() {
        return label;
    }

    public SimpleDepGraph getDepGraph() {
        return depGraph;
    }

    public String getTree() {
        return tree;
    }

    public int getArg1HeadPos() {
        return arg1HeadPos;
    }

    public int getArg2HeadPos() {
        return arg2HeadPos;
    }
    
    @Override
    public String toString() {
        return conn;
    }
}
