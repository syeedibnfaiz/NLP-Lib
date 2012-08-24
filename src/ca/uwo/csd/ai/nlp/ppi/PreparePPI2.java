/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
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
public class PreparePPI2 {
    private final static GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final static BLLIPParser PARSER_ANNOTATOR = null;//new BLLIPParser();
    static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    
    public static void prepareIOB2(File oldRoot, File newRoot) throws IOException {
        File oldIOBRoot = new File(oldRoot, "iob");
        File newIOBRoot = new File(newRoot, "iob");
        newIOBRoot.mkdir();
        File[] files = oldIOBRoot.listFiles();
        TokWord word = new TokWord(".");
        word.setTag("P1", "O");
        word.setTag("P2", "O");
        word.setTag("N1", "O");
        word.setTag("N2", "O");
        for (File file : files) {
            Text text = TEXT_READER.read(file);
            File newFile = new File(newIOBRoot, file.getName());
            FileWriter iobWriter = new FileWriter(newFile);
            for (Sentence s : text) {
                int size = s.size();
                if (!s.get(size-1).word().equals(".")) {
                    s.add(word);
                }
                for (TokWord tw : s) {
                    iobWriter.write(tw.word() + "\t");                    
                    String p1 = tw.getTag("P1");
                    if (p1 == null) {
                        p1 = "O";
                    }
                    iobWriter.write(p1 + "\t");
                    String p2 = tw.getTag("P2");
                    if (p2 == null) {
                        p2 = "O";
                    }
                    iobWriter.write(p2 + "\t");
                    String n1 = tw.getTag("N1");
                    if (n1 == null) {
                        n1 = "O";
                    }
                    iobWriter.write(n1 + "\t");
                    String n2 = tw.getTag("N2");
                    if (n2 == null) {
                        n2 = "O";
                    }
                    iobWriter.write(n2 + "\n");
                }
                iobWriter.write("\n");
            }
            iobWriter.close();
        }
    }
    
    public static void prepareTrees(File corpusRoot) throws IOException {
        File iobRoot = new File(corpusRoot, "iob");
        File treeRoot = new File(corpusRoot, "trees");
        treeRoot.mkdir();
        File[] files = iobRoot.listFiles();
        for (File file : files) {
            System.out.println("Processing: " + file.getName());
            Text text = TEXT_READER.read(file);
            File treeFile = new File(treeRoot, file.getName().replace(".txt", ".mrg"));
            FileWriter treeWriter = new FileWriter(treeFile);
            for (Sentence s : text) {
                try {
                    s = PARSER_ANNOTATOR.annotate(s);
                    treeWriter.write(s.getParseTree().pennString());
                } catch (Exception ex) {
                    System.out.println("Parse failed for: " + s);
                    treeWriter.write("(ROOT)\n");
                }
            }
            treeWriter.close();
        }
    }
    public static void prepareDeps(File corpusRoot) throws IOException {
        File treeRoot = new File(corpusRoot, "trees");
        File depRoot = new File(corpusRoot, "depsCC");        
        depRoot.mkdir();        
        PTBFileReader reader = new PTBFileReader();
        File[] treeFiles = treeRoot.listFiles();
        for (File treeFile : treeFiles) {
            System.out.println("Processing: " + treeFile.getName());
            FileWriter depWriter = new FileWriter(new File(depRoot, treeFile.getName().replace(".mrg", ".dep")));            
            List<Tree> trees = reader.read(treeFile);
            for (Tree t : trees) {
                GrammaticalStructure gs = GSF.newGrammaticalStructure(t);
                //Collection<TypedDependency> dependencies = gs.typedDependencies();
                Collection<TypedDependency> dependencies = gs.typedDependenciesCCprocessed();
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
    }
    public static void main(String args[]) throws IOException {
        //File oldRoot = new File("./resource/relation/PPI");
        File newRoot = new File("./resource/relation/PPI2");
        /*newRoot.mkdir();
        File[] files = oldRoot.listFiles();
        for (File dir : files) {
        File newDir = new File(newRoot, dir.getName());
        newDir.mkdir();
        prepareIOB2(dir, newDir);
        }*/
        //get trees
        File[] corpora = newRoot.listFiles();
        for (File corpusRoot : corpora) {
            prepareTrees(corpusRoot);
            prepareDeps(corpusRoot);
        }
    }
}
