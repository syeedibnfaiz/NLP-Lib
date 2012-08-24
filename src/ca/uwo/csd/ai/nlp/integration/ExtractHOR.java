/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.integration;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Document;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import ca.uwo.csd.ai.nlp.relx.ppi.RunRelex;
import ca.uwo.csd.ai.nlp.utils.OSentenceBoundaryDetector;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


/**
 *
 * @author Syeed Ibn Faiz
 */
public class ExtractHOR {
    final static BioDomainAnnotator DOMAIN_ANNOTATOR = new BioDomainAnnotator();
    final static GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final static PTBFileReader TREE__READER = new PTBFileReader();
    final static SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final RunRelex RELEX = new RunRelex();
    static final OSentenceBoundaryDetector SB_DETECTOR = new OSentenceBoundaryDetector();
    static final SimpleSentReader SENT_READER = new SimpleSentReader();
    static final ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator();    
    
    private ConnectiveExtractor connExtractor;
    private Arg1Extractor arg1Extractor;
    private Arg2Extractor arg2Extractor;
    private SenseExtractor senseExtractor;

    public ExtractHOR() {
        System.out.println("Loading models..");
        connExtractor = new ConnectiveExtractor(false);
        arg2Extractor = new Arg2Extractor(false);
        senseExtractor = new SenseExtractor(false);
        arg1Extractor = new Arg1Extractor(false);
        System.out.println("Loading done.");
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
    
    private void annotateRelations(Sentence s) {
        SimpleDepGraph ccDepGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        Set<Pair<Integer, Integer>> predictedInteractions = RELEX.getPredictedInteractions(s, ccDepGraph);
        int count = 0;
        for (Pair<Integer, Integer> pair : predictedInteractions) {
            int e1 = pair.first();
            int e2 = pair.second();
            TokWord word1 = s.get(e1);
            TokWord word2 = s.get(e2);
            if (word1.getTag("P1") == null) {
                word1.setTag("P1", String.valueOf(count));
            } else {
                word1.setTag("P1", word1.getTag("P1") + ", " + String.valueOf(count));
            }
            if (word2.getTag("P2") == null) {
                word2.setTag("P2", String.valueOf(count));
            } else {
                word2.setTag("P2", word2.getTag("P2") + ", " + String.valueOf(count));
            }
            System.out.println(word1+" - "+word2);
            count++;
        }
        for (TokWord word : s) {
            if (word.getTag("P1") == null) {
                word.setTag("P1", "O");
            }
            if (word.getTag("P2") == null) {
                word.setTag("P2", "O");
            }
        }
    }
    public void extract(String rawText) {
        String[] sentences = SB_DETECTOR.getSentences(rawText);
        Text text = new Text();
        List<Tree> trees = new ArrayList<Tree>();
        List<SimpleDepGraph> deps = new ArrayList<SimpleDepGraph>();
        for (String sent : sentences) {
            Sentence s = SENT_READER.read(sent);
            System.out.println("Sent: " + s);
            s = PARSER_ANNOTATOR.annotate(s);
            trees.add(s.getParseTree());
            deps.add(new SimpleDepGraph(s.getParseTree(), null));
            assignPOS(s);
            s = DOMAIN_ANNOTATOR.annotate(s);
            annotateRelations(s);
            text.add(s);
        }
        
        Document document = new Document(trees, deps);
        for (int i = 0; i < text.size(); i++) {
            extract(document, text, i);
        }        
    }
    public void extractFromPPICorpus(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg"); 
            File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            //File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");
            
            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            //List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);
            
            Document document = new Document(trees, deps);
            for (int i = 0; i < text.size(); i++) {
                extract(document, text, i);                
            }
        }
    }
    
