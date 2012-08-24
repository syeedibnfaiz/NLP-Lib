/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import kernel.CustomKernel;
import kernel.ds.SparseVector;
import libsvm.svm_node;

/**
 *
 * @author tonatuni
 */
public class SimpleKernel2 implements CustomKernel {
    final int LEFT = 1;
    final int MIDDLE = 2;
    final int RIGHT = 3;
    
    HashMap<String, Integer> map;
    int count;
    
    public SimpleKernel2() {
        map = new HashMap<String, Integer>();
    }
    
    private int getIndex(String word) {
        Integer i = map.get(word);
        if (i == null) {
            map.put(word, count);
            count++;
            return count - 1;
        } else {
            return i;
        }
    }
    
    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;

        return evaluateSimpleKernel(instance1, instance2);        
    }
    
    public double evaluateSimpleKernel(RelationInstance instance1, RelationInstance instance2) {
        if (getType(instance1) != getType(instance2)) {
            return 0;
        }

        int lcsIndex1 = getLCSIndexInPath(instance1);
        int lcsIndex2 = getLCSIndexInPath(instance2);        
        
        SparseVector v1 = makeVector(instance1, lcsIndex1);
        SparseVector v2 = makeVector(instance2, lcsIndex2);
        
        double gamma = 0.001953125;       
        double score = Math.exp(-gamma * v1.square(v2));
        return score;
    }
    
    private SparseVector makeVector(RelationInstance instance, int lcsIndex) {
        SparseVector vector = new SparseVector();
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        for (int i = 1; i < lcsIndex; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = "W1=" + s.get(index).getTag("BASE");
                if (!set.contains(lemma)) {
                    vector.add(getIndex(lemma), 1.0);
                    set.add(lemma);
                }
                String pos = "POS1=" + s.get(index).getTag("POS");
                if (!set.contains(pos)) {
                    vector.add(getIndex(pos), 1.0);
                    set.add(pos);
                }
            } else {
                String reln = path.get(i);
                if (!set.contains(reln)) {
                    vector.add(getIndex(reln), 1.0);
                    set.add(reln);
                }
            }
        }
        vector.add(getIndex("H1="), lcsIndex / 2.0);
        vector.add(getIndex("D1="), Math.abs(instance.entity1 - instance.lcs));

        vector.add(getIndex(s.get(lcsIndex).word()), 1.0);

        set = new HashSet<String>();
        for (int i = lcsIndex + 1; i < path.size() - 1; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = "W2=" + s.get(index).getTag("BASE");
                if (!set.contains(lemma)) {
                    vector.add(getIndex(lemma), 1.0);
                    set.add(lemma);
                }
                String pos = "POS2=" + s.get(index).getTag("POS");
                if (!set.contains(pos)) {
                    vector.add(getIndex(pos), 1.0);
                    set.add(pos);
                }
            } else {
                String reln = path.get(i);
                if (!set.contains(reln)) {
                    vector.add(getIndex(reln), 1.0);
                    set.add(reln);
                }
            }
        }
        vector.add(getIndex("H2="), (path.size() - lcsIndex) / 2.0);
        vector.add(getIndex("D2="), Math.abs(instance.entity2 - instance.lcs));

        int protein = 0;
        int rel = 0;
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                protein++;
            }
            if (s.get(i).getTag("DOMAIN") != null) {
                rel++;
            }
        }

        vector.add(getIndex("C1="), protein);
        vector.add(getIndex("C2="), rel);

        vector.sortByIndices();
        return vector;
    }

    
    private HashSet<String> makeRightHashSet(RelationInstance instance, int lcsIndex) {
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        for (int i = lcsIndex + 1; i < path.size() - 1; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = s.get(index).getTag("BASE");
                set.add(lemma);
            } else {
                set.add(path.get(i));
            }
        }
        set.add("REL=" + s.get(lcsIndex).word());
        return set;
    }
    
    private HashSet<String> makeFullHashSet(RelationInstance instance, int lcsIndex) {
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        for (int i = 1; i < lcsIndex; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = s.get(index).getTag("BASE");
                set.add(lemma);
            } else {
                set.add(path.get(i));
            }
        }
        for (int i = lcsIndex + 1; i < path.size() - 1; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = s.get(index).getTag("BASE");
                set.add(lemma);
            } else {
                set.add(path.get(i));
            }
        }
        set.add("REL=" + s.get(lcsIndex).word());
        return set;
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
    
    private double dot(HashSet<String> set1, HashSet<String> set2) {
        int score = 0;
        for (String w : set1) {
            if (set2.contains(w)) {
                score++;
            }
        }
        return score;        
    }
}
