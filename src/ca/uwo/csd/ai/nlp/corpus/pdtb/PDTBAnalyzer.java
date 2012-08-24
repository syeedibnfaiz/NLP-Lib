/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.OChunker;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PDTBAnalyzer {
    String pipedAnnRoot;
    String rawRoot;
    String ptbRoot;
    static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    static final ConnectiveAnalyzer CONNECTIVE_ANALYZER = new ConnectiveAnalyzer();
    static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    
    public PDTBAnalyzer(String pipedAnnRoot, String rawRoot, String ptbRoot) {
        this.pipedAnnRoot = pipedAnnRoot;
        this.rawRoot = rawRoot;
        this.ptbRoot = ptbRoot;
        if (!new File(pipedAnnRoot).isDirectory() || !new File(rawRoot).isDirectory() || !new File(ptbRoot).isDirectory()) {
            throw new IllegalArgumentException("pipedAnnRoot, rawroot and ptbRoot should be existing directories.");
        }
    }
    
    public List<String> getUniqueConnHead() {        
        HashSet<String> connHeadSet = new HashSet<String>();
        
        HashSet<String> skipSectionSet = new HashSet<String>();
        //skipSectionSet.add("00");skipSectionSet.add("01");skipSectionSet.add("23");skipSectionSet.add("24");  
        
        File pipedAnnRootDir = new File(pipedAnnRoot);
        File[] annSectionDirs = pipedAnnRootDir.listFiles(new FileFilter() {
                               public boolean accept(File file) {
                                   return file.isDirectory();
                               }
                           });
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        for (File annSectionDir : annSectionDirs) {
            if (skipSectionSet.contains(annSectionDir.getName())) continue;
            
            File[] annFiles = annSectionDir.listFiles(new FilenameFilter() {
                                   public boolean accept(File file, String string) {
                                       return string.endsWith(".pipe");
                                   }
                               });
            for (File annFile : annFiles) {
                System.out.println("Reading: " + annFile.getAbsolutePath());
                List<PDTBRelation> relations = pipedFileReader.read(annFile);
                for (PDTBRelation relation: relations) {
                    if (relation.getType().equals("Explicit")) {
                        if (!connHeadSet.contains(relation.getConnHead())) {
                            connHeadSet.add(relation.getConnHead());
                        }
                    }
                }
            }
        }
        
        return new ArrayList<String>(connHeadSet);
    }
    
    public static void writeUniqueHeads() {
        //PDTBAnalyzer analyzer = new PDTBAnalyzer("/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/piped_data", "/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/RAW/WSJ", "/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/ptb");
        PDTBAnalyzer analyzer = new PDTBAnalyzer("./pdtb_v2/piped_data", "./pdtb_v2/RAW/WSJ", "./pdtb_v2/ptb");
        List<String> uniqueConnHeadList = analyzer.getUniqueConnHead();        
        System.out.println(uniqueConnHeadList.size());
        try {
            FileWriter writer = new FileWriter("./resource/ml/data/pdtb/explicit_conn_types");
            for (String conn: uniqueConnHeadList) {
                writer.write(conn + "\n");                
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(PDTBAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void analyzeConnMod() {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN", "SENSE"});
        Text headText = textReader.read(new File("./resource/ml/data/pdtb/explicit_relations_w_iteonn_sense"));
        System.out.println(headText.size());
        Text fullText = textReader.read(new File("./resource/ml/data/pdtb/explicit_relations_full_conn"));
        System.out.println(fullText.size());
        
        for (int i = 0; i < fullText.size(); i++) {
            Sentence head = headText.get(i);
            Sentence full = fullText.get(i);
            for (int j = 0; j < full.size(); j++) {
                head.get(j).setTag("CONN2", full.get(j).getTag("CONN"));
            }
        }
        fullText.clear();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/pdtb/explicit_relations_tree"));
            String line = null;
            for (Sentence s : headText) {
                line = reader.readLine();
                s.setProperty("parse_tree", line);
            }
            reader.close();            
            
            SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
            TreeFactory tf = new LabeledScoredTreeFactory();
            HashMap<String, Integer> lcaMap = new HashMap<String, Integer>();
            for (Sentence s : headText) {
                String treeStr = s.getProperty("parse_tree");
                TreeReader tr = new PennTreeReader(new StringReader(treeStr), tf);
                Tree root = tr.readTree();
                for (int i = 0; i < s.size(); i++) {
                    TokWord word = s.get(i);
                    if (word.getTag("CONN").startsWith("B") && word.getTag("CONN2").startsWith("I")) {
                        int end = i - 1;
                        int start = i - 1;
                        while (start > 0 && s.get(start).getTag("CONN2").startsWith("I")) {
                            start--;
                        }
                        //System.out.println(root.getLeaves().size()+"-"+start + "-"+end);
                        //Tree lca = analyzer.getLCA(root, start, end);
                        int connEnd = i;
                        for (int j = i + 1; j < s.size(); j++) {
                            if (s.get(j).getTag("CONN").startsWith("I")) {
                                connEnd = j;
                            } else {
                                break;
                            }
                        }
                        Tree lca = analyzer.getLCA(root, i, connEnd);
                        lca = analyzer.getSibling(root, lca, -1);
                        String label = (lca == null)?"null":lca.value();
                        if (lcaMap.containsKey(label)) {
                            lcaMap.put(label, lcaMap.get(label)+1);
                        } else {
                            lcaMap.put(label, 1);
                        }
                        /*if (!label.equals("RB")) {
                            int connEnd = i;
                            for (int j = i+1; j < s.size(); j++) {
                                if (s.get(j).getTag("CONN").startsWith("I")) {
                                    connEnd = j;
                                } else break;
                            }
                            System.out.println(label+": " + s.toString(start, end) + ": " + s.toString(i, connEnd));
                        }*/
                    } else if (false && word.getTag("CONN").startsWith("B") && word.getTag("CONN2").startsWith("B")) {
                        int connEnd = i;
                        for (int j = i + 1; j < s.size(); j++) {
                            if (s.get(j).getTag("CONN").startsWith("I")) {
                                connEnd = j;
                            } else {
                                break;
                            }
                        }
                        Tree lca = analyzer.getLCA(root, i, connEnd);
                        Tree sibling = analyzer.getSibling(root, lca, -1);
                        /*if (sibling != null && sibling.value().matches("RB|NP|PP|ADVP")) {
                            List<Tree> leaves = sibling.getLeaves();
                            System.out.print(sibling.value() + ": ");
                            for (int j = 0; j < leaves.size(); j++) {
                                System.out.print(leaves.get(j).value() + " ");
                            }
                            System.out.println(": " + s.toString(i, connEnd));
                        }*/
                        if (sibling != null) {
                            String label = sibling.value();
                            if (lcaMap.containsKey(label)) {
                                lcaMap.put(label, lcaMap.get(label) + 1);
                            } else {
                                lcaMap.put(label, 1);
                            }
                        }
                    }
                }
            }
            Set<Entry<String, Integer>> entrySet = lcaMap.entrySet();
            List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(entrySet);
            Collections.sort(list, new Comparator<Entry<String, Integer>>() {

                @Override
                public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                    if (o1.getValue() > o2.getValue()) return -1;
                    else if (o1.getValue() < o2.getValue()) return 1;
                    else return 0;
                }
            });
            for (Entry<String, Integer> e : list) {
                System.out.println(e.getKey()+"-"+e.getValue());
            }
        } catch (IOException ex) {
            Logger.getLogger(PDTBAnalyzer.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
    }
    static int getBetterHeadPos(Tree root, SimpleDepGraph depGraph, int oldPos) {
        int newPos = oldPos;
        //if (root.getLeaves().get(oldPos).parent(root).value().matches("MD")) return oldPos;
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(oldPos);
        if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("det|nn|cop|aux.*|prep.*")) {
            newPos = depDependencies.get(0).gov();
        }
        return newPos;
    }
    public static void analyzeArg2DepHead(Tree root, List<Tree> gornNodes, SimpleDepGraph depGraph, int connStart, int connEnd) {
        //int headPos = getHeadPosFromDepGraph(root, gornNodes, depGraph);
        Tree syntacticHead = null;
        try {
            syntacticHead = HEAD_ANALYZER.getSyntacticHead(root, gornNodes);
            if (syntacticHead == null) {
                System.out.println("Caught null head!");
                return;
            }
        } catch (Exception ex) {
            System.out.println("exception caught!");
            return;
        }
        
        int headPos = treeAnalyzer.getLeafPosition(root, syntacticHead);
        headPos = getBetterHeadPos(root, depGraph, headPos);
        String conn = getConnString(root, connStart, connEnd);
        String category = CONNECTIVE_ANALYZER.getCategory(conn.toLowerCase());
        if (category == null) category = "Conj";
        if (category.matches("Su.*|Conj")) {
            List<Integer> reachableIndices = depGraph.getReachableIndices(headPos, true, 100);
            boolean flg = false;
            int parent = depGraph.getParent(headPos);
            if (parent == -1 || connStart <= parent && parent <= connEnd) flg = true;
            if (reachableIndices != null) {
                for (int i = connStart; i <= connEnd; i++) {
                    if (reachableIndices.contains(i)) {
                        flg = true;
                        break;
                    }
                }
                if (!flg) {
                    System.out.println("false");
                    Sentence s = new Sentence(root);
                    System.out.println(s);
                    System.out.println(conn);
                    System.out.println("head: " + s.get(headPos));
                    System.out.println("-----------------");
                } else {
                    System.out.println("true");
                }
            } else {
                System.out.println("null");
            }
        }
    }
    public static void analyzeArg2Head() {
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        
        //File pdtbRoot = new File("./pdtb_v2/piped_data_2");
        //File ptbRoot = new File ("./pdtb_v2/ptb");
        File pdtbRoot = new File("./pdtb_v2/piped_data");
        File ptbRoot = new File ("./package/treebank_3/parsed/mrg/wsj");
        File depRoot = new File ("./package/treebank_3/parsed/mrg/dep");
        File[] pdtbSections = pdtbRoot.listFiles();
        HashMap<String, Integer> headTypMap = new HashMap<String, Integer>();
        int count = 0;
        for (File pdtbSection : pdtbSections) {
            //if (!pdtbSection.getName().matches("23|24")) continue;
            File[] pdtbFiles = pdtbSection.listFiles();            
            for (File pdtbFile : pdtbFiles) {
                File ptbFile = new File(ptbRoot, pdtbSection.getName() + "/" + pdtbFile.getName().replace(".pipe", ".mrg"));
                File depFile = new File(depRoot, pdtbSection.getName() + "/" + pdtbFile.getName().replace(".pipe", ".dep"));
                List<PDTBRelation> relations = pipedFileReader.read(pdtbFile);
                List<Tree> trees = ptbFileReader.read(ptbFile);
                List<SimpleDepGraph> depGraphs = depFileReader.read(depFile);
                for (PDTBRelation relation : relations) {
                    if (relation.getType().equals("Explicit")) {
                        String gornAddress = relation.getArg2GornAddress();
                        if (gornAddress.equals("")) {
                            System.out.println("gorn address empty!!!");
                            continue;
                        }
                        
                        GornAddressList gaList;
                        try {
                            gaList = new GornAddressList(gornAddress);
                        } catch (RuntimeException ex) {
                            System.out.println(ex.getMessage()+": " + gornAddress);
                            continue;
                        }
                        
                        boolean sentential = true;
                        int lineNumber = gaList.get(0).getLineNumber();
                        for (GornAddress gAddress : gaList) {
                            if (gAddress.getLineNumber() != lineNumber) {
                                sentential = false;
                                break;
                            }
                        }
                        String connectiveGornAddress = relation.getConnectiveGornAddress();
                        GornAddressList connGornAddressList = new GornAddressList(connectiveGornAddress);
                        sentential = (lineNumber == connGornAddressList.get(0).getLineNumber());
                        
                        List<Tree> connHeadLeaves = connAnalyzer.getConnHeadLeaves(trees.get(lineNumber), connectiveGornAddress, relation.getConnHead());
                        if (!connHeadLeaves.isEmpty()) {
                            Tree root = trees.get(lineNumber);
                            int connStart = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(0));
                            int connEnd = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(connHeadLeaves.size() - 1));
                            if ((connEnd - connStart) > 4) { //handle if..else, etc.
                                connEnd = connStart;
                            }
                            List<Tree> gornNodes = treeAnalyzer.getGornNodes(trees.get(lineNumber), gornAddress);
                            SimpleDepGraph depGraph = depGraphs.get(lineNumber);
                            //analyzeArg2DepHead(root, gornNodes, depGraph, connStart, connEnd);
                            String conn = getConnString(root, connStart, connEnd);
                            if (conn.matches("but|But")) {
                                /*Tree head = headAnalyzer.getSyntacticHead(root, gornNodes);
                                if (head != null) {
                                    List<Tree> path = root.pathNodeToNode(root.getLeaves().get(connStart), head);
                                    String s = "";
                                    for (int i = 1; i < path.size() - 1; i++) {
                                        s += path.get(i).value()+":";
                                    }
                                    if (path != null) {
                                        count++;
                                        if (!headTypMap.containsKey(s)) {
                                            headTypMap.put(s, 1);
                                        } else {
                                            headTypMap.put(s, headTypMap.get(s) + 1);
                                        }
                                        //System.out.println(path);
                                        if (new Random().nextInt(100) == 5) {
                                            Sentence sent = new Sentence(root);
                                            System.out.println(sent);
                                            System.out.println(s);
                                        }
                                    } else {
                                        System.out.println("Path is null");
                                    }
                                } else {
                                    System.out.println("Head is null");
                                }*/
                                String sense = relation.getSense();
                                if (headTypMap.containsKey(sense)) {
                                    headTypMap.put(sense, headTypMap.get(sense) + 1);
                                } else {
                                    headTypMap.put(sense, 1);
                                }
                            }
                        }
                        if (false && sentential) {
                            List<Tree> gornNodes = treeAnalyzer.getGornNodes(trees.get(lineNumber), gornAddress);
                            //Tree syntacticHead = headAnalyzer.getSyntacticHead(trees.get(lineNumber), gornNodes);
                            String tmpTree = treeAnalyzer.getPennOutput(trees.get(lineNumber));
                            Tree syntacticHead = headAnalyzer.getSemanticHead(trees.get(lineNumber), gornNodes);
                            if (syntacticHead == null) {
                                System.out.println("Head null!!!!");
                            } else {
                                Tree parent = syntacticHead.parent(trees.get(lineNumber));
                                if (headTypMap.containsKey(parent.value())) {
                                    headTypMap.put(parent.value(), headTypMap.get(parent.value()) + 1);
                                } else {
                                    headTypMap.put(parent.value(), 1);
                                }
                                /*if (!parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                                    System.out.println(parent.value());
                                    trees.get(lineNumber).pennPrint();
                                    System.out.println(relation.getArg2RawText());                                    
                                }*/
                                /*if (parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                                    System.out.println(pdtbFile.getAbsolutePath());
                                    System.out.println(parent.value());
                                    trees.get(lineNumber).pennPrint();
                                    System.out.println("original tree: " + tmpTree);
                                    System.out.println(relation.getArg1RawText());  
                                    System.out.println(relation.getArg1GornAddress());
                                    int headPos = treeAnalyzer.getLeafPosition(trees.get(lineNumber), syntacticHead);
                                    System.out.println("Head: " + trees.get(lineNumber).getLeaves().get(headPos).value());
                                    System.out.println("Conn: " + relation.getConnRawText());
                                    System.out.println("arg2: " + relation.getArg2RawText());
                                    System.out.println("-----------------------");
                                }*/
                                
                            }
                            
                        } else {
                            //System.out.println("non-sentential!!!");
                        }
                    }
                }
            }
        }
        System.out.println("count: " + count);
        Set<Entry<String, Integer>> entrySet = headTypMap.entrySet();
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
    private static void analyzeSpan(int headIndex, SimpleDepGraph depGraph, List<Tree> gornNodes, Tree root) {
        List<Integer> reachableIndices = depGraph.getReachableIndices(headIndex, true, 100);
        Set<Integer> spanSet = new HashSet<Integer>();
        for (Tree gornNode : gornNodes) {
            List<Tree> leaves = gornNode.getLeaves();
            for (Tree leaf : leaves) {
                spanSet.add(treeAnalyzer.getLeafPosition(root, leaf));
            }
        }

        if (reachableIndices != null) {
            Sentence s = new Sentence(root);
            /*int wrongCount = 0;
            for (Integer reachableIndex : reachableIndices) {
                if (s.get(reachableIndex).getTag("POS").matches("NN.*")
                        && !spanSet.contains(reachableIndex)) {
                    wrongCount++;
                }
            }
            System.out.println(wrongCount);*/

            int missCount = 0;
            Set<Integer> missSet = new HashSet<Integer>();
            for (Integer spanIndex : spanSet) {
                if (s.get(spanIndex).getTag("POS").matches("NN.*")
                        && !reachableIndices.contains(spanIndex)) {
                    missCount++;
                    missSet.add(spanIndex);
                }
            }
            System.out.println(missCount);
            if (missCount > 2) {
                System.out.println(s);
                System.out.println("Head: " + s.get(headIndex).word());
                System.out.print("missed: ");
                for (Integer missIndex : missSet) {
                    System.out.print(s.get(missIndex).word() + ", ");
                }
                System.out.println("");
            }
        } else {
            System.out.println("Null reachableIndices!");
        }          
    }
    
    public static void analyzeArgSpan() {
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        
        
        File pdtbRoot = new File("./pdtb_v2/piped_data");
        File ptbRoot = new File ("./package/treebank_3/parsed/mrg/wsj");
        File depRoot = new File("./package/treebank_3/parsed/mrg/dep");
        File[] pdtbSections = pdtbRoot.listFiles();
        HashMap<String, Integer> headTypMap = new HashMap<String, Integer>();
        
        for (File pdtbSection : pdtbSections) {            
            File[] pdtbFiles = pdtbSection.listFiles();
            for (File pdtbFile : pdtbFiles) {
                File ptbFile = new File(ptbRoot, pdtbSection.getName() + "/" + pdtbFile.getName().replace(".pipe", ".mrg"));
                File depFile = new File(depRoot, pdtbSection.getName() + "/" + pdtbFile.getName().replace(".pipe", ".dep"));
                List<PDTBRelation> relations = pipedFileReader.read(pdtbFile);
                List<Tree> trees = ptbFileReader.read(ptbFile);
                List<SimpleDepGraph> depGraphs = depFileReader.read(depFile);
                for (PDTBRelation relation : relations) {
                    if (relation.getType().equals("Explicit")) {
                        GornAddressList arg2GornAddressList = new GornAddressList(relation.getArg2GornAddress());
                        int arg2Line = arg2GornAddressList.get(0).getLineNumber();
                        GornAddressList arg1GornAddressList = new GornAddressList(relation.getArg1GornAddress());
                        int arg1Line = arg1GornAddressList.get(0).getLineNumber();
                        /*if (arg1Line == arg2Line) {
                        List<Tree> arg2GornNodes = getArgGornNodes(trees.get(arg2Line), relation.getArg2GornAddress());
                        List<Tree> arg1GornNodes = getArgGornNodes(trees.get(arg1Line), relation.getArg1GornAddress());
                        Tree lca1 = treeAnalyzer.getLCA(trees.get(arg1Line), arg1GornNodes);
                        Tree lca2 = treeAnalyzer.getLCA(trees.get(arg2Line), arg2GornNodes);
                        if (lca1 != null && lca2 != null) {
                        if (lca1.dominates(lca2)) {
                        System.out.println("Yes");
                        } else {
                        System.out.println("no");
                        }
                        }
                        }*/
                        List<Tree> arg2GornNodes = getArgGornNodes(trees.get(arg2Line), relation.getArg2GornAddress());
                        List<Tree> arg1GornNodes = getArgGornNodes(trees.get(arg1Line), relation.getArg1GornAddress());
                        Tree arg2Root = trees.get(arg2Line);
                        Tree arg1Root = trees.get(arg1Line);
                        SimpleDepGraph arg2DepGraph = depGraphs.get(arg2Line);
                        SimpleDepGraph arg1DepGraph = depGraphs.get(arg1Line);
                        Tree arg2Head = headAnalyzer.getSyntacticHead(arg2Root, arg2GornNodes);
                        Tree arg1Head = headAnalyzer.getSyntacticHead(arg1Root, arg1GornNodes);
                        if (arg2Head != null) {
                            int arg2HeadIndex = treeAnalyzer.getLeafPosition(arg2Root, arg2Head);
                            //fix arg2HeadIndex if it is a dependent of cop|nn|cux|auxpass relation
                            List<SimpleDependency> depDependencies = arg2DepGraph.getDepDependencies(arg2HeadIndex);
                            if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("nn|cop|aux.*")) {
                                arg2HeadIndex = depDependencies.get(0).gov();
                            }
                            //analyzeSpan(arg2HeadIndex, arg2DepGraph, arg2GornNodes, arg2Root);
                            int arg1HeadIndex = treeAnalyzer.getLeafPosition(arg1Root, arg1Head);
                            //fix arg2HeadIndex if it is a dependent of cop|nn|cux|auxpass relation
                            depDependencies = arg1DepGraph.getDepDependencies(arg1HeadIndex);
                            if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("nn|cop|aux.*")) {
                                arg1HeadIndex = depDependencies.get(0).gov();
                            }
                            //analyzeSpan(arg1HeadIndex, arg1DepGraph, arg1GornNodes, arg1Root);
                            List<Integer> arg2ReachableIndices = arg2DepGraph.getReachableIndices(arg2HeadIndex, true, 100);
                            List<Integer> arg1ReachableIndices = arg1DepGraph.getReachableIndices(arg1HeadIndex, true, 100);
                            int commonReachable = 0;
                            for (Integer index : arg1ReachableIndices) {
                                if (arg2ReachableIndices != null && arg2ReachableIndices.contains(index)) {
                                    commonReachable++;
                                }
                            }
                            System.out.println(commonReachable == arg1ReachableIndices.size());
                        } else {
                            System.out.println("Null head!");
                        }
                    }
                }
            }
        }
    }
    
    private static List<Tree> getArgGornNodes(Tree root, String gornAddress) {        
        String tokens[] = gornAddress.split(";");
        int lineNum = -1;
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            int line = Integer.parseInt(token.split(",")[0]);
            if (lineNum == -1) lineNum = line;
            else if (line != lineNum) continue;
            sb.append(token+";");
        }
        return treeAnalyzer.getGornNodes(root, sb.toString());
    }
    
    public static void analyzeChunks() {        
        PTBFileReader ptbFileReader = new PTBFileReader();        
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        OChunker chunker = new OChunker();
        File ptbRoot = new File ("./package/treebank_3/parsed/mrg/wsj");
        File[] ptbSections = ptbRoot.listFiles();
        int miss = 0;
        int correct = 0;
        for (File ptbSection : ptbSections) {
            if (!ptbSection.getName().matches("00|01|02|03|04|05")) continue;
            File[] ptbFiles = ptbSection.listFiles();
            for (File ptbFile : ptbFiles) {
                List<Tree> trees = ptbFileReader.read(ptbFile);
                for (Tree root : trees) {
                    Sentence s = new Sentence(root);
                    s = chunker.annotate(s);
                    List<Tree> leaves = root.getLeaves();
                    for (int i = 0; i < s.size(); i++) {
                        String tag = s.get(i).getTag("CHUNK");
                        
                        if (!tag.equals("O")) {
                            Tree chunkTree = leaves.get(i).ancestor(2, root);
                            String treeTag = chunkTree.value();
                            if (treeTag.startsWith("QP")) treeTag = "NP";                            
                            tag = tag.substring(2, 3);
                            if (treeTag.startsWith(tag)) {
                                correct++;
                            } else {
                                //System.out.println(s.get(i).getTag("CHUNK")+"<>"+chunkTree.value());
                                miss++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Correct: " + correct);
        System.out.println("Miss: " + miss);
    }
    public static void analyzeSyntax(String trainingFile, String parsedTrainingFile, String trainingDepFile) throws IOException {
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        Text trainingText = textReader.read(new File(trainingFile));
        BufferedReader reader = new BufferedReader(new FileReader(parsedTrainingFile));
        SimpleDepFileReader depFileReader = new SimpleDepFileReader();
        List<SimpleDepGraph> depGraphs = depFileReader.read(new File(trainingDepFile));
        String line;
        int total = 0;
        int count = 0;
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < trainingText.size(); i++) {
            Sentence s = trainingText.get(i);
            //SimpleDepGraph depGraph = depGraphs.get(i);                                   
            line = reader.readLine();            
            Tree root = treeAnalyzer.getPennTree(line);
            for (int j = 0; j < s.size(); j++) {
                /*if (s.get(j).getTag("CONN").startsWith("B")) {
                    String conn = s.get(j).word().toLowerCase();
                    String cat = connAnalyzer.getCategory(conn);
                    if (cat == null) {
                        cat = "Conj-adverbial";
                    }
                    //if (cat.startsWith("Sub") || cat.startsWith("Coo")) {
                    if (cat.startsWith("Conj")) {
                        Tree t = root.getLeaves().get(j);
                        Tree anc = t.ancestor(2, root);
                        if (anc.value().startsWith("S")) {
                            count++;                            
                        }
                        if (!map.containsKey(anc.value().substring(0, 1))) {
                            map.put(anc.value().substring(0, 1), 1);
                        } else {
                            map.put(anc.value().substring(0, 1), map.get(anc.value().substring(0, 1)) + 1);
                        }
                        total++;
                    }
                }*/
                if (s.get(j).getTag("CONN").startsWith("B") && s.get(j).word().matches("And|and")) {
                    Tree t = root.getLeaves().get(j);
                    Tree anc = t.ancestor(2, root);
                    if (!map.containsKey(anc.value())) {
                        map.put(anc.value(), 1);
                    } else {
                        map.put(anc.value(), map.get(anc.value()) + 1);
                    }
                    total++;
                }
            }
        }
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
        System.out.println("total = " + total);
        System.out.println("count = " + count);
    }
    
    public static int getHeadPosFromDepGraph(Tree root, List<Tree> gornNodes, SimpleDepGraph depGraph) {
        Set<Integer> span = new HashSet<Integer>();
        for (Tree t : gornNodes) {
            List<Tree> leaves = t.getLeaves();
            for (Tree leaf : leaves) {
                span.add(treeAnalyzer.getLeafPosition(root, leaf));
            }
        }
        List<Integer> candidates = new ArrayList<Integer>();
        for (Integer dep : span) {
            List<SimpleDependency> deps = depGraph.getDepDependencies(dep); //get parent(s)
            boolean isCandidate = true;
            for (SimpleDependency sDep : deps) {
                if (span.contains(sDep.gov())) {
                    isCandidate = false;
                    break;
                }
            }
            if (isCandidate) {
                candidates.add(dep);
            }
        }
        String rules[] =  new String[]{"V.*", "J.*", "NNS?", "MD"};
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < rules.length; i++) {
            for (Integer candidate : candidates) {
                Tree parent = leaves.get(candidate).parent(root);
                if (parent.value().matches(rules[i])) {
                    return candidate;
                }
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        } else {
            return candidates.get(0);
        }
    }
    
    private static String getConnString(Tree root, int connStart, int connEnd) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        for (int i = connStart; i <= connEnd; i++) {
            Tree leaf = leaves.get(i);
            if (sb.length() != 0) sb.append(" ");
            sb.append(leaf.value());
        }
        return sb.toString();
    }
    public static List<Pair<Integer, Integer>> getConnCandidates(Sentence s, ConnectiveAnalyzer connAnalyzer) {
        List<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
        ArrayList<String> words = s.getWords();
        for (int i = 0; i < words.size(); i++) {
            
            int j = -1;
            if (!s.get(i).getTag("CONN").matches("(B.*)|(DB.*)")) {
                for (int k = 0; k < 4 && ((i + k) < words.size()); k++) {                    
                    if (connAnalyzer.isBaseConnective(s.toString(i, i + k).toLowerCase())) {
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
                result.add(new Pair<Integer, Integer>(i, j));
                i = j;
            }
        }
        return result;
    }
    
    public static void analyzeConnective(String iobFile, String treeFile) throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN"});
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        
        Text text = textReader.read(new File(iobFile));
        System.out.println(text.size());        
                
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;        
                
        for (int i = 0; i < text.size(); i++) {
            Sentence s = text.get(i);            
            line = reader.readLine();            
            if (line.equals("")) continue;
            Tree root = treeAnalyzer.getPennTree(line);
            List<Tree> leaves = root.getLeaves();
            
            List<Pair<Integer, Integer>> connCandidates = getConnCandidates(s, connAnalyzer);
            for (Pair<Integer, Integer> candidate : connCandidates) {
                int start = candidate.first();
                int end = candidate.second();
                Tree startLeaf = leaves.get(start);
                Tree endLeaf = leaves.get(end);
                for (int j = 2; j < 5; j++) {
                    Tree ancestor = startLeaf.ancestor(j, startLeaf);
                    if (ancestor.value().startsWith("S")) {
                        break;
                    }
                }
            }
        }
    }
    public static void main(String args[]) throws IOException {
        //analyzeConnMod();
        //writeUniqueHeads();
        analyzeArg2Head();
        //analyzeArgSpan();
        
        //analyzeChunks();
        //analyzeSyntax("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_2_22");
        //analyzeSyntax("./resource/ml/data/pdtb/conn_id/explicit_relations_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_tree_24", "./resource/ml/data/pdtb/conn_id/explicit_relations_dep_24");
        //analyzeSyntax("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_24", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_24", "./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_dep_24");
    }
}