    public void extract(Document document, Text text, int arg2Index) {        
        Sentence s = text.get(arg2Index);
        Tree root = document.getTree(arg2Index);
        SimpleDepGraph depGraph = document.getDepGraph(arg2Index);        
        s.setParseTree(root);
        
        Set<Pair<Integer, Integer>> connectives = connExtractor.extract(s, root, depGraph);        
        for (Pair<Integer, Integer> connective : connectives) {
            int connStart = connective.first();
            int connEnd = connective.second();
            int arg2Head = arg2Extractor.extractArg2(s, root, depGraph, connStart, connEnd);
            if (arg2Head != -1) {
                String sense = senseExtractor.extract(s, root, depGraph, connStart, connEnd, arg2Head);
                Pair<Integer, Integer> arg1 = arg1Extractor.extract(document, arg2Index, arg2Head, connStart, connEnd);
                if (arg1 != null) {
                    int arg1Index = arg1.first();
                    int arg1Head = arg1.second();
                    extract(document, text, arg2Index, arg2Head, arg1Index, arg1Head, connStart, connEnd, sense);
                }
            }
        }        
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
    
    public void extract(Document document, Text text, int arg2Index, int arg2Head, int arg1Index, int arg1Head, int connStart, int connEnd, String sense) {
        Sentence arg2Sentence = text.get(arg2Index);
        Sentence arg1Sentence = text.get(arg1Index);
        Set<Pair<Integer, Integer>> arg2Pairs = getHeadOrientedPairs(arg2Sentence, arg2Head);
        Set<Pair<Integer, Integer>> arg1Pairs = getHeadOrientedPairs(arg1Sentence, arg1Head);
        if (!arg1Pairs.isEmpty() && !arg2Pairs.isEmpty()) {
            String conn = arg2Sentence.toString(connStart, connEnd);
            String arg2HeadWord = arg2Sentence.get(arg2Head).word();
            String arg1HeadWord = arg1Sentence.get(arg1Head).word();
            System.out.println("");
            System.out.println(arg1HeadWord + " | " + conn + " | " + arg2HeadWord + " | " + sense);
            if (arg1Index != arg2Index) {
                System.out.println("Arg1Sent: " + arg1Sentence);
                System.out.println("Arg2Sent: " + arg2Sentence);
            } else {
                System.out.println("Sent: " + arg2Sentence);
            }
            Set<Integer> arg1Span = new HashSet<Integer>();
            Set<Integer> arg2Span = new HashSet<Integer>();
            getArgumentSpans(document, arg2Index, arg1Index, arg2Head, arg1Head, arg2Span, arg1Span);
            
            for (Pair<Integer, Integer> arg1Pair : arg1Pairs) {
                if (arg1Span.contains(arg1Pair.first()) && arg1Span.contains(arg1Pair.second())) {
                    for (Pair<Integer, Integer> arg2Pair : arg2Pairs) {
                        if (arg2Span.contains(arg2Pair.first()) && arg2Span.contains(arg2Pair.second())) {
                            System.out.print(arg1Sentence.get(arg1Pair.first()) + "-" + arg1Sentence.get(arg1Pair.second()));
                            System.out.print(" : ");
                            System.out.println(arg2Sentence.get(arg2Pair.first()) + "-" + arg2Sentence.get(arg2Pair.second()));
                        }
                    }
                }
            }
        }
    }
    
    private Set<Pair<Integer, Integer>> getHeadOrientedPairs(Sentence s, int headPos) {
        Set<Pair<Integer, Integer>> pairs = new HashSet<Pair<Integer, Integer>>();
        Set<Pair<Integer, Integer>> goldInteractionPairs = getGoldInteractionPairs(s);
        for (Pair<Integer, Integer> interaction : goldInteractionPairs) {
            int e1 = interaction.first();
            int e2 = interaction.second();
            if ((e1 <= headPos && headPos <= e2) || (Math.abs(e1-headPos) < 5) || (Math.abs(e2-headPos) < 5)) {
                pairs.add(interaction);
            }
        }
        return pairs;
    }
    private int getBetterHeadPos(Tree root, SimpleDepGraph depGraph, int oldPos) {
        int newPos = oldPos;        
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(oldPos);
        if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("det|nn|cop|aux.*|prep.*|complm")) {
            newPos = depDependencies.get(0).gov();
        }
        return newPos;
    }
    
    private void getArgumentSpans(Document document, int arg2Index, int arg1Index, int arg2Head, int arg1Head, Set<Integer> arg2Span, Set<Integer> arg1Span) {
        SimpleDepGraph arg1DepGraph = document.getDepGraph(arg1Index);
        SimpleDepGraph arg2DepGraph = document.getDepGraph(arg2Index);
        
        arg1Head = getBetterHeadPos(document.getTree(arg1Index), arg1DepGraph, arg1Head);
        arg2Head = getBetterHeadPos(document.getTree(arg2Index), arg2DepGraph, arg2Head);
        
        List<Integer> reachable1 = arg1DepGraph.getReachableIndices(arg1Head, true, 100);
        List<Integer> reachable2 = arg2DepGraph.getReachableIndices(arg2Head, true, 100);
        
        if (arg1Index == arg2Index) {
            if (reachable1.contains(arg2Head)) {
                for (Integer i : reachable2) {
                    reachable1.remove(i);
                }
            } else if (reachable2.contains(arg1Head)) {
                for (Integer i : reachable1) {
                    reachable2.remove(i);
                }
            }
        }
        
        arg1Span.addAll(reachable1);
        arg2Span.addAll(reachable2);
    }
    
    public static void main(String args[]) {
        ExtractHOR extractor = new ExtractHOR();
        //extractor.extractFromPPICorpus("./resource/relation/PPI2/BioInfer");
        Scanner in = new Scanner(System.in);
        String line;
        while ((line = in.nextLine()) != null) {
            extractor.extract(line);
        }
    }
}
