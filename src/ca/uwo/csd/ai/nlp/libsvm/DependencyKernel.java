
package ca.uwo.csd.ai.nlp.libsvm;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import java.util.ArrayList;
import java.util.List;
import kernel.CustomKernel;
import libsvm.svm_node;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DependencyKernel implements CustomKernel {
    private final static int MAX_NODE = 300;
    double mem[][] = new double[MAX_NODE][MAX_NODE];
    double lambda;

    public DependencyKernel(double lambda) {
        this.lambda = lambda;
    }

    public DependencyKernel() {
        this(0.5);
    }
    
    
    
    /*@Override
    public double evaluate(svm_node x, svm_node y) {
        if (!(x.data instanceof DepGraph) || !(y.data instanceof DepGraph)) {
            throw new IllegalArgumentException("Node data does not contain dependency tree.");
        }
        
        DepGraph dg1 = (DepGraph) x.data;
        DepGraph dg2 = (DepGraph) y.data;
        
        SimpleDepGraph depGraph1 = dg1.depGraph;
        SimpleDepGraph depGraph2 = dg2.depGraph;
        int lcs1 = dg1.lcs;
        int lcs2 = dg2.lcs;
        Sentence s1 = dg1.s;
        Sentence s2 = dg2.s;
        
        if (lcs1 < 0 || lcs2 < 0) {
            //System.out.println("LCS");
            return 0;
        }
        
        List<Integer> nodes1 = getNodes(depGraph1, lcs1);
        List<Integer> nodes2 = getNodes(depGraph2, lcs2);
        
        
        int N1 = nodes1.size();
        int N2 = nodes2.size();
        
        //fill mem with -1.0
        initMem(mem, s1.size(), s2.size());
        
        double result = 0.0;
        for (int i = 0; i < N1; i++) {
            for (int j = 0; j < N2; j++) {
                result += compute(nodes1.get(i), nodes2.get(j), depGraph1, depGraph2, s1, s2, mem);
            }
        }
        
        return result;
    }
    
    double compute(int i, int j, SimpleDepGraph depGraph1, SimpleDepGraph depGraph2, Sentence s1, Sentence s2, double[][] mem) {
        if (mem[i][j] >= 0) {            
            return mem[i][j];
        }
        
        mem[i][j] = 0;
        if (s1.get(i).word().equalsIgnoreCase(s2.get(j).word())) {            
            List<SimpleDependency> deps1 = depGraph1.getGovDependencies(i);
            List<SimpleDependency> deps2 = depGraph2.getGovDependencies(j);
            
            if (!deps1.isEmpty() && !deps2.isEmpty()) {
                
                mem[i][j] = 1.0;
                for (int k = 0; k < deps1.size(); k++) {
                    SimpleDependency sDep1 = deps1.get(k);
                    for (int l = 0; l < deps2.size(); l++) {
                        SimpleDependency sDep2 = deps2.get(l);
                        if (sDep1.reln().equals(sDep2.reln()) && 
                                (s1.get(sDep1.dep()).word().equalsIgnoreCase(s2.get(sDep2.dep()).word()))) {
                            mem[i][j] *= 2.0 + compute(sDep1.dep(), sDep2.dep(), depGraph1, depGraph2, s1, s2, mem);
                        }
                    }
                }
                mem[i][j] -= 1.0;
                mem[i][j] *= lambda * lambda;
                if (mem[i][j] < 0) {
                    System.out.println("WTF");
                }
            }
        }
        
        return mem[i][j];
    }
    private void initMem(double[][] mem, int N1, int N2) {
        for (int i = 0; i < N1; i++) {
            for (int j = 0; j < N2; j++) {
                mem[i][j] = -1.0;
            }
        }
    }
    
    private List<Integer> getNodes(SimpleDepGraph depGraph, int i) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        addNodes(list, depGraph, i);
        return list;
    }
    
    private void addNodes(List<Integer> list, SimpleDepGraph depGraph, int i) {
        list.add(i);
        List<SimpleDependency> deps = depGraph.getGovDependencies(i);
        for (SimpleDependency dep : deps) {
            addNodes(list, depGraph, dep.dep());
        }
    }*/

    @Override
    public double evaluate(svm_node x, svm_node y) {        
        if (!(x.data instanceof DepGraph) || !(y.data instanceof DepGraph)) {
            throw new IllegalArgumentException("Node data does not contain dependency tree.");
        }
        
        DepGraph dg1 = (DepGraph) x.data;
        DepGraph dg2 = (DepGraph) y.data;
        
        SimpleDepGraph depGraph1 = dg1.depGraph;
        SimpleDepGraph depGraph2 = dg2.depGraph;
        int lcs1 = dg1.lcs;
        int lcs2 = dg2.lcs;
        Sentence s1 = dg1.s;
        Sentence s2 = dg2.s;
        
        if (lcs1 < 0 || lcs2 < 0) {
            //System.out.println("LCS");
            return 0;
        }
        
        if (!getPosition(dg1.entity1, dg1.entity2, lcs1).equals(getPosition(dg2.entity1, dg2.entity2, lcs2))) {
            return 0.0;
        }
        
        double result = evaluate(depGraph1, depGraph2, s1, s2, lcs1, lcs2);
        
        return result;
    }
    
    private String getPosition(int entity1, int entity2, int lcs) {
        if (lcs >= entity1 && lcs <= entity2) {
            return "between";
        } else if (lcs < entity1) {
            return "before";
        } else {
            return "after";
        }
    }
    private double evaluate(SimpleDepGraph depGraph1, SimpleDepGraph depGraph2, Sentence s1, Sentence s2, int node1, int node2) {
        double result = 0.0;
        if (isMatch(depGraph1, depGraph2, s1, s2, node1, node2)) {
            result = getScore(depGraph1, depGraph2, s1, s2, node1, node2);
            result += evaluateChildren(depGraph1, depGraph2, s1, s2, node1, node2);
        }
        return result;
    }
    
    private double evaluateChildren(SimpleDepGraph depGraph1, SimpleDepGraph depGraph2, Sentence s1, Sentence s2, int node1, int node2) {
        List<SimpleDependency> deps1 = depGraph1.getGovDependencies(node1);
        List<SimpleDependency> deps2 = depGraph2.getGovDependencies(node2);
        int n1 = deps1.size();
        int n2 = deps2.size();
        int[][] l = new int[n1 + 1][n2 + 1];
        //fill maxLength
        l[n1][n2] = 0;
        for (int i = 0; i < n1; i++) l[i][n2] = 0;
        for (int j = 0; j < n2; j++) l[n1][j] = 0;
        for (int i = n1 - 1; i >= 0; i--) {
            for (int j = n2 - 1; j >= 0; j--) {
                if (isMatch(depGraph1, depGraph2, s1, s2, deps1.get(i).dep(), deps2.get(j).dep())) {
                    l[i][j] = l[i+1][j+1] + 1;
                } else {
                    l[i][j] = 0;
                }
            }
        }
        
        double result = 0.0;
        double[][] c = new double[n1 + 1][n2 + 1];
        c[n1][n2] = 0.0;
        for (int i = 0; i < n1; i++) c[i][n2] = 0.0;
        for (int j = 0; j < n2; j++) c[n1][j] = 0.0;
        for (int i = n1 - 1; i >= 0; i--) {
            for (int j = n2 - 1; j >= 0; j--) {
                if (isMatch(depGraph1, depGraph2, s1, s2, deps1.get(i).dep(), deps2.get(j).dep())) {
                    c[i][j] = lambda * c[i + 1][j + 1];
                    c[i][j] += (lambda * (1 - Math.pow(lambda, l[i][j]))/(1 - lambda)) * evaluate(depGraph1, depGraph2, s1, s2, deps1.get(i).dep(), deps2.get(j).dep());
                } else {
                    c[i][j] = 0;
                }
                result += c[i][j];
            }
        }
        return result;
    }
    
    private boolean isMatch(SimpleDepGraph depGraph1, SimpleDepGraph depGraph2, Sentence s1, Sentence s2, int node1, int node2) {
        //return s1.get(node1).getTag("POS").equals(s2.get(node2).getTag("POS"));
        String posTag1 = s1.get(node1).getTag("POS");
        String posTag2 = s2.get(node2).getTag("POS");
        String genPOS1 = posTag1.substring(0, Math.min(2, posTag1.length()));
        String genPOS2 = posTag2.substring(0, Math.min(2, posTag2.length()));
        
        return (genPOS1.equals(genPOS2));
    }
    
    private double getScore(SimpleDepGraph depGraph1, SimpleDepGraph depGraph2, Sentence s1, Sentence s2, int node1, int node2) {
        double result = 0.0;
        //same POS
        String pos1 = s1.get(node1).getTag("POS");
        String pos2 = s2.get(node2).getTag("POS");
        if (pos1.equals(pos2)) {
            //result += 1.0;
        }
        
        //same dependency
        int parent1 = depGraph1.getParent(node1);
        int parent2 = depGraph2.getParent(node2);
        if (parent1 != -1 && parent2 != -1) {
            String reln1 = depGraph1.getDependency(parent1, node1).reln();
            String reln2 = depGraph2.getDependency(parent2, node2).reln();
            if (reln1.equals(reln2)) {
                //result += 1.0;
            }
        }
        
        //both are domain words
        if (s1.get(node1).getTag("DOMAIN") != null && s2.get(node2).getTag("DOMAIN") != null) {
            //result += 1.0;
            if (s1.get(node1).getTag("DOMAIN").equals(s2.get(node2).getTag("DOMAIN"))) {
                //result += 1.0;
            }
        }
        
        //same word
        if (s1.get(node1).word().equalsIgnoreCase(s2.get(node2).word())) {
            result += 1.0;
        }
        
        //both are protein
        if (s1.get(node1).word().contains("PROT") && s2.get(node2).word().contains("PROT")) {
            //result += 1.0;
        }
        
        return result;
    }
}
