/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.aimed;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
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
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class AnalyzeAImed {
    final static GeniaTagger TAGGER = null;//new GeniaTagger();
    
    public void analyze(String iobRootPath, String parseRootPath, String depRootPath) {
        File rootDir = new File(iobRootPath);
        File parseRootDir = new File(parseRootPath);
        File depRootDir = new File(depRootPath);
        File[] files = rootDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
        PTBFileReader ptbReader = new PTBFileReader();
        SimpleDepFileReader depReader = new SimpleDepFileReader();
        Map<String, Integer> analysisMap = new HashMap<String, Integer>();
        
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
            Text text = reader.read(file);
            File parseFile = new File(parseRootDir, file.getName() + ".mrg");
            File depFile = new File(depRootDir, file.getName() + ".dep");
            List<Tree> trees = ptbReader.read(parseFile);
            List<SimpleDepGraph> depGraphs = depReader.read(depFile);
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                //System.out.println(s);
                SimpleDepGraph depGraph = depGraphs.get(i);
                try {
                    //s = TAGGER.annotate(s);              
                    s.setParseTree(trees.get(i));                    
                    analyzeSentence(s, depGraph, analysisMap);
                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex.getMessage());
                    continue;
                }                
            }
        }
        
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(analysisMap.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>> () {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (o1.getValue() < o2.getValue()) return -1;
                else if (o1.getValue() > o2.getValue()) return 1;
                return 0;
            }
            
        });
        
        for (Entry<String, Integer> entry : list) {
            System.out.println(entry.getKey() + "-" + entry.getValue());
        }
    }
    
    void analyzeSentence(Sentence s, SimpleDepGraph depGraph, Map<String, Integer> analysisMap) {
        Set<String> relations = new HashSet<String>();
        Map<String, Integer> p1PairToIndex = new HashMap<String, Integer>();
        
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O") && word.getTag("LEXE").equals("B")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token : tokens) {
                    p1PairToIndex.put(token, i);
                }
            }
            if (!word.getTag("P2").equals("O")  && word.getTag("LEXE").equals("B")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token : tokens) {
                    if (p1PairToIndex.containsKey(token)) {
                        int first = p1PairToIndex.get(token);
                        //relations.add(first + "-" + i);
                        analyzeRelation(s, depGraph, analysisMap, first, i);
                    } else {
                        System.out.println("WTF!!");
                    }
                }
            }
        }
    }
    
    void analyzeRelation(Sentence s, SimpleDepGraph depGraph, Map<String, Integer> analysisMap, int first, int second) {
        first = getEntityHeadPosition(s, depGraph, first, "det|nn|amod|abbrev|num|conj");
        second = getEntityHeadPosition(s, depGraph, second, "det|nn|amod|num");
        //int lcs = depGraph.getLCS(first, second);
        //String lcsWord = s.get(lcs).word();
        //add(analysisMap, lcsWord.toLowerCase());
        //add(analysisMap, s.get(first).word());
        //add(analysisMap, "-"+s.get(second).word());
        //int root1 = getRoot(depGraph, first);
        //int root2 = getRoot(depGraph, second);
        //add(analysisMap, s.get(root1).word());
        //add(analysisMap, s.get(root2).word());
        //add(analysisMap, String.valueOf(root1 == root2));
        List<String> pathList = depGraph.getPathAsList(first, second, false);
        String path = pathList.toString();        
        boolean test = true;
        StringBuilder sb = new StringBuilder();
        for (String reln : pathList) {
            if (!reln.matches("-?(nn|amod)")) {
                sb.append(reln + ", ");
            }
        }
        add(analysisMap, sb.toString());
        //add(analysisMap, String.valueOf(pathList.get(0)));
        //add(analysisMap, String.valueOf(pathList != null && pathList.size() == 1));
        
        
        //add(analysisMap, path);
        
        if (test && false) {
            System.out.println("======");
            System.out.println(s.get(first).word());
            System.out.println(s.get(second).word());
            System.out.println(s);
            System.out.println(path);
            System.out.println(depGraph.toString(s.getParseTree()));
        }
    }
    
    int getRoot(SimpleDepGraph depGraph, int pos) {        
        while (true) {
            List<SimpleDependency> deps = depGraph.getDepDependencies(pos);
            if (deps.isEmpty()) break;
            pos = deps.get(0).gov();
        }
        return pos;
    }
    int getEntityHeadPosition(Sentence s, SimpleDepGraph depGraph, int entity, String pattern) {
        /*int endPos = startPos;
        for (int i = startPos + 1; i < s.size(); i++) {
        if (s.get(i).getTag("LEXE").equals("I")) {
        endPos = i;
        } else {
        break;
        }
        }
         return endPos;*/
        boolean visited[] = new boolean[s.size()];
        int dep = entity;
        while (true) {
            visited[dep] = true;
            List<SimpleDependency> deps = depGraph.getDepDependencies(dep, Pattern.compile(pattern));
            if (deps.isEmpty()) return dep;
            dep = deps.get(0).gov();
            if (visited[dep]) {
                return entity;
            }
        }
        
        
        /*Tree root = s.getParseTree();        
        Tree lca = TREE_ANALYZER.getLCA(root, startPos, endPos);
        Tree head = HEAD_ANALYZER.getCollinsHead(lca);
        int pos = TREE_ANALYZER.getLeafPosition(root, head);*/
        
        //return pos;
    }
    
    void add(Map <String, Integer> analysisMap, String s) {
        if (analysisMap.containsKey(s)) {
            analysisMap.put(s, analysisMap.get(s) + 1);
        } else {
            analysisMap.put(s, 1);
        }
    }
    
    public void checkSynchronization(String iobRootPath, String parseRootPath) {
        File rootDir = new File(iobRootPath);
        File parseRootDir = new File(parseRootPath);        
        File[] files = rootDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
        PTBFileReader ptbReader = new PTBFileReader();
        
        int count = 0;
        for (File file : files) {
            //System.out.println(file.getAbsolutePath());
            Text text = reader.read(file);
            File parseFile = new File(parseRootDir, file.getName() + ".mrg");            
            List<Tree> trees = ptbReader.read(parseFile);            
            //System.out.println("size: " + text.size() + " & " + trees.size());
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                Tree root  = trees.get(i);
                List<Tree> leaves = root.getLeaves();
                if (s.size() != leaves.size()) {
                    System.out.println("Size mismatch!");
                } else {
                    for (int j = 0; j < s.size(); j++) {
                        if (!s.get(j).word().equals(leaves.get(j).value())) {
                            System.out.println("Word mismatch: " + s.get(j).word() + " & " + leaves.get(j).value());
                        }
                        if (s.get(j).getTag("LEXE").equals("B")) {
                            count++;
                        }
                    }
                }
            }
        }
        System.out.println("#protein: " + count);
    }
    
    public void analyzeDiscourseRelations(String iobRootPath, String parseRootPath) {
        File rootDir = new File(iobRootPath);
        File parseRootDir = new File(parseRootPath);        
        File[] files = rootDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
        PTBFileReader ptbReader = new PTBFileReader();
        //DiscourseAnnotator discourseAnnotator = new DiscourseAnnotator();
        DiscourseAnnotator discourseAnnotator = new DiscourseAnnotator("./resource/ml/models/biodrb/biodrb_conn.model", null, null);
        HashMap<String, Integer> connMap = new HashMap<String, Integer>();
        for (File file : files) {
            //System.out.println(file.getAbsolutePath());
            Text text = reader.read(file);
            File parseFile = new File(parseRootDir, file.getName().replace(".txt", ".mrg"));            
            List<Tree> trees = ptbReader.read(parseFile);                                    
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                Tree root  = trees.get(i);
                s.setParseTree(root);
                s.markAnnotation("PARSED");
                s = discourseAnnotator.annotate(s);
                boolean flag = false;
                boolean relation = false;
                for (int j = 0; j < s.size(); j++) {
                    TokWord word = s.get(j);
                    if (word.getTag("DIS_CONN").equals("B-conn") && !(word.word().matches("also|by|to|However"))) {
                        flag = true;      
                        String conn = getConnString(s, j).toLowerCase();
                        if (connMap.containsKey(conn)) {
                            connMap.put(conn, connMap.get(conn) + 1);
                        } else {
                            connMap.put(conn, 1);
                        }
                    } else if (!word.getTag("P1").equals("O")) {
                        relation = true;
                    }
                }
                if (flag && relation) {
                    //System.out.println(s);
                    
                    for (TokWord word : s) {
                        System.out.print(word.word());
                        if (!word.getTag("DIS_CONN").equals("O")) {
                            System.out.print("/" + word.getTag("DIS_CONN"));
                        }
                        System.out.print(" ");
                    }
                    if (relation) {
                        System.out.println("\n*");
                    } else {
                        System.out.println("");
                    }
                    System.out.println(parseFile.getName());
                    System.out.println(i);
                    System.out.println("----------------------------");
                }
            }
        }
        
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(connMap.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
    }
    private String getConnString(Sentence s, int startIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.get(startIndex).word());
        for (int i = startIndex + 1; i < s.size(); i++) {
            if (s.get(i).getTag("DIS_CONN").startsWith("I")) {
                sb.append(" ").append(s.get(i).word());
            }
        }
        return sb.toString();
    }
    public static void main(String args[]) {
        AnalyzeAImed analyzer = new AnalyzeAImed();
        //analyzer.analyze("./resource/relation/aimed/iob", "./resource/relation/aimed/trees", "./resource/relation/aimed/deps");
        //analyzer.analyze("./resource/relation/aimed/iob", "./resource/relation/aimed/trees", "./resource/relation/aimed/minipar_dep");
        //analyzer.checkSynchronization("./resource/relation/aimed/iob", "./resource/relation/aimed/trees");
        
        //get discourse connectives marked
        //analyzer.analyzeDiscourseRelations("./resource/relation/PPI2/AImed/iob", "./resource/relation/PPI2/AImed/trees");
        analyzer.analyzeDiscourseRelations("./resource/relation/PPI2/BioInfer/iob", "./resource/relation/PPI2/BioInfer/trees");
    }
}
