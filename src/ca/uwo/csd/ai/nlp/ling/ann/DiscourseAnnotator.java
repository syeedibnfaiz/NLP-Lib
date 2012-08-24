/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Arg1ConnInstance;
import ca.uwo.csd.ai.nlp.ml.pdtb.arg.Arg2ConnInstance;
import ca.uwo.csd.ai.nlp.ml.ConnectiveInstance;
import ca.uwo.csd.ai.nlp.ml.PDTBConnectiveInstance;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.types.Instance;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed ibn Faiz
 */
public class DiscourseAnnotator implements Annotator {
    private static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    private static final ConnectiveAnalyzer CONN_ANALYZER = new ConnectiveAnalyzer();
    private static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    private static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    private static final ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator();
    private Classifier connClassifier;
    private Classifier arg2Classifier;
    private Classifier arg1Classifier;
    private HashSet<String> connSet;

    public DiscourseAnnotator() {
        this("./resource/ml/models/pdtb/pdtb_conn.model", "./resource/ml/models/pdtb/pdtb_arg1.model", "./resource/ml/models/pdtb/pdtb_arg2.model");
    }
            
    public DiscourseAnnotator(String path2ConnClassifier, String path2Arg1Classifier, String path2Arg2Classifier) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2ConnClassifier));
            connClassifier =  (Classifier) ois.readObject();            
            //ois = new ObjectInputStream(new FileInputStream(path2Arg2Classifier));
            //arg2Classifier =  (Classifier) ois.readObject();
            //ois = new ObjectInputStream(new FileInputStream(path2Arg1Classifier));
            //arg1Classifier =  (Classifier) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        connSet = new HashSet<String>();
        fillConnSet();
    }
    private void fillConnSet() {
        try {
            ////BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/all_conn_lexicon.txt"));
            //BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/explicit_conn_types"));
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_explicit_conn_types.txt"));            
            String line;
            while ((line = reader.readLine()) != null) {
                connSet.add(line);                
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(DiscourseAnnotator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public Sentence annotate(Sentence s) {
        if (!s.isAnnotatedBy("PARSED")) {
            s = PARSER_ANNOTATOR.annotate(s);
        }
        Tree root = s.getParseTree();
        String strTree = TREE_ANALYZER.getPennOutput(root);
        /*GrammaticalStructure gs = GSF.newGrammaticalStructure(root);
        Collection<TypedDependency> dependencies = gs.typedDependencies();
        SimpleDepGraph depGraph = new SimpleDepGraph(dependencies);*/
        //root.pennPrint();
        //System.out.println(dependencies);
        s.setProperty("parse_tree", strTree);
        //annotate(s, root, depGraph);
        annotate(s, root, null);
        return s;
    }

    private void annotate(Sentence s, Tree root, SimpleDepGraph depGraph) {
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            int j = -1;
            for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                if (connSet.contains(s.toString(i, i + k).toLowerCase())) {
                    j = i + k;
                }
            }
            if (j != -1) {
                PDTBConnectiveInstance instance = new PDTBConnectiveInstance(s, i, j, true, depGraph);
                Instance instance1 = connClassifier.getInstancePipe().instanceFrom(instance);
                Classification classification = connClassifier.classify(instance1);
                if (classification.bestLabelIsCorrect()) {
                    //System.out.println(s.toString(i, j) + " is a connective.");
                    for (int k = i; k <= j; k++) {
                        if (k == i) s.get(k).setTag("DIS_CONN", "B-conn");
                        else s.get(k).setTag("DIS_CONN", "I-conn");
                    }
                    //annotateArg2Head(s, root, depGraph, i, j);
                }
            }
        }
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).getTag("DIS_CONN") == null) {
                s.get(i).setTag("DIS_CONN", "O");
            }
            /*if (s.get(i).getTag("ARG2") == null) {
                s.get(i).setTag("ARG2", "O");
            }
            if (s.get(i).getTag("ARG1") == null) {
                s.get(i).setTag("ARG1", "O");
            }*/
        }
    }
    private void annotateArg2Head(Sentence s, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd) {
        String conn = s.toString(connStart, connEnd);
        String category = CONN_ANALYZER.getCategory(conn.toLowerCase());
        String strTree = TREE_ANALYZER.getPennOutput(root);
        List<Tree> leaves = root.getLeaves();
        
        int initialPos = 0;
        if (category.matches("Sub.*|Coord.*")) {
            initialPos = connEnd + 1;
        }
        int sz = leaves.size();
        double maxScore = -100000.0;
        int bestHeadPos = -1;
        for (int i = initialPos; i < sz; i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            if (parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                Arg2ConnInstance instance = new Arg2ConnInstance(strTree, depGraph, connStart, connEnd, i, true, null);
                Instance instance1 = arg2Classifier.getInstancePipe().instanceFrom(instance);
                Classification classification = arg2Classifier.classify(instance1);
                MaxEnt maxEnt = (MaxEnt) arg2Classifier;
                double score[] = new double[2];
                maxEnt.getClassificationScores(instance1, score);
                double best = 0;
                if (classification.bestLabelIsCorrect()) {
                    best = Math.max(score[0], score[1]);
                } else {
                    best = Math.min(score[0], score[1]);
                }
                if (best > maxScore) {
                    maxScore = best;
                    bestHeadPos = i;
                }
            }
        }
        if (bestHeadPos != -1) {
            s.get(bestHeadPos).setTag("ARG2", String.valueOf(connStart));
            if (category.matches("Sub.*") ||
                (category.matches("Conj.*|Coor.*") && connStart > 0)) {
                annotateArg1Head(s, root, depGraph, connStart, connEnd, bestHeadPos);
            }
        }        
    }
    private void annotateArg1Head(Sentence s, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int arg2HeadPos) {
        String conn = s.toString(connStart, connEnd);        
        String strTree = TREE_ANALYZER.getPennOutput(root);
        List<Tree> leaves = root.getLeaves();
        
        double maxScore = -100000.0;
        int bestHeadPos = -1;
        for (int i = 0; i < leaves.size(); i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            if (parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                Arg1ConnInstance instance = new Arg1ConnInstance(strTree, depGraph, connStart, connEnd, i, arg2HeadPos, "true", null);
                Instance instance1 = arg1Classifier.getInstancePipe().instanceFrom(instance);
                Classification classification = arg1Classifier.classify(instance1);
                MaxEnt maxEnt = (MaxEnt) arg1Classifier;
                double score[] = new double[2];
                maxEnt.getClassificationScores(instance1, score);
                double best = 0;
                if (classification.bestLabelIsCorrect()) {
                    best = Math.max(score[0], score[1]);
                } else {
                    best = Math.min(score[0], score[1]);
                }
                if (best > maxScore) {
                    maxScore = best;
                    bestHeadPos = i;
                }
            }
        }
        if (bestHeadPos != -1) {
            s.get(bestHeadPos).setTag("ARG1", String.valueOf(connStart));
        }
    }
    
    @Override
    public String[] getFieldNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static void main(String args[]) {
        SimpleSentReader sentReader = new SimpleSentReader();
        ParserAnnotator annotator = new ParserAnnotator();
        DiscourseAnnotator discAnnotator = new DiscourseAnnotator();
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            s = discAnnotator.annotate(s);
            System.out.println(s.toString(new String[]{"DIS_CONN"}));
            /*System.out.println(s.toString(new String[]{"DIS_CONN", "ARG1", "ARG2"}));
            System.out.println("===============================\n");
            for (int i = 0; i < s.size(); i++) {
                TokWord word = s.get(i);
                if (!word.getTag("DIS_CONN").equals("O")) {
                    String pos = String.valueOf(i);
                    String arg1 = null;
                    String arg2 = null;
                    for (int j = 0; j < s.size(); j++) {
                        if (s.get(j).getTag("ARG1").equals(pos)) arg1 = s.get(j).word() + "-"+j;
                        if (s.get(j).getTag("ARG2").equals(pos)) arg2 = s.get(j).word() + "-"+j;
                    }
                    System.out.print("Arg1:-" + arg1);
                    String conn = word.word();
                    for (int j = i + 1; j < s.size() && s.get(j).getTag("DIS_CONN").startsWith("I"); j++) {
                        conn += " " + s.get(j).word();
                    }
                    System.out.print(" :: " + conn + " :: ");
                    System.out.println("Arg2:-" + arg2);
                }
            }
            System.out.println("===============================");*/
        }
    }
}
