/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.util.DirectoryFilter;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PDTB2ConnDBGS {
    String pipedAnnRoot;
    String rawRoot;
    String ptbRoot;
    
    private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    private static final GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    private static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public PDTB2ConnDBGS(String pipedAnnRoot, String rawRoot, String ptbRoot) {
        this.pipedAnnRoot = pipedAnnRoot;
        this.rawRoot = rawRoot;
        this.ptbRoot = ptbRoot;
        if (!new File(pipedAnnRoot).isDirectory() || !new File(rawRoot).isDirectory() || !new File(ptbRoot).isDirectory()) {
            throw new IllegalArgumentException("pipedAnnRoot, rawroot and ptbRoot should be existing directories.");
        }
        
    }
    
    public void processExplicitRelations() {
        HashSet<String> skipSectionSet = new HashSet<String>();
        //skipSectionSet.add("00");skipSectionSet.add("01");skipSectionSet.add("23");skipSectionSet.add("24");
        //skipSectionSet.add("22");
        File pipedAnnRootDir = new File(pipedAnnRoot);
        File rawRootDir = new File(rawRoot);
        File ptbRootDir = new File(ptbRoot);
        File[] annSectionDirs = pipedAnnRootDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        RawFileReader rawFileReader = new RawFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        FileWriter relationWriter = null;
        FileWriter treeWriter = null;
        FileWriter depWriter = null;
        try {
            relationWriter = new FileWriter("gs_explicit_relations_0_1");
            treeWriter = new FileWriter("gs_explicit_relations_tree_0_1");
            depWriter = new FileWriter("gs_explicit_relations_dep_0_1");
        } catch (IOException ex) {
            Logger.getLogger(PDTB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (File annSectionDir : annSectionDirs) {
            //if (skipSectionSet.contains(annSectionDir.getName())) continue;
            if (!annSectionDir.getName().matches("00|01")) continue;
            //if (!annSectionDir.getName().matches("24")) continue;
            
            File rawSectionDir = new File(rawRootDir, annSectionDir.getName());
            File ptbSectionDir = new File(ptbRootDir, annSectionDir.getName());
            
            File[] annFiles = annSectionDir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File file, String string) {
                    return string.endsWith(".pipe");
                }
            });
            for (File annFile : annFiles) {
                System.out.println("Reading: " + annFile.getAbsolutePath());
                File rawFile = new File(rawSectionDir, annFile.getName().replace(".pipe", ""));
                File ptbFile = new File(ptbSectionDir, annFile.getName().replace(".pipe", ".mrg"));
                List<PDTBRelation> relations = pipedFileReader.read(annFile);
                //printStat(relations);
                List<RawSentence> rawSentences = rawFileReader.read(rawFile);
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                List<ParsedSentence> parsedSentences = new ArrayList<ParsedSentence>();
                for (Tree ptbTree : ptbTrees) {
                    parsedSentences.add(new ParsedSentence(ptbTree));
                }
                
                process(relations, rawSentences, ptbTrees, parsedSentences, relationWriter, treeWriter, depWriter);
            }
        }
        try {
            relationWriter.close();
            treeWriter.close();
            depWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(PDTB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    private void process(List<PDTBRelation> relations, List<RawSentence> rawSentences, List<Tree> ptbTrees, List<ParsedSentence> parsedSentences, FileWriter relationWriter, FileWriter treeWriter, FileWriter depWriter) {
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        
        for (PDTBRelation relation : relations) {
            if (relation.getType().equals("Explicit")) {
                String connGornAd = relation.getConnectiveGornAddress();
                GornAddressList gAdList = new GornAddressList(connGornAd);
                int index = gAdList.get(0).getLineNumber();
                Tree root = ptbTrees.get(index);
                List<Tree> gornNodes = TREE_ANALYZER.getGornNodes(root, connGornAd);
                
                
                List<Integer> connPositions = new ArrayList<Integer>();
                StringBuilder sb = new StringBuilder();
                for (Tree node : gornNodes) {
                    List<Tree> leaves = node.getLeaves();
                    for (Tree leaf : leaves) {
                        int pos = TREE_ANALYZER.getLeafPosition(root, leaf);
                        connPositions.add(pos);
                        sb.append(parsedSentences.get(index).get(pos).word() + " ");
                    }
                }
                
                if (sb.toString().trim().equals(relation.getConnRawText())) {
                    System.out.println("True!!");
                } else {
                    System.out.println("False!!");
                    //System.out.println(relation.toString());
                    System.out.println(sb.toString());
                    System.out.println("Connective: " + relation.getConnRawText());
                    System.out.println("Head: " + relation.getConnHead());
                }
                String connHead = relation.getConnHead();
                if (connHead.equals("afterward")) {
                    for (Integer pos : connPositions) {
                        String word = parsedSentences.get(index).get(pos).word().toLowerCase();
                        if (word.matches("afterwards?")) {
                            parsedSentences.get(index).get(pos).setTag("DIS_CON", "B-Conn");
                        }
                    }
                } else if (connHead.matches("if then|if else|either or|neither nor")) {
                    String tokens[] = connHead.split("\\s+");
                    for (Integer pos : connPositions) {
                        String word = parsedSentences.get(index).get(pos).word().toLowerCase();
                        if (word.equals(tokens[0])) {
                            parsedSentences.get(index).get(pos).setTag("DIS_CON", "DB-Conn");
                            parsedSentences.get(index).get(pos).setTag("SENSE", relation.getSense());
                        } else if (word.equals(tokens[1])) {
                            parsedSentences.get(index).get(pos).setTag("DIS_CON", "DI-Conn");
                            parsedSentences.get(index).get(pos).setTag("SENSE", relation.getSense());
                        }
                    }
                } else {
                    String tokens[] = connHead.split("\\s+");
                    for (int i = 0; i <= connPositions.size() - tokens.length; i++) {                        
                        boolean flg = true;                        
                        for (int k = 0; k < tokens.length; k++) {
                            int pos = connPositions.get(i + k);
                            String word = parsedSentences.get(index).get(pos).word().toLowerCase();
                            if (!tokens[k].equals(word)) {
                                flg = false;
                                break;
                            }
                        }
                        if (flg) {
                            for (int k = 0; k < tokens.length; k++) {
                                int pos = connPositions.get(i + k);
                                if (k == 0) {
                                    parsedSentences.get(index).get(pos).setTag("DIS_CON", "B-Conn");
                                    parsedSentences.get(index).get(pos).setTag("SENSE", relation.getSense());
                                } else {
                                    parsedSentences.get(index).get(pos).setTag("DIS_CON", "I-Conn");
                                    parsedSentences.get(index).get(pos).setTag("SENSE", relation.getSense());
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        for (ParsedSentence parsedSentence : parsedSentences) {
            try {
                for (TokWord word : parsedSentence) {
                    /*if (word.getTag("-NONE-") != null) {
                        continue;
                    }*/
                    relationWriter.write(word.word() + " ");                    
                    if (word.getTag("DIS_CON") == null) {
                        relationWriter.write("O");
                    } else {
                        relationWriter.write(word.getTag("DIS_CON"));
                    }
                    /*if (word.getTag("SENSE") == null) {
                        relationWriter.write(" O");
                    } else {
                        relationWriter.write(" "+word.getTag("SENSE"));
                    }*/
                    relationWriter.write("\n");
                }
                relationWriter.write("\n");
                treeWriter.write(analyzer.getPennOutput(parsedSentence.getParseTree()) + "\n");
                relationWriter.flush();
                treeWriter.flush();
                /*Tree root = parsedSentence.getParseTree();
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
                depWriter.flush();*/
                
                //System.out.println(parsedSentence.toString("DIS_CON"));
            } catch (IOException ex) {
                System.err.println("Error at process: " + ex.getMessage());
            }
        }
    }
    public void printStat(List<PDTBRelation> relations) {
        for (PDTBRelation relation: relations) {
            if (relation.getType().matches("Explicit") && relation.getConnSpanList().size() > 1) {
                System.out.println(relation.getConnRawText() + " :" + relation.getConnHead());
            }
        }
    }
    public static void main(String args[]) {
        //PDTB pdtb = new PDTB("/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/piped_data", "/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/RAW/WSJ", "/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/ptb");
        //PDTB2ConnDB pdtb = new PDTB2ConnDB("./pdtb_v2/piped_data", "./pdtb_v2/RAW/WSJ", "./pdtb_v2/ptb");
        PDTB2ConnDBGS pdtb = new PDTB2ConnDBGS("./pdtb_v2/piped_data", "./pdtb_v2/RAW/WSJ", "./package/treebank_3/parsed/mrg/wsj");
        pdtb.processExplicitRelations();
    }
}
