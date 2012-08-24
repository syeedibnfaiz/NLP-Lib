/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.biodrb;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddress;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed ibn Faiz
 */
public class BioDRBAnalyzer {
    
    public static void analyzeArgumentHead() {
        GenericTextReader gTextReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN","ARG1S","ARG1E","ARG2S","ARG2E"});
        Text text = gTextReader.read(new File("./resource/ml/data/discourse/SentConnArg12_final.txt"));
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/SentConnArg12_final_parsed.txt"));
            String line;
            TreeFactory tf = new LabeledScoredTreeFactory();
            SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
            HeadAnalyzer headAnalyzer = new HeadAnalyzer();
            HashMap<String, Integer> map = new HashMap<String, Integer>();
            for (Sentence s : text) {
                line = reader.readLine();
                TreeReader tr = new PennTreeReader(new StringReader(line), tf);
                Tree root = tr.readTree();
                int arg1S = -1;
                int arg1E = -1;
                for (int i = 0; i < s.size(); i++) {
                    if (s.get(i).getTag("ARG2S").equals("1")) {
                        arg1S = i;
                    }
                    if (s.get(i).getTag("ARG2E").equals("1")) {
                        arg1E = i;
                    }
                }
                if (arg1S == -1 || arg1E == -1) continue;
                
                Tree lca = analyzer.getLCA(root, arg1S, arg1E);
                Tree head = headAnalyzer.getSemanticHead(lca);
                String headWord = null;
                if (head != null) {
                    headWord = head.value();
                    head = head.parent(root);
                }
                
                String value = (head == null)?"null":head.value();
                if (map.containsKey(value)) {
                    map.put(value, map.get(value)+1);
                } else {
                    map.put(value, 1);
                    System.out.println(value+": " + "head: "+ headWord + ", "+s.toString(arg1S, arg1E));
                    System.out.println(s);
                    //System.out.println(line);
                }                
            }
            List<Entry<String,Integer>> list = new ArrayList<Entry<String, Integer>>(map.entrySet());
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
            Logger.getLogger(BioDRBAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void findExamples4UnknownConn() {
        Map<String, String> map = new HashMap<String, String>();
        List<String> lines = Util.readLines("./resource/ml/data/biodrb/unknown_types.txt");
        //put unknown connectives
        for (String line : lines) {
            String[] tokens = line.split("\t");
            if (tokens[1].equals("**")) {
                map.put(tokens[0], "**");
            }
        }
        
        String annRoot = "./resource/ml/data/biodrb/newAnn";
        String parseRoot = "./resource/ml/data/biodrb/parses";
        
        File annDir = new File(annRoot);
        File parseDir = new File(parseRoot);
        File[] files = annDir.listFiles();        
        PDTBPipedFileReader annReader = new PDTBPipedFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        for (File file : files) {
            File parseFile = new File(parseDir, file.getName().replace(".pipe", ".mrg"));
            List<Tree> trees = ptbFileReader.read(parseFile);
            List<PDTBRelation> relations = annReader.read(file);
            for (PDTBRelation relation : relations) {
                if (relation.getType().equals("Explicit")) {
                    if (map.containsKey(relation.getConnRawText().toLowerCase())) {
                        String value = map.get(relation.getConnRawText().toLowerCase());
                        if (value.equals("**")) {
                            GornAddress gornAddress = new GornAddress(relation.getArg2GornAddress());
                            Sentence s = new Sentence(trees.get(gornAddress.getLineNumber()));
                            value = "Sense: "+relation.getSense() + "\n";
                            value += "Sentense: "+s.toString() + "\n";
                            value += "Arg1: "+relation.getArg1RawText() + "\n";
                            value += "Arg2: "+relation.getArg2RawText();
                            
                            map.put(relation.getConnRawText().toLowerCase(), value);
                        }
                    }
                }
            }
        }
        List<String> list = new ArrayList<String>(map.keySet());
        Collections.sort(list);
        for (String conn : list) {
            String value = map.get(conn);
            System.out.println("Connective: "+conn);
            System.out.println(value);
            System.out.println("Category: ");
            System.out.println("");
        }
    }
    public static void fillUnknownTypes() throws IOException {
        List<String> lines = Util.readLines("./resource/ml/data/biodrb/unknown_biodrb_conn_examples.txt");
        FileWriter writer = new FileWriter("./resource/ml/data/biodrb/unknown_biodrb_conn_examples_syeed.txt");
        Scanner in = new Scanner(System.in);
        int count = 0;
        for (String line : lines) {            
            if (line.startsWith("Category:")) {
                System.out.println("Count: " + (++count));
                System.out.println("Category: [1-coord / 2-sub / 3-adv / 4-prep]: ");
                String input = in.nextLine();
                String result = "";
                if (input.contains("1")) result += "Coordinator ";
                if (input.contains("2")) result += "Subordinator ";
                if (input.contains("3")) result += "Adverbial ";
                if (input.contains("4")) result += "Prepositional ";
                result = result.trim();
                writer.write(line + result + "\n");
            } else {
                System.out.println(line);
                writer.write(line+"\n");
            }
            writer.flush();
        }
        writer.close();
    }
    
    public static void mergeUnknownTypes() throws IOException {
        List<String> lines1 = Util.readLines("./resource/ml/data/biodrb/unknown_biodrb_conn_examples_prof.txt");
        List<String> lines2 = Util.readLines("./resource/ml/data/biodrb/unknown_biodrb_conn_examples_syeed.txt");
        FileWriter writer = new FileWriter("./resource/ml/data/biodrb/unknown_biodrb_conn_examples_merged.txt");        
        
        for (int i = 0; i < lines1.size(); i++) {            
            String line1 = lines1.get(i);
            String line2 = lines2.get(i);
            System.out.println(line1);
            System.out.println(line2);
            if (line1.startsWith("Category:")) {
                //String line2 = lines2.get(i);
                int where = line2.indexOf(':');
                line2 = line2.substring(where + 2);
                writer.write(line1 + " || " + line2 + "\n\n");
            } else if (line1.startsWith("Connective:")){                
                writer.write(line1+"\n");
            }
            writer.flush();
        }
        writer.close();
    }
    public static void main(String args[]) throws IOException {
        //analyzeArgumentHead();
        //fillUnknownTypes();
        mergeUnknownTypes();
    }
}
