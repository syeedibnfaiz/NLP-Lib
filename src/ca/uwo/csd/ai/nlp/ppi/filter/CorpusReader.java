/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import ca.uwo.csd.ai.nlp.ppi.ExBioDomainAnnotator;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CorpusReader {

    //final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2", "BASE"});
    final private PTBFileReader TREE_READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    final private SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    final private BioDomainAnnotator domainAnnotator = new BioDomainAnnotator("./resource/relation/biomedical_terms2.txt");    
    final private PPIInfoReader infoReader = new PPIInfoReader();
    
    public List<RelationInstance> getRelationInstances(String corpusRoot) {
        ArrayList<RelationInstance> relationInstances = new ArrayList<RelationInstance>();
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            //File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");
            File infoFile = new File(corpusRoot + "/info", docId + ".txt");
            
            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE_READER.read(treeFile);
            //List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);
            List<HashMap<String, String[]>> maps = infoReader.read(infoFile);
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);                
                Tree root = trees.get(i);
                s.setParseTree(root);
                TREE_ANALYZER.assignPOS(s);
                //s.setParseTree(null);
                s = domainAnnotator.annotate(s);
                //root = null;

                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                HashMap<String, String[]> map = maps.get(i);
                relationInstances.addAll(getRelationInstances(s, ccDepGraph, docId, map));
            }
        }
        return relationInstances;
    }

    private List<RelationInstance> getRelationInstances(Sentence s, SimpleDepGraph depGraph, String docId, HashMap<String, String[]> map) {
        List<RelationInstance> instances = new ArrayList<RelationInstance>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();
        Map<String, Integer> n1Map = new HashMap<String, Integer>();

        //find P1 and N1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token : tokens) {
                    p1Map.put(token, i);
                }
            }
            if (!word.getTag("N1").equals("O")) {
                String[] tokens = word.getTag("N1").split(", ");
                for (String token : tokens) {
                    n1Map.put(token, i);
                }
            }
        }

        //find P2 and N2 and match with P1 and N2
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token : tokens) {
                    Integer entity1 = p1Map.get(token);
                    int lcs = getLCS2(s, depGraph, entity1, i);
                    List<String> backBonePath = getBackBonePath(s, depGraph, entity1, i, lcs);
                    String[] pairIds = map.get("+" + token);
                    instances.add(new RelationInstance(s, depGraph, backBonePath, lcs, entity1, i, true, docId, pairIds));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token : tokens) {
                    Integer entity1 = n1Map.get(token);
                    int lcs = getLCS2(s, depGraph, entity1, i);
                    List<String> backBonePath = getBackBonePath(s, depGraph, entity1, i, lcs);
                    String[] pairIds = map.get("-" + token);                    
                    instances.add(new RelationInstance(s, depGraph, backBonePath, lcs, entity1, i, false, docId, pairIds));
                }
            }
        }
        return instances;
    }

    private int getLCS(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1);

        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2);

        int lcs = -1;
        for (int i = 0; i < ancestors2.size(); i++) { //closest to e2
            int ancestor = ancestors2.get(i);
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }
        /*for (int i = 0; i < ancestors1.size(); i++) { //closest to e1
        int ancestor = ancestors1.get(i);            
        if (ancestor != entity2 && ancestors2.contains(ancestor)) {                
        lcs = ancestor; //path = e1 ---- lcs ++++ e2
        break;
        }
        }*/
        /*int lMin = Integer.MAX_VALUE;
        int bMin = Integer.MAX_VALUE;
        int rMin = Integer.MAX_VALUE;
        int lLCS = -1;
        int bLCS = -1;
        int rLCS = -1;
        for (int i = 0; i < ancestors2.size(); i++) {
        int ancestor = ancestors2.get(i);
        if (ancestors1.contains(ancestor)) {
        int j = ancestors1.indexOf(ancestor);
        int dist = i + j;
        if (ancestor <= entity1 && dist < lMin) {
        lLCS = ancestor;
        lMin = dist;
        } else if (ancestor <= entity2 && dist < bMin) {
        bLCS = ancestor;
        bMin = dist;
        } else if (ancestor > entity2 && dist < rMin) {
        rLCS = ancestor;
        rMin = dist;
        }
        }
        }
        
        int min = Math.min(lMin, Math.min(bMin, rMin));
        
        if (bMin == min) {  //giving higher priority to between pattern
        lcs = bLCS;
        } else if (lMin == min) {
        lcs = lLCS;
        } else {
        lcs = rLCS;
        }*/

        return lcs;
    }

    private int getLCS2(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1);

        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2);

        int lcs = -1;
        for (int i = 0; i < ancestors2.size(); i++) { //closest to e2
            int ancestor = ancestors2.get(i);
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }
        
        //int oldLcs = lcs;
        if (lcs != -1 && (lcs < entity1 || lcs > entity2) && (entity1 != entity2) && s.get(entity1).getTag("DOMAIN") == null && s.get(entity2).getTag("DOMAIN") == null) {            
            String pos = s.get(lcs).getTag("POS");
            if (pos.startsWith("N") && s.get(lcs).getTag("DOMAIN") == null) {
                List<SimpleDependency> depDependencies = depGraph.getDepDependencies(lcs);
                for (SimpleDependency dep : depDependencies) {
                    int gov = dep.gov();
                    if (s.get(gov).getTag("DOMAIN") != null) {
                        lcs = gov;
                    }
                }
            }
        }
        /*if (lcs != oldLcs) {
            System.out.println(s);
            System.out.println(s.get(entity1) + "<>" + s.get(entity2));
            System.out.println("old: " + s.get(oldLcs));
            System.out.println("new: " + s.get(lcs));
        }*/
        return lcs;
    }

    private List<String> getBackBonePath(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2, int lcs) {

        if (lcs == -1) {    //path = e1 ---- e2 or e1 ++++ e2
            return null;
        }
        return getPath(depGraph, entity1, entity2, lcs);
    }

    private void addAncestors(List<Integer> ancestors, Sentence s, SimpleDepGraph depGraph, int node) {
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(node);
        //ancestors.add(node);
        boolean[] visited = new boolean[s.size()];

        while (!queue.isEmpty()) {
            int next = queue.poll();
            if (visited[next]) {
                continue;
            }
            visited[next] = true;
            List<SimpleDependency> deps = depGraph.getDepDependencies(next);
            for (SimpleDependency dependency : deps) {
                int gov = dependency.gov();
                String reln = dependency.reln();
                if (reln.matches("conj_(and|or)")) {    //otherwise may create loops
                    continue;
                }
                ancestors.add(gov);
                queue.add(gov);
            }
        }
    }

    private List<String> getPath(SimpleDepGraph depGraph, int entity1, int entity2, int lcs) {

        List<SimpleDependency> path1 = null;
        List<SimpleDependency> path2 = null;

        if (lcs != entity1) {
            path1 = depGraph.getPathAsRelnList(lcs, entity1, true);
        } else {
            path1 = null;
        }

        if (lcs != entity2) {
            path2 = depGraph.getPathAsRelnList(lcs, entity2, true);
        } else {
            path2 = null;
        }

        ArrayList<String> path = new ArrayList<String>();
        if (path1 == null) {
            path.add(String.valueOf(lcs));
            path.add("-null");
        } else {
            for (int i = path1.size() - 1; i >= 0; i--) {
                int dep = path1.get(i).dep();
                path.add(String.valueOf(dep));
                path.add("-" + path1.get(i).reln());
            }
        }
        if (path2 == null) {
            path.add(String.valueOf(lcs));
            path.add("null");
            path.add(String.valueOf(entity2));
        } else {
            for (int i = 0; i < path2.size(); i++) {
                int gov = path2.get(i).gov();
                path.add(String.valueOf(gov));
                path.add(path2.get(i).reln());
            }
            path.add(String.valueOf(entity2));
        }

        return path;
    }

    private List<String> getNullPath(int i, int j) {
        List<String> path = new ArrayList<String>();
        path.add(String.valueOf(i));
        path.add("null");
        path.add(String.valueOf(j));
        return path;
    }
}
