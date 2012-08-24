/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class Rule2 implements Rule {

    @Override
    public Set<Pair<Integer, Integer>> getCandidates(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        Set<Pair<Integer, Integer>> relationPairs = new HashSet<Pair<Integer, Integer>>();
        
        for (int i = 0; i < s.size(); i++) {
            if (insideDomainPhrase(s, i, depGraph) || s.get(i).word().contains("PROTEIN")) {                                
                boolean[] visited = new boolean[s.size()];
                Set<Integer> agents = new HashSet<Integer>();
                getReachables(s, ccDepGraph, agents, i, visited, true, "agent|prep.*|appos|abbrev|nn|amod|dep");
                for (Integer e1 : agents) {
                    for (Integer e2 : agents) {
                        if (e1 < e2) {
                            relationPairs.add(new Pair<Integer, Integer>(e1, e2));
                        }
                    }
                }
            }
        }
        return relationPairs;
    }
    
    private void getReachables(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> targets, int index, boolean[] visited, boolean domainSpecific, String relnPattern) {
        if (visited[index]) return;
        visited[index] = true;
        if (s.get(index).word().contains("PROTEIN")) {
            targets.add(index);
        }
        if (!domainSpecific && s.get(index).getTag("DOMAIN") != null) {
            domainSpecific = true;
        }
        for (SimpleDependency dependency : ccDepGraph.getGovDependencies(index)) {            
            int dep = dependency.dep();
            String reln = dependency.reln();
            if (reln.matches(relnPattern)) {
                getReachables(s, ccDepGraph, targets, dep, visited, domainSpecific, relnPattern);
            }
        }
    }
    boolean insideDomainPhrase(Sentence s, int index, SimpleDepGraph depGraph) {
        if (s.get(index).getTag("DOMAIN") != null) return true;
        for (SimpleDependency dependency : depGraph.getGovDependencies(index)) {
            int dep = dependency.dep();
            if (s.get(dep).getTag("DOMAIN") != null) {
                return true;
            }
        }  
        for (SimpleDependency dependency : depGraph.getDepDependencies(index)) {
            int gov = dependency.gov();
            if (s.get(gov).getTag("DOMAIN") != null) {
                return true;
            }
        }  
        return false;
    }
}
