/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.integration;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Arg2RankConnInstance;
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
public class Arg2Extractor {
    private Classifier classifier;
    static final String pdtbModelPath = "./resource/ml/models/pdtb/pdtb_arg2.model";
    static final String biodrbModelPath = "./resource/ml/models/biodrb/biodrb_arg2.model";
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public Arg2Extractor(boolean pdtb) {
        String modelPath = pdtbModelPath;
        if (!pdtb) {
            modelPath = biodrbModelPath;
        }
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(modelPath));
            classifier =  (Classifier) ois.readObject();                        
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public int extractArg2(Sentence s, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd) {
        List<Integer> headPositions = new ArrayList<Integer>();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < s.size(); i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            if (parent.value().matches("VB.*|NN.*|JJ.*|MD.*|AUX.*")) {
                headPositions.add(i);            
            }
        }
        if (headPositions.isEmpty()) return -1;
        String tree = TREE_ANALYZER.getPennOutput(root);
        Arg2RankConnInstance instance = new Arg2RankConnInstance(tree, depGraph, connStart, connEnd, headPositions, null, -1);
        Instance instance1 = classifier.getInstancePipe().instanceFrom(instance);
        Classification classification = classifier.classify(instance1);
        int index = Integer.parseInt(classification.getLabeling().getBestLabel().toString());
        
        return headPositions.get(index);
    }
    
    public static void main(String args[]) {
        ParserAnnotator parser = new ParserAnnotator();
        SimpleSentReader reader = new SimpleSentReader();
        Scanner in = new Scanner(System.in);
        String line;
        ConnectiveExtractor extractor = new ConnectiveExtractor(true);
        Arg2Extractor arg2Extractor = new Arg2Extractor(true);
        
        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            s = parser.annotate(s);
            Tree root = s.getParseTree();
            String strTree = TREE_ANALYZER.getPennOutput(root);
            s.setProperty("parse_tree", strTree);
            Set<Pair<Integer, Integer>> connectives = extractor.extract(s, null);
            SimpleDepGraph depGraph = new SimpleDepGraph(root, null);
            
            for (Pair<Integer, Integer> pair : connectives) {
                System.out.println("conn:" + s.toString(pair.first(), pair.second()));
                int arg2Head = arg2Extractor.extractArg2(s, root, depGraph, pair.first(), pair.second());
                System.out.println("Arg2Head: " + s.get(arg2Head));
            }
        }
    }
}
