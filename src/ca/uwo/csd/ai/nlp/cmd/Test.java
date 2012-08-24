/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.cmd;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.utils.TreeJPanel;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.RankMaxEnt;
import cc.mallet.classify.RankMaxEntTrainer;
import cc.mallet.types.FeatureVectorSequence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author tonatuni
 */
public class Test {
    public static void main(String args[]) throws IOException {
        /*TreeFactory treeFactory = new LabeledScoredTreeFactory();
        StringReader sr = new StringReader("(S1 H:0 (S H:0 (VP H:0 (VB H:0 test) (NP H:0 (PRP H:0 it))) (. H:0 .)))");
        TreeReader reader = new PennTreeReader(sr, treeFactory);
        Tree t = reader.readTree();
        t.pennPrint();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        Tree lCA = analyzer.getLCA(t, t.getLeaves().subList(0, 1));
        System.out.println(lCA.score());*/
        /*CharniakParser parser = new CharniakParser("C:\\UWO\\thesis\\Parsers\\BLLIP-bllip-parser-cbd8816\\first-stage\\PARSE\\parseit.exe", "C:/UWO/thesis/Parsers/BLLIP-bllip-parser-cbd8816/first-stage/EN/");
        LPSentReader reader = new LPSentReader();
        Sentence s = reader.read("This is a test.");
        Tree tree = parser.getBestParse(s);
        tree.pennPrint();*/
        //test2();       
        //System.out.println("point".matches("point(s|ed)"));
        /*int a[] = {1,2, 1, 4, 3};
        Arrays.sort(a);
        for (int i = 0; i < a.length; i++) {
            System.out.println(a[i]);
        }*/
        //generateBioDRBConn2();
        //checkDepReln(new File("./pdtb_v2/dep2"));
        //testLPSentReader();
        /*System.out.println(System.getProperty("java.home"));
        System.out.println("Waiting for 15 seconds...");
        try {
            Thread.sleep(10000);
            System.exit(0);
        } catch (InterruptedException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        //generateAutoConnIDData();
        //generateAutoConnIDDep();
        //testHeads();
        
        //processPostmedSegmentedFiles();
        //comparePostmed();
        /*regexText();
        HashMap h;
        String s;*/
        /*String a[] = {"a", "b", "c", "d", "e"};
        Collections.shuffle(Arrays.asList(a));
        for (String x : a) {
            System.out.println(x);
        } */       
        
        //parserTest();
        //mergeBibFiles("C:\\Users\\tonatuni\\Dropbox\\Thesis\\thesis\\bib", "C:\\Users\\tonatuni\\Dropbox\\Thesis\\thesis\\references.bib");
        //ftest();
        //TreeJPanel x;
        String s = "PROTEIN12";
        int i = s.indexOf("PROTEIN1");
        System.out.println(s.charAt(8));
        
    }
    
