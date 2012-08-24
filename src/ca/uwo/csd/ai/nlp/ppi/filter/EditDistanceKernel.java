/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.List;
import kernel.CustomKernel;
import libsvm.svm_node;

/**
 *
 * @author tonatuni
 */
public class EditDistanceKernel implements CustomKernel {
    final int LEFT = 1;
    final int MIDDLE = 2;
    final int RIGHT = 3;
    
    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;
        
        if (getType(instance1) != getType(instance2)) {
            return 0;
        }

        int lcsIndex1 = getLCSIndexInPath(instance1);
        int lcsIndex2 = getLCSIndexInPath(instance2);        
        List<String> path1 = instance1.path;
        List<String> path2 = instance2.path;
        Sentence s1 = instance1.s;
        Sentence s2 = instance2.s;
        
        double score1 = getScore(s1, path1, 0, lcsIndex1, s2, path2, 0, lcsIndex2);
        double score2 = getScore(s1, path1, lcsIndex1, path1.size()-1, s2, path2, lcsIndex2, path2.size()-1);
        double score3 = getScore(s1, path1, 0, path1.size()-1, s2, path2, 0, path2.size()-1);
        
        return score1 + score2 + score3;
    }
    private double getScore(Sentence s1, List<String> path1, int start1, int end1, Sentence s2, List<String> path2, int start2, int end2) {
        int len1 = end1 - start1 + 1;
        int len2 = end2 - start2 + 1;
        int[][] c = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) c[i][0] = i;
        for (int i = 0; i <= len2; i++) c[0][i] = i;
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int index1 = start1 + i - 1;
                int index2 = start2 + j - 1;
                if (index1 % 2 == index2 % 2) {
                    if (index1 % 2 == 0) { //word
                        int p = Integer.parseInt(path1.get(index1));
                        int q = Integer.parseInt(path2.get(index2));
                        String word1 = s1.get(p).getTag("BASE");
                        String word2 = s2.get(q).getTag("BASE");
                        if (word1.equals(word2) || (word1.contains("PROTEIN") && word2.contains("PROTEIN"))) {
                            c[i][j] = c[i-1][j-1];
                        } else if (s1.get(p).getTag("DOMAIN") != null && s2.get(q).getTag("DOMAIN") != null && s1.get(p).getTag("POS").charAt(0) == s2.get(q).getTag("POS").charAt(0)) {
                            c[i][j] = c[i-1][j-1];
                        } else {
                            c[i][j] = Math.min(c[i-1][j], c[i][j-1]) + 1;
                        }
                    } else {
                        if (path1.get(index1).equals(path2.get(index2))) {
                            c[i][j] = c[i-1][j-1];
                        } else {
                            c[i][j] = Math.min(c[i-1][j], c[i][j-1]) + 1;
                        }
                    }
                } else {
                    c[i][j] = Math.min(c[i-1][j], c[i][j-1]) + 1;
                }
            }
        }
        
        return Math.exp(-1.0 * c[len1][len2]/Math.max(len1, len2));
    }
    
    private int getLCSIndexInPath(RelationInstance instance) {
        int lcs = instance.lcs;
        List<String> backBonePath = instance.path;
        String lcsStr = String.valueOf(lcs);
        int dist = 0;
        for (int i = 0; i < backBonePath.size(); i++) {
            if (backBonePath.get(i).equals(lcsStr)) {
                dist = i;
                break;
            }
        }
        return dist;
    }
    
    private int getType(RelationInstance instance) {
        if (instance.lcs < instance.entity1) {
            return LEFT;
        } else if (instance.lcs < instance.entity2) {            
            return MIDDLE;
        } else {
            return RIGHT;
        }
    }
}
