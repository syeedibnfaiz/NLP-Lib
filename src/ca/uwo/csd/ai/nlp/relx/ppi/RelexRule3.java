/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelexRule3 extends RelexRule {
    static final Pattern PATTERN = Pattern.compile("RELW?(PREP)W?PROT1W?(PUNC|CONJ|PROT|REL)*(PREP)W?PROT2");
    
    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();
        int domainTermPos = -1;
        for (int i = 0; i < s.size(); i++) {
            if (((i-domainTermPos) < 5) && s.get(i).word().contains("PROTEIN")) {
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        if (check(s, ccDepGraph, i, j)) {
                            relations.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                }
            }
            if (s.get(i).getTag("DOMAIN") != null) {
                domainTermPos = i;
            }
        }
        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).getTag("POS").matches("N.*") && s.get(i).getTag("DOMAIN") != null) {
                Set<Integer> reachables = new HashSet<Integer>();
                getReachables(s, ccDepGraph, reachables, i, new boolean[s.size()], true, "agent|prep_(of|by|to|on|for|in|through|with|between)|nn|amod");
                for (int e1 : reachables) {
                    for (int e2 : reachables) {
                        if (e1 < e2 && i < e1) {
                            relations.add(new Pair<Integer, Integer>(e1, e2));
                        }
                    }
                }
            }
        }
        //partmod
        List<SimpleDependency> dependencies = ccDepGraph.getDependencies("partmod");
        for (SimpleDependency dependency : dependencies) {
            int gov = dependency.gov();
            int dep = dependency.dep();
            if (s.get(dep).getTag("DOMAIN") != null) {
                Set<Integer> reachableTargets = new HashSet<Integer>();
                getReachables(s, ccDepGraph, reachableTargets, dep, new boolean[s.size()], true, "agent|prep_(of|by|to|on|for|in|through|with|between)|nn|amod");
                Set<Integer> reachableAgents = new HashSet<Integer>();
                getReachables(s, ccDepGraph, reachableAgents, gov, new boolean[s.size()], true, "agent|prep_(of|by|to|on|for|in|through|with|between)|nn|amod");
                for (int agent : reachableAgents) {
                    for (int target : reachableTargets) {
                        if (agent < target) {
                            relations.add(new Pair<Integer, Integer>(agent, target));
                        }
                    }
                }
            }
        }                
        
        return relations;
    }
    
    private boolean check(Sentence s, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        List<String> pathList = ccDepGraph.getPathAsList(entity1, entity2, false);                
        if (pathList != null) {
            for (int k = 1; k < pathList.size(); k++) {
                if (pathList.get(k).matches("-?prep_between") && pathList.get(k - 1).matches("-?prep_between")) {                    
                    return true;                    
                }
            }
        }
        return false;
    }
    private void getReachables(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> targets, int index, boolean[] visited, boolean domainSpecific, String relnPattern) {
        if (visited[index]) return;
        visited[index] = true;
        if (domainSpecific && s.get(index).word().contains("PROTEIN")) {
            targets.add(index);
        }
        if (!domainSpecific && s.get(index).getTag("DOMAIN") != null) {
            domainSpecific = true;
        }
        for (SimpleDependency dependency : ccDepGraph.getGovDependencies(index)) {            
            int dep = dependency.dep();
            String reln = dependency.reln();
            if (relnPattern.equals("*") || reln.matches(relnPattern)) {
                getReachables(s, ccDepGraph, targets, dep, visited, domainSpecific, relnPattern);
            }
        }
    }
    private String getSentPattern(Sentence s, int entity1, int entity2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.size(); i++) {
            if (i == entity1) {
                sb.append("PROT1");
            } else if (i == entity2) {
                sb.append("PROT2");
            } else if (s.get(i).word().contains("PROTEIN")) {
                sb.append("PROT");
            } else if (s.get(i).getTag("DOMAIN") != null) {
                sb.append("REL");
            } else if (s.get(i).word().matches("[/,:;-]")) {
                sb.append("PUNC");
            } else if (s.get(i).getTag("POS").matches("IN|TO")) {
                sb.append("PREP");
            } else if (s.get(i).word().matches("and|or")) {
                sb.append("CONJ");
            } else {
                sb.append("W");
            }            
        }
        return sb.toString();
    }
}
