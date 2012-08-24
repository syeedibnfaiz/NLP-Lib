/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

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
 * @author tonatuni
 */
public class Rule1 implements Rule {

    @Override
    public Set<Pair<Integer, Integer>> getCandidates(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        Set<Pair<Integer, Integer>> relationPairs = new HashSet<Pair<Integer, Integer>>();
        List<SimpleDependency> dependencies = ccDepGraph.getDependencies(".*subj|.*subjpass|rcmod");
        for (SimpleDependency seed : dependencies) {
            int gov = seed.gov();
            int dep = seed.dep();            
            if (!isNegated(gov, depGraph) && !isNegated(dep, depGraph)) { //no negation of the main verb
                Set<Integer> agents = new HashSet<Integer>();
                Set<Integer> targets = new HashSet<Integer>();
                boolean[] visited = new boolean[s.size()];
                if (!seed.reln().matches("nsubj|nsubjpass")) {
                    getTargets(s, ccDepGraph, targets, dep, visited, false, "NONE");
                    getAgents(s, ccDepGraph, agents, gov, visited, "nn|amod|appos|conj.*|prep.*|dobj|dep|abbrev");
                } else {                    
                    List<Integer> reachableIndices = depGraph.getReachableIndices(dep, true, 100);
                    for (Integer r : reachableIndices) {
                        visited[r] = true;
                    }                    
                    getTargets(s, ccDepGraph, targets, gov, visited, false, "NONE");

                    for (Integer r : reachableIndices) {
                        visited[r] = false;
                    }                       
                    visited[dep] = false;
                    getAgents(s, ccDepGraph, agents, dep, visited, "nn|amod|appos|conj.*|prep.*|dobj|dep|abbrev|partmod");
                }
                for (Integer agent : agents) {
                    for (Integer target : targets) {
                        int entity1 = agent;
                        int entity2 = target;
                        if (agent > target) {
                            entity1 = target;
                            entity2 = agent;
                        }
                        relationPairs.add(new Pair<Integer, Integer>(entity1, entity2));
                    }
                }
            }
        }
        return relationPairs;
    }
    
    private void getTargets(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> targets, int index, boolean[] visited, boolean domainSpecific, String filterReln) {
        if (visited[index]) return;
        visited[index] = true;
        if (/*(domainSpecific || insideDomainPhrase(s, index, ccDepGraph)) && */ s.get(index).word().contains("PROTEIN")) {
            targets.add(index);
        }
        if (!domainSpecific && s.get(index).getTag("DOMAIN") != null) {
            domainSpecific = true;
        }
        for (SimpleDependency dependency : ccDepGraph.getGovDependencies(index)) {            
            int dep = dependency.dep();
            String reln = dependency.reln();
            if (!reln.matches(filterReln)) {
                getTargets(s, ccDepGraph, targets, dep, visited, domainSpecific, filterReln);
            }
        }
    }

    private void getAgents(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> agents, int index, boolean[] visited, String relnPattern) {
        if (visited[index]) return;
        visited[index] = true;
        if (s.get(index).word().contains("PROTEIN")) {
            agents.add(index);
        }
        for (SimpleDependency dependency : ccDepGraph.getGovDependencies(index)) {            
            if (dependency.reln().matches(relnPattern)) {
                int dep = dependency.dep();
                getAgents(s, ccDepGraph, agents, dep, visited, relnPattern);
            }
        }
        //head of a coordination list is the first conjunct
        for (SimpleDependency dependency : ccDepGraph.getDepDependencies(index, Pattern.compile("abbrev|appos|conj.*"))) {
            int gov = dependency.gov();
            getAgents(s, ccDepGraph, agents, gov, visited, relnPattern);
        }
    }
    boolean isNegated(int index, SimpleDepGraph depGraph) {
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(index);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().endsWith("neg")) return true;
        }
        return false;
    }
    boolean insideDomainPhrase(Sentence s, int index, SimpleDepGraph depGraph) {
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
