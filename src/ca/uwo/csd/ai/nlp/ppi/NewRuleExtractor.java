/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class NewRuleExtractor {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    final BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();
    
    private static class PPIPair {
        int entity1, entity2;
        boolean interact;
        Sentence s;
        SimpleDepGraph depGraph;
        SimpleDepGraph ccDepGraph;
        public PPIPair(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2, boolean interact) {
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.interact = interact;
            this.s = s;
            this.depGraph = depGraph;
            this.ccDepGraph = ccDepGraph;
        }        
    }
    private List<PPIPair> getInteractionPairs(String corpusRoot, List<String> docIds) {
        List<PPIPair> pairs = new ArrayList<PPIPair>();
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");
            
            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                Tree root = trees.get(i);
                SimpleDepGraph depGraph = deps.get(i);
                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                s.setParseTree(root);
                assignPOS(s);
                s = domainAnnotator.annotate(s);
                pairs.addAll(getInteractionPairs(s, depGraph, ccDepGraph));
            }
        }
        return pairs;
    }
    private List<PPIPair> getInteractionPairs(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        List<PPIPair> pairs = new ArrayList<PPIPair>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();
        Map<String, Integer> n1Map = new HashMap<String, Integer>();
        
        //find P1 and N1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token: tokens) {
                    p1Map.put(token, i);
                }
            }
            if (!word.getTag("N1").equals("O")) {
                String[] tokens = word.getTag("N1").split(", ");
                for (String token: tokens) {
                    n1Map.put(token, i);
                }
            }
        }
        
        //find P2 and N2 and match with P1 and N2
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = p1Map.get(token);
                    pairs.add(new PPIPair(s, depGraph, ccDepGraph, entity1, i, true));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = n1Map.get(token);
                    pairs.add(new PPIPair(s, depGraph, ccDepGraph, entity1, i, false));
                }
            }
        }
        return pairs;
    }
    private void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    
    public void checkCandidateExtractor(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        File treesDir = new File(corpusRoot, "trees");
        File depDir = new File(corpusRoot, "deps");
        File ccDepDir = new File(corpusRoot, "depsCC");
        File[] files = iobDir.listFiles();
        
        int tp = 0;
        int fp = 0;
        int fn = 0;
        
        Rule rule1 = new Rule1();
        Rule rule2 = new Rule2();
        Rule rule3 = new Rule3();
        int miss = 0;
        for (File file : files) {
            Text text = TEXT_READER.read(file);
            List<Tree> trees = TREE__READER.read(new File(treesDir, file.getName().replace(".txt", ".mrg")));
            List<SimpleDepGraph> deps = DEP_READER.read(new File(depDir, file.getName().replace(".txt", ".dep")));
            List<SimpleDepGraph> ccDeps = DEP_READER.read(new File(ccDepDir, file.getName().replace(".txt", ".dep")));
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                s.setParseTree(trees.get(i));
                assignPOS(s);
                s = domainAnnotator.annotate(s);
                List<PPIPair> pairs = getInteractionPairs(s, deps.get(i), ccDeps.get(i));
                
                Set<Pair<Integer, Integer>> candidates = rule1.getCandidates(s, deps.get(i), ccDeps.get(i));
                candidates.addAll(rule2.getCandidates(s, deps.get(i), ccDeps.get(i)));
                candidates.addAll(rule3.getCandidates(s, deps.get(i), ccDeps.get(i)));
                
                Map<String, Boolean> map = new HashMap<String, Boolean>();
                for (PPIPair pair : pairs) {
                    map.put(String.valueOf(pair.entity1) + "_" + String.valueOf(pair.entity2), pair.interact);
                }                
                for (Pair<Integer, Integer> candidate : candidates) {
                    String key = String.valueOf(candidate.first()) + "_" + String.valueOf(candidate.second());
                    if (map.containsKey(key)) {
                        if (/*check(s, deps.get(i), ccDeps.get(i), candidate.first(), candidate.second()) != -1 &&*/ map.get(key) == true) {
                            tp++;                            
                        }
                        else {                            
                            fp++;
                        }
                    } else {
                        System.out.println("error");
                    }
                }
                
                for (PPIPair pair : pairs) {
                    if (pair.interact) {
                        Pair<Integer, Integer> p = new Pair<Integer, Integer>(pair.entity1, pair.entity2);
                        if (!candidates.contains(p)) {
                            miss++;
                            System.out.println("Miss");
                            System.out.println(pair.s);
                            System.out.println(pair.s.get(pair.entity1));
                            System.out.println(pair.s.get(pair.entity2));
                            System.out.println(ccDeps.get(i).getPath(pair.entity1, pair.entity2));
                        }
                    }
                }
                
            }
        }
        System.out.println("tp = " + tp);
        System.out.println("fp = " + fp);        
        System.out.println("miss: " + miss);
    }
    
    public static void main(String args[]) {
        NewRuleExtractor extractor = new NewRuleExtractor();
        extractor.checkCandidateExtractor("./resource/relation/PPI2/AIMed");
    }
}
