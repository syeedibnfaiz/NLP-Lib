/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.analysis;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author tonatuni
 */
public class PDTBConnectiveAnalysis {
    final static ConnectiveAnalyzer CONN_ANALYZER = new ConnectiveAnalyzer();
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    final static HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    //final static TreeFactory tf = new LabeledScoredTreeFactory();
    
    public static class Connective {
        String conn;
        String cat;
        int start;
        int end;

        public Connective(String conn, String cat, int start, int end) {
            this.conn = conn;
            this.cat = cat;
            this.start = start;
            this.end = end;
        }
        
    }
    public static void analyzeSyntax(String mainFile, String treeFile, String depFile) throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        Text text = textReader.read(new File(mainFile));
        //PTBFileReader ptbReader = new PTBFileReader();
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(depFile));
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;
        
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for (int j = 0; j < text.size(); j++) {
            Sentence s = text.get(j);
            line = reader.readLine();
            Tree root = TREE_ANALYZER.getPennTree(line); 
            SimpleDepGraph depGraph = depGraphs.get(j);
            for (int i = 0; i < s.size(); i++) {
                if (s.get(i).getTag("CONN").matches("D?B.*")) {
                    Connective conn = getConnective(s, i);
                    if (conn.cat.startsWith("Conj")) {
                        //analyzeSelfcategory(s, root, conn, countMap);
                        analyzeDependency(s, root, conn, depGraph, countMap);
                    }
                }
            }
        }
        
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(countMap.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
        
    }
    public static void compare(String mainFile, String treeFile, String depFile) throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        Text text = textReader.read(new File(mainFile));
        //PTBFileReader ptbReader = new PTBFileReader();
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(depFile));
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;
        
        Map<String, Integer> posCountMap = new HashMap<String, Integer>();
        Map<String, Integer> negCountMap = new HashMap<String, Integer>();
        int index = 0;
        for (Sentence s : text) {
            ArrayList<String> words = s.getWords();
            line = reader.readLine();
            Tree root = TREE_ANALYZER.getPennTree(line); 
            SimpleDepGraph depGraph = depGraphs.get(index++);
            
            for (int i = 0; i < words.size(); i++) {

                int j = -1;
                if (!s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                    for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {
                        if (CONN_ANALYZER.isBaseConnective(s.toString(i, i + k).toLowerCase())) {
                            j = i + k;
                        }
                    }
                } else {
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
                    String conn = s.toString(i, j);
                    String cat = CONN_ANALYZER.getCategory(conn.toLowerCase());
                    if (cat == null) {
                        cat = "Conj-adverb";
                    }
                    if (cat.startsWith("Sub")) {
                        Connective connective = new Connective(conn.toLowerCase(), cat, i, j);
                        if (s.get(i).getTag("CONN").startsWith("B")) {
                            //true        
                            analyzeSelfcategory(s, root, connective, posCountMap, true);
                            //analyzeDependency(s, root, connective, depGraph, posCountMap);
                        } else if (s.get(i).getTag("CONN").startsWith("DB")) {                 //if..then, either..or
                            //true                        
                            analyzeSelfcategory(s, root, connective, posCountMap, true);
                            //analyzeDependency(s, root, connective, depGraph, posCountMap);
                        } else if (!s.get(i).getTag("CONN").startsWith("DI")) {                 //don't take then of if..then                           
                            //false
                            analyzeSelfcategory(s, root, connective, negCountMap, false);
                            //analyzeDependency(s, root, connective, depGraph, negCountMap);
                        }
                    }
                    i = j;
                }
            }
        }
        
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(posCountMap.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        
        System.out.println("True Class");
        System.out.println("--------------------------");
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
        System.out.println("--------------------------");
        
        System.out.println("False Class");
        list = new ArrayList<Entry<String, Integer>>(negCountMap.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return -o1.getValue().compareTo(o2.getValue());
            }
        });
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
        System.out.println("===========================");
    }
    
    public static void analyzeSelfcategory(Sentence s, Tree root, Connective conn, Map<String, Integer> countMap, boolean b) {
        Tree lca = TREE_ANALYZER.getLCA(root, conn.start, conn.end);
        String str = "null";
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != root && parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }
            if (lca != null) {
                Tree right = TREE_ANALYZER.getSibling(root, lca, 1);
                /*if (right != null) {
                    Tree head = HEAD_ANALYZER.getCollinsHead(right);
                    if (head != null){                        
                        //str = head.value();
                        str = head.parent(root).value();
                    } 
                    else System.out.println("Null Head!!");
                    //str = left.value();
                }*/
                //str = lca.value();
                Tree left = TREE_ANALYZER.getSibling(root, lca, -1);
                str = "";
                if (left != null) str += left.value().split("-")[0] + "-";
                else str += "null-";
                
                if (right != null) str += right.value().split("-")[0];
                else str += "null";
            }                 
        }
        /*String str = "null";
        List<Tree> leaves = root.getLeaves();
        if (conn.start > 0) {
            Tree lca = TREE_ANALYZER.getLCA(root, conn.start-1, conn.start);
            if (lca != null) {
                str = lca.value();
            }
        }*/
        /*if (b == true && str.equals("VP")) {
            System.out.println("Conn: " + conn.conn);
            System.out.println(s);
        }*/
        //str = conn.conn;
        if (countMap.containsKey(str)) {
            countMap.put(str, countMap.get(str) + 1);
        } else {
            countMap.put(str, 1);
        }
    }
    public static void analyzeDependency(Sentence s, Tree root, Connective conn, SimpleDepGraph depGraph, Map<String, Integer> countMap) {
        int head = CONN_ANALYZER.getHeadWord(root, conn.start, conn.end);
        List<SimpleDependency> dependencies = depGraph.getDepDependencies(head);
        List<Tree> leaves = root.getLeaves();
        for (SimpleDependency sDep : dependencies) {
            int gov = sDep.gov();
            String reln = sDep.reln();
            Tree govTree = leaves.get(gov);
            String govPOS = govTree.parent(root).value();
            //String str = reln + "-" + govPOS;
            //String str = String.valueOf(gov < conn.start);
            boolean govHasSubj = false;
            //String str = s.get(gov).word();
            List<SimpleDependency> govDependencies = depGraph.getGovDependencies(gov);
            for (SimpleDependency tmp : govDependencies) {
                if (tmp.reln().matches(".*subj.*")) {
                    govHasSubj = true;
                    break;
                }
            }
            String str = String.valueOf(govHasSubj);
            if (countMap.containsKey(str)) {
                countMap.put(str, countMap.get(str) + 1);
            } else {
                countMap.put(str, 1);
            }
            break;
        }
    }
    public static Connective getConnective(Sentence s, int start) {
        int end = start;
        for (int i = start + 1; i < s.size(); i++) {
            if (s.get(i).getTag("CONN").startsWith("I")) {
                end = i;
            } else {
                break;
            }
        }
        String conn = s.toString(start, end).toLowerCase();
        String cat = CONN_ANALYZER.getCategory(conn);
        if (cat == null) cat = "Conj-adverb";
        
        return new Connective(conn, cat, start, end);
    }
    
    public static void main(String args[]) throws IOException {
       //compare("./resource/ml/data/pdtb/conn_id/explicit_relations_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_2_24");
       compare("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22");
        //analyzeSyntax("./resource/ml/data/pdtb/conn_id/explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_2_22");
    }
}
