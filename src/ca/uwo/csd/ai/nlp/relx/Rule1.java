/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ExtendedDepGraph;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <code>Rule1</code> applies rule1 of RelEx.
 * @author Syeed Ibn Faiz
 */
public class Rule1 extends Rule {
    //PATH_PATTERN is the path-chainging pattern that is applied when traversing the dep-graph starting from the seed
    private static final Pattern PATH__PATTERN = Pattern.compile("dep|agent|.*comp|.*obj|advcl|(inf|part|rc)mod|prep.*");
    private static final Pattern AGENT_NP_PATTERN = Pattern.compile("nn|amod");
    private static final Pattern TARGET_NP_PATTERN = Pattern.compile("nn|amod|advmod");
    private static final Pattern passivePattern1 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\s(via|after|with|if|once|requires"
            + "|require|when|through|due to|in case|provided that|effect of|result of|member of|in response to"
            + "|under.*<<NN.*control|under( the)? control of|depend(s|ed|ent)? ([a-z]+)? on).*");
    private static final Pattern passivePattern2 = Pattern.compile(".*(requires"
            + "|belongs? to|require|depend(s|ed|ent)? (on|upon)).*");
    private static final Pattern passivePattern3 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\sby.*");
    private static final Pattern passivePattern4 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\sby.*(time|times|fold|(ing<<VB)).*");
    private Set<String> restrictionTerms;        
    
    public Rule1(Set<String> restrictionTerms) {
        this.restrictionTerms = restrictionTerms;
    }
        
