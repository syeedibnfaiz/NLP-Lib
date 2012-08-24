
package ca.uwo.csd.ai.nlp.libsvm;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import kernel.CustomKernel;
import kernel.ds.SparseVector;
import libsvm.svm_node;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LCSDepPathKernel implements CustomKernel {

    final static int SCORE_STEM = 3;
    final static int SCORE_DOM_OR_PROT = 2;
    final static int SCORE_POS = 1;
    final static int SCORE_UNIT = 6;    
    final static int SCORE_REL = 6;
    final static double LAMBDA = 0.8;
    final static SnowballStemmer STEMMER = new englishStemmer();
    
    HashMap<String, Integer> map;
    int wordCount;
    
    public LCSDepPathKernel() {
        map = new HashMap<String, Integer>();
        wordCount = 0;
    }
    
    private int getIndex(String word) {
        Integer value = map.get(word);
        if (value == null) {
            map.put(word, wordCount++);
            if (map.size() % 1000 == 0) {
                System.out.println("map_size: "+map.size());
            }
            return wordCount - 1;
        }
        return value;
    }
    
    private SparseVector getVector(Sentence s, int start, int end) {
        SparseVector vector = new SparseVector();
        for (int i = start; i <= end; i++) {
            if (!s.get(i).word().startsWith("PROTEIN")) {
                //STEMMER.setCurrent(s.get(i).word().toLowerCase());
                //STEMMER.stem();
                vector.add(getIndex(s.get(i).word()), 1.0);
                if (i < end && !s.get(i+1).word().startsWith("PROTEIN")) { //2-gram
                    vector.add(getIndex(s.get(i).word() + "-" + s.get(i+1).word()), 1.0);
                }
            }            
        }
        vector.sortByIndices();
        return vector;
    }
    
    private double evaluateGlobalContextKernel(DepGraph dg1, DepGraph dg2) {
        double score = 0.0;
        //fore-between
        if (dg1.entity1 > 0 && dg2.entity1 > 0) {
            SparseVector fb1 = getVector(dg1.s, 0, dg1.entity1 - 1);
            SparseVector fb2 = getVector(dg2.s, 0, dg2.entity1 - 1);
            score += fb1.dot(fb2) / Math.sqrt((fb1.size()) * (fb2.size()));
        }
        
        //between
        if (dg1.entity2 > (dg1.entity1+1) && dg2.entity2 > (dg2.entity1+1)) {
            SparseVector b1 = getVector(dg1.s, dg1.entity1 + 1, dg1.entity2 - 1);
            SparseVector b2 = getVector(dg2.s, dg2.entity1 + 1, dg2.entity2 - 1);
            score += b1.dot(b2) / Math.sqrt((b1.size()) * (b2.size()));
        }
        
        //between-after
        if (dg1.entity2 < (dg1.s.size() - 1) && dg2.entity2 < (dg2.s.size() - 1)) {
            SparseVector ba1 = getVector(dg1.s, dg1.entity2 + 1, dg1.s.size() - 1);
            SparseVector ba2 = getVector(dg2.s, dg2.entity2 + 1, dg2.s.size() - 1);
            score += ba1.dot(ba2) / Math.sqrt((ba1.size()) * (ba2.size()));
        }
        
        //System.out.println("score: " + score);
        return score;
    }
    
    @Override
    public double evaluate(svm_node x, svm_node y) {
        if (!(x.data instanceof DepGraph) || !(y.data instanceof DepGraph)) {
            throw new IllegalArgumentException("Node data does not contain dependency tree.");
        }
        
        DepGraph dg1 = (DepGraph) x.data;
        DepGraph dg2 = (DepGraph) y.data;                                
        
        double score = evaluateDepPathLCSKernel(dg1, dg2);
        //double score = evaluateGlobalContextKernel(dg1, dg2);
        //double score = evaluateGlobalContextKernel(dg1, dg2) + 2.0* evaluateDepPathLCSKernel(dg1, dg2);
        return score;
    }
    
    private double evaluateDepPathLCSKernel(DepGraph dg1, DepGraph dg2) {
        Sentence s1 = dg1.s;
        Sentence s2 = dg2.s;
        
        List<String> path1 = getBackBonePath(dg1);
        List<String> path2 = getBackBonePath(dg2);
        
        if (path1 == null || path2 == null) {
            /*System.out.println("null");
            if (path1 == null) {
                System.out.println(s1);
                System.out.println(s1.get(dg1.entity1) + "-" + s1.get(dg1.entity2));
                System.out.println(dg1.depGraph.getPath(dg1.entity1, dg1.entity2));
                System.out.println("");
            }
            if (path2 == null) {
                System.out.println(s2);
                System.out.println(s2.get(dg2.entity1) + "-" + s2.get(dg2.entity2));
                System.out.println(dg2.depGraph.getPath(dg2.entity1, dg2.entity2));
                System.out.println("");
            }*/
            return 0.0;
        }
        
        int m = path1.size();
        int n = path2.size();
        
        int[][] c = new int[m+1][n+1];
        
        for (int i = 1; i < m - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                if ((i%2) != (j%2)) {
                    c[i][j] = Math.max(c[i-1][j], c[i][j-1]);
                } else {
                    int score = getScore(s1, s2, path1.get(i), path2.get(j), i%2);
                    c[i][j] = Math.max(c[i-1][j-1] + score, Math.max(c[i-1][j], c[i][j-1]));
                }
            }
        }
        //System.out.println(c[m-2][n-2]);
        double score = Math.pow(LAMBDA, Math.abs(m - n)/2) * (1.0 * c[m-2][n-2])/(SCORE_UNIT * (Math.min(m, n)-2));
        //double score = (1.0 * c[m-2][n-2])/(SCORE_UNIT * (Math.min(m, n)-2));
        /*System.out.println(s1);
        System.out.println(path1);
        System.out.println(s2);
        System.out.println(path2);
        System.out.println("Score: " + score + "\n");*/
        return score;
    }
    
    public List<String> getBackBonePath(DepGraph dg) {                
        SimpleDepGraph depGraph = dg.depGraph;        
        Sentence s = dg.s;
        
        int entity1 = dg.entity1;
        int entity2 = dg.entity2;
        
        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1, false);
        
        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2, false);
        
        int lcs = -1;
        for (Integer ancestor : ancestors2) {
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }
        
        if (lcs == -1) {    //path = e1 ---- e2 or e1 ++++ e2
            //return null;
            List<SimpleDependency> relations = depGraph.getPathAsRelnList(entity1, entity2, false);
            if (relations == null) {
                return null;
            }
            List<String> path = new ArrayList<String>();            
            
            boolean rightDirection = (depGraph.getDependency(relations.get(0).gov(), relations.get(0).dep()) != null);
            for (int i = 0; i < relations.size(); i++) {
                int gov = relations.get(i).gov();                
                path.add(String.valueOf(gov));
                String reln = relations.get(i).reln();
                if (rightDirection) {
                    path.add(reln);
                } else {
                    path.add("-"+reln);
                }                                  
            }
            path.add(String.valueOf(relations.get(relations.size() - 1).dep()));
            /*path.add(String.valueOf(entity2));
            System.out.println(s);
            System.out.println(s.get(entity1) + "-" + s.get(entity2));
            System.out.println(depGraph.getPath(entity1, entity2));
            System.out.println(relations);
            System.out.println("path: "+path);*/
            return path;
        }
        return getPath(dg, lcs);
    }
    
    private void addAncestors(List<Integer> ancestors, Sentence s, SimpleDepGraph depGraph, int node, boolean onlyRelTerms) {                                
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(node);
        boolean[] visited = new boolean[s.size()];
        
        while (!queue.isEmpty()) {
            int next = queue.poll();
            if (visited[next]) {
                continue;
            }
            visited[next] = true;
            List<SimpleDependency> deps = depGraph.getDepDependencies(next);
            for (SimpleDependency dependency : deps) {
                int gov = dependency.gov();
                if (onlyRelTerms) {
                    if (s.get(gov).getTag("DOMAIN") != null) {
                        ancestors.add(gov);
                    }
                } else {
                    ancestors.add(gov);
                }
                queue.add(gov);
            }
        }
    }
    
    private List<String> getPath(DepGraph dg, int lcs) {
        SimpleDepGraph depGraph = dg.depGraph;
        int entity1 = dg.entity1;
        int entity2 = dg.entity2;
                
        List<SimpleDependency> path1 = depGraph.getPathAsRelnList(lcs, entity1, true);
        List<SimpleDependency> path2 = depGraph.getPathAsRelnList(lcs, entity2, true);        
        
        ArrayList<String> path = new ArrayList<String>();
        for (int i = path1.size() - 1; i >= 0; i--) {
            int dep = path1.get(i).dep();
            path.add(String.valueOf(dep));
            path.add("-" + path1.get(i).reln());
        }
        for (int i = 0; i < path2.size(); i++) {
            int gov = path2.get(i).gov();
            path.add(String.valueOf(gov));
            path.add(path2.get(i).reln());
        }
        path.add(String.valueOf(entity2));
        return path;
    }

    private int getScore(Sentence s1, Sentence s2, String val1, String val2, int pos) {
        int score = 0;
        if (pos == 0) { //compare word
            int index1 = Integer.parseInt(val1);
            int index2 = Integer.parseInt(val2);
            String word1 = s1.get(index1).word();
            String word2 = s2.get(index2).word();
            String stem1 = getStem(word1);
            String stem2 = getStem(word2);
            if (stem1.equals(stem2)) {
                score += SCORE_STEM + SCORE_DOM_OR_PROT;
                if (s1.get(index1).getTag("POS").equals(s2.get(index2).getTag("POS"))) {
                    score += SCORE_POS;
                }
            } else if (s1.get(index1).getTag("DOMAIN") !=  null && s2.get(index2).getTag("DOMAIN") !=  null) {
                score += SCORE_DOM_OR_PROT;
                if (s1.get(index1).getTag("POS").equals(s2.get(index2).getTag("POS"))) {
                    score += SCORE_POS;
                }
            } else if (s1.get(index1).getTag("POS").equals(s2.get(index2).getTag("POS"))) {
                score += SCORE_POS;
            }
        } else { //compare relation
            if (val1.equals(val2)) {
                score += SCORE_REL;
            } else if (val1.startsWith("-nsubj") && val2.startsWith("-nsubj")) {
                score += SCORE_REL/2;
            } else if (val1.startsWith("prep") && val2.startsWith("prep")) {
                score += SCORE_REL/2;
            } else if (val1.startsWith("-prep") && val2.startsWith("-prep")) {
                score += SCORE_REL/2;
            }
        }
        return score;
    }
    
    private String getStem(String word) {
        STEMMER.setCurrent(word);
        STEMMER.stem();
        return STEMMER.getCurrent();
    }
    
}
