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
import java.util.List;
import java.util.regex.Pattern;

/**
 * <code> Rule </code> represents the skeleton of a rule.
 * @author Syeed Ibn Faiz
 */
public abstract class RelexRule {
    
    
    public abstract List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph depGraph);
    
    public List<List<SimpleDependency>> getPathsFrmSrc(Sentence s, int start, Pattern startPattern, SimpleDepGraph depGraph, Pattern pathPattern) {
        List<List<SimpleDependency>> paths = new ArrayList<List<SimpleDependency>>(); 
        
        List<SimpleDependency> seeds = depGraph.getGovDependencies(start, startPattern);
        for (SimpleDependency seed : seeds) {
            List<SimpleDependency> curPath = new ArrayList<SimpleDependency>();
            curPath.add(seed);
            addPath(seed.dep(), depGraph, curPath, paths, pathPattern, new boolean[s.size()]);
        }
        
        return paths;
    }
    
    /**
     * Finds all paths in the dependency tree that start with a relation having a specified pattern
     * and the following links also satisfy another fixed pattern
     * @param s
     * @param depGraph
     * @param startPattern
     * @param pathpattern
     * @return 
     */
    List<List<SimpleDependency>> getAllPaths(Sentence s, SimpleDepGraph depGraph, String startPattern, Pattern pathpattern) {        
        List<List<SimpleDependency>> paths = new ArrayList<List<SimpleDependency>>();
        
        List<SimpleDependency> seeds = depGraph.getDependencies(startPattern);        
        for (SimpleDependency seed : seeds) {            
            List<SimpleDependency> curPath = new ArrayList<SimpleDependency>();
            curPath.add(seed);
            addPath(seed.dep(), depGraph, curPath, paths, pathpattern, new boolean[s.size()]);
        }
        return paths;
    }
    
    void addPath(int gov, SimpleDepGraph depGraph, List<SimpleDependency> curPath, 
            List<List<SimpleDependency>> paths, Pattern pathPattern, boolean visited[]) {
        
        if (visited[gov]) return;        
        visited[gov] = true;
        
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(gov, pathPattern);
        if (dependencies.isEmpty()) { //terminal node, save path
            List<SimpleDependency> newPath = new ArrayList<SimpleDependency>(curPath);
            paths.add(newPath);
        } else {
            for (SimpleDependency sDep:dependencies) {
                curPath.add(sDep);
                addPath(sDep.dep(), depGraph, curPath, paths, pathPattern, visited);
                curPath.remove(curPath.size() - 1);
            }
        }
        visited[gov] = false;
    }
    
    /**
     * Finds the rightmost entity in a noun phrase given its head
     * @param head
     * @param s
     * @param depGraph
     * @param includeHead
     * @return 
     */
    int getHeadEntityFromNP(int head, Sentence s, SimpleDepGraph depGraph, Pattern pattern) {
        int entity = -1;
        List<Integer> reachable = new ArrayList<Integer>();
        reachable.add(head);
        
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(head, pattern);
        for (SimpleDependency sDep:dependencies) {
            reachable.add(sDep.dep());
        }        
        //choose the rightmost entity            
        for (Integer pos : reachable) {
            if (s.get(pos).word().contains("PROTEIN")) {
                if (pos > entity) {
                    entity = pos;
                }
            }
        }
        return entity;
    }
    
    /**
     * Finds agents given the head of an NP.
     * It first tries to find in the chunk, then in appositive.
     * @param head
     * @param s
     * @param depGraph
     * @return 
     */
    List<Integer> getEntitiesFromNP(int head, Sentence s, SimpleDepGraph depGraph, Pattern pattern) {
        List<Integer> agents = new ArrayList<Integer>();
        int agent = getHeadEntityFromNP(head, s, depGraph, pattern);
        List<SimpleDependency> dependencies;
        if (agent != -1) {
            agents.add(agent);            
            if (agent != head) {
                //now since the agent is different from head, there is possibility
                //that this agent appears in conjunction with another entity
                //eg. tagA and tagD genes were...
                //now since the head of a coordination list is the first conjunct
                //we need to look for incoming conj.* dependencies towards agent
                dependencies = depGraph.getDepDependencies(agent, Pattern.compile("conj.*"));
                for (SimpleDependency sDep : dependencies) {
                    int gov = sDep.gov();
                    int nextAgent = getHeadEntityFromNP(gov, s, depGraph, pattern);
                    if (nextAgent != -1) {
                        agents.add(nextAgent);
                    }
                }
            }            
        }
        //handle appos if no agent in nominal subject
        if (agents.isEmpty()) {
            dependencies = depGraph.getGovDependencies(head, Pattern.compile("appos"));
            for (SimpleDependency sDep : dependencies) {                
                int apposAgent = getHeadEntityFromNP(sDep.dep(), s, depGraph, pattern);
                if (apposAgent != -1) {
                    agents.add(apposAgent);
                }
            }
        }
        return agents;
    }
    
    Pair<Integer, Integer> getNPChunk(int pos, Sentence s, SimpleDepGraph depGraph) {
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(pos, Pattern.compile("nn|amod|advmod"));
        dependencies.addAll(depGraph.getDepDependencies(pos, Pattern.compile("nn|amod|advmod")));
        int min = pos;
        int max = pos;
        for (SimpleDependency sDep : dependencies) {
            min = Math.min(min, sDep.dep());
            min = Math.min(min, sDep.gov());
            max = Math.max(max, sDep.dep());
            max = Math.max(max, sDep.gov());
        }
        if (max == -1) return null;
        return new Pair<Integer, Integer>(min, max);
    }
    
    String getChunkStr(Pair<Integer,Integer> chunk, Sentence s) {
        StringBuilder chunkStr = new StringBuilder();
        for (int j = chunk.first(); j <= chunk.second(); j++) {            
            chunkStr.append(s.get(j).word()).append(" ");
        }
        return chunkStr.toString();
    }
    
    public void showPaths(Sentence s, List<List<SimpleDependency>> paths) {
        for (List<SimpleDependency> path : paths) {
            showPath(s, path);
        }
    }
    
    public void showPath(Sentence s, List<SimpleDependency> path) {
        for (SimpleDependency sDep : path) {
            int gov = sDep.gov();
            int dep = sDep.dep();
            System.out.print(s.get(gov).word() +"-["+sDep.reln()+"]-"+s.get(dep).word() + " ");
        }
        System.out.println("");
    }
}
