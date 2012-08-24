/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Rule2 extends Rule {
    
    final static Pattern pathPattern = Pattern.compile("prep.(of|by|to|on|for|in|through|with)");
    static final Pattern NP_PATTERN = Pattern.compile("nn|amod|advmod");
    private Set<String> restrictionTerms;

    public Rule2(Set<String> restrictionTerms) {
        this.restrictionTerms = restrictionTerms;
    }
    
    @Override
    public List<Relation> findRelations(Sentence s, SimpleDepGraph depGraph) {
        List<List<SimpleDependency>> paths = getAllPaths(s, depGraph, "prep.(of|by|to|on|for|in|through|with)", pathPattern);        
        return filterCandidatePaths(s, depGraph, paths);
    }
    
    List<Relation> filterCandidatePaths(Sentence s, SimpleDepGraph depGraph, List<List<SimpleDependency>> paths) {
        List<Relation> relations = new ArrayList<Relation>();
        boolean used[][] = new boolean[s.size()][s.size()];
        for (List<SimpleDependency> path : paths) {              
            showPath(s, path);
            relations.addAll(filterCandidatePath(s, depGraph, path));
        }
        return relations;
    }
    
    //TODO: need to override getall paths to get rid of redundant paths
    List<Relation> filterCandidatePath(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path) {
        List<Relation> relations = new ArrayList<Relation>();
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
                System.out.println("not domain specific!");
                continue;
            }
            
            String reln = sDep.reln();
            for (Integer agent : agents) {
                for (Integer target : targets) {
                    if (reln.endsWith("by")) { //passive
                        relations.add(new Relation(target, agent));
                        System.out.println(s.get(target).word() +"-"+s.get(agent).word());
                    } else {
                        relations.add(new Relation(agent, target));
                        System.out.println(s.get(agent).word() +"-"+s.get(target).word());
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
            if (govPOS.matches("NN.*")) {                
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
            }
        }
        return false;
    }
}
