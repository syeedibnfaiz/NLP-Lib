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
public class Arg2ConnInstance extends Instance{
    int connStart;
    int connEnd;
    int headPos;
    Object label;
    String conn;
    SimpleDepGraph depGraph;
    String tree;
    PDTBRelation reln;
    
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    
    public Arg2ConnInstance(String tree, SimpleDepGraph depGraph, int connStart, int connEnd, int headPos, Object target, PDTBRelation reln) {
        super(tree, null, null, null);
        this.depGraph = depGraph;
        this.connStart = connStart;
        this.connEnd = connEnd;
        this.headPos = headPos;
        this.label = target;
        this.tree = tree;
        this.reln = reln;
        this.setSource(this);
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
            Tree head = leaves.get(headPos);
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

    public int getHeadPos() {
        return headPos;
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
    
    @Override
    public String toString() {
        return conn;
    }
}
