/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBConnAnalyzer {
    final int ONLY_POSITIVE = 1;
    final int ONLY_NEGATIVE = 2;
    final int BOTH = 3;
    
    GenericTextReader textReader;
    SyntaxTreeAnalyzer treeAnalyzer;
    ConnectiveAnalyzer connectiveAnalyzer;
    
    public PDTBConnAnalyzer() {
        textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        treeAnalyzer = new SyntaxTreeAnalyzer();
        connectiveAnalyzer = new ConnectiveAnalyzer();
    }
    
    public void analyze(String iobFile, String treeFile, String depFile, int type) throws IOException {
        Text trainingText = textReader.read(new File(iobFile));
        System.out.println(trainingText.size());        
                
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;        
        
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(depFile));
        Map<String, Integer> map = new HashMap<String, Integer>();
        
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence sentence = trainingText.get(i);            
            SimpleDepGraph depGraph = depGraphs.get(i);            
            line = reader.readLine();            
            if (line.equals("")) continue;            
            sentence.setProperty("parse_tree", line);
            analyze(sentence, depGraph, type, map);
        }
        
        reader.close();
        outputMap(map);
    }
    
    public void outputMap(Map<String, Integer> map) {
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
        }
    }
    public void analyze(Sentence s, SimpleDepGraph depGraph, int type, Map<String, Integer> map) throws IOException {
        Tree root = treeAnalyzer.getPennTree(s.getProperty("parse_tree"));
        List<Pair<Integer, Integer>> candidates = getCandidates(s, type);
        for (Pair<Integer, Integer> candidate : candidates) {
            int start = candidate.first();
            int end = candidate.second();
            String connective = s.toString(start, end);
            String category = connectiveAnalyzer.getCategory(connective.toLowerCase());
            if (category.startsWith("Coord")) {
                Tree lca = treeAnalyzer.getLCA(root, start, end);
                Tree parent = lca.parent(root);
                if (parent.value().startsWith("S")) {
                    incrementKeyValue(map, parent.value());
                } else {
                    incrementKeyValue(map, connective + "-" + parent.value());
                }
            }
        }
    }
    
    public void incrementKeyValue(Map<String, Integer> map, String key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }
    public List<Pair<Integer, Integer>> getCandidates(Sentence s, int type) {
        List<Pair<Integer, Integer>> candidates = new ArrayList<Pair<Integer, Integer>>();
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {            
            int j = -1;
            if ((type == ONLY_NEGATIVE || type == BOTH) && !s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                    if (connectiveAnalyzer.isBaseConnective(s.toString(i, i + k).toLowerCase())) {
                        j = i + k;
                    }
                }
            } else if ((type == ONLY_POSITIVE || type == BOTH) && s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
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
                candidates.add(new Pair<Integer, Integer>(i, j));
                i = j;
            }
        }
        return candidates;
    }

    public static void main(String args[]) throws IOException {
        PDTBConnAnalyzer analyzer = new PDTBConnAnalyzer();
        analyzer.analyze("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_dep_2_22", analyzer.ONLY_POSITIVE);
    }
}
