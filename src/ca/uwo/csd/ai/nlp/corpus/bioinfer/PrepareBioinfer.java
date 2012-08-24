/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.bioinfer;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
import ca.uwo.csd.ai.nlp.utils.OTokenizer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import opennlp.tools.util.Span;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PrepareBioinfer {
    final static BLLIPParser PARSER_ANNOTATOR = null;//new BLLIPParser();
    static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    
    public static int markEntity(Sentence s, Span[] spans, int startOffset, int endOffset) {
        boolean started = false;
        int start = -1;
        for (int i = 0; i < spans.length; i++) {
            if (startOffset >= spans[i].getStart() && startOffset < spans[i].getEnd()) {
                s.get(i).setTag("LEXE", "B");
                started = true;
                start = i;
            } else if (started) {
                if (spans[i].getStart() > endOffset) {
                    break;
                }
                s.get(i).setTag("LEXE", "I");
            }
        }
        if (!started) {
            System.out.println("miss!");
        }
        return start;
    }
    /**
     * OpenNLP tokenizer does a horrible job with '(' and ')'
     * @param sentence
     * @param spans
     * @return 
     */
    public static Span[] correctSpans(String sentence, Span[] spans) {
        List<Span> spanList = new ArrayList<Span>();
        for (int i = 0; i < spans.length; i++) {
            Span span = spans[i];
            int start = span.getStart();
            int end = span.getEnd();
            String token = sentence.substring(start, end);
            if (token.equals("(") || token.equals(")")) {
                spanList.add(span);
            } else if ((token.startsWith("(") && !token.contains(")")) || token.startsWith(")") && !token.contains("(")) {
                Span firstSpan = new Span(start, start + 1);
                Span secondSpan = new Span(start + 1, end);
                spanList.add(firstSpan);
                spanList.add(secondSpan);
            } else if (token.endsWith("(") || token.endsWith(")")) {                
                Span firstSpan = new Span(start, end - 1);
                Span secondSpan = new Span(end-1, end);
                spanList.add(firstSpan);
                spanList.add(secondSpan);
            } else if (token.contains("(") && !token.contains(")")) {
                int where = start + token.indexOf('(');                
                Span firstSpan = new Span(start, where);
                Span secondSpan = new Span(where, where+1);
                Span thirdSpan = new Span(where+1, end);
                spanList.add(firstSpan);
                spanList.add(secondSpan);
                spanList.add(thirdSpan);
            } else if (token.contains(")") && !token.contains("(")) {
                int where = start + token.indexOf(')');                
                Span firstSpan = new Span(start, where);
                Span secondSpan = new Span(where, where+1);
                Span thirdSpan = new Span(where+1, end);
                spanList.add(firstSpan);
                spanList.add(secondSpan);
                spanList.add(thirdSpan);
            } else {
                spanList.add(span);
            }
        }
        Span[] newSpans = new Span[spanList.size()];
        for (int i = 0; i < spanList.size(); i++) {
            newSpans[i] = spanList.get(i);
        }
        return newSpans;
    }
    
    public static void markInteraction(Sentence s, int index1, int index2, int count) {
        int tmp = -1;
        if (index1 > index2) {
            tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        TokWord word1 = s.get(index1);        
        if (word1.getTag("P1") == null) {
            word1.setTag("P1", String.valueOf(count));
        } else {
            word1.setTag("P1", word1.getTag("P1") + ", " + String.valueOf(count));
        }
        
        TokWord word2 = s.get(index2);
        if (word2.getTag("P2") == null) {
            word2.setTag("P2", String.valueOf(count));
        } else {
            word2.setTag("P2", word2.getTag("P2") + ", " + String.valueOf(count));
        }
    }
    public static void process(String corpusFilePath, String iobDirPath, String sentenceDirPath) throws IOException {
        
        Document document = Util.readXML(corpusFilePath);
        OTokenizer tokenizer = new OTokenizer();
        NodeList sentNodes = document.getElementsByTagName("sentence");
        int totalInteractions = 0;
        
        Text text = new Text();
        for (int i = 0; i < sentNodes.getLength(); i++) {
            Element sentElement = (Element) sentNodes.item(i);
            String line = sentElement.getAttribute("text");                        
            Span[] spans = tokenizer.getSpans(line);
            spans = correctSpans(line, spans);
            String[] tokens = new String[spans.length];
            for (int j = 0; j < spans.length; j++) {
                tokens[j] = line.substring(spans[j].getStart(), spans[j].getEnd());
            }
            Sentence s = new Sentence(tokens);
            Map<String, Integer> entityMap = new HashMap<String, Integer>();
            //get entities
            NodeList entityNodes = sentElement.getElementsByTagName("entity");
            for (int j = 0; j < entityNodes.getLength(); j++) {
                Element entityElement = (Element) entityNodes.item(j);
                String id = entityElement.getAttribute("id");
                String offsetList[] = entityElement.getAttribute("charOffset").split(",");
                String offsets[] = offsetList[offsetList.length-1].split("-");                
                int startOffset = Integer.parseInt(offsets[0]);
                int endOffset = Integer.parseInt(offsets[1]);
                int index = markEntity(s, spans, startOffset, endOffset);
                entityMap.put(id, index);
            }
            
            
            //get interactions
            NodeList interactionNodes = sentElement.getElementsByTagName("interaction");
            int count = 0;
            Set<String> flagSet = new HashSet<String>();
            for (int j = 0; j < interactionNodes.getLength(); j++) {
                Element interactionElement = (Element) interactionNodes.item(j);
                String entity1Id = interactionElement.getAttribute("e1");
                String entity2Id = interactionElement.getAttribute("e2");
                int index1 = -1;
                if (entityMap.containsKey(entity1Id)) {
                    index1 = entityMap.get(entity1Id);
                }
                int index2 = -1;
                if (entityMap.containsKey(entity2Id)) {
                    index2 = entityMap.get(entity2Id);
                }
                if (index1 != -1 && index2 != -1 && (index1 != index2)) {
                    String signature = String.valueOf(index1) + "_" + String.valueOf(index2);
                    if (!flagSet.contains(signature)) {
                        count++;
                        totalInteractions++;
                        markInteraction(s, index1, index2, count);
                        flagSet.add(signature);
                    }
                    
                }
            }
            //System.out.println(s.toString(new String[]{"P1", "P2"}));            
            text.add(s);
        }
        System.out.println("total: " + totalInteractions);
        File iobDir = new File(iobDirPath);
        File sentDir = new File(sentenceDirPath);
        iobDir.mkdir();
        sentDir.mkdir();
        FileWriter iobWriter = new FileWriter(new File(iobDir, "iob.txt"));
        FileWriter sentWriter = new FileWriter(new File(sentDir, "sentences.txt"));
        for (Sentence s : text) {
            for (TokWord word : s) {
                iobWriter.write(word.word() + "\t");
                String lex = word.getTag("LEXE");
                if (lex == null) lex = "O";
                iobWriter.write(lex + "\t");
                String p1 = word.getTag("P1");
                if (p1 == null) p1 = "O";
                iobWriter.write(p1 + "\t");
                String p2 = word.getTag("P2");
                if (p2 == null) p2 = "O";
                iobWriter.write(p2 + "\n");
            }
            iobWriter.write("\n");
            sentWriter.write(s + "\n");
        }
        iobWriter.close();
        sentWriter.close();
    }
    public static void createparseTree(String iobFilePath, String treeDirPath) throws IOException {
        File treeDir = new File(treeDirPath);
        treeDir.mkdir();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});            
        Text text = reader.read(new File(iobFilePath));
        FileWriter parseWriter = new FileWriter(new File(treeDir, "bioinfer.txt.mrg"));
        for (Sentence s : text) {
            try {                
                s = PARSER_ANNOTATOR.annotate(s);
                parseWriter.write(s.getParseTree().pennString());
            } catch (IllegalArgumentException ex) {
                parseWriter.write("(ROOT)\n");
            }
        }
        parseWriter.close();
    }
    public static void prepareDepGraphs(String treeRootPath, String depRootPath) throws IOException {
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
    public static void main(String args[]) throws IOException {
        //process("./resource/relation/bioinfer/bioinfer-1.2.0b-unified-format.xml", "./resource/relation/bioinfer/iob", "./resource/relation/bioinfer/sentences");
        //createparseTree("./resource/relation/bioinfer/iob/bioinfer.txt", "./resource/relation/bioinfer/trees");
        prepareDepGraphs("./resource/relation/bioinfer/trees", "./resource/relation/bioinfer/depsCC");
    }
}
