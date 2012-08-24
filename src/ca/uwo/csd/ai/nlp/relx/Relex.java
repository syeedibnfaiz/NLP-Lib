/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.relx.LLLDataInstance.InteractionPair;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Relex {
    private static final LLLLexicon LEXICON = new LLLLexicon("./resource/relation/LLL/dictionary_data.txt");
    //private static final LLLLexicon LEXICON = new LLLLexicon("./resource/relation/LLL/dictionary_data_test.txt");
    private static final LLLDataReader DATA_READER = new LLLDataReader();
    private static final Pattern passivePattern1 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\s(via|after|with|if|once|requires"
            + "|require|when|through|due to|in case|provided that|effect of|result of|member of|in response to|in (the)? control of"
            + "|under (the)? control of|depend(s|ed|ent)? ([a-z]+)? on).*");
    private static final Pattern passivePattern2 = Pattern.compile(".*(requires"
            + "|require|depend(s|ed|ent)? on).*");
    private static final Pattern passivePattern3 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\sby.*");
    private static final Pattern passivePattern4 = Pattern.compile(".*(t|d|ion|ing)<<(VB|AUX|MD|NN|JJ).*\\sby.*(time|times|fold|(ing<<VB)).*");
    private HashSet<String> restrictionTerms;
    
    public Relex() {
        restrictionTerms = new HashSet<String>();
        List<String> lines = Util.readLines("./resource/relation/LLL/relex_restriction_terms.txt");
        for (String line : lines) {
            int pos = line.indexOf(':');
            String tokens[] = line.substring(pos + 1).split("\\|");
            for (String token : tokens) {
                restrictionTerms.add(token);
            }
        }
    }
    
    public List< List<SimpleDependency> > getType1Relations(Sentence s, SimpleDepGraph depGraph) {
        if (depGraph == null) {
            depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        }
        List< List<SimpleDependency> > relations = new ArrayList< List<SimpleDependency> > ();        
        List<SimpleDependency> subjDependencies = depGraph.getDependencies("nsubj|nsubjpass");
        
        for (SimpleDependency subjDep : subjDependencies) {
            int gov = subjDep.gov();
            int dep = subjDep.dep();
            
            List<SimpleDependency> tmpList = new ArrayList<SimpleDependency>();
            tmpList.add(subjDep);
            boolean visited[] = new boolean[s.size()];
            exploreType1Relation(s, depGraph, subjDep.gov(), tmpList, relations, visited);
        }
        return relations;
    }
    
    public void exploreType1Relation(Sentence s, SimpleDepGraph depGraph, int i, List<SimpleDependency> tmpList, List<List<SimpleDependency>> relations, boolean visited[]) {
        if (visited[i]) return;
        visited[i] = true;
        
        String chunkTag = s.get(i).getTag("CHUNK");
        if (chunkTag.matches(".*NP")) {
            List<SimpleDependency> newList = new ArrayList<SimpleDependency>(tmpList);
            relations.add(newList);            
        }
        
        List<SimpleDependency> dependents = depGraph.getGovDependencies(i);        
        for (SimpleDependency dep : dependents) {
            String reln = dep.reln();
            //if (reln.matches("dobj|iobj|pobj|prep.*|xcomp|agent|infmod")) {
            //if (reln.matches("dobj|iobj|pobj|prep.*|xcomp|agent|infmod|rcmod|dobj")) {
            //if (reln.matches("dobj|iobj|pobj|prep.*|xcomp|agent|infmod|rcmod|dobj|ccomp|dep|conj.*")) {
            //include nn, i.e. every path, less post processing
            //if (reln.matches("nn|(d|i|p)obj|agent|(inf|rc)mod|(x|c)comp|dep|conj.*|prep.*")) {            
            //if (!reln.matches("(nsubj|nsubjpass|xsubj)")) {            
            //if (!reln.matches("(nsubj|nsubjpass|xsubj|conj.*|rcmod|partmod)")) {            
            //if (!reln.matches("(nsubj|nsubjpass|xsubj|conj.*)")) {            
            if (!reln.matches("(nsubj|nsubjpass|xsubj|conj.*|nn|amod|advmod|det)")) {            
                tmpList.add(dep);                
                exploreType1Relation(s, depGraph, dep.dep(), tmpList, relations, visited);
                tmpList.remove(tmpList.size() - 1);
            }
        }                        
    }
    
    public List< List<SimpleDependency> > getType2Relations(Sentence s, SimpleDepGraph depGraph) {
        if (depGraph == null) {
            depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        }
        List< List<SimpleDependency> > relations = new ArrayList< List<SimpleDependency> > ();
        boolean visited[] = new boolean[s.size()];
        for (SimpleDependency sDep : depGraph) {
            int gov = sDep.gov();
            String reln = sDep.reln();
            if (reln.matches("prep.(of|by|to|on|for|in|through|with)") &&
                !visited[gov] && s.get(gov).getTag("CHUNK").matches(".*NP")) {
                System.out.println(sDep);
                List<SimpleDependency> relation = new ArrayList<SimpleDependency>();
                exploreType2Relation(s, depGraph, sDep, relation, visited);
                if (!relation.isEmpty()) {
                    relations.add(relation);
                    //marking visited NP chunks
                    for (SimpleDependency sd : relation) {
                        visited[sd.gov()] = true;
                        visited[sd.dep()] = true;
                    }
                }
            }
        }
        return relations;
    }
    
    public void exploreType2Relation(Sentence s, SimpleDepGraph depGraph, SimpleDependency sDep, List<SimpleDependency> relation, boolean visited[]) {
        int dep = sDep.dep();
        if (!visited[dep] && s.get(dep).getTag("CHUNK").matches(".*NP")) {
            relation.add(sDep);
            List<SimpleDependency> dependencies = depGraph.getGovDependencies(dep);
            for (SimpleDependency nextDep : dependencies) {
                String reln = nextDep.reln();
                if (reln.matches("prep_(of|by|to|on|for|in|through|with)")) {
                    exploreType2Relation(s, depGraph, nextDep, relation, visited);
                }
            }
        }
    }
    
    public List< List<SimpleDependency> > getType3Relations(Sentence s, SimpleDepGraph depGraph) {
        if (depGraph == null) {
            depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        }
        List< List<SimpleDependency> > relations = new ArrayList< List<SimpleDependency> > ();
        List<SimpleDependency> betweenDeps[] = new List[s.size()];
        List<SimpleDependency> betweenDependencies = depGraph.getDependencies("prep_between");
        for (SimpleDependency sDep : betweenDependencies) {            
            int gov = sDep.gov();
            if (betweenDeps[gov] == null) betweenDeps[gov] = new ArrayList<SimpleDependency>();
            betweenDeps[gov].add(sDep);
        }
        
        for (int i = 0; i < s.size(); i++) {
            if (betweenDeps[i] != null /*&& betweenDeps[i].size() >= 2*/) {
                List<SimpleDependency> relation = new ArrayList<SimpleDependency>();
                for (int j = 0; j < betweenDeps[i].size(); j++) {
                    relation.add(betweenDeps[i].get(j));
                }
                relations.add(relation);
            }
        }
        return relations;
    }
    private boolean correctOrder(Sentence s, List<SimpleDependency> relation) {
        StringBuilder sb = new StringBuilder();
        if (relation.size() > 1) {
            SimpleDependency firstDep = relation.get(0);
            if (firstDep.reln().equals("nsubjpass")) {
                int gov = firstDep.gov();
                Chunk chunk = new Chunk(s, gov);
                for (int i = chunk.getStart(); i <= chunk.getEnd(); i++) {
                    String word = s.get(i).word();
                    if (restrictionTerms.contains(word.toLowerCase())) {                        
                        return false; //why??
                    }
                }
            }
        }
        for (int i = 0; i < relation.size(); i++) {
            int gov = relation.get(i).gov();
            int dep = relation.get(i).dep();
            String reln = relation.get(i).reln();
            
            if (i == 0) {
                Chunk chunk = new Chunk(s, dep);
                for (int j = chunk.getStart(); j <= chunk.getEnd(); j++) {
                    sb.append(" " + s.get(j).word());
                    if (s.get(j).getTag("POS").matches("(VB|NN|JJ).*")) {
                        sb.append("<<" + s.get(j).getTag("POS"));
                    }
                }
                continue;
            }
            Chunk chunk = new Chunk(s, gov);
            for (int j = chunk.getStart(); j <= chunk.getEnd(); j++) {
                sb.append(" " + s.get(j).word());
                if (s.get(j).getTag("POS").matches("(VB|NN|JJ).*") && !s.get(j).word().matches("requires?|effect|provided|effect|result|member|response|control|depend(s|ed|ent)?")) {
                    sb.append("<<" + s.get(j).getTag("POS"));
                }
            }
            /*sb.append(" ").append(s.get(gov).word());
            if (s.get(gov).getTag("POS").matches("(VB|NN|JJ).*") && !s.get(gov).word().matches("requires?|effect|provided|effect|result|member|response|control|depend(s|ed|ent)?")) {
                sb.append("<<" + s.get(gov).getTag("POS"));
            }*/
            
            if (reln.startsWith("prep_")) {
                sb.append(" ").append(reln.substring(5));
            }                        
        }
        //System.out.println(sb);
        if (passivePattern1.matcher(sb).matches() || passivePattern2.matcher(sb).matches()) {
            if (passivePattern1.matcher(sb).matches()) {
                System.out.println("passive construct1: " + sb);
            } else if (passivePattern1.matcher(sb).matches()) {
                System.out.println("passive construct2: " + sb);
            }
            
            return false;
        } else if (passivePattern3.matcher(sb).matches() && !passivePattern4.matcher(sb).matches()) {
            return false;
        }
        
        //System.out.println("No Match!!");
        return true;
    }
    private boolean isDomainSpecific(Sentence s, SimpleDepGraph depGraph, List<SimpleDependency> relation) {
        for (int i = 0; i < relation.size(); i++) {
            SimpleDependency sDep = relation.get(i);
            int gov = sDep.gov();
            Chunk chunk = new Chunk(s, gov);
            for (int j = chunk.getStart(); j <= chunk.getEnd(); j++) {
                String word = s.get(j).word();
                String tokens[] = word.split("-");
                for (String token : tokens) {
                    if (restrictionTerms.contains(token.toLowerCase())) {
                        return true;
                    }
                }
            }
            if (i == 0) {
                int dep = sDep.dep();
                Tree root = s.getParseTree();
                Tree depLeaf = root.getLeaves().get(dep);
                Tree npNode = null;
                int h = 2;
                while (true) {
                    Tree anc = depLeaf.ancestor(h, root);
                    if (anc != null && anc.value().matches("NP.*")) {
                        npNode = anc;
                    } else {
                        break;
                    }
                    h++;
                }
                if (npNode != null) {
                    List<Tree> leaves = npNode.getLeaves();
                    for (int j = 0; j < leaves.size(); j++) {
                        if (restrictionTerms.contains(leaves.get(j).value().toLowerCase())) {
                            System.out.println("found term: " + leaves.get(j).value());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public List<LLLDataInstance.InteractionPair> filterType1Relations(Sentence s, SimpleDepGraph depGraph, List< List<SimpleDependency> > relations) {        
        List<LLLDataInstance.InteractionPair> interactions = new ArrayList<LLLDataInstance.InteractionPair>();
        for (List<SimpleDependency> relation : relations) {
            if (!isDomainSpecific(s, depGraph, relation)) {
                System.out.println("not domain specific!");
                continue;
            }
            System.out.println("domain specific!");
            SimpleDependency firstDep = relation.get(0);
            int agent = -1;
            if (firstDep.reln().matches("nsubj.*")) {
                agent = firstDep.dep();
            } else {
                agent = firstDep.gov();
            }
            
            //if (s.get(agent).getTag("LEX").equals("NO")) { //not a vocabulary word
            if (!s.get(agent).getTag("LEXE").equals("B")) { //not a vocabulary word
                //System.out.println("agent="+s.get(agent).word() + ", Lex=No");
                List<Integer> reachable = new ArrayList<Integer>();
                //explore(depGraph, agent, "prep.*|amod|nn|advmod|conj.*", new boolean[s.size()], reachable); //rcmod?
                //explore(depGraph, agent, "prep.*|amod|nn|advmod|conj.*|dep", new boolean[s.size()], reachable); //rcmod?
                //explore(depGraph, agent, "appos|prep.*|amod|nn|advmod|conj.*|dep", new boolean[s.size()], reachable); //rcmod?
                explore(depGraph, agent, "partmod|dobj|appos|prep.*|amod|nn|advmod|conj.*|dep", new boolean[s.size()], reachable); //rcmod?
                
                
                for (Integer i : reachable) {
                    //if (s.get(i).getTag("LEX").equals("YES")) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        agent = i;
                        break;
                    }
                }
                
            }            
            //now it may be the case that lexe-B is not reachable from agent so far
            if (s.get(agent).getTag("LEXE").equals("I")) {
                Chunk chunk = new Chunk(s, agent);
                for (int i = chunk.getStart(); i <= chunk.getEnd(); i++) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        agent = i;
                        break;
                    }
                }
            }
            //System.out.println("\t final agent="+getLexChunk(s, agent));
            
            SimpleDependency lastDep = relation.get(relation.size() - 1);
            int target = lastDep.dep();
            //System.out.println("target="+s.get(target).word());
            
            //if (s.get(target).getTag("LEX").equals("NO")) { //not a vocabulary word
            if (!s.get(target).getTag("LEXE").equals("B")) { //not a vocabulary word
                //System.out.println("target="+s.get(target).word() + ", Lex=No");
                List<Integer> reachable = new ArrayList<Integer>();
                //explore(depGraph, target, "prep.*|amod|nn|advmod|conj.*", new boolean[s.size()], reachable);
                //explore(depGraph, target, "amod|nn|advmod|conj.*", new boolean[s.size()], reachable);
                //main path now traverses everything
                //explore(depGraph, target, "amod|advmod", new boolean[s.size()], reachable);
                //do not need to explore, in fact!
                //explore(depGraph, target, "amod|advmod", new boolean[s.size()], reachable);
                explore(depGraph, target, "nn|amod|advmod", new boolean[s.size()], reachable);
                for (Integer i : reachable) {
                    //System.out.println(s.get(i).word() +" is reachable from: " + s.get(target).word());
                    //if (s.get(i).getTag("LEX").equals("YES")) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        target = i;
                        break;
                    }
                }                
            }
            //now it may be the case that lexe-B is not reachable from agent so far
            if (s.get(target).getTag("LEXE").equals("I")) {
                Chunk chunk = new Chunk(s, target);
                for (int i = chunk.getStart(); i <= chunk.getEnd(); i++) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        target = i;
                        break;
                    }
                }
            }
            System.out.println("\t final target="+getLexChunk(s, target));
            //if (s.get(target).getTag("LEX").equals("YES") && s.get(agent).getTag("LEX").equals("YES")) {
            if (s.get(target).getTag("LEXE").equals("B") && s.get(agent).getTag("LEXE").equals("B")) {
                //reverse agent, target for passive constructs
                if (/*firstDep.reln().equals("nsubjpass") ||*/ !correctOrder(s, relation)) {
                    int tmp = agent;
                    agent = target;
                    target = tmp;
                }
                interactions.add(new LLLDataInstance.InteractionPair(agent, target));
                //System.out.println("\tagent: " + agent);
                //System.out.println("\ttarget: " + target);
            }
        }
        //remove duplicates
        List<LLLDataInstance.InteractionPair> uniqueInteractions = new ArrayList<InteractionPair>();
        boolean duplicate[] = new boolean[interactions.size()];
        for (int i = 0; i < interactions.size(); i++) {
            if (!duplicate[i]) {
                for (int j = i + 1; j < interactions.size(); j++) {
                    if (interactions.get(i).agent == interactions.get(j).agent && (interactions.get(i).target == interactions.get(j).target)) {
                        duplicate[j] = true;
                    } else if (interactions.get(i).agent == interactions.get(j).target && (interactions.get(i).target == interactions.get(j).agent)) {
                        if (interactions.get(j).agent < interactions.get(j).target) { //eliminated non-passive one
                            duplicate[j] = true;
                        } else {
                            continue;
                        }
                    }
                }
                uniqueInteractions.add(interactions.get(i));
            }
        }
        return uniqueInteractions;
    }
    
    public List<LLLDataInstance.InteractionPair> filterType2Relations(Sentence s, SimpleDepGraph depGraph, List< List<SimpleDependency> > relations) {
        List<LLLDataInstance.InteractionPair> interactions = new ArrayList<LLLDataInstance.InteractionPair>();
        for (List<SimpleDependency> relation : relations) {
            for (SimpleDependency sDep : relation) {
                int gov = sDep.gov();
                int dep = sDep.dep();
                
                List<Integer> reachable = new ArrayList<Integer>();                
                //get agent(s)
                explore(depGraph, gov, "amod|nn|advmod|conj.*", new boolean[s.size()], reachable);
                List<Integer> agents = new ArrayList<Integer>();
                for (Integer i : reachable) {
                    //if (s.get(i).getTag("LEX").equals("YES")) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        agents.add(i);                        
                    }
                }
                //get target(s)
                reachable.clear();
                explore(depGraph, dep, "amod|nn|advmod|conj.*", new boolean[s.size()], reachable);
                List<Integer> targets = new ArrayList<Integer>();
                for (Integer i : reachable) {
                    //if (s.get(i).getTag("LEX").equals("YES")) {
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        //System.out.println("** " + s.get(i).word() + " is reachable from: " + s.get(dep).word());
                        targets.add(i);                        
                    }
                }
                
                //make pairs
                for (Integer i : agents) {
                    for (Integer j : targets) {
                        interactions.add(new InteractionPair(i, j));
                    }
                }
            }
        }
        return interactions;
    }
    
    public List<LLLDataInstance.InteractionPair> filterType3Relations(Sentence s, SimpleDepGraph depGraph, List< List<SimpleDependency> > relations) {
        List<LLLDataInstance.InteractionPair> interactions = new ArrayList<LLLDataInstance.InteractionPair>();
        for (List<SimpleDependency> relation : relations) {
            List<Integer> deps = new ArrayList<Integer>();
            int gov = -1;
            for (SimpleDependency sDep : relation) {
                int dep = sDep.dep();
                gov = sDep.gov();
                System.out.println("dep: " + s.get(dep).word());
                List<Integer> reachable = new ArrayList<Integer>();                                
                explore(depGraph, dep, "amod|nn|advmod|prep.*", new boolean[s.size()], reachable);
                for (Integer i : reachable) {                    
                    if (s.get(i).getTag("LEXE").equals("B")) {
                        //System.out.println("&& " + s.get(i).word() + " is reachable from: " + s.get(dep).word());
                        deps.add(i);
                        break;
                    }
                }
            }
            //check domain specificity of gov node
            Chunk chunk = new Chunk(s, gov);
            System.out.println(chunk);
            boolean domainSpecific = false;
            for (int i = chunk.getStart(); i <= chunk.getEnd(); i++) {
                if (restrictionTerms.contains(s.get(i).word().toLowerCase())) {
                    domainSpecific = true;
                    break;
                }
            }
            if (!domainSpecific) {
                System.out.println("skipping..");
                continue;
            }
            
            for (int i = 0; i < deps.size(); i++) {
                for (int j = i + 1; j < deps.size(); j++) {
                    interactions.add(new InteractionPair(deps.get(i), deps.get(j)));
                }
            }
            if (deps.size() == 1) {
                int dep = deps.get(0);
                List<SimpleDependency> dependencies = depGraph.getGovDependencies(dep);
                for (SimpleDependency sDep : dependencies) {
                    if (sDep.reln().matches("conj.*")) {
                        int dep2 = sDep.dep();
                        List<Integer> reachable = new ArrayList<Integer>();
                        explore(depGraph, dep2, "amod|nn|advmod|prep.*", new boolean[s.size()], reachable);
                        for (Integer i : reachable) {
                            if (s.get(i).getTag("LEXE").equals("B")) {                                                                
                                interactions.add(new InteractionPair(dep, dep2));
                                break;
                            }
                        }
                    }
                }
            }
            
        }
        return interactions;
    }
    private void explore(SimpleDepGraph depGraph, int start, String relnPat, boolean visited[], List<Integer> reachable) {
        visited[start] = true;
        reachable.add(start);
        List<SimpleDependency> dependencies = depGraph.getGovDependencies(start);
        for (SimpleDependency sDep : dependencies) {
            if (!visited[sDep.dep()] && sDep.reln().matches(relnPat)) {
                explore(depGraph, sDep.dep(), relnPat, visited, reachable);
            }
        }
    }
    public void showRelations(Sentence s, List<List<SimpleDependency>> relations) {
        for (List<SimpleDependency> relation : relations) {
            showRelation(s, relation);
        }
    }
    public void showRelation(Sentence s, List<SimpleDependency> relation) {
        for (int i = 0; i < relation.size(); i++) {
            SimpleDependency sDep = relation.get(i);
            int gov = sDep.gov();
            int dep = sDep.dep();
            String reln = sDep.reln();
            if (i == 0 && reln.matches("nsubj.*")) {
                Chunk chunk = new Chunk(s, dep);
                //System.out.print(s.toString(chunk.getStart(), chunk.getEnd()));                
                System.out.print(s.get(dep).word());
                System.out.print("-[" + reln + "]-");
            } else {
                String word = s.get(gov).word();
                if (s.get(gov).getTag("CHUNK").matches(".*NP")) {
                    Chunk chunk = new Chunk(s, gov);
                    word = s.toString(chunk.getStart(), chunk.getEnd());
                }
                //System.out.print(word);
                System.out.print(s.get(gov).word());
                System.out.print("-[" + reln + "]-");
                if (i == (relation.size() - 1)) {
                    Chunk chunk = new Chunk(s, dep);
                    //System.out.println(s.toString(chunk.getStart(), chunk.getEnd()));
                    System.out.println(s.get(dep).word());
                }
            }
            
        }
    }
    public List<InteractionPair> getInteractions(Sentence s) {
        List<InteractionPair> interactions = new ArrayList<InteractionPair>();
        SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        List<List<SimpleDependency>> relations = getType1Relations(s, depGraph);
        s = LEXICON.annotate(s);
        List<InteractionPair> filteredType1Relations = filterType1Relations(s, depGraph, relations);
        interactions.addAll(filteredType1Relations);
        
        relations = getType2Relations(s, depGraph);
        List<InteractionPair> filteredType2Relations = filterType2Relations(s, depGraph, relations);
        interactions.addAll(filteredType2Relations);
        
        relations = getType3Relations(s, depGraph);
        List<InteractionPair> filteredType3Relations = filterType3Relations(s, depGraph, relations);
        interactions.addAll(filteredType3Relations);
        
        //remove duplicates
        List<LLLDataInstance.InteractionPair> uniqueInteractions = new ArrayList<InteractionPair>();
        boolean duplicate[] = new boolean[interactions.size()];
        for (int i = 0; i < interactions.size(); i++) {
            if (!duplicate[i] && (interactions.get(i).agent != interactions.get(i).target)) {
                for (int j = i + 1; j < interactions.size(); j++) {
                    if (interactions.get(i).agent == interactions.get(j).agent && (interactions.get(i).target == interactions.get(j).target)) {
                        duplicate[j] = true;
                    }
                }
                uniqueInteractions.add(interactions.get(i));
            }
        }
        return uniqueInteractions;                  
    }
    
    public void testLLLTestingset(String filePath) throws IOException {
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        FileWriter writer = new FileWriter("./resource/relation/LLL/test_result.txt");
        List<LLLDataInstance> dataInstances = LLLDataReader.readTrainingData(filePath);
        
        for (LLLDataInstance dataInstance : dataInstances) {
            Sentence s = dataInstance.s;
            s = correctTokenization(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            
            List<InteractionPair> interactions = getInteractions(s);
            writer.write("ID\t" + dataInstance.id + "\n");
            HashSet<String> agentSet = new HashSet<String>();
            HashSet<String> targetSet = new HashSet<String>();
            List<String> pairs = new ArrayList<String>();
            
            //writer.write(s.toString("LEXE") + "\n");
            //writer.write(s.toString() + "\n");
            
            for (InteractionPair ip : interactions) {
                //String agent = s.get(ip.agent).word();
                String agent = getLexChunk(s, ip.agent);
                if (!LEXICON.contains(agent) && agent.contains("-")) {
                    int pos = agent.lastIndexOf('-');
                    agent = agent.substring(0, pos);                    
                }
                String synonym = LEXICON.hasSynonym(agent);
                if (synonym != null) {
                    agent = synonym;
                }
                
                //String target = s.get(ip.target).word();
                String target = getLexChunk(s, ip.target);
                if (!LEXICON.contains(target) && target.contains("-")) {
                    int pos = target.lastIndexOf('-');
                    target = target.substring(0, pos);                    
                }
                synonym = LEXICON.hasSynonym(target);
                if (synonym != null) {
                    target = synonym;
                }
                agentSet.add(agent);
                targetSet.add(target);
                pairs.add("'"+agent+"','" +target+"'");
            }
            writer.write("agents");
            for (String agent : agentSet) {
                writer.write("\t");
                writer.write("agent('" + agent + "')");
            }
            writer.write("\n");
            writer.write("targets");
            for (String target : targetSet) {
                writer.write("\t");
                writer.write("target('" + target + "')");
            }
            writer.write("\n");
            writer.write("genic_interactions");
            for (String pair : pairs) {
                writer.write("\t");
                writer.write("genic_interaction(" + pair + ")");
            }
            writer.write("\n\n");            
        }
        writer.close();
    }
    private String getLexChunk(Sentence s, int pos) {
        
        while (pos >0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            return null;
        }
        int start = pos;
        int end = start;
        pos++;
        while (pos < s.size() && s.get(pos).getTag("LEXE").equals("I")) {
            end = pos;
            pos++;
        }
        return s.toString(start, end);
    }
    
    public void testLLLTrainingset(String filePath) {
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        
        List<LLLDataInstance> dataInstances = LLLDataReader.readTrainingData(filePath);
        for (LLLDataInstance dataInstance : dataInstances) {
            Sentence s = dataInstance.s;
            s = correctTokenization(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            
            List<InteractionPair> interactions = getInteractions(s);
            List<InteractionPair> goldenInteractions = dataInstance.interactions;
            
            System.out.println("--------------");
            //System.out.println(s.toString("LEX"));
            System.out.println(s.toString("LEXE"));
            System.out.println(s);
            boolean found[] = new boolean[goldenInteractions.size()];
            for (InteractionPair ip : interactions) {
                //String ipAgent = s.get(ip.agent).word();
                String ipAgent = getLexChunk(s, ip.agent);
                //String ipTarget = s.get(ip.target).word();
                String ipTarget = getLexChunk(s, ip.target);
                if (ipAgent.contains("-")) {
                    int pos = ipAgent.indexOf('-');
                    String first = ipAgent.substring(0, pos);
                    if (LEXICON.contains(first)) ipAgent = first;
                }
                if (ipTarget.contains("-")) {
                    int pos = ipTarget.indexOf('-');
                    String first = ipTarget.substring(0, pos);
                    if (LEXICON.contains(first)) ipTarget = first;
                }
                boolean correct = false;
                for (int i = 0; i < goldenInteractions.size(); i++) {
                    if (found[i]) continue;
                    InteractionPair gip = goldenInteractions.get(i);
                    String gipAgent = dataInstance.words.get(gip.agent);
                    String gipTarget = dataInstance.words.get(gip.target);
                    if (gipAgent.equals(ipAgent) && gipTarget.equals(ipTarget)) {
                        correct = true;
                        found[i] = true;
                        break;
                    } else if (gipAgent.equals(ipTarget) && gipTarget.equals(ipAgent)) {
                        /*correct = true;
                        found[i] = true;
                        break;*/
                        System.out.println("REVERSE!");
                    }
                }
                if (correct) {
                    System.out.println("TP: " + getLexChunk(s, ip.agent) + "-" + getLexChunk(s, ip.target));
                } else {
                    System.out.println("FP: " + getLexChunk(s, ip.agent) + "-" + getLexChunk(s, ip.target));
                }
            }
            
            for (int i = 0; i < goldenInteractions.size(); i++) {
                InteractionPair ip = goldenInteractions.get(i);
                if (!found[i]) {
                    System.out.println("FN: " + dataInstance.words.get(ip.agent) + "-" + dataInstance.words.get(ip.target));
                    showGoldenInteractionPath(ip, dataInstance.words.get(ip.agent), dataInstance.words.get(ip.target), s, depGraph);
                }
            }
            System.out.println("--------------");
        }
    }
    private void showGoldenInteractionPath(InteractionPair ip, String ipAgent, String ipTarget, Sentence s, SimpleDepGraph depGraph) {
        int agent = ip.agent;
        int target = ip.target;
        
        for (int i = agent - 4; i < s.size(); i++) {
            if (i < 0) continue;
            if (s.get(i).word().matches(ipAgent+"(-.*)?")) {
                agent = i;
                break;
            }            
        }        
        for (int i = target - 4; i < s.size(); i++) {
            if (i < 0) continue;
            if (s.get(i).word().matches(ipTarget+"(-.*)?")) {
                target = i;
                break;
            }
        }
        System.out.print(s.get(agent).word()+"-");
        String path = depGraph.getPath(agent, target);
        System.out.print(path);
        System.out.println("-"+s.get(target).word());
    }
    public Sentence correctTokenization(Sentence s) {
        Sentence newS = new Sentence();
        for (int i = 0; i < s.size(); i++) {
            String word = s.get(i).word();
            if (word.startsWith("(") && word.endsWith(")")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else if (word.startsWith("(") && !word.equals("(")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1)));
            } else if (word.endsWith(")") && !word.equals(")") && !word.contains("(")) {                
                newS.add(new TokWord(word.substring(0, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else {
                newS.add(new TokWord(word));
            }
        }
        return newS;
    }
    
    public static void testInput() {
        //SimpleSentReader sentReader = new SimpleSentReader();
        LPSentReader sentReader = new LPSentReader("(/|~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");//new LPSentReader("(~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");
        //LPSentReader sentReader = new LPSentReader("(~|\\||\\+|-|'|\\d|\\p{L})+|\\S");
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        Scanner in = new Scanner(System.in);
        String line;
        Relex relex = new Relex();
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            //System.out.println(s);
            s = relex.correctTokenization(s);
            //System.out.println(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            System.out.println(s.toString("POS"));
            s.getParseTree().pennPrint();
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            System.out.println(depGraph.toString(s.getParseTree()));
            List<List<SimpleDependency>> relations = relex.getType1Relations(s, depGraph);
            System.out.println("------------Type1---------------");
            relex.showRelations(s, relations);            
            System.out.println("-------------Filtered Typ11--------------");
            s = LEXICON.annotate(s);
            System.out.println(s.toString("LEXE"));
            List<InteractionPair> interactions = relex.filterType1Relations(s, depGraph, relations);
            for (InteractionPair ip : interactions) {
                System.out.println(relex.getLexChunk(s, ip.agent) +"-"+relex.getLexChunk(s, ip.target));
            }
            System.out.println("------------Type2---------------");
            relations = relex.getType2Relations(s, depGraph);
            relex.showRelations(s, relations);
            
            System.out.println("-------------Filtered Type2--------------");            
            interactions = relex.filterType2Relations(s, depGraph, relations);
            for (InteractionPair ip : interactions) {
                System.out.println(s.get(ip.agent).word() +"-"+s.get(ip.target).word());
            }
            System.out.println("------------Type3---------------");
            relations = relex.getType3Relations(s, depGraph);
            relex.showRelations(s, relations);            
            
            System.out.println("-------------Filtered Type3--------------");            
            interactions = relex.filterType3Relations(s, depGraph, relations);
            for (InteractionPair ip : interactions) {
                System.out.println(relex.getLexChunk(s, ip.agent) +"-"+relex.getLexChunk(s, ip.target));
            }
        }
    }
    public static void main(String args[]) throws IOException {
        testInput();
        Relex relex = new Relex();
        //relex.testLLLTrainingset("./resource/relation/LLL/genic_interaction_data.txt");
        relex.testLLLTestingset("./resource/relation/LLL/basic_test_data.txt");
    }
}