    public static void ftest() {
        //double a[] = {92.88,95.41,93.32,95.74,92.83,94.80,93.28,94.80,93.55,95.56,93.09,95.43,95.00,95.24,93.23,95.53,92.98,95.48,92.44,95.74};
        /*double a[] = {94.05,95.95,92.48,94.18,91.76,95.64,92.87,97.22,93.02,95.53,93.32,95.59,93.58,95.52,93.44,96.76,91.95,96.60,93.98,96.10};
        double fscore  = 0.0;
        for (int i = 0; i < a.length; i += 2) {
            System.out.println("Precision: " + a[i]);
            System.out.println("Recall: " + a[i+1]);
            fscore += (2*a[i]*a[i+1])/(a[i] + a[i+1]);
            System.out.println("Fscore: " + (2*a[i]*a[i+1])/(a[i] + a[i+1]) + "\n");
        }*/
        double fscore = 0;
        double[] a = {95.76,94.66,95.10,95.61,95.22,95.32,95.07,95.23,94.73,95.11};
        for (int i = 0; i < a.length; i++) {
            fscore += a[i];
        }
        System.out.println("Macro-Fscore: " + fscore/10);
    }
    public static void regexText() {
        String s = "This is a test, following [2, 3, 6 ] and many others [ 19-prev ]";
        s = s.replaceAll("\\[[A-Za-z0-9,\\s]+\\]", "");
        System.out.println(s);
    }
    public static void processPostmedSegmentedFiles() throws IOException {
        File postmedRoot = new File("./resource/ml/data/biodrb/postmed-output");
        File modifiedPostmedRoot = new File("./resource/ml/data/biodrb/postmed-output/modified");
        File[] files = postmedRoot.listFiles();
        for (File file : files) {
            File newPostmedFile = new File(modifiedPostmedRoot, file.getName().replace("postmed-segmented", "out"));            
            FileWriter writer = new FileWriter(newPostmedFile);
            List<String> lines = Util.readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.equals("sentence")) {
                    String offsets = lines.get(i + 1);
                    String sentence = lines.get(i + 2);
                    String[] tokens = offsets.split("\\s+");
                    writer.write(tokens[0] + "\n");
                    writer.write(tokens[1] + "\n");
                    writer.write(sentence + "\n");
                    i += 2;
                }
            }
            writer.close();
        }
    }
    public static void comparePostmed() {
        File sentenceRoot = new File("./resource/ml/data/biodrb/sentences");
        File postmedRoot = new File("./resource/ml/data/biodrb/postmed-output");
        File[] files = sentenceRoot.listFiles();
        
        for (File file : files) {
            File postmedFile = new File(postmedRoot, file.getName() + ".out");
            List<String> lines = Util.readLines(file);
            List<String> postmedLines = Util.readLines(postmedFile);
            System.out.println(file.getName());
            System.out.println(lines.size()/3);
            System.out.println(postmedLines.size());
            System.out.println(lines.size()/3 - postmedLines.size());
            System.out.println("");
            if (file.getName().equals("1421503.txt")) {
                compare(file, postmedFile);
                System.out.println("");
            }
        }
    }
    public static void compare(File sentenceFile, File postmedFile) {
        List<String> lines = Util.readLines(sentenceFile);
        List<String> postmedLines = Util.readLines(postmedFile);
        Set<String> set = new HashSet<String>();
        for (String line : postmedLines) {
            String[] tokens = line.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String token : tokens) {
                sb.append(token.split("_")[0]);
            }
            set.add(sb.toString());
        }
        
        for (int i = 2; i < lines.size(); i += 3) {
            String line = lines.get(i);
            String[] tokens = line.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String token : tokens) {
                sb.append(token);
            }
            if (!set.contains(sb.toString())) {
                System.out.println(line.replace("\n", ""));
            }
        }
    }
    public static void testHeads() {
        PTBFileReader reader = new PTBFileReader();
        List<Tree> trees = reader.read(new File("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22"));
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        for (Tree t : trees) {
            
            Tree collinsHead = headAnalyzer.getSyntacticHead(t.getChild(0));
            Tree syntacticHead = headAnalyzer.getSyntacticHead(t.getChild(0));
            if (collinsHead == null) {
                System.out.println("Collins head null!");
                t.pennPrint();
            } else if (syntacticHead == null) {
                System.out.println("Syntactic head null!");
                t.pennPrint();
            } else if (!collinsHead.value().equals(syntacticHead.value())) {
                t.pennPrint();
                System.out.println("collinsHead: " + collinsHead);
                System.out.println("SyntacticHead: " + syntacticHead);
            }
        }
    }
    public static void generateAutoConnIDDep() throws IOException {
        List<String> lines = Util.readLines("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_tree_2_22");
        FileWriter depWriter = new FileWriter("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_dep_2_22");
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        for (String line : lines) {
            Tree root = analyzer.getPennTree(line);
            GrammaticalStructure gs = gsf.newGrammaticalStructure(root);
            Collection<TypedDependency> dependencies = gs.typedDependencies();
            boolean first = true;
            for (TypedDependency td : dependencies) {
                if (!first) {
                    depWriter.write("\t");
                } else {
                    first = false;
                }
                depWriter.write(td.toString());
            }
            depWriter.write("\n");
        }
        depWriter.close();
    }
    public static void generateAutoConnIDData() throws IOException {
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", " ", new String[]{"Word", "CONN"});
        Text text = reader.read(new File("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_2_22"));
        FileWriter writer = new FileWriter("./resource/ml/data/pdtb/conn_id/gs/auto_explicit_relations_2_22");
        List<String> lines = Util.readLines("./resource/ml/data/pdtb/conn_id/gs/gs_explicit_relations_tree_2_22");
        TreeFactory tf = new LabeledScoredTreeFactory();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            StringReader sr = new StringReader(line);
            TreeReader tr = new PennTreeReader(sr, tf);
            Tree root = tr.readTree();
            List<Tree> leaves = root.getLeaves();
            Sentence s = text.get(i);
            if (s.size() != leaves.size()) {
                System.out.println(s);
                System.out.println(line);
            }
            for (int j = 0; j < leaves.size(); j++) {
                Tree parent = leaves.get(j).parent(root);
                if (!parent.value().equals("-NONE-")) {
                    writer.write(s.get(j).word() + " " + s.get(j).getTag("CONN") + "\n");
                }
            }
            writer.write("\n");
        }
        writer.close();
    }
    public static void testLPSentReader() {
        //LPSentReader reader = new LPSentReader("(~|\\||\\+|-|'|\\d|\\p{L}|\\S)((~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})*|\\S)");
        LPSentReader reader = new LPSentReader("((\\p{L}|\\d)(~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})*)|\\S");
        String str = "this is a test (demo).";
        Sentence s = reader.read(str);
        System.out.println(s);
    }
    public static void checkDepReln(File dir) {
        SimpleDepFileReader reader = new SimpleDepFileReader();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                checkDepReln(file);
            } else {
                List<SimpleDepGraph> depGraphs = reader.read(file);                
                for (SimpleDepGraph depGraph : depGraphs) {
                    boolean flg[] = new boolean[500];
                    for (SimpleDependency dep : depGraph) {                        
                        if (flg[dep.dep()] == false) {
                            System.out.println("beep");
                        } 
                        flg[dep.dep()] = true;
                    }
                }                
            }
        }
        
    }
    public static void test(Integer i) {
        i = 4;
    }
    
    public static void generateBioDRBConn2() throws IOException {
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"word", "pos", "chunk", "CONN"});
        Text text = reader.read(new File("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_2.txt"));
        FileWriter writer = new FileWriter("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn2.txt");
        for (Sentence s : text ) {
            for (TokWord word : s) {
                writer.write(word.word() + " " + word.getTag("CONN") + "\n");
                if (!word.getTag("CONN").equals("O")) {
                    System.out.println(word.word());
                }
            }
            writer.write("\n");
        }
        writer.close();
    }
    public static void test2() {
        String f1 = "./resource/ml/data/pdtb/explicit_conn_types";
        String f2 = "./resource/ml/data/pdtb/knotts cue phrases.txt";
        
        HashMap<String, String> map = new HashMap<String, String>();
        List<String> lines = Util.readLines(f2);
        for (String line : lines) {
            String tokens[] = line.split("::");
            map.put(tokens[0].trim(), tokens[1].trim());
        }
        lines = Util.readLines(f1);
        for (String conn : lines) {
            if (map.containsKey(conn)) {
                System.out.println(conn+"\t"+map.get(conn));
            } else {
                System.out.println(conn+"\t"+"Conj-adverb");
            }
        }
    }
    
    public static void mergeBibFiles(String dir, String output) throws IOException {
        File[] files = new File(dir).listFiles(new FileExtensionFilter("bib"));
        FileWriter writer = new FileWriter(output);
        for (File file : files) {
            List<String> lines = Util.readLines(file);
            for (String line : lines) {
                writer.write(line + "\n");
            }
            writer.write("\n");
        }
        writer.close();
    }
    
    public static void parserTest() {
        LexicalizedParser parser = new LexicalizedParser("./lib/englishPCFG.ser.gz");
        String s = "This is a test sentence .";
        String[] tokens = s.split(" ");
        List<String> list = new ArrayList<String>(Arrays.asList(tokens));
        ArrayList<Word> wordList = edu.stanford.nlp.ling.Sentence.toUntaggedList(list);
        parser.parse(wordList);
        Tree tree = parser.getBestParse();
        tree.pennPrint();
    }
}
