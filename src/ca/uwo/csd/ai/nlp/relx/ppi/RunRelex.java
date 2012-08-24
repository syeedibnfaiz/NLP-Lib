package ca.uwo.csd.ai.nlp.relx.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import ca.uwo.csd.ai.nlp.ppi.PPIUtility;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RunRelex {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    
    BioDomainAnnotator domainAnnotator;
    Map<String, Integer> map = new HashMap<String, Integer>();
    //static final PPIUtility PPI_UTILITY = new PPIUtility();
    
    private HashSet<String> restrictionTerms;
    
    public RunRelex() {
        this("./resource/relation/biomedical_terms.txt");        
    }
    
    /**
     * Constructs a <code>RunRelex</code> object
     * @param relationTermsFilePath name of the file containing a set of relations terms, each term on a separate line
     */
    public RunRelex(String relationTermsFilePath) {
        restrictionTerms = new HashSet<String>();
        List<String> lines = Util.readLines(relationTermsFilePath);
        for (String line : lines) {            
            restrictionTerms.add(line);
        }
        domainAnnotator = new BioDomainAnnotator(relationTermsFilePath);
    }
    
    public void test(String corpusRoot, int tp_fn) {
        
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        TestResult result = getResult(corpusRoot, docIds);
        System.out.println("TP: " + result.tp);
        System.out.println("FP: " + result.fp);
        double precision = result.tp*1.0/(result.tp + result.fp);
        double recall = result.tp*1.0/tp_fn;
        double fscore = 2*precision*recall/(precision + recall);
        System.out.println("corpus: " + corpusRoot);
        System.out.println("Precision:" + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-score: " + fscore);
    }
    
    private TestResult getResult(String corpusRoot, List<String> docIds) {        
        int tp = 0;
        int fp = 0;
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
                TestResult result = getResult(docId, i, s, depGraph, ccDepGraph);
                tp += result.tp;
                fp += result.fp;
            }
        }
        return new TestResult(tp, fp, -1, -1);
    }

    private void show(String docId, int lineNum) {
        //PPI_UTILITY.showSentence(docId.substring(0, docId.indexOf('_')), docId, lineNum, true, true);
    }
    private TestResult getResult(String docId, int lineNum, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        Set<Pair<Integer, Integer>> goldInteractionPairs = getGoldInteractionPairs(s);
        Set<Pair<Integer, Integer>> predictedInteractions = getPredictedInteractions(s, ccDepGraph);
        int tp = 0;
        int fp = 0;
        for (Pair<Integer, Integer> pair : predictedInteractions) {
            if (goldInteractionPairs.contains(pair)) {
                tp++;
                if (s.size() < 30) {
                    //System.out.println(s);
                }
            } else {
                fp++;
            }
        }
        //get misses -FNs
        for (Pair<Integer, Integer> pair : goldInteractionPairs) {
            if (!predictedInteractions.contains(pair)) {
                System.out.println(docId + "-" + lineNum);
                System.out.println("sent: " + s);
                System.out.println("e1: " + s.get(pair.first()));
                System.out.println("e2: " + s.get(pair.second()));
                System.out.println(ccDepGraph.getPathAsList(pair.first(), pair.second(), false));
                System.out.println("--------------");
            }
        }
        return new TestResult(tp, fp, -1, -1);
    }
    
    public Set<Pair<Integer, Integer>> getPredictedInteractions(Sentence s, SimpleDepGraph ccDepGraph) {                 
        List<RelexRule> rules = new ArrayList<RelexRule>();
        rules.add(new RelexRule1(restrictionTerms));
        rules.add(new RelexRule2(restrictionTerms));
        rules.add(new RelexRule2b());        
        rules.add(new RelexRule3());
        rules.add(new RelexRule5());
        Set<Pair<Integer, Integer>> relations = new HashSet<Pair<Integer, Integer>>();
        for (RelexRule rule : rules) {
            relations.addAll(rule.findRelations(s, ccDepGraph));
        }        
        return relations;
    }
    private Set<Pair<Integer, Integer>> getGoldInteractionPairs(Sentence s) {
        Set<Pair<Integer, Integer>> pairs = new HashSet<Pair<Integer, Integer>>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();        
        
        //find P1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token: tokens) {
                    p1Map.put(token, i);
                }
            }            
        }
        
        //find P2 and match with P1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = p1Map.get(token);                    
                    pairs.add(new Pair<Integer, Integer>(entity1, i));
                }
            }            
        }
        return pairs;
    }
    
    /**
     * Assign POS using the parsed tree
     * @param s 
     */
    public void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    
    private static class TestResult {

        int tp, fp, fn, tn;

        public TestResult() {
        }

        public TestResult(int tp, int fp, int fn, int tn) {
            this.tp = tp;
            this.fp = fp;
            this.fn = fn;
            this.tn = tn;
        }
    }
    
    public void showMap(int limit) {
        Set<Entry<String, Integer>> entrySet = map.entrySet();
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(entrySet);
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return -1;
                } else if (o1.getValue() < o2.getValue()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });        
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
            if (limit == 0) break;
            limit--;            
        }
    }
    public static void main(String args[]) {
        RunRelex tRelex = new RunRelex();
        tRelex.test("./resource/relation/PPI2/AIMed", 1000);
        //tRelex.test("./resource/relation/PPI2/BioInfer", 2534);
        //tRelex.test("./resource/relation/PPI2/HPRD50", 163);
        //tRelex.test("./resource/relation/PPI2/IEPA", 335);
        //tRelex.test("./resource/relation/PPI2/LLL", 164);                
    }
}
