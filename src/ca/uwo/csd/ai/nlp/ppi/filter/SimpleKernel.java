/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
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
public class SimpleKernel implements CustomKernel {

    final int LEFT = 1;
    final int MIDDLE = 2;
    final int RIGHT = 3;
    final static BioDomainAnnotator DOMAIN_ANNOTATOR = new BioDomainAnnotator();
    HashMap<String, Double> similarityMap;
    final SynSetMapper mapper = new SynSetMapper();

    public SimpleKernel() {
        List<String> lines = Util.readLines("./resource/relation/similarity_score_wup.txt");
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

    private double evaluateGlobalContextKernel(RelationInstance instance1, RelationInstance instance2) {
        if (getType(instance1) != getType(instance2)) {
            return 0;
        }

        double score = 0.0;
        //fore-between
        if (instance1.entity1 > 0 && instance2.entity1 > 0) {
            HashSet<String> fb1 = getVector(instance1.s, 0, instance1.entity1 - 1);
            HashSet<String> fb2 = getVector(instance2.s, 0, instance2.entity1 - 1);
            double s = dot2(fb1, fb2);
            double d = Math.sqrt((fb1.size()) * (fb2.size()));
            if (d > 0) {
                s /= d;
            }
            score += s;
        }

        //between
        if (instance1.entity2 > (instance1.entity1 + 1) && instance2.entity2 > (instance2.entity1 + 1)) {
            HashSet<String> b1 = getVector(instance1.s, instance1.entity1, instance1.entity2);
            HashSet<String> b2 = getVector(instance2.s, instance2.entity1, instance2.entity2);
            double s = dot2(b1, b2);
            double d = Math.sqrt((b1.size()) * (b2.size()));
            if (d > 0) {
                s /= d;
            }
            score += s;
        }

        //between-after
        if (instance1.entity2 < (instance1.s.size() - 1) && instance2.entity2 < (instance2.s.size() - 1)) {
            HashSet<String> ba1 = getVector(instance1.s, instance1.entity2 + 1, instance1.s.size() - 1);
            HashSet<String> ba2 = getVector(instance2.s, instance2.entity2 + 1, instance2.s.size() - 1);
            double s = dot2(ba1, ba2);
            double d = Math.sqrt((ba1.size()) * (ba2.size()));
            if (d > 0) {
                s /= d;
            }
            score += s;
        }

        //System.out.println("score: " + score);
        return score;
    }

    private HashSet<String> getVector(Sentence s, int start, int end) {
        HashSet<String> vector = new HashSet<String>();
        for (int i = start; i <= end; i++) {
            String word = s.get(i).word().toLowerCase();
            if (word.contains("-")) {
                word = word.substring(word.lastIndexOf('-') + 1);
            }
            if (word.startsWith("PROTEIN")) {
                continue;
            }

            char pos = s.get(i).getTag("POS").toLowerCase().charAt(0);
            if (pos == 'v' || pos == 'n' || pos == 'j' || pos == 'r' || pos == 'i' || pos == 't') {
                //vector.add(s.get(i).getTag("BASE"));
                vector.add(word);
            }
        }
        return vector;
    }

    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;

        return evaluateSimpleKernel(instance1, instance2);
        //return evaluateGlobalContextKernel(instance1, instance2);

        //return 1.5 * evaluateSimpleKernel(instance1, instance2) * evaluateGlobalContextKernel(instance1, instance2);
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
        double d = Math.sqrt(left1.size() * left2.size());
        if (d > 0) {
            score1 /= d;
        }

        HashSet<String> right1 = makeHashSet(instance1, lcsIndex1, instance1.path.size() - 1);
        HashSet<String> right2 = makeHashSet(instance2, lcsIndex2, instance2.path.size() - 1);

        double score2 = dot(right1, right2);
        d = Math.sqrt(right1.size() * right2.size());
        if (d > 0) {
            score2 /= d;
        }

        left1.addAll(right1);
        left2.addAll(right2);
        double score3 = dot(left1, left2);
        d = Math.sqrt(left1.size() * left2.size());
        if (d > 0) {
            score3 /= d;
        }

        //double similarityScore = measureSimilarity(instance1, instance2);

        return score1 + score2 + score3;// + 0.009 * similarityScore;                
    }

