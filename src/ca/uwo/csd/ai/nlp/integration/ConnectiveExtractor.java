/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.integration;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ml.PDTBConnectiveInstance;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonatuni
 */
public class ConnectiveExtractor {
    private Classifier classifier;
    private HashSet<String> connSet;
    static final String pdtbModelPath = "./resource/ml/models/pdtb/pdtb_conn.model";
    static final String biodrbModelPath = "./resource/ml/models/biodrb/biodrb_conn.model";
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public ConnectiveExtractor(boolean pdtb) {
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
        connSet = new HashSet<String>();
        if (pdtb) {
            fillConnSet("./resource/ml/data/pdtb/explicit_conn_types");
        } else {
            fillConnSet("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_explicit_conn_types.txt");
        }
    }
    
    private void fillConnSet(String connLexiconPath) {
        try {            
            BufferedReader reader = new BufferedReader(new FileReader(connLexiconPath));
            String line;
            while ((line = reader.readLine()) != null) {
                connSet.add(line);                
            }
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(ConnectiveExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public Set<Pair<Integer,Integer>> extract(Sentence s, Tree root, SimpleDepGraph depGraph) {
        String tree = TREE_ANALYZER.getPennOutput(root);
        s.setProperty("parse_tree", tree);
        return extract(s, depGraph);
    }
    
    public Set<Pair<Integer,Integer>> extract(Sentence s, SimpleDepGraph depGraph) {
        Set<Pair<Integer,Integer>> result = new HashSet<Pair<Integer, Integer>>();
        
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
                Instance instance1 = classifier.getInstancePipe().instanceFrom(instance);
                Classification classification = classifier.classify(instance1);
                if (classification.bestLabelIsCorrect()) {
                    result.add(new Pair<Integer, Integer>(i, j));
                }
                i = j;
            }
        }
        return result;
    }
    
    private Set<Pair<Integer,Integer>> getGoldInstances(Sentence s) {        
        Set<Pair<Integer,Integer>> connectives = new HashSet<Pair<Integer, Integer>>();
        
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                j = i;
                for (int k = i + 1; k < words.size(); k++) {
                    if (s.get(k).getTag("CONN").startsWith("I")) {
                        j = k;
                    } else {
                        break;
                    }
                }
            }
            if (j != -1) {
                String conn = s.toString(i, j).toLowerCase();
                if (connSet.contains(conn)) {
                    connectives.add(new Pair<Integer, Integer>(i, j));
                    i = j;
                }
            }
        }
        return connectives;
    }
    
    public static void testCorpus(String iobFile, String treeFile, boolean pdtb) throws IOException {        
        ConnectiveExtractor extractor = new ConnectiveExtractor(pdtb);        
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        Text trainingText = textReader.read(new File(iobFile));
        
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;
        int tp = 0;
        int fp = 0;
        int tp_fn = 0;
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence sentence = trainingText.get(i);            
            line = reader.readLine();
            if (line.equals("")) continue;            
            sentence.setProperty("parse_tree", line);
            Set<Pair<Integer, Integer>> goldInstances = extractor.getGoldInstances(sentence);
            tp_fn += goldInstances.size();
            
            Set<Pair<Integer, Integer>> extract = extractor.extract(sentence, null);
            for (Pair<Integer, Integer> pair : extract) {
                if (goldInstances.contains(pair)) {
                    tp++;
                } else {
                    fp++;
                }
            }
        }
        System.out.println("Precision: " + (tp/(1.0*(tp + fp))));
        System.out.println("Recall: " + (tp/(1.0*(tp_fn))));
    }
    
    public static void main(String args[]) throws IOException {
        testCorpus("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn2.txt","./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree", false);
        //testCorpus("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22", true);
        
        /*ParserAnnotator parser = new ParserAnnotator();
        SimpleSentReader reader = new SimpleSentReader();
        Scanner in = new Scanner(System.in);
        String line;
        ConnectiveExtractor extractor = new ConnectiveExtractor(false);
        
        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            s = parser.annotate(s);
            Tree root = s.getParseTree();
            String strTree = TREE_ANALYZER.getPennOutput(root);
            s.setProperty("parse_tree", strTree);
            List<Pair<Integer, Integer>> connectives = extractor.extract(s, null);
            for (Pair<Integer, Integer> pair : connectives) {
                System.out.println(s.toString(pair.first(), pair.second()));
            }
        }*/
    }
}
