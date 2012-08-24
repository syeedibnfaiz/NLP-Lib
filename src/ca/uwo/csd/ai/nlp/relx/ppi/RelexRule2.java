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
public class RelexRule2 extends RelexRule {
    
    final static Pattern pathPattern = Pattern.compile("prep_(of|by|to|on|for|in|through|with)");
    static final Pattern NP_PATTERN = Pattern.compile("nn|amod|advmod");
    private Set<String> restrictionTerms;

    public RelexRule2(Set<String> restrictionTerms) {
        this.restrictionTerms = restrictionTerms;
    }
    
    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph depGraph) {
        List<List<SimpleDependency>> paths = getAllPaths(s, depGraph, "prep_(of|by|to|on|for|in|through|with)", pathPattern);        
        return filterCandidatePaths(s, depGraph, paths);
    }
    
    List<Pair<Integer, Integer>> filterCandidatePaths(Sentence s, SimpleDepGraph depGraph, List<List<SimpleDependency>> paths) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();
        boolean used[][] = new boolean[s.size()][s.size()];
        for (List<SimpleDependency> path : paths) {              
            //showPath(s, path);
            relations.addAll(filterCandidatePath(s, depGraph, path));
        }
        return relations;
    }
    
    //TODO: need to override getall paths to get rid of redundant paths
    List<Pair<Integer, Integer>> filterCandidatePath(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < path.size(); i++) {
            SimpleDependency sDep = path.get(i);
            int gov = sDep.gov();
            int dep = sDep.dep();
            String govPOS = s.get(gov).getTag("POS");
            String depPOS = s.get(dep).getTag("POS");
            if (!govPOS.matches("NN.*") || !depPOS.matches("NN.*")) {
                continue;
            }
            Set<Integer> agents = new HashSet<Integer>();
            agents.addAll(getEntitiesFromNP(gov, s, depGraph, NP_PATTERN));
            
            Set<Integer> targets = new HashSet<Integer>();
            targets.addAll(getEntitiesFromNP(dep, s, depGraph, NP_PATTERN));
            if (agents.isEmpty() || targets.isEmpty()) continue;
            
            
            if (!isDomainSpecific(s, depGraph, path.subList(0, i + 1))) {                
                continue;
            }
            
            String reln = sDep.reln();
            for (Integer agent : agents) {
                for (Integer target : targets) {                    
                    if (agent < target) {
                        relations.add(new Pair<Integer, Integer>(agent, target));
                    } else {
                        relations.add(new Pair<Integer, Integer>(target, agent));
                    }
                }
            }
        }
        return relations;
    }
    
    public boolean isDomainSpecific(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path) {
        for (int i = 0; i < path.size(); i++) {
            SimpleDependency sDep = path.get(i);
            int gov = sDep.gov();
            //int dep = sDep.dep();
            String govPOS = s.get(gov).getTag("POS");
            //String depPOS = s.get(dep).word();
            /*if (govPOS.matches("NN.*")) {                
                String chunkTokens[] = getChunkStr(getNPChunk(gov, s, depGraph), s).split("[-\\s]+");
                if (chunkTokens != null) {
                    for (String token : chunkTokens) {
                        if (restrictionTerms.contains(token.toLowerCase())) {
                            System.out.println("found " + token);
                            return true;
                        }
                    }
                }
            } else {
                String word = s.get(gov).word();
                if (restrictionTerms.contains(word.toLowerCase())) {
                    System.out.println("found " + word);
                    return true;
                }
            }*/
            if (govPOS.matches("N.*")) {
                Pair<Integer, Integer> chunk = getNPChunk(gov, s, depGraph);
                for (int j = chunk.first(); j <= chunk.second(); j++) {
                    if (s.get(j).getTag("DOMAIN") != null) {
                        return true;
                    }
                }
            } else if (s.get(i).getTag("DOMAIN") != null) {
                return true;
            }
        }
        return false;
    }
}
