/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.aimed;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PrepareAIMed {
    static final GeniaTagger TAGGER = null; //= new GeniaTagger();
    //final static ParserAnnotator PARSER_ANNOTATOR =null;//= new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
    final static BLLIPParser PARSER_ANNOTATOR = null;//new BLLIPParser();
    static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    static final GeniaTagger tagger = new GeniaTagger();
    
    public void preProcess(String originalRootPath, String newRootPath, String parseRootPath) throws IOException {
        File originalRoot = new File(originalRootPath);
        File newRoot = new File(newRootPath);
        File parseRoot = new File(parseRootPath);
        newRoot.mkdir();
        parseRoot.mkdir();
        
        File[] files = originalRoot.listFiles(new FileExtensionFilter("txt"));
        
        for (File file : files) {
            File newFile = new File(newRoot, file.getName());
            File parseFile = new File(parseRoot, file.getName() + ".mrg");
            processFile(file, newFile, parseFile);
        }
    }
    
    /***
     * Creates IOB formatted file
     * @param oldFile contains tokenized XML annotated sentences.
     * @param newFile contains lines having the format: WORD PROT P1-List P2-List
     * @throws IOException 
     */
    void processFile(File oldFile, File newFile, File parseFile) throws IOException {
        System.out.println("Processing: " + oldFile.getAbsolutePath());
        List<String> lines = Util.readLines(oldFile);
        FileWriter writer = new FileWriter(newFile);
        FileWriter parseWriter = new FileWriter(parseFile);
        
        for (String line : lines) {
            line = line.trim();
            String[] tokens = line.split("\\s+");
            int prot = 0;            
            List<String> p1 = new ArrayList<String>();
            List<String> p2 = new ArrayList<String>();
            int wordCount = 0;            
            
            Sentence s = new Sentence();
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.equals("<prot>")) {
                    prot++;                    
                } else if (token.equals("</prot>")) {
                    prot--;
                    if (prot == 0) {    //skipping embedded <prot> </prot>
                        wordCount = 0;
                    }
                } else if (token.equals("<p1")) {                    
                    String pair = tokens[i + 1].split("=")[1];
                    p1.add(pair);
                    i += 2;
                } else if (token.equals("<p2")) {                    
                    String pair = tokens[i + 1].split("=")[1];
                    p2.add(pair);
                    i += 2;
                } else if (token.equals("</p1>")) {
                    p1.clear();                    
                } else if (token.equals("</p2>")) {
                    p2.clear();                    
                } else {
                    s.add(new TokWord(token));
                    
                    writer.write(token + "\t");
                    if (prot == 0) {
                        writer.write("O\t");
                    } else if (wordCount == 0){
                        wordCount = 1;
                        writer.write("B\t");
                    } else {
                        writer.write("I\t");
                    }
                    if (!p1.isEmpty()) {
                        String p1Pairs = p1.toString();
                        p1Pairs = p1Pairs.substring(1, p1Pairs.length() - 1);
                        writer.write(p1Pairs + "\t");
                    } else {
                        writer.write("O\t");
                    }
                    
                    if (!p2.isEmpty()) {
                        String p2Pairs = p2.toString();
                        p2Pairs = p2Pairs.substring(1, p2Pairs.length() - 1);
                        writer.write(p2Pairs);
                    } else {
                        writer.write("O");
                    }                                                            
                    writer.write("\n");                    
                }
            }
            writer.write("\n");
            try {
                //s = TAGGER.annotate(s);
                s = PARSER_ANNOTATOR.annotate(s);
                parseWriter.write(s.getParseTree().pennString());
            } catch (IllegalArgumentException ex) {
                parseWriter.write("(ROOT)\n");
            }            
        }
        writer.close();
        parseWriter.close();
    }
    
    public void checkData(String iobRootPath) {
        File root = new File(iobRootPath);
        File[] files = root.listFiles();
        
        int count = 0;
        for (File file : files) {
            GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
            Text text = reader.read(file);
            for (Sentence s : text) {
                for (TokWord word : s) {
                    String tag = word.getTag("P1");
                    if (!tag.equals("O")) {
                        count += tag.split(",").length;
                        String[] tokens = tag.split(", ");
                        for (int i = 0; i < tokens.length; i++) {
                            System.out.print(tokens[i] + ":");
                        }
                        System.out.println("\n"+tag.split(", ").length);
                    }
                }
            }
        }
        System.out.println("Positive: " + count);
    }
    public void analyzeRawFiles(String rawRootPath) {
        File rawDir = new File(rawRootPath);
        File[] files = rawDir.listFiles();
        int count = 0;
        for (File file : files) {
            List<String> lines = Util.readLines(file);
            for (String line : lines) {
                /*if (line.length() >= 1024) {
                System.out.println(line);
                count++;
                }*/
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    if (token.equals("<p2")) {
                        count++;
                    }
                }
            }
        }
        System.out.println("count: " + count);
    }
    
    String[] correctTokenization(String[] tokens) {
        List<String> list = new ArrayList<String>();
        
        list.add(tokens[0]);
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].equals("-")) {
                if (tokens[i-1].matches(".*[a-zA-Z0-9]") && i < (tokens.length-1) && tokens[i+1].matches("[a-zA-Z0-9].*")) {
                    String lastToken = list.remove(list.size() - 1);
                    String newToken = lastToken + "-" + tokens[i+1];
                    list.add(newToken);
                    i++;
                } else {
                    list.add(tokens[i]);
                }
            } else {
                list.add(tokens[i]);
            }
        }
        String[] newTokens = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            newTokens[i] = list.get(i);
        }
        return newTokens;
    }
    
    void fix(File file, FileWriter writer) throws IOException {
        List<String> lines = Util.readLines(file);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("AB - ")) {
                line = line.substring(5);
            } else if (line.startsWith("TI - ")) {
                line = line.substring(5);
            }
            line = line.trim();
            if (line.matches("^\\S+\\s+:.*")) {
                int where = line.indexOf(':');
                line = line.substring(where + 1);
            }
            line = line.trim();
            
            //new
            line = line.replaceAll("\\[[A-Za-z0-9,\\s]+\\]", "");
            
            String segments[];            
            if (line.contains(" . ")) {
                segments = line.split(" \\. ");                
            } else {
                segments = new String[1];
                segments[0] = line;
            }
            
            for (String segment : segments) {
                String[] tokens = segment.split("\\s+");
                tokens = correctTokenization(tokens);
                for (int i = 0; i < tokens.length; i++) {
                    if (i > 0) {
                        writer.write(" ");
                    }
                    writer.write(tokens[i]);
                }
                if (!tokens[tokens.length - 1].equals(".")) {
                    writer.write(" .");
                }
                writer.write("\n");
            }
        }
    }
    public void segmentAndTokenize(String origRootPath, String newRootPath) throws IOException {
        File origRoot = new File(origRootPath);
        File newRoot = new File(newRootPath);
        newRoot.mkdir();
        File[] files = origRoot.listFiles();
        for (File file : files) {
            File newFile = new File(newRoot, file.getName() + ".txt");
            FileWriter writer = new FileWriter(newFile);
            fix(file, writer);
            writer.close();
        }
    }
    public void prepareDepGraphs(String treeRootPath, String depRootPath) throws IOException {
        File treeRootDir = new File(treeRootPath);
        File depRootDir = new File(depRootPath);
        depRootDir.mkdir();
        PTBFileReader treeReader = new PTBFileReader();        
        File[] files = treeRootDir.listFiles();
        for (File file : files) {
            System.out.println("Processing: " + file.getAbsolutePath());
            File depFile = new File(depRootDir, file.getName().replace(".mrg", ".dep"));
            FileWriter writer = new FileWriter(depFile);
            List<Tree> trees = treeReader.read(file);
            for (Tree t : trees) {                
                GrammaticalStructure gs = GSF.newGrammaticalStructure(t);
                //Collection<TypedDependency> dependencies = gs.typedDependencies();
                Collection<TypedDependency> dependencies = gs.typedDependenciesCCprocessed();
                boolean first = true;
                for (TypedDependency td : dependencies) {
                    if (!first) {
                        writer.write("\t");
                    } else {
                        first = false;
                    }
                    writer.write(td.toString());
                }
                writer.write("\n");
            }
            writer.close();
        }
    }
    public void generateSentences(String iobPath, String outputPath) throws IOException {
        File iobRoot = new File(iobPath);
        File outputRoot = new File(outputPath);
        outputRoot.mkdir();        
        
        File[] files = iobRoot.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});            
        for (File file : files) {
            File outputFile = new File(outputRoot, file.getName());
            Text text = reader.read(file);
            FileWriter writer = new FileWriter(outputFile);
            for (Sentence s : text) {
                writer.write(s.toString() + "\n");
            }
            writer.close();
        }
        
    }
    
    private HashMap<String, List<Integer>> prepareHashMap(Sentence s) {
        HashMap<String, List<Integer>> map = new HashMap<String, List<Integer>>();
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (map.containsKey(word.word())) {
                map.get(word.word()).add(i + 1);
            } else {
                List<Integer> list = new ArrayList<Integer>();
                list.add(i + 1);
                map.put(word.word(), list);
            }
        }
        return map;
    }
    
    private int findClosestMatch(HashMap<String, List<Integer>> map, int i, String word, Sentence s) {
        word = word.toLowerCase();
        int min = Integer.MAX_VALUE;
        int minIndex = -1;
        String[] tokens = word.split("\\s+");
        int len = tokens.length;
        for (int j = 0; j <= (s.size() - len); j++) {
            boolean flg = true;
            for (int k = 0; k < len; k++) {
                /*if (s.get(j + k).word().matches("am|is|are|was|were") && tokens[k].equals("be")) {
                    //ok
                }*/
                if (!s.get(j + k).word().toLowerCase().equals(tokens[k]) && !s.get(j + k).word().toLowerCase().startsWith(tokens[k]) && !s.get(j + k).getTag("BASE").toLowerCase().equals(tokens[k])) {
                    flg = false;
                    break;
                }
            }
            if (flg) {
                if (Math.abs(i - (j + len-1)) < min) {
                    min = Math.abs(i - (j + len-1));
                    minIndex = j + len - 1;
                }
            }
        }
        
        return minIndex + 1;
    }
    public void prepareDepsFromMiniparOutput(String iobPath, String miniparPath, String outputPath) throws IOException {
        File iobRoot = new File(iobPath);
        File outputRoot = new File(outputPath);
        File miniparRoot = new File(miniparPath);
        outputRoot.mkdir();
        
        File[] files = iobRoot.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});            
        for (File file : files) {
            File outputFile = new File(outputRoot, file.getName() + ".dep");
            File miniparFile = new File(miniparRoot, file.getName());
            List<String> miniparLines = Util.readLines(miniparFile);
            Text text = reader.read(file);
            FileWriter writer = new FileWriter(outputFile);
            int index = 0;
            for (Sentence s : text) {
                s = tagger.annotate(s);
                HashMap<String, List<Integer>> map = prepareHashMap(s);
                List<String> deps = new ArrayList<String>();
                int count = 0;
                while (count < s.size()) {
                    if (miniparLines.get(index).matches("^\\d.*")) {                        
                        String[] tokens = miniparLines.get(index).split("\\t");
                        if (tokens.length == 6) {
                            String reln = tokens[4];
                            String dep = s.get(count).word() + "-" + (count + 1);
                            //System.out.println(miniparLines.get(index));
                            String gov = tokens[5].substring(5, tokens[5].length() - 2);
                            int govIndex = findClosestMatch(map, count, gov, s);
                            if (govIndex > 0) {
                                gov = s.get(govIndex-1).word() + "-" + govIndex;
                                deps.add(reln + "(" + gov + ", " + dep + ")");                                
                            } else {
                                System.out.println("Word not found");
                                System.out.println("Word: " + gov);
                                System.out.println("sentence: " + s);
                            }
                        }
                        count++;
                    }
                    index++;
                }
                for (int i = 0; i < deps.size(); i++) {
                    if (i != 0) {
                        writer.write("\t");
                    }
                    writer.write(deps.get(i));
                }
                writer.write("\n");
            }
            writer.close();
            
        }
    }
    
    public static void main(String args[]) throws IOException {
        PrepareAIMed tool = new PrepareAIMed();
        tool.preProcess("G:\\UWO\\thesis\\public corpus\\AImed\\interactions", "./resource/relation/aimed/iob", "./resource/relation/aimed/trees");
        //tool.checkData("./resource/relation/aimed/iob");
        //tool.analyzeRawFile("G:\\UWO\\thesis\\public corpus\\AImed\\interactions");
        //tool.segmentAndTokenize("G:\\UWO\\thesis\\public corpus\\AImed\\interactions", "./resource/relation/aimed/raw");
        //tool.analyzeRawFiles("./resource/relation/aimed/raw");
        //tool.prepareDepGraphs("./resource/relation/aimed/trees", "./resource/relation/aimed/depsCC");
        //tool.generateSentences("./resource/relation/aimed/iob", "./resource/relation/aimed/sentences");
        //tool.prepareDepsFromMiniparOutput("./resource/relation/aimed/iob", "./resource/relation/aimed/minipar_output", "./resource/relation/aimed/minipar_dep");
    }
}
