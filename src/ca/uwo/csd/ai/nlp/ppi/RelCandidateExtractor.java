/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelCandidateExtractor {
    private final static Pattern form1Pat = Pattern.compile("PROTEIN[0-9]+.*RELV.*PROTEIN[0-9]+");
    private final static Pattern form2Pat = Pattern.compile("PROTEIN[0-9]+.*REL.*PROTEIN[0-9]+");
    private final static Pattern form3Pat = Pattern.compile("RELN\\.PREP.*PROTEIN[0-9]+.*PROTEIN[0-9]+");
    private final static Pattern form5Pat = Pattern.compile("PROTEIN[0-9]+.*PROTEIN[0-9]+\\.(W\\.|PREP\\.){0,2}REL");
    
    List<Pair<Integer, Integer>> applyRule1(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();
        List<SimpleDependency> deps = ccDepGraph.getDependencies("rcmod|.*subj|.*subjpass");        
        
        for (SimpleDependency seedDep : deps) {
            int gov = seedDep.gov();
            int dep = seedDep.dep();
            String reln = seedDep.reln();
            //if they are cc processed nsubj they might have been rcmod in depGraph
            SimpleDependency normalDep = depGraph.getDependency(gov, dep);
            if (normalDep != null) {
                System.out.println("prev: "+reln);
                reln = normalDep.reln();        
                System.out.println("new: "+reln);
            }
            List<Integer> reachable1 = null;
            List<Integer> reachable2 = null;
            Set<Integer> entitySet2 = new HashSet<Integer>();
            Set<Integer> entitySet1 = new HashSet<Integer>();
            if (reln.matches("rcmod")) {
                if (isNegated(dep, depGraph)) continue;
                reachable2 = depGraph.getReachableIndices(dep, true, 100);
                reachable1 = depGraph.getReachableIndices(gov, true, 10);
                for (Integer i : reachable2) {
                    if (s.get(i).word().contains("PROTEIN")) {
                        entitySet2.add(i);
                    }
                }
                for (Integer i : reachable1) {
                    if (s.get(i).word().contains("PROTEIN") && !entitySet2.contains(i)) {
                        entitySet1.add(i);
                    }
                }
            } else {
                if (isNegated(gov, depGraph)) continue;
                reachable2 = depGraph.getReachableIndices(gov, true, 100);
                reachable1 = depGraph.getReachableIndices(dep, true, 10);
                for (Integer i : reachable1) {
                    if (s.get(i).word().contains("PROTEIN")) {
                        entitySet1.add(i);
                    }
                }
                for (Integer i : reachable2) {
                    if (s.get(i).word().contains("PROTEIN") && !entitySet1.contains(i)) {
                        entitySet2.add(i);
                    }
                }
            }
            Set<String> set = new HashSet<String>();
            for (Integer i : entitySet1) {
                for (Integer j : entitySet2) {
                    int entity1 = i;
                    int entity2 = j;
                    if (i > j) {
                        entity1 = j;
                        entity2 = i;
                    }
                    String key = String.valueOf(entity1) + "-" + String.valueOf(entity2);
                    if (set.contains(key)) {
                        System.out.println("duplicate!");
                        continue;
                    }
                    else {
                        set.add(key);
                    }
                    String sentPattern = getSentPattern(s, entity1, entity2);
                    if (form1Pat.matcher(sentPattern).find()) {
                        System.out.println("Form1");
                        pairs.add(new Pair<Integer, Integer>(entity1, entity2));
                    } else if (form2Pat.matcher(sentPattern).find()) {
                        System.out.println("Form2");
                        pairs.add(new Pair<Integer, Integer>(entity1, entity2));
                    } /*else if (form3Pat.matcher(sentPattern).find()) {
                        System.out.println("Form3");
                        pairs.add(new Pair<Integer, Integer>(entity1, entity2));
                    } else if (form5Pat.matcher(sentPattern).find()) {
                        System.out.println("Form5");
                        pairs.add(new Pair<Integer, Integer>(entity1, entity2));
                    } */                   
                }
            }
            
            //form5
            //PRO and PRO REL
            for (Integer i : entitySet1) {
                for (Integer j : entitySet1) {
                    if (i < j) {
                        String sentPattern = getSentPattern(s, i, j);
                        if (form5Pat.matcher(sentPattern).find()) {
                            System.out.println("Form5");                            
                            int lcs = depGraph.getLCS(i, j);
                            if (s.get(lcs).getTag("DOMAIN") != null) {
                                pairs.add(new Pair<Integer, Integer>(i, j));
                            }                            
                        }
                    }
                }
            }
            
            //form3
            //REL PRO1 PRO2
            for (Integer i : entitySet2) {
                for (Integer j : entitySet2) {
                    if (i < j) {
                        String sentPattern = getSentPattern(s, i, j);
                        if (form3Pat.matcher(sentPattern).find()) {
                            System.out.println("Form3");
                            int lcs = depGraph.getLCS(i, j);
                            if (s.get(lcs).getTag("DOMAIN") != null) {
                                pairs.add(new Pair<Integer, Integer>(i, j));
                            }
                            
                        }
                    }
                }
            }
        }
        return pairs;
    }
    boolean isNegated(int index, SimpleDepGraph depGraph) {
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(index);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().endsWith("neg")) return true;
        }
        return false;
    }
    void getReachableNodes(int start, Sentence s, SimpleDepGraph depGraph, boolean directed) {
        int size = s.size();
        boolean[] visited = new boolean[size];
        List<Integer> reachables = new ArrayList<Integer>();
    }
    private String getSentPattern(Sentence s, int entity1, int entity2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.size(); i++) {
            if (i == entity1 || i == entity2) {
                sb.append(s.get(i).word());
            } else if (s.get(i).getTag("DOMAIN") != null) {
                sb.append("REL" + s.get(i).getTag("POS").substring(0, 1)); //RELV or RELN
            } else if (s.get(i).word().matches("[/,-]")) {
                sb.append(s.get(i).word());
            } else if (s.get(i).getTag("POS").matches("IN|TO")) {
                sb.append("PREP");
            } else if (s.get(i).word().matches("and|or")) {
                sb.append("CONJ");
            } else {
                sb.append("W");
            }
            sb.append(".");
        }
        return sb.toString();
    }
    private void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    public List<Pair<Integer, Integer>> getCandidates(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();
        pairs.addAll(applyRule1(s, depGraph, ccDepGraph));
        return pairs;
    }
}
