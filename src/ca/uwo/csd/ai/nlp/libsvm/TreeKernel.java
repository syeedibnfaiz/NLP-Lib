/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.libsvm;

import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class TreeKernel implements Kernel {
    private double lambda; //penalizing factor
    
    public TreeKernel() {
        this(0.5);
    }
    
    public TreeKernel(double lambda) {
        this.lambda = lambda;
    }
    
    @Override
    public double evaluate(Object k1, Object k2) {
        if (!(k1 instanceof Tree) || !(k2 instanceof Tree)) {
            throw new IllegalArgumentException("Arguments should be Trees");
        }
        Tree t1 = (Tree) k1;
        Tree t2 = (Tree) k2;
        
        List<Tree> nodes1 = getNodes(t1);
        List<Tree> nodes2 = getNodes(t2);
        
        int N1 = nodes1.size();
        int N2 = nodes2.size();
        
        double[][] mem = new double[N1][N2];
        initMem(mem);
        
        double result = 0.0;
        for (int i = 0; i < N1; i++) {
            for (int j = 0; j < N2; j++) {
                result += compute(i, j, nodes1, nodes2, mem);
            }
        }
        
        return result;
    }
    
    private double compute(int i, int j, List<Tree> nodes1, List<Tree> nodes2, double[][] mem) {
        if (mem[i][j] >= 0) {
            return mem[i][j];
        }
        if (sameProduction(nodes1.get(i), nodes2.get(j))) {
            mem[i][j] = lambda * lambda;
            if (!nodes1.get(i).isLeaf() && !nodes2.get(j).isLeaf()) {
                List<Tree> childList1 = nodes1.get(i).getChildrenAsList();
                List<Tree> childList2 = nodes2.get(j).getChildrenAsList();
                for (int k = 0; k < childList1.size(); k++) {
                    mem[i][j] *= 1 + compute(nodes1.indexOf(childList1.get(k)), nodes2.indexOf(childList2.get(k)), nodes1, nodes2, mem);
                }
            }
        } else {
            mem[i][j] = 0.0;            
        }
        
        return mem[i][j];
    }
    
    private boolean sameProduction(Tree t1, Tree t2) {
        if (t1.value().equals(t2.value())) {
            List<Tree> childList1 = t1.getChildrenAsList();
            List<Tree> childList2 = t2.getChildrenAsList();
            if (childList1.size() == childList2.size()) {
                for (int i = 0; i < childList1.size(); i++) {
                    if (!childList1.get(i).value().equals(childList2.get(i).value())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    private void initMem(double[][] mem) {
        for (int i = 0; i < mem.length; i++) {
            for (int j = 0; j < mem[i].length; j++) {
                mem[i][j] = -1.0;
            }
        }
    }
    private List<Tree> getNodes(Tree t) {
        ArrayList<Tree> nodes = new ArrayList<Tree>();
        addNodes(t, nodes);
        return nodes;
    }
    
    private void addNodes(Tree t, List<Tree> nodes) {
        nodes.add(t);
        List<Tree> childList = t.getChildrenAsList();
        for (Tree child : childList) {
            addNodes(child, nodes);
        }
    }
    
    
}
