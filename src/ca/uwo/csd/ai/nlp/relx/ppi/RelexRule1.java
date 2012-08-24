/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <code>Rule1</code> applies rule1 of RelEx.
 * @author Syeed Ibn Faiz
 */
public class RelexRule1 extends RelexRule {
    //PATH_PATTERN is the path-chainging pattern that is applied when traversing the dep-graph starting from the seed
    //private static final Pattern PATH__PATTERN = Pattern.compile("dep|agent|.*comp|.*obj|advcl|(inf|part|rc)mod|prep.*");    
    private static final Pattern PATH__PATTERN = Pattern.compile("dep|agent|.*comp|.*obj|advcl|(inf|part|rc)mod|prep.*|abbrev|parataxis");    
    private static final Pattern AGENT_NP_PATTERN = Pattern.compile("nn|amod|abbrev");
    private static final Pattern TARGET_NP_PATTERN = Pattern.compile("nn|amod|advmod");    
    private Set<String> restrictionTerms;        
    
    public RelexRule1(Set<String> restrictionTerms) {
        this.restrictionTerms = restrictionTerms;
    }
        
    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph depGraph) {
        List<List<SimpleDependency>> paths = getCandidatePaths(s, depGraph);
        return filterCandidatePaths(s, depGraph, paths);        
    }
    
    List<List<SimpleDependency>> getCandidatePaths(Sentence s, SimpleDepGraph depGraph) {
        List<List<SimpleDependency>> paths = getAllPaths(s, depGraph, "nsubj|nsubjpass", PATH__PATTERN);                
        return paths;
    }
    
    /**
     * Changes only one line, explores with the gov of seed instead of its dep.
     * @param s
     * @param depGraph
     * @param startPattern
     * @param pathpattern
     * @return 
     */
    @Override
    List<List<SimpleDependency>> getAllPaths(Sentence s, SimpleDepGraph depGraph, String startPattern, Pattern pathpattern) {
        
        List<List<SimpleDependency>> paths = new ArrayList<List<SimpleDependency>>();
        List<SimpleDependency> seeds = depGraph.getDependencies(startPattern);
        
        for (SimpleDependency seed : seeds) {            
            List<SimpleDependency> curPath = new ArrayList<SimpleDependency>();
            curPath.add(seed);
            //addPath(depGraph, seed.gov(), pathpattern, curPath, paths, new boolean[s.size()]);
            addPath(seed.gov(), depGraph, curPath, paths, pathpattern, new boolean[s.size()]);
        }
        return paths;
    }        
    
    List<Pair<Integer, Integer>> filterCandidatePaths(Sentence s, SimpleDepGraph depGraph, List<List<SimpleDependency>> paths) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();
        for (List<SimpleDependency> path : paths) {            
            relations.addAll(filterCandidatePath(s, depGraph, path));
        }
        return relations;
    }
    
    List<Pair<Integer, Integer>> filterCandidatePath(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();
        //showPath(s, path);
        Set<Integer> agents = getAgents(path.get(0).dep(), s, depGraph, path);        
        if (!agents.isEmpty()) {
            Set<Pair<Integer,Integer>> targets = getTargets(s, depGraph, path);
            
            for (Pair<Integer,Integer> target : targets) {
                /*System.out.print("target: " + s.get(target.first()).word());
                System.out.print("\tcorrect order: "+checkRoleOrder(s, depGraph, path, target.second()));
                System.out.print("\tdomain specific: "+isDomainSpecific(s, depGraph, path, target.second()));
                System.out.println("\tnegated: "+isNegated(s, depGraph, path, target.second()));*/
                if ( isDomainSpecific(s, depGraph, path, target.second()) && !isNegated(s, depGraph, path, target.second())) {                    
                    for (Integer agent : agents) {
                        if (agent < target.first()) {
                            relations.add(new Pair<Integer, Integer>(agent, target.first()));
                        } else {
                            relations.add(new Pair<Integer, Integer>(target.first(), agent));
                        }                        
                    }                    
                }
            }            
        }
        return relations;
    }
    
    private Set<Integer> getAgents(int subj, Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> candidatePath) {
        List<SimpleDependency> dependencies;
        Set<Integer> agents = new HashSet<Integer>();        
        agents.addAll(getEntitiesFromNP(subj, s, depGraph, AGENT_NP_PATTERN));
        
        //handle PP
        if (true/*agents.isEmpty()*/) {
            List<List<SimpleDependency>> prepPaths = //dep?
                    //getPathsFrmSrc(s, subj, Pattern.compile("prep.*"), depGraph, Pattern.compile("prep.*|agent"));
                    getPathsFrmSrc(s, subj, Pattern.compile("prep.*|partmod"), depGraph, Pattern.compile("prep.*|agent|dobj|dep"));
            //showPaths(s, prepPaths);           
            
            for (Integer agent : agents) {
                if (agent != subj) {
                    prepPaths.addAll(getPathsFrmSrc(s, agent, Pattern.compile("prep.*|partmod"), depGraph, Pattern.compile("prep.*|agent|dobj")));
                }
            }
            for (List<SimpleDependency> path : prepPaths) {
                //take only the last dependent of a prep_of|prep_like|prep_including relation
                int prepHead = -1;
                for (SimpleDependency sDep : path) {
                    if (sDep.reln().matches("prep_(of|from|like|including|in)")) {                    
                        int anNPHead = sDep.dep();                        
                        prepHead = Math.max(prepHead, anNPHead);
                    }
                }
                if (prepHead != -1) {                    
                    agents.addAll(getEntitiesFromNP(prepHead, s, depGraph, AGENT_NP_PATTERN));
                }
            }            
        }
        
        //handle rcmod
        //do not consider an rcmod if the seed and the rcmod shares the same governor
        int mainHead = -1;
        if (!candidatePath.isEmpty()) {
            mainHead = candidatePath.get(0).gov();
        }
        dependencies = depGraph.getGovDependencies(subj, Pattern.compile("rcmod"));
        List<Integer> rcmodAgents = new ArrayList<Integer>();
        for (SimpleDependency sDep : dependencies) {
            int dep = sDep.dep();
            String rcHead = s.get(dep).word().toLowerCase();
            if (dep != mainHead && (rcHead.matches("similar|include(s|d)?|members?|identical|involve(s|d)?")/* || s.get(dep).getTag("DOMIN") != null)*/)) {
                List<List<SimpleDependency>> rcmodPaths = getPathsFrmSrc(s, dep, Pattern.compile(".*obj|prep.*"), depGraph, Pattern.compile(".*obj|prep.*"));
                for (List<SimpleDependency> path : rcmodPaths) {
                    for (SimpleDependency tDep : path) {
                        int head = tDep.dep();                        
                        List<Integer> tmpAgents = getEntitiesFromNP(head, s, depGraph, AGENT_NP_PATTERN);
                        if (!tmpAgents.isEmpty()) {
                            rcmodAgents.addAll(tmpAgents);
                            break; //arbitrary decision
                        }
                    }
                }
            }
        }        
        agents.addAll(rcmodAgents);
        
        return agents;
    }
    
    private Set<Pair<Integer, Integer>> getTargets(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> candidatePath) {
        Set<Pair<Integer, Integer>> targets = new HashSet<Pair<Integer, Integer>>();
        //StringBuilder relnChain = new StringBuilder();        
        for (int i = 1; i < candidatePath.size(); i++) {
            int head = candidatePath.get(i).dep();
            //filter rcmod tail
            String reln = candidatePath.get(i).reln();
            if (reln.matches("rcmod")) {
                String rcHead = s.get(head).word().toLowerCase();
                if (!rcHead.matches("similar|include(s|d)?|members?|identical|involve(s|d)?") && s.get(head).getTag("DOMAIN") == null) {
                    break;
                }
            }
            
            if (!s.get(head).getTag("POS").startsWith("NN")) {
                continue;
            }
            //targets.addAll(getEntitiesFromNP(head, s, depGraph, TARGET_NP_PATTERN));
            List<Integer> entities = getEntitiesFromNP(head, s, depGraph, TARGET_NP_PATTERN);
            for (Integer e : entities) {
                targets.add(new Pair<Integer, Integer>(e, i));
            }
        }
        return targets;
    }
        
    @Override
    String getChunkStr(Pair<Integer,Integer> chunk, Sentence s) {
        StringBuilder chunkStr = new StringBuilder();
        for (int j = chunk.first(); j <= chunk.second(); j++) {
            if (s.get(j).getTag("POS").matches("(VB|NN|JJ).*") && !s.get(j).word().matches("belong(s|ed)?|requires?|effect|provided|effect|result|member|response|control|depend(s|ed|ent)?")) {
                chunkStr.append(s.get(j).word() + "<<" + s.get(j).getTag("POS") + " ");
            } else {
                chunkStr.append(s.get(j).word()).append(" ");
            }
        }
        return chunkStr.toString();
    }
        
    private int getEntityFollowingLinks(int head, Sentence s, SimpleDepGraph depGraph, Pattern linkPattern) {
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(head, linkPattern);
        for (SimpleDependency sDep : dependencies) {            
            int agent = getHeadEntityFromNP(sDep.dep(), s, depGraph, AGENT_NP_PATTERN);
            if (agent != -1) {
                return agent;
            }
        }
        return -1;
    }
    private boolean isNegated(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path, int endIndex) {
        for (int i = 0; i <= endIndex; i++) {
            int gov = path.get(i).gov();
            int dep = path.get(i).dep();
            if (!depGraph.getGovDependencies(gov, Pattern.compile("neg")).isEmpty()) {
                return true;
            }
            if (!depGraph.getGovDependencies(dep, Pattern.compile("neg")).isEmpty()) {
                return true;
            }
        }
        return false;
    }
    private boolean isDomainSpecific(Sentence s, Pair<Integer, Integer> chunk) {
        for (int i = chunk.first(); i <= chunk.second(); i++) {
            /*if (restrictionTerms.contains(s.get(i).word().toLowerCase())) {
                return true;
            }*/
            if (s.get(i).getTag("DOMAIN") != null) {
                return true;
            }
        }
        return false;
    }
    private boolean isDomainSpecific(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path, int endIndex) {
        for (int i = 0; i <= endIndex; i++) {
            int gov = path.get(i).gov();
            int dep = path.get(i).dep();
            if (s.get(gov).getTag("DOMAIN") != null || s.get(dep).getTag("DOMAIN") != null) return true;
            if (i == 0) {
                if (isDomainSpecific(s, getNPChunk(dep, s, depGraph))) {
                    return true;
                }                
            } else {
                if (isDomainSpecific(s, getNPChunk(gov, s, depGraph))) return true;
                if (i == endIndex) {
                    if (isDomainSpecific(s, getNPChunk(dep, s, depGraph))) return true;
                    break;
                }
            }
        }
        return false;
    }
    
    @Override
    public void showPath(Sentence s, List<SimpleDependency> path) {        
        for (int i = 0; i < path.size(); i++) {
            SimpleDependency sDep = path.get(i);
            int gov = sDep.gov();
            int dep = sDep.dep();
            if (i == 0) {
                System.out.print(s.get(dep).word() +"-["+sDep.reln()+"]-");
            } else {
                System.out.print(s.get(gov).word() +"-["+sDep.reln()+"]-");
                if (i == (path.size() - 1)) {
                    System.out.print(s.get(dep).word());
                }
            }            
        }
        System.out.println("");
    }
    
    public static Set<String> getRestrictionTerms() {
        Set<String> terms = new HashSet<String>();
        List<String> lines = Util.readLines("./resource/relation/LLL/relex_restriction_terms.txt");
        for (String line : lines) {
            int pos = line.indexOf(':');
            String tokens[] = line.substring(pos + 1).split("\\|");
            terms.addAll(Arrays.asList(tokens));
        }
        return terms;
    }
    
    public static void main(String args[]) {        
    }
}
