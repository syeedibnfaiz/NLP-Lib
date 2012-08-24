package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.utils.SocketUtil;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Starts with the IOB and Info files created by <code>CorpusProcessor</code>,
 * then simplifies the sentences by simplifying NP's and completing incomplete
 * sentences (e.g., titles). Then creates parses trees and dependency trees.
 * Creates PPI3
 * @author Syeed Ibn Faiz
 */
public class CorpusProcessor3 {

    private final static GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2", "LEMMA"});
    private final static BioDomainAnnotator BIO_DOMAIN_ANNOTATOR = new BioDomainAnnotator();
    private final static GeniaTagger GENIA_TAGGER = new GeniaTagger();
    private final static BLLIPParser PARSER_ANNOTATOR = null;//new BLLIPParser();
    private final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    private SocketUtil socket;
    
    public static void main(String[] args) throws IOException {
        CorpusProcessor3 processor = new CorpusProcessor3();
        processor.simplify();
        //processor.prepareTrees();
        //processor.prepareDeps();
    }

    public void prepareDeps() throws IOException {
        socket = new SocketUtil("localhost", 8662);
        prepareDeps("./resource/relation/PPI4/LLL");
        prepareDeps("./resource/relation/PPI4/HPRD50");
        prepareDeps("./resource/relation/PPI4/IEPA");
        prepareDeps("./resource/relation/PPI4/AIMed");
        prepareDeps("./resource/relation/PPI4/BioInfer");
    }
    
    public void prepareDeps(String corpusRoot) throws IOException {
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
                String pennOutput = TREE_ANALYZER.getPennOutput(t);
                socket.sendLine(pennOutput + "\n");
                String dependencies = socket.readline();
                dependencies = dependencies.trim();
                depWriter.write(dependencies + "\n");                
            }
            depWriter.close();
        }
    }
            
    public void prepareTrees() throws IOException {
        socket = new SocketUtil("78.129.218.202", 8662);
        //prepareTrees("./resource/relation/PPI4/LLL");
        prepareTrees("./resource/relation/PPI4/HPRD50");
        prepareTrees("./resource/relation/PPI4/IEPA");
        prepareTrees("./resource/relation/PPI4/AIMed");
        prepareTrees("./resource/relation/PPI4/BioInfer");
        socket.closeConnection();
    }
    
    public void prepareTrees(String corpusRoot) throws IOException {
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
                    if (isRelationBearing(s)) {
                        //s = PARSER_ANNOTATOR.annotate(s);
                        //treeWriter.write(s.getParseTree().pennString());
                        socket.sendLine(s.toString() + "\n");
                        treeWriter.write(socket.readline() + "\n");
                    } else {
                        treeWriter.write("(ROOT (S (X Dummy) (X Tree)))\n");
                    }
                } catch (Exception ex) {
                    System.out.println("Parse failed for: " + s);
                    treeWriter.write("(ROOT)\n");
                }
            }
            treeWriter.close();
        }
    }

    private boolean isRelationBearing(Sentence s) {
        boolean relation = false;
        for (TokWord word : s) {
            if (!word.getTag("P1").equals("O") || !word.getTag("N1").equals("O")) {
                relation = true;
                break;
            }
        }
        return relation;
    }

    public void simplify() throws IOException {
        simplifyIOB("./resource/relation/PPI5/LLL");
        simplifyIOB("./resource/relation/PPI5/HPRD50");
        simplifyIOB("./resource/relation/PPI5/IEPA");
        simplifyIOB("./resource/relation/PPI5/AIMed");
        simplifyIOB("./resource/relation/PPI5/BioInfer");
    }

    public void simplifyIOB(String corpusRoot) throws IOException {
        System.out.println(corpusRoot);
        File corpusDir = new File(corpusRoot);
        File iobDir = new File(corpusDir, "iob");

        File[] iobFiles = iobDir.listFiles();
        for (File iobFile : iobFiles) {
            simplifyIOBFile(iobFile);
        }
    }

    private void simplifyIOBFile(File iobFile) throws IOException {
        Text text = TEXT_READER.read(iobFile);
        for (int i = 0; i < text.size(); i++) {
            Sentence s = text.get(i);
            s = GENIA_TAGGER.annotate(s);
            BIO_DOMAIN_ANNOTATOR.annotate(s);
            cleanUp(s);            
            completeSentence(s);
            text.set(i, s);
        }
        writeSimplifiedIOB(iobFile, text);
    }

    private void writeSimplifiedIOB(File iobFile, Text text) throws IOException {
        FileWriter writer = new FileWriter(iobFile);
        for (Sentence s : text) {
            for (TokWord word : s) {
                writer.write(word.word());
                String tags[] = {"P1", "P2", "N1", "N2"};
                for (String tag : tags) {
                    String value = word.getTag(tag);
                    if (value == null) {
                        value = "O";
                    }
                    writer.write("\t" + value);
                }
                String lemma = word.getTag("BASE");
                if (lemma != null) {
                    writer.write("\t" + lemma);
                } else {
                    writer.write("\t" + word.word());
                }
                writer.write("\n");
            }
            writer.write("\n");
        }
        writer.close();
    }

    private Sentence cleanUp(Sentence s) {        
        //cleaning up for BLLIP parser
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("\\/")) {
                s.get(i).setWord(s.get(i).word().replace("\\/", "/"));
            }
        }
        return s;
    }

    private Sentence completeSentence(Sentence s) {
        int size = s.size();
        TokWord terminalToken = null;
        if (s.get(size - 1).word().matches("[.?!]")) {
            terminalToken = s.get(size - 1);
            s.remove(size - 1);
        } else {
            terminalToken = createTokenWord(".");
        }

        if (isIncompleteSentence(s)) {
            s.add(createTokenWord("can"));
            s.add(createTokenWord("be"));
            s.add(createTokenWord("shown"));
        }
        s.add(terminalToken);

        return s;
    }

    private boolean isIncompleteSentence(Sentence s) {
        boolean incomplete = true;
        for (TokWord word : s) {
            if (word.getTag("POS").matches("VB(P|D|Z)?|MD|AUX")) {
                incomplete = false;
                break;
            }
        }
        return incomplete;
    }

    private TokWord createTokenWord(String word) {
        TokWord tokWord = new TokWord(word);
        tokWord.setTag("P1", "O");
        tokWord.setTag("P2", "O");
        tokWord.setTag("N1", "O");
        tokWord.setTag("N2", "O");
        return tokWord;
    }
}
