package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import kernel.CustomKernel;
import kernel.ds.SparseVector;
import libsvm.svm_node;

/**
 *
 * @author tonatuni
 */
public class PathStructureKernel implements CustomKernel {
    final double DIST_WT = 0.5;
    final double DIFF_WT = 0.9;
    final FeatureSet2Vector featureSet2Vector = new FeatureSet2Vector();
    
    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;

        if (instance1.type != instance2.type) {
            return 0;
        }
        double s = evaluate(instance1, instance2);
        //System.out.println(s);
        return s;
    }
    
    private double evaluate(RelationInstance instance1, RelationInstance instance2) {
        double leftScore = evaluate(getLeftPath(instance1), getLeftPath(instance2));
        double rightScore = evaluate(getRightPath(instance1), getRightPath(instance2));
        double semanticScore = evaluateSemantics(instance1, instance2);
        return leftScore * rightScore + semanticScore;
        
        /*double score1 = instance1.leftVector.normDot(instance2.leftVector);
        double score2 = instance1.rightVector.normDot(instance2.rightVector);
        double score3 = instance1.fullVector.normDot(instance2.fullVector);
        return leftScore + rightScore + + semanticScore *10 + score1*10 + score2*10 + score3 *10;*/
    }
        
    private double evaluateSemantics(RelationInstance instance1, RelationInstance instance2) {
        SparseVector v1 = featureSet2Vector.getVector(getDomainSet(instance1));
        SparseVector v2 = featureSet2Vector.getVector(getDomainSet(instance2));
        return v1.normDot(v2);
    }
    
    private HashSet<String> getDomainSet(RelationInstance instance) {
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        for (int i = 0; i < path.size(); i += 2) {
            int index = Integer.parseInt(path.get(i));
            if (instance.s.get(index).getTag("DOMAIN") != null) {                
                set.add("REL-" + instance.s.get(index).getTag("POS").charAt(0));
            }
            set.add(instance.s.get(index).getTag("BASE").toLowerCase());
        }
        return set;
    }
    private double evaluate(List<String> path1, List<String> path2) {
        int sz1 = path1.size();
        int sz2 = path2.size();
        double totalScore = 0;
        for (int i = 0; i < sz1; i++) {
            double maxScore = 0;
            String w1 = path1.get(i);
            for (int j = 0; j < sz2; j++) {
                String w2 = path2.get(j);
                if (i % 2 == 0 && j % 2 == 0) { //POS
                    double score = 0;
                    if (w1.equals(w2)) {
                        score = 1;
                    } else if (w1.charAt(0) == w2.charAt(0)){
                        score = 0.5;
                    }
                    if (score > 0) {
                        int index1 = i/2;
                        int index2 = j/2;
                        double diff = Math.abs(index1 - index2);
                        double dist = (index1 + index2)/2.0;
                        score *= Math.pow(DIFF_WT, diff);
                        score *= Math.pow(DIST_WT, dist);
                        if (score > maxScore) {
                            maxScore = score;
                        }
                    }
                } else if (i % 2 == 1 && j % 2 == 1) {
                    double score = 0;
                    if (w1.equals(w2)) {
                        score = 1;
                    } else if (w1.startsWith("prep") && w2.startsWith("prep")){
                        score = 0.5;
                    } else if (w1.startsWith("nsubj") && w2.startsWith("nsubj")){
                        score = 0.5;
                    }
                    if (score > 0) {
                        int index1 = (i-1)/2;
                        int index2 = (j-1)/2;
                        double diff = Math.abs(index1 - index2);
                        double dist = (index1 + index2)/2.0;
                        score *= Math.pow(DIFF_WT, diff);
                        score *= Math.pow(DIST_WT, dist);
                        if (score > maxScore) {
                            maxScore = score;
                        }
                    }
                }
            }            
            totalScore += maxScore;
        }
        
        return totalScore/Math.sqrt(getNorm(path1) * getNorm(path2));
    }
    
    private List<String> getLeftPath(RelationInstance instance) {
        List<String> leftPath = new ArrayList<String>();        
        for (int i = instance.lcsIndex; i >= 0; i--) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(instance.path.get(i));
                leftPath.add(instance.s.get(index).getTag("POS"));
                //leftPath.add(instance.s.get(index).getTag("POS") + instance.s.get(index).getTag("BASE").toLowerCase());
            } else {
                leftPath.add(instance.path.get(i).substring(1));
            }            
        }
        return leftPath;
    }
    
    private List<String> getRightPath(RelationInstance instance) {
        List<String> rightPath = new ArrayList<String>();               
        for (int i = instance.lcsIndex; i < instance.path.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(instance.path.get(i));
                rightPath.add(instance.s.get(index).getTag("POS"));
                //rightPath.add(instance.s.get(index).getTag("POS") + instance.s.get(index).getTag("BASE").toLowerCase());
            } else {
                rightPath.add(instance.path.get(i));
            }            
        }
        return rightPath;
    }
    
    private double getNorm(List<String> path) {
        int sz = path.size();
        int wLen = (sz + 1)/2;
        int rLen = sz/2;
        
        return sumOfSeries(DIST_WT, wLen) + sumOfSeries(DIST_WT, rLen);
    }
    
    private double sumOfSeries(double r, double n) {
        return (Math.pow(r, n) - 1.0)/(r - 1.0);
    }
    
    public static void main(String[] args) {
        PathStructureKernel kernel = new PathStructureKernel();
        List<String> list = new ArrayList<String>();
        list.add("VB");
        list.add("nsubj");
        list.add("NN");
        List<String> list2 = new ArrayList<String>();
        list2.add("VB");
        list2.add("nsubj");
        list2.add("NNP");
        double evaluate = kernel.evaluate(list, list2);
        System.out.println(evaluate);
    }
}