    private double measureSimilarity(RelationInstance instance1, RelationInstance instance2) {
        Sentence s1 = instance1.s;
        Sentence s2 = instance2.s;
        List<String> path1 = instance1.path;
        List<String> path2 = instance2.path;

        double maxScore = 0;
        for (int i = 0; i < path1.size(); i += 2) {
            int index1 = Integer.parseInt(path1.get(i));
            if (s1.get(index1).getTag("DOMAIN") != null) {
                String word1 = s1.get(index1).getTag("BASE").toLowerCase();
                if (word1.contains("-")) {
                    word1 = word1.substring(word1.lastIndexOf('-') + 1);
                }

                for (int j = 0; j < path2.size(); j += 2) {
                    int index2 = Integer.parseInt(path2.get(j));
                    if (s2.get(index2).getTag("DOMAIN") != null) {
                        String word2 = s2.get(index2).getTag("BASE").toLowerCase();
                        if (word2.contains("-")) {
                            word2 = word2.substring(word2.lastIndexOf('-') + 1);
                        }
                        String key = null;
                        if (word1.compareTo(word2) < 0) {
                            key = word1 + ":" + word2;
                        } else {
                            key = word2 + ":" + word1;
                        }
                        Double score = this.similarityMap.get(key);
                        if (score != null && score > maxScore) {
                            maxScore = score;
                        }
                    }
                }
            }
        }
        return maxScore;
    }

    private HashSet<String> lcsFeatureSet(RelationInstance instance) {
        HashSet<String> set = new HashSet<String>();
        Sentence s = instance.s;
        SimpleDepGraph depGraph = instance.depGraph;
        int lcs = instance.lcs;
        String lcsPOS = s.get(lcs).getTag("POS").substring(0, 1);
        String lcsLemma = s.get(lcs).getTag("BASE");
        set.add(lcsPOS);
        set.add(lcsLemma);
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(lcs);
        for (SimpleDependency dep : govDependencies) {
            set.add(dep.reln());
        }
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(lcs);
        for (SimpleDependency dep : depDependencies) {
            set.add("-" + dep.reln());
            set.add(s.get(dep.gov()).getTag("BASE"));
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

    private HashSet<String> makeHashSet(RelationInstance instance, int start, int end) {
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        //SimpleDepGraph depGraph = instance.depGraph;        
        for (int i = start; i <= end; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                //set.add(s.get(index).word());
                //set.add(s.get(index).getTag("BASE").toLowerCase());
                String word = s.get(index).word().toLowerCase();
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                }
                set.add(word);

                if (s.get(index).getTag("DOMAIN") != null) {
                    char pos = s.get(index).getTag("POS").charAt(0);
                    /*if (s.get(index).getTag("POS").startsWith("V")) {
                    set.add("REL-V");
                    } else {
                    set.add("REL-N");
                    }*/
                    set.add("REL-" + pos);
                }

                /*if (index == instance.lcs) {
                set.add("*" + word);
                }*/

            } else {
                String reln = path.get(i);
                if (reln.startsWith("prep")) {
                    set.add("prep");
                } else if (reln.startsWith("-prep")) {
                    set.add("-prep");
                }
                if (reln.equals("nsubjpass")) {
                    reln = "nsubj";
                } else if (reln.equals("-nsubjpass")) {
                    reln = "-nsubj";
                }

                set.add(reln);
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

    private double dot2(HashSet<String> set1, HashSet<String> set2) {
        int score = 0;
        HashSet<Integer> synSet2 = new HashSet<Integer>();
        for (String word : set2) {
            if (DOMAIN_ANNOTATOR.isDomainTerm(word.toLowerCase())) {
                HashSet<Integer> set = mapper.getSynSet(word);
                if (set != null) {
                    synSet2.addAll(set);
                }
            }
        }
        for (String w : set1) {
            if (set2.contains(w)) {
                score++;
            } else if (DOMAIN_ANNOTATOR.isDomainTerm(w.toLowerCase())) {
                HashSet<Integer> synSet = mapper.getSynSet(w);
                if (synSet != null && mapper.doesIntersect(synSet, synSet2)) {
                    score++;
                }
            }
        }
        return score;
    }

    private double poly(HashSet<String> set1, HashSet<String> set2, double gamma, double coef, double degree) {
        double score = dot(set1, set2);
        score = Math.pow((gamma * score) + coef, degree);
        score /= Math.sqrt((Math.pow((gamma * set1.size()) + coef, degree)) * Math.pow((gamma * set2.size()) + coef, degree));
        return score;
    }
}
