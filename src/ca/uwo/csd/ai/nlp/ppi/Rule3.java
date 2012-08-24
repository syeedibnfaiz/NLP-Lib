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
import java.util.List;
import java.util.Set;

/**
 *
 * @author tonatuni
 */
public class Rule3 implements Rule {

    @Override
    public Set<Pair<Integer, Integer>> getCandidates(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        Set<Pair<Integer, Integer>> relationPairs = new HashSet<Pair<Integer, Integer>>();
        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        List<String> pathList = ccDepGraph.getPathAsList(i, j, false);
                        List<Integer> pathIndices = ccDepGraph.getPathAsIndexList(i, j, false);
                        boolean domain = false;
                        if (pathList != null) {
                            for (int k = 1; k < pathList.size(); k++) {                                
                                if (insideDomainPhrase(s, pathIndices.get(k), depGraph)) {
                                    domain = true;
                                }
                                if (pathList.get(k).matches("-?prep_between") && pathList.get(k - 1).matches("-?prep_between")) {
                                    relationPairs.add(new Pair<Integer, Integer>(i, j));
                                    break;
                                }/* else if (pathList.get(k).matches("-?prep.*") && pathList.get(k - 1).matches("-?prep.*")) {
                                    if (domain) {
                                        relationPairs.add(new Pair<Integer, Integer>(i, j));
                                        break;
                                    }
                                }*/
                            }
                        }
                    }
                }
            }
        }
        return relationPairs;
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
