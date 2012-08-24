/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.hprd50;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
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
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PrepareHPRD50 {
    final static BLLIPParser PARSER_ANNOTATOR = null;//new BLLIPParser();
    static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    private void writeSentence(FileWriter writer, Sentence s) throws IOException {
        for (TokWord word : s) {
            writer.write(word.word() + "\t");
            if (word.getTag("LEXE") != null) {
                writer.write(word.getTag("LEXE") + "\t");
            } else {
                writer.write("O\t");
            }
            if (word.getTag("P1") != null) {
                writer.write(word.getTag("P1") + "\t");
            } else {
                writer.write("O\t");
            }
            if (word.getTag("P2") != null) {
                writer.write(word.getTag("P2"));
            } else {
                writer.write("O");
            }
            writer.write("\n");
        }
        writer.write("\n");
    }
    private Sentence prepareSentence(String text) {
        Sentence s = new Sentence();
        String[] tokens = text.split("\\s+");
        String last = null;
        for (String token : tokens) {
            int where = token.lastIndexOf('-');
            if (where == -1) {
                TokWord word = new TokWord(token);
                s.add(word);
            } else {
                TokWord word = new TokWord(token.substring(0, where));
                s.add(word);
            }
            last = token;
        }
        if (last == null || !last.equals(".")) {
            s.add(new TokWord("."));
        }
        return s;
    }
    public void prepare(String sourcePath, String outputDirPath) throws IOException {
        File outputDir = new File(outputDirPath);
        outputDir.mkdir();
        List<String> rawLines = Util.readLines(sourcePath);
        FileWriter writer = null;
        int annotCount = 0;
        Sentence s = null;
        for (String line : rawLines) {
            if (!line.equals("")) {
                String[] tokens = line.split("\\t");
                String type = tokens[1];
                if (type.equals("sent")) {
                    String fileName = tokens[0].substring(0, tokens[0].indexOf('.'));
                    writer = new FileWriter(new File(outputDir, fileName + ".txt"), true);                    
                    s = prepareSentence(tokens[2]);
                } else if (type.equals("annot")) {
                    int first = Integer.parseInt(tokens[2]) - 1;
                    int second = Integer.parseInt(tokens[3]) - 1;
                    int label = Integer.parseInt(tokens[5].trim());
                    s.get(first).setTag("LEXE", "B");
                    s.get(second).setTag("LEXE", "B");
                    if (label > 3) {
                        annotCount++;
                        if (s.get(first).getTag("P1") == null) {
                            s.get(first).setTag("P1", String.valueOf(annotCount));
                        } else {
                            s.get(first).setTag("P1", s.get(first).getTag("P1") + ", " + String.valueOf(annotCount));
                        }
                        if (s.get(second).getTag("P2") == null) {
                            s.get(second).setTag("P2", String.valueOf(annotCount));
                        } else {
                            s.get(second).setTag("P2", s.get(second).getTag("P2") + ", " + String.valueOf(annotCount));
                        }
                    }
                }
            } else {
                writeSentence(writer, s);
                writer.close();
                annotCount = 0;
            }
        }
    }
    public void prepareParseTree(String iobPath, String treePath) throws IOException {
        File iobDir = new File(iobPath);
        File treeDir = new File(treePath);
        treeDir.mkdir();
        File[] files = iobDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
            File treeFile = new File(treeDir, file.getName() + ".mrg");
            FileWriter writer = new FileWriter(treeFile);
            Text text = reader.read(file);
            for (Sentence s : text) {
                try {                
                    s = PARSER_ANNOTATOR.annotate(s);
                    writer.write(s.getParseTree().pennString());
                } catch (IllegalArgumentException ex) {
                    System.out.println("Error!");
                    writer.write("(ROOT)\n");
                }
            }
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
    public static void main(String args[]) throws IOException {
        PrepareHPRD50 tool = new PrepareHPRD50();
        tool.prepare("./resource/relation/HPRD50/hprd50_annot_final.txt", "./resource/relation/HPRD50/iob");
        //tool.prepareParseTree("./resource/relation/HPRD50/iob", "./resource/relation/HPRD50/trees");
        //tool.prepareDepGraphs("./resource/relation/HPRD50/trees", "./resource/relation/HPRD50/depsCC");
    }
}
