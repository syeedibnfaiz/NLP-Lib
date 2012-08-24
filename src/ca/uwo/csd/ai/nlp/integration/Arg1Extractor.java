/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.integration;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Arg1RankInstance;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Document;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import edu.stanford.nlp.trees.Tree;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Arg1Extractor {
    private Classifier classifier;
    static final String pdtbModelPath = "./resource/ml/models/pdtb/pdtb_arg1.model";
    static final String biodrbModelPath = "./resource/ml/models/biodrb/biodrb_arg1.model";
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    
    private ConnectiveAnalyzer connAnalyzer;
    
    public Arg1Extractor(boolean pdtb) {
        String modelPath = pdtbModelPath;
        if (!pdtb) {
            modelPath = biodrbModelPath;
            connAnalyzer = new ConnectiveAnalyzer("./resource/ml/data/biodrb/connective_types");
        } else {
            connAnalyzer = new ConnectiveAnalyzer();
        }
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(modelPath));
            classifier =  (Classifier) ois.readObject();                        
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    List<Pair<Integer, Integer>> getCandidates(Document document, int arg2Line, int connStart, int connEnd) {
        List<Util.Pair<Integer, Integer>> candidates = new ArrayList<Util.Pair<Integer, Integer>>();
        
        int distance = 10;   
        Sentence arg2Sentence = document.getSentence(arg2Line);
        //String conn = arg2Sentence.toString(connStart, connEnd).toLowerCase();
        //String category = connAnalyzer.getCategory(conn);
        int connHeadPos = connAnalyzer.getHeadWord(arg2Sentence.getParseTree(), connStart, connEnd);
        SimpleDepGraph arg2DepGraph = document.getDepGraph(arg2Line);
        List<Integer> reachable = arg2DepGraph.getReachableIndices(connHeadPos, false, distance);
        for (Integer i : reachable) {
            if (arg2Sentence.get(i).getTag("POS").matches("VB.*|NNS?|JJ.*|MD")) {
                candidates.add(new Util.Pair<Integer, Integer>(arg2Line, i));
            }
        }
        Tree mainHead = HEAD_ANALYZER.getCollinsHead(arg2Sentence.getParseTree().getChild(0));
        if (mainHead != null) {
            int mainHeadPos = TREE_ANALYZER.getLeafPosition(arg2Sentence.getParseTree(), mainHead);
            List<String> pathAsList = arg2DepGraph.getPathAsList(connHeadPos, mainHeadPos, false);
            if (pathAsList != null) {
                distance = distance - (1 + pathAsList.size());
            } else {
                //System.out.println("No path from connHead to mainHead!");
                distance--;
            }
        }
        //if (arg1Line == arg2Line) return candidates;
        
        for (int i = arg2Line - 1; i >= 0 && distance >= 0; i--) {
            Sentence sentence = document.getSentence(i);
            SimpleDepGraph depGraph = document.getDepGraph(i);
            mainHead = HEAD_ANALYZER.getCollinsHead(sentence.getParseTree().getChild(0));
            if (mainHead == null) {
                distance--;
                continue;
            }
            int mainHeadPos = TREE_ANALYZER.getLeafPosition(sentence.getParseTree(), mainHead);
            reachable = depGraph.getReachableIndices(mainHeadPos, false, distance);                        
            if (reachable == null) {
                distance--;
                continue;
            }
            for (Integer j : reachable) {
                if (sentence.get(j).getTag("POS").matches("VB.*|NNS?|JJ.*|MD")) {
                    candidates.add(new Util.Pair<Integer, Integer>(i, j));
                }
            }
            distance -= 2;
        }
        return candidates;
    }
    
    Pair<Integer, Integer> extract(Document document, int arg2Line, int arg2HeadPos, int connStart, int connEnd) {
        List<Pair<Integer, Integer>> candidates = getCandidates(document, arg2Line, connStart, connEnd);
        if (candidates.isEmpty()) return null;
        Arg1RankInstance instance = new Arg1RankInstance(document, candidates, arg2Line, arg2HeadPos, connStart, connEnd, -1);
        Instance instance1 = classifier.getInstancePipe().instanceFrom(instance);
        Classification classification = classifier.classify(instance1);
        int index = Integer.parseInt(classification.getLabeling().getBestLabel().toString());
        
        return candidates.get(index);
    }
    Pair<Integer, Integer> extract(Tree root, SimpleDepGraph depGraph, int arg2HeadPos, int connStart, int connEnd) {
        Document document = new Document(root, depGraph);
        return extract(document, 0, arg2HeadPos, connStart, connEnd);
    }
    public static void main(String args[]) {
        ParserAnnotator parser = new ParserAnnotator();
        SimpleSentReader reader = new SimpleSentReader();
        Scanner in = new Scanner(System.in);
        String line;
        ConnectiveExtractor extractor = new ConnectiveExtractor(true);
        Arg2Extractor arg2Extractor = new Arg2Extractor(true);
        SenseExtractor senseExtractor = new SenseExtractor(true);
        Arg1Extractor arg1Extractor = new Arg1Extractor(true);
        
        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            s = parser.annotate(s);
            Tree root = s.getParseTree();
            String strTree = TREE_ANALYZER.getPennOutput(root);
            s.setProperty("parse_tree", strTree);
            Set<Pair<Integer, Integer>> connectives = extractor.extract(s, null);
            SimpleDepGraph depGraph = new SimpleDepGraph(root, null);
            
            for (Pair<Integer, Integer> pair : connectives) {
                System.out.println("Conn: " + s.toString(pair.first(), pair.second()));
                int arg2Head = arg2Extractor.extractArg2(s, root, depGraph, pair.first(), pair.second());
                System.out.println("Arg2Head: " + s.get(arg2Head));
                String sense = senseExtractor.extract(s, root, depGraph, pair.first(), pair.second(), arg2Head);
                System.out.println("Sense: " + sense);
                int arg1Head = arg1Extractor.extract(root, depGraph, arg2Head, pair.first(), pair.second()).second();
                System.out.println("Arg1Head: " + s.get(arg1Head));
            }
        }
    }
}
