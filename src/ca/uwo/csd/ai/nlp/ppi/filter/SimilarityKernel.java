/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import kernel.CustomKernel;
import libsvm.svm_node;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SimilarityKernel implements CustomKernel {
    private final static BioDomainAnnotator DOMAIN_ANNOTATOR = new BioDomainAnnotator();
    
    final int LEFT = 1;
    final int MIDDLE = 2;
    final int RIGHT = 3;
    HashMap<String, Double> similarityMap;

    public SimilarityKernel() {
        this("./resource/relation/similarity_score_wup.txt");
    }
    
    public SimilarityKernel(String similarityScoreFile) {
        List<String> lines = Util.readLines(similarityScoreFile);
        similarityMap = new HashMap<String, Double>();
        for (String line : lines) {
            String[] tokens = line.split("\\s+");
            String key = tokens[0] + ":" + tokens[1];
            double value = Double.parseDouble(tokens[2]);
            if (value >= 0) {
                similarityMap.put(key, value);
            }
        }
    }
    
    public double evaluateSimilarity(RelationInstance instance1, RelationInstance instance2) {        
        if (getType(instance1) != getType(instance2)) {
            return 0;
        }

        //int lcsIndex1 = getLCSIndexInPath(instance1);
        //int lcsIndex2 = getLCSIndexInPath(instance2);        
        
        HashSet<String> set1 = makeSimilarityHashSet(instance1, 0, instance1.entity1);
        HashSet<String> set2 = makeSimilarityHashSet(instance2, 0, instance2.entity1);
        double score1 = getScore(set1, set2);
        int min = Math.min(set1.size(), set2.size());
        if (min != 0) {
            score1 /= min;
        }
        
        set1 = makeSimilarityHashSet(instance1, instance1.entity1, instance1.entity2);
        set2 = makeSimilarityHashSet(instance2, instance2.entity1, instance2.entity2);
        double score2 = getScore(set1, set2);
        min = Math.min(set1.size(), set2.size());
        if (min != 0) {
            score2 /= min;
        }
        
        set1 = makeSimilarityHashSet(instance1, instance1.entity2, instance1.s.size()-1);
        set2 = makeSimilarityHashSet(instance2, instance2.entity2, instance2.s.size()-1);
        double score3 = getScore(set1, set2);
        min = Math.min(set1.size(), set2.size());
        if (min != 0) {
            score3 /= min;
        }
        
        //System.out.println(score1 + score2 + score3);
        return score1 + score2 + score3;
    }

    private double getScore(HashSet<String> set1, HashSet<String> set2) {
        double score = 0;
        for (String word1 : set1) {
            double localMax = 0;
            for (String word2 : set2) {
                String key = null;
                if (word1.compareTo(word2) < 0) {
                    key = word1 + ":" + word2;
                } else {
                    key = word2 + ":" + word1;
                }
                Double v = similarityMap.get(key);
                if (v != null && v > localMax) {
                    localMax = v;
                }
            }
            if (localMax < 0.4) {
                localMax = 0;
            }
            score += localMax;
        }
        return score;
    }
    
    private HashSet<String> makeSimilarityHashSet(RelationInstance instance, int start, int end) {
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();

        for (int i = start; i <= end; i++) {
            if (s.get(i).getTag("DOMAIN") != null) {
                String word = s.get(i).getTag("BASE").toLowerCase();
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                    if (!DOMAIN_ANNOTATOR.isDomainTerm(word)) {
                        continue;
                    }
                }
                set.add(word);
            }
        }

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

    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;
        
        return evaluateSimilarity(instance1, instance2);// + 0.35*evaluateSimpleKernel(instance1, instance2);
    }
    
    public double evaluateSimpleKernel(RelationInstance instance1, RelationInstance instance2) {
        if (getType(instance1) != getType(instance2)) {
            return 0;
        }

        int lcsIndex1 = getLCSIndexInPath(instance1);
        int lcsIndex2 = getLCSIndexInPath(instance2);        

        HashSet<String> left1 = makeHashSet(instance1, 1, lcsIndex1);
        HashSet<String> left2 = makeHashSet(instance2, 1, lcsIndex2);

        double score1 = dot(left1, left2);
        score1 /= Math.sqrt(left1.size() * left2.size());
        //double score1 = poly(left1, left2, 0.5, 0, 3);

        HashSet<String> right1 = makeHashSet(instance1, lcsIndex1, instance1.path.size() - 1);
        HashSet<String> right2 = makeHashSet(instance2, lcsIndex2, instance2.path.size() - 1);

        double score2 = dot(right1, right2);
        score2 /= Math.sqrt(right1.size() * right2.size());
        //double score2 = poly(right1, right2, 0.5, 0, 3);

        left1.addAll(right1);
        left2.addAll(right2);
        double score3 = dot(left1, left2);
        score3 /= Math.sqrt(left1.size() * left2.size());
        //double score3 = poly(left1, left2, 0.5, 0, 3);

        /*HashSet<String> lcsSet1 = lcsFeatureSet(instance1);
        HashSet<String> lcsSet2 = lcsFeatureSet(instance2);
        double score4 = dot(lcsSet1, lcsSet2);
        score4 /= Math.sqrt(lcsSet1.size() * lcsSet2.size());*/
        
        return (score1 + score2 + score3)/3.0;
    }
    
    private HashSet<String> makeHashSet(RelationInstance instance, int start, int end) {
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        //SimpleDepGraph depGraph = instance.depGraph;        
        for (int i = start; i <= end; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                set.add(s.get(index).word());
                /*String lemma = s.get(index).getTag("BASE");
                if (s.get(index).word().contains("PROTEIN")) {
                    set.add("PROTEIN");
                } else {
                    set.add(lemma);                    
                }*/
                
                if (s.get(index).getTag("DOMAIN") != null) {
                    if (s.get(index).getTag("POS").startsWith("V")) {
                        set.add("REL-V");
                    } else {
                        set.add("REL-N");
                    }
                }
                                
            } else {
                String reln = path.get(i);
                if (reln.startsWith("prep")) {
                    set.add("prep");
                } else if (reln.startsWith("-prep")) {
                    set.add("-prep");
                }
                set.add(path.get(i));
            }
        }

        return set;
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