    @Override
    public List<Relation> findRelations(Sentence s, SimpleDepGraph depGraph) {
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
    
    /*private void addPath(SimpleDepGraph depGraph, int gov, Pattern pathPattern,
            List<SimpleDependency> curPath, List<List<SimpleDependency>> paths, boolean visited[]) {        
        
        if (visited[gov]) {
            return;
        }
        visited[gov] = true;        
        
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(gov, pathPattern);                
        if (dependencies.isEmpty()) {
            List<SimpleDependency> newPath = new ArrayList<SimpleDependency>(curPath);
            paths.add(newPath);
        } else {
            for (SimpleDependency dep : dependencies) {                
                curPath.add(dep);                
                addPath(depGraph, dep.dep(), pathPattern, curPath, paths, visited);
                curPath.remove(curPath.size() - 1);
            }
        }        
        visited[gov] = false;
    }*/
    
    List<Relation> filterCandidatePaths(Sentence s, SimpleDepGraph depGraph, List<List<SimpleDependency>> paths) {
        List<Relation> relations = new ArrayList<Relation>();
        for (List<SimpleDependency> path : paths) {            
            relations.addAll(filterCandidatePath(s, depGraph, path));
        }
        return relations;
    }
    
    List<Relation> filterCandidatePath(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path) {
        List<Relation> relations = new ArrayList<Relation>();
        showPath(s, path);
        Set<Integer> agents = getAgents(path.get(0).dep(), s, depGraph, path);
        System.out.print("Agent: ");
        for (Integer agent : agents) {
            System.out.print(s.get(agent).word() + ", ");
        }
        System.out.println("");
        if (!agents.isEmpty()) {
            Set<Pair<Integer,Integer>> targets = getTargets(s, depGraph, path);
            for (Pair<Integer,Integer> target : targets) {
                System.out.print("target: " + s.get(target.first).word());
                System.out.print("\tcorrect order: "+checkRoleOrder(s, depGraph, path, target.second));
                System.out.print("\tdomain specific: "+isDomainSpecific(s, depGraph, path, target.second));
                System.out.println("\tnegated: "+isNegated(s, depGraph, path, target.second));
                if (isDomainSpecific(s, depGraph, path, target.second) && !isNegated(s, depGraph, path, target.second)) {
                    if (checkRoleOrder(s, depGraph, path, target.second)) {
                        for (Integer agent : agents) {
                            relations.add(new Relation(agent, target.first));
                        }
                    } else {
                        for (Integer agent : agents) {
                            relations.add(new Relation(target.first, agent));
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
                    getPathsFrmSrc(s, subj, Pattern.compile("prep.*|partmod"), depGraph, Pattern.compile("prep.*|agent|dobj"));
            showPaths(s, prepPaths);           
            
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
            if (dep != mainHead && rcHead.matches("similar|include(s|d)?|members?|identical|involve(s|d)?")) {
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
                if (!rcHead.matches("similar|include(s|d)?|members?|identical|involve(s|d)?")) {
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
        for (int j = chunk.first; j <= chunk.second; j++) {
            if (s.get(j).getTag("POS").matches("(VB|NN|JJ).*") && !s.get(j).word().matches("belong(s|ed)?|requires?|effect|provided|effect|result|member|response|control|depend(s|ed|ent)?")) {
                chunkStr.append(s.get(j).word() + "<<" + s.get(j).getTag("POS") + " ");
            } else {
                chunkStr.append(s.get(j).word()).append(" ");
            }
        }
        return chunkStr.toString();
    }
    
    private boolean checkRoleOrder(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path, int endIndex) {
        StringBuilder pathSb = new StringBuilder();
        for (int i = 0; i <= endIndex; i++) {
            int gov = path.get(i).gov();
            int dep = path.get(i).dep();            
            if (i == 0) {                
                pathSb.append(getChunkStr(getNPChunk(dep, s, depGraph), s)).append(" ");
            }else {
                String govPOS = s.get(gov).getTag("POS");
                if (govPOS.matches("NN.*")) {
                    pathSb.append(getChunkStr(getNPChunk(gov, s, depGraph), s));
                } else if (govPOS.matches("(VB|JJ).*") && !s.get(gov).word().matches("belong(s|ed)?|requires?|effect|provided|effect|result|member|response|control|depend(s|ed|ent)?")){
                    pathSb.append(s.get(gov).word()).append("<<").append(govPOS).append(" ");
                } else {
                    pathSb.append(s.get(gov).word() + " ");
                }
                String reln = path.get(i).reln();
                if (reln.matches("prep_.*")) {
                    pathSb.append(reln.substring(reln.indexOf('_') + 1)).append(" ");
                } else if (reln.equals("agent")) {
                    pathSb.append("by ");
                }
                if (i == endIndex) {
                    String depPOS = s.get(dep).getTag("POS");
                    if (depPOS.matches("NN.*")) {
                        pathSb.append(getChunkStr(getNPChunk(dep, s, depGraph), s));
                    } else {
                        pathSb.append(s.get(dep).word()).append(" ");
                    }
                    break; //end of path
                }
            }
        }
        String pathStr = pathSb.toString();
        //System.out.println(pathStr);
        if (passivePattern1.matcher(pathStr).matches() || passivePattern2.matcher(pathStr).matches()) {
            /*if (passivePattern1.matcher(pathStr).matches()) {
                System.out.println("passive construct1: " + pathStr);
            } else if (passivePattern1.matcher(pathStr).matches()) {
                System.out.println("passive construct2: " + pathStr);
            } */           
            return false;
        } else if (passivePattern3.matcher(pathStr).matches() && !passivePattern4.matcher(pathStr).matches()) {
            //System.out.println("passive construct3!4: " + pathStr);
            return false;
        }
        return true;
    }
    private int getEntityFollowingLinks(int head, Sentence s, SimpleDepGraph depGraph, Pattern linkPattern) {
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(head, linkPattern);
        for (SimpleDependency sDep : dependencies) {
            System.out.println(sDep);
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
        for (int i = chunk.first; i <= chunk.second; i++) {
            if (restrictionTerms.contains(s.get(i).word().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    private boolean isDomainSpecific(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> path, int endIndex) {
        for (int i = 0; i <= endIndex; i++) {
            int gov = path.get(i).gov();
            int dep = path.get(i).dep();
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
        LPSentReader sentReader = new LPSentReader("(/|~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        Scanner in = new Scanner(System.in);
        String line;
        Relex relex = new Relex();
        Rule1 rule1 = new Rule1(getRestrictionTerms());
        Rule2 rule2 = new Rule2(getRestrictionTerms());
        LexSynAnnotator lexAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            //System.out.println(s);
            s = relex.correctTokenization(s);
            //System.out.println(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            s = lexAnnotator.annotate(s);
            System.out.println(s.toString("LEXE"));
            s.getParseTree().pennPrint();
            //SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            ExtendedDepGraph depGraph = new ExtendedDepGraph(s.getParseTree(), "CCProcessed");
            System.out.println(depGraph.toString(s.getParseTree()));
            
            //rule1.findRelations(s, depGraph);
            rule2.findRelations(s, depGraph);
        }
    }
}
