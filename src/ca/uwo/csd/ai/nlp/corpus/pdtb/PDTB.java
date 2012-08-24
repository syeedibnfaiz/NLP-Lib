/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.util.DirectoryFilter;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PDTB {
    String pipedAnnRoot;
    String rawRoot;
    String ptbRoot;

    public PDTB(String pipedAnnRoot, String rawRoot, String ptbRoot) {
        this.pipedAnnRoot = pipedAnnRoot;
        this.rawRoot = rawRoot;
        this.ptbRoot = ptbRoot;
        if (!new File(pipedAnnRoot).isDirectory() || !new File(rawRoot).isDirectory() || !new File(ptbRoot).isDirectory()) {
            throw new IllegalArgumentException("pipedAnnRoot, rawroot and ptbRoot should be existing directories.");
        }
        
    }
    
    public void processExplicitRelations() {
        HashSet<String> skipSectionSet = new HashSet<String>();
        skipSectionSet.add("00");skipSectionSet.add("01");skipSectionSet.add("23");skipSectionSet.add("24");
        skipSectionSet.add("22");
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
        try {
            relationWriter = new FileWriter("explicit_relations_test_2_21");
            treeWriter = new FileWriter("explicit_relations_tree_test_2_21");
        } catch (IOException ex) {
            Logger.getLogger(PDTB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (File annSectionDir : annSectionDirs) {
            if (skipSectionSet.contains(annSectionDir.getName())) continue;
            //if (!annSectionDir.getName().matches("23|24")) continue;
            
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
                process(relations, rawSentences, ptbTrees, parsedSentences, relationWriter, treeWriter);
            }
        }
        try {
            relationWriter.close();
            treeWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(PDTB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    private void process(List<PDTBRelation> relations, List<RawSentence> rawSentences, List<Tree> ptbTrees, List<ParsedSentence> parsedSentences, FileWriter relationWriter, FileWriter treeWriter) {
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        
        for (PDTBRelation relation : relations) {
            if (relation.getType().equals("Explicit")) {
                //System.out.println(relation.getType() + "," + relation.getConnRawText());
                SpanList connSpanList = relation.getConnSpanList();
                /*if (connSpanList.size() > 1) {
                    continue;  //skipping if..then , neither..nor and either or
                }*/
                for (int k = 0; k < connSpanList.size(); k++) {
                    /*int connStart = connSpanList.get(0).getStart();
                    int connEnd = connSpanList.get(0).getEnd();*/
                    int connStart = connSpanList.get(k).getStart();
                    int connEnd = connSpanList.get(k).getEnd();
                    
                    int index = -1;
                    for (int i = 0; i < rawSentences.size(); i++) {
                        RawSentence rSentence = rawSentences.get(i);
                        if (connStart >= rSentence.getStartOffset() && connEnd <= rSentence.getEndOffset()) {
                            index = i;
                            //System.out.println("raw: " + rSentence);
                            //System.out.println("Parsed: " + parsedSentences.get(i));
                            break;
                        }
                    }
                    //System.out.println("Index = " + index);
                    //find connHead in conn
                    String connHead = relation.getConnHead();
                    String conn = relation.getConnRawText();
                    if (connSpanList.size() > 1) {  //redefine
                        String tokens[] = relation.getConnRawText().split("\\s+");
                        conn = tokens[k];
                        tokens = relation.getConnHead().split("\\s+");
                        connHead = tokens[k];
                    } /*else { //for full connective
                        connHead = conn;
                    }*/
                    int headStart = conn.toLowerCase().indexOf(connHead.toLowerCase());
                    if (headStart == -1) {
                        System.out.println("Head in not in connective!");
                        System.out.println("Conn: " + conn);
                        System.out.println("head: " + connHead);
                        continue;
                    }

                    //remove spaces from connHead
                    String connHeadCollapsed = conn.substring(headStart, headStart + connHead.length()).replaceAll("\\s+", "");
                    //update start end markers for connHead
                    connStart += headStart;
                    connEnd = connStart + connHead.length() - 1; //inclusive 

                    String connPrefix = rawSentences.get(index).getPrefix(connEnd); //raw_sentence.substring(0, connEnd+1)
                    ParsedSentence parsedSentence = parsedSentences.get(index);
                    int endWordPos = parsedSentence.matchPrefix(connPrefix);
                    if (endWordPos == -1) {
                        System.out.println("EndWordPos == -1!");
                        System.out.println("connPrefix: " + connPrefix);
                        System.out.println("raw: " + rawSentences.get(index));
                        System.out.println("parsedSent: " + parsedSentence);
                        continue;
                    }
                    int startWordPos = -1;

                    for (int i = endWordPos; i >= 0; i--) {
                        if (parsedSentence.getSequence(i, endWordPos).equals(connHeadCollapsed)) {
                            startWordPos = i;
                            break;
                        } else if (parsedSentence.getSequence(i, endWordPos).equals(connHeadCollapsed+".")) {
                            startWordPos = i;
                            break;
                        } else if (connHeadCollapsed.endsWith("fterward") && parsedSentence.getSequence(i, endWordPos).equals(connHeadCollapsed+"s")) {
                            startWordPos = i;
                            break;
                        }
                    }
                    if (startWordPos == -1) {
                        System.out.println("StartWordPos == -1!!");
                        System.out.println(connHeadCollapsed);
                        System.out.println("parsedSent: " + parsedSentence);
                        continue;
                    }
                    if (connSpanList.size() == 1) {
                        parsedSentence.get(startWordPos).setTag("DIS_CON", "B-conn");
                    } else {
                        if (k == 0) {
                            parsedSentence.get(startWordPos).setTag("DIS_CON", "DB-conn");
                        } else {
                            parsedSentence.get(startWordPos).setTag("DIS_CON", "DI-conn");
                        }
                    }
                    parsedSentence.get(startWordPos).setTag("SENSE", relation.getSense());
                    for (int i = startWordPos + 1; i <= endWordPos; i++) {
                        parsedSentence.get(i).setTag("DIS_CON", "I-conn");
                        //parsedSentence.get(i).setTag("SENSE", relation.getSense());
                    }
                }
            }
        }
        for (ParsedSentence parsedSentence : parsedSentences) {
            try {
                for (TokWord word : parsedSentence) {
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
                System.out.println(parsedSentence.toString("DIS_CON"));
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
        PDTB pdtb = new PDTB("./pdtb_v2/piped_data", "./pdtb_v2/RAW/WSJ", "./pdtb_v2/ptb");
        pdtb.processExplicitRelations();
    }
}
