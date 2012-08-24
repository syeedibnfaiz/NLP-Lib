/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.ArrayList;
import java.util.List;
import kernel.CustomKernel;
import libsvm.svm_node;

/**
 *
 * @author tonatuni
 */
public class DepWalkKernel implements CustomKernel {
    final int LEFT = 1;
    final int MIDDLE = 2;
    final int RIGHT = 3;
    
    int qMin;
    int qMax;

    public DepWalkKernel(int qMin, int qMax) {
        this.qMin = qMin;
        this.qMax = qMax;
    }

    public DepWalkKernel() {
        this(2, 2);
    }

    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;

        if (getType(instance1) != getType(instance2)) {
            return 0;
        }
        return evaluate(instance1, instance2);///Math.sqrt(evaluate(instance1, instance1) * evaluate(instance2, instance2));
    }

    private double evaluate(RelationInstance instance1, RelationInstance instance2) {
        double[] scores = new double[qMax+1];
        List<String> path1 = instance1.path;
        List<String> path2 = instance2.path;

        int[][][] c = new int[path1.size()][path2.size()][qMax + 1];
        for (int i = 0; i < path1.size() - 2; i += 2) {
            for (int j = 0; j < path2.size() - 2; j += 2) {
                c[i][j][2] = getTScore(instance1, i, instance2, j, 2 * 2 - 1);
                if (c[i][j][2] > scores[2]) {
                    scores[2] = c[i][j][2];
                }
            }
        }
        for (int k = 3; k <= qMax; k++) {
            for (int i = 0; i < path1.size() - 2 * (k - 1); i += 2) {
                for (int j = 0; j < path2.size() - 2 * (k - 1); j += 2) {
                    c[i][j][k] = c[i + 2][j + 2][k - 1];
                    c[i][j][k] += getTScoreWord(instance1, path1.get(i), instance2, path2.get(j));
                    c[i][j][k] += getTScoreReln(path1.get(i + 1), path2.get(j + 1));
                    if (c[i][j][k] > scores[k]) {
                        scores[k] = c[i][j][k];
                    }
                }
            }
        }

        double score = 0;
        for (int i = 2; i <= qMax; i++) {
            score += scores[i];
        }

        return score;
    }

    private String getWord(RelationInstance instance, int index) {
        String word = instance.s.get(index).getTag("BASE");
        if (index == instance.entity1) {
            word = "ENTITY1";
        } else if (index == instance.entity2) {
            word = "ENTITY2";
        }
        return word;
    }

    private int getTScore(RelationInstance instance1, int start1, RelationInstance instance2, int start2, int length) {
        int score = 0;
        for (int j = 0; j < length; j++) {
            if (j % 2 == 0) {
                score += getTScoreWord(instance1, instance1.path.get(start1), instance2, instance2.path.get(start2));
            } else {
                score += getTScoreReln(instance1.path.get(start1),  instance2.path.get(start2));
            }
            start1++;
            start2++;
        }
        return score;
    }

    private int getTScoreWord(RelationInstance instance1, String sIndex1, RelationInstance instance2, String sIndex2) {
        int score = 0;
        int index1 = Integer.parseInt(sIndex1);
        int index2 = Integer.parseInt(sIndex2);


        String word1 = getWord(instance1, index1);
        String word2 = getWord(instance2, index2);

        if (word1.equals(word2)) {
            if (word1.startsWith("ENTITY")) {
                score = 3;
            } else {
                score = 1;
            }
        }

        return score;
    }

    private int getTScoreReln(String reln1, String reln2) {
        if (reln1.equals(reln2)) {
            return 6;
        }
        return 0;
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
