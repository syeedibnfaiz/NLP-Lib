/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a new piped annotation with gorn addresses.
 * @author mibnfaiz
 */
public class PDTB2NewPDTB {
    String pipedAnnRoot;
    String rawRoot;
    String ptbRoot;
    String newAnnRoot;
    
    public PDTB2NewPDTB(String pipedAnnRoot, String rawRoot, String ptbRoot, String newAnnRoot) {
        this.pipedAnnRoot = pipedAnnRoot;
        this.rawRoot = rawRoot;
        this.ptbRoot = ptbRoot;
        this.newAnnRoot = newAnnRoot;
        if (!new File(pipedAnnRoot).isDirectory() || !new File(rawRoot).isDirectory() || !new File(ptbRoot).isDirectory() || !new File(newAnnRoot).isDirectory()) {
            throw new IllegalArgumentException("pipedAnnRoot, rawroot and ptbRoot should be existing directories.");
        }
        
    }
    
    public void createNewAnnotations() {
        
        File pipedAnnRootDir = new File(pipedAnnRoot);
        File rawRootDir = new File(rawRoot);
        File ptbRootDir = new File(ptbRoot);
        File newAnnrootDir = new File(newAnnRoot);
        File[] annSectionDirs = pipedAnnRootDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        RawFileReader rawFileReader = new RawFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        for (File annSectionDir : annSectionDirs) {        
            
            File rawSectionDir = new File(rawRootDir, annSectionDir.getName());
            File ptbSectionDir = new File(ptbRootDir, annSectionDir.getName());
            File newAnnSectionDir = new File(newAnnrootDir, annSectionDir.getName());
            newAnnSectionDir.mkdir();
            
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
                File newAnnFile = new File(newAnnSectionDir, annFile.getName());
                
                List<PDTBRelation> relations = pipedFileReader.read(annFile);
                //printStat(relations);
                List<RawSentence> rawSentences = rawFileReader.read(rawFile);
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                List<ParsedSentence> parsedSentences = new ArrayList<ParsedSentence>();
                for (Tree ptbTree : ptbTrees) {
                    parsedSentences.add(new ParsedSentence(ptbTree));
                }
                process(relations, rawSentences, ptbTrees, parsedSentences, newAnnFile);
            }
        }        
        
    }
    private void process(List<PDTBRelation> relations, List<RawSentence> rawSentences, List<Tree> ptbTrees, List<ParsedSentence> parsedSentences, File newAnnFile) {
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        try {
            BufferedWriter annWriter = new BufferedWriter(new FileWriter(newAnnFile));
            for (PDTBRelation relation : relations) {
                //manage conn
                if (relation.getType().equals("Explicit") && !relation.getConnRawText().isEmpty()) {
                    SpanList spanList = relation.getConnSpanList();                    
                    relation.setConnectiveGornAddress(getGornAddress(spanList, relation, rawSentences, ptbTrees, parsedSentences));
                    
                    spanList = relation.getArg1SpanList();
                    relation.setArg1GornAddress(getGornAddress(spanList, relation, rawSentences, ptbTrees, parsedSentences));
                    
                    spanList = relation.getArg2SpanList();
                    relation.setArg2GornAddress(getGornAddress(spanList, relation, rawSentences, ptbTrees, parsedSentences));
                }
                annWriter.write(relation.toString() + "\n");
            }
            annWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(PDTB2NewPDTB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    private String getGornAddress(SpanList spanList,PDTBRelation relation, List<RawSentence> rawSentences, List<Tree> ptbTrees, List<ParsedSentence> parsedSentences) {
        StringBuilder gornAddress = new StringBuilder();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        for (Span span : spanList) {
            List<Integer> indices = findSpanInSentence(span, rawSentences);
            if (indices.isEmpty()) {
                System.out.println(span);                
                throw new RuntimeException("index == -1");
            }
            for (Integer index: indices) {                
                int start = -1;
                if (rawSentences.get(index).getStartOffset() >= span.getStart()) {
                    start = 0;
                } else {
                    String firstPrefix = rawSentences.get(index).getPrefix(span.getStart() - 1);
                    start = parsedSentences.get(index).matchPrefix(firstPrefix) + 1;
                    if (start == -1) {
                        System.out.println("start == -1");
                        System.out.println(firstPrefix);
                        System.out.println(rawSentences.get(index));
                        System.out.println(analyzer.getPennOutput(ptbTrees.get(index)));
                    }
                }
                
                int end = -1;
                if (rawSentences.get(index).getEndOffset() <= span.getEnd()) {
                    end = ptbTrees.get(index).getLeaves().size() - 1;
                } else {
                    String secondPrefix = rawSentences.get(index).getPrefix(span.getEnd() - 1);
                    end = parsedSentences.get(index).matchPrefix(secondPrefix);
                    if (end == -1) {                        
                        System.out.println("end == -1");
                        System.out.println(secondPrefix);
                        System.out.println(rawSentences.get(index));
                        System.out.println(analyzer.getPennOutput(ptbTrees.get(index)));
                    }
                }
                List<Tree> gornNodes;
                try {
                    gornNodes = analyzer.findGornNodes(ptbTrees.get(index), start, end);
                } catch (RuntimeException ex) {
                    System.out.println(indices.size());
                    System.out.println("span.start=" + span.getStart());
                    System.out.println("span.end=" + span.getEnd());
                    System.out.println("rawStart=" + rawSentences.get(index).getStartOffset());
                    System.out.println("rawEnd=" + rawSentences.get(index).getEndOffset());
                    System.out.println("Tree size =" + ptbTrees.get(index).getLeaves().size());
                    
                    System.out.println(analyzer.getPennOutput(ptbTrees.get(index)));
                    System.out.println(start);
                    System.out.println(end);
                    throw ex;
                }

                String address = analyzer.getGornAddress(ptbTrees.get(index), gornNodes, index);
                if (gornAddress.length() > 0) {
                    gornAddress.append(";");
                }
                gornAddress.append(address);
            }            
        }
        return gornAddress.toString();
    }
    private List<Integer> findSpanInSentence(Span span, List<RawSentence> rawSentences) {
        int start = span.getStart();
        int end = span.getEnd(); 
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < rawSentences.size(); i++) {
            if (start >= rawSentences.get(i).getStartOffset() && start < rawSentences.get(i).getEndOffset()) {
                list.add(i);
            } else if ((end-1) >= rawSentences.get(i).getStartOffset() && end <= rawSentences.get(i).getEndOffset()) {
                list.add(i);
            } else if (start < rawSentences.get(i).getStartOffset() && end > rawSentences.get(i).getEndOffset()) {
                list.add(i);
            }
        }
        return list;
    }
    public void printStat(List<PDTBRelation> relations) {
        for (PDTBRelation relation: relations) {
            if (relation.getType().matches("Explicit") && relation.getConnSpanList().size() > 1) {
                System.out.println(relation.getConnRawText() + " :" + relation.getConnHead());
            }
        }
    }
    
    public void testAnnotation() {
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        File rawRootDir = new File(rawRoot);
        File ptbRootDir = new File(ptbRoot);
        File newAnnrootDir = new File(newAnnRoot);
        File[] annSectionDirs = newAnnrootDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        RawFileReader rawFileReader = new RawFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        for (File annSectionDir : annSectionDirs) {        
            
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
                List<RawSentence> rawSentences = rawFileReader.read(rawFile);
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                List<ParsedSentence> parsedSentences = new ArrayList<ParsedSentence>();
                for (Tree ptbTree : ptbTrees) {
                    parsedSentences.add(new ParsedSentence(ptbTree));
                }
                for (PDTBRelation relation : relations) {
                    if (!relation.getType().equals("Explicit")) continue;
                    String connectiveGornAddress = relation.getArg2GornAddress();
                    String tokens[] = connectiveGornAddress.split(";");
                    StringBuilder sb = new StringBuilder();
                    for (String token : tokens) {
                        //System.out.println("token = "+token);
                        if (token.isEmpty()) {
                            System.out.println(relation);
                            continue;
                        }
                        int index = Integer.parseInt(token.split(",")[0]);
                        Tree gornNode = analyzer.getGornNode(ptbTrees.get(index), token);
                        List<Tree> leaves = gornNode.getLeaves();
                        for (Tree leaf : leaves) {
                            if (!leaf.value().matches("-LRB-|-RRB-|-LCB-|-RCB-")) {
                                sb.append(removeNonAlphaNumeric(leaf.value()));
                            }
                        }
                    }
                    String rawText = relation.getArg2RawText().replaceAll("can't", "cannt");
                    rawText = rawText.replaceAll("won't", "willnt");
                    rawText = rawText.replaceAll("ain't", "ISnt");
                    if (!sb.toString().matches(removeNonAlphaNumeric(rawText))) {
                        System.out.println("raw:" + rawText);
                        System.out.println(sb);
                    }
                }
            }
        }
    }
    private String removeNonAlphaNumeric(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            //|| line.charAt(i) == ',' is added for new piped version, coz , were being included in the gorn address
            if (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == ',') {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }
    public static void main(String args[]) {
        PDTB2NewPDTB pdtb = new PDTB2NewPDTB("./pdtb_v2/piped_data", "./pdtb_v2/RAW/WSJ", "./pdtb_v2/ptb", "./pdtb_v2/piped_data_2");
        pdtb.createNewAnnotations();
        //pdtb.testAnnotation();
    }
}
