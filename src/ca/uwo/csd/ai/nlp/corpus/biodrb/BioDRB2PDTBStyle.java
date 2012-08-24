/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.biodrb;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddress;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.Span;
import ca.uwo.csd.ai.nlp.corpus.pdtb.SpanList;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.OSentenceBoundaryDetector;
import ca.uwo.csd.ai.nlp.utils.Util;
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts BioDRB data files in PDTB format
 * @author Syeed Ibn Faiz
 */
public class BioDRB2PDTBStyle {
    private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    private static final GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    private static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public void createConnIOBFile(String bioDRBFile, String outputFile) throws IOException {
        GenericTextReader gTextReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"WORD","1", "2", "CONN"});
        Text text = gTextReader.read(new File(bioDRBFile));
        FileWriter writer = new FileWriter(outputFile);
        
        for (Sentence s : text) {
            for (int i = 0; i < s.size(); i++) {
                TokWord word = s.get(i);
                if (word.getTag("CONN").startsWith("B-")) {
                    int j = i + 1;
                    while (j < s.size() && s.get(j).getTag("CONN").startsWith("I-")) {
                        j++;
                    }
                    markBaseConnective(s, i, j - 1);
                }
            }
            for (TokWord word : s) {
                writer.write(word.word() + " " + word.getTag("CONN") + "\n");
            }
            writer.write("\n");
        }
        writer.close();
    }
    
    private void markBaseConnective(Sentence s, int connStart, int connEnd) {
        int bestLength = -1;
        int bestStart = -1;
        int bestEnd = -1;
        for (int i = connStart; i <= connEnd; i++) {
            for (int j = i; j <= connEnd; j++) {
                if ((j-i) > bestLength) {
                    String conn = s.toString(i, j).toLowerCase();
                    if (connAnalyzer.isBaseConnective(conn)) {
                        bestLength = (j - i);
                        bestStart = i;
                        bestEnd = j;
                    }
                }
            }
        }
        for (int i = connStart; i <= connEnd; i++) {
            s.get(i).setTag("CONN", "O");
        }
        if (bestLength == -1) {
            System.err.println("Base conn not found for: " + s.toString(connStart, connEnd));            
        } else {
            s.get(bestStart).setTag("CONN", "B-conn");
            for (int i = bestStart + 1; i <= bestEnd; i++) {
                s.get(i).setTag("CONN", "I-conn");
            }
        }
    }
    public void formatTreeFile(String inputFile, String outputFile) throws IOException {        
        List<String> lines = Util.readLines(inputFile);
        FileWriter writer = new FileWriter(outputFile);
        for (int i = 0; i < lines.size(); i++) {
            if ((i %2) == 1) {
                String line = lines.get(i);
                if (!line.equals("")) {
                    line = "(null" + line.substring(3);
                }
                writer.write(line + "\n");
            }
        }
        writer.close();
    }
    public void createDepFile(String treeFile, String outputFile) throws IOException {
        TreeFactory tf = new LabeledScoredTreeFactory();
        List<Tree> trees = new ArrayList<Tree>();
        BufferedReader reader = new BufferedReader(new FileReader(treeFile));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) trees.add(null);
            else {
                TreeReader tr = new PennTreeReader(new StringReader(line), tf);
                trees.add(tr.readTree());
            }
        }
        reader.close();
        FileWriter writer = new FileWriter(outputFile);
        for (Tree root : trees) {
            if (root == null) {
                writer.write("\n");
                continue;
            }
            GrammaticalStructure gs = null;
            try {
                gs = gsf.newGrammaticalStructure(root);                               
            } catch (RuntimeException ex) {
                writer.write("\n");
                continue;
            }
            Collection<TypedDependency> dependencies = gs.typedDependencies();
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
            writer.flush();
        }
        writer.close();
    }
    
    /**
     * Takes the sentential IOB type of file as input
     * @param iobFile
     * @param treeFile
     * @throws IOException 
     */
    public void createPDTBStyledFiles(String iobFile, String treeFile) throws IOException {
        TreeFactory tf = new LabeledScoredTreeFactory();
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        GenericTextReader gTextReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN","ARG1S","ARG1E","ARG2S","ARG2E"});
        Text text = gTextReader.read(new File(iobFile));
        FileWriter relnWriter = new FileWriter("./resource/ml/data/pdtb/biodrb/biodrb.pipe");
        FileWriter ptbTreeWriter = new FileWriter("./resource/ml/data/pdtb/biodrb/biodrb.mrg");  
        FileWriter depWriter = new FileWriter("./resource/ml/data/pdtb/biodrb/biodrb.dep");
        ptbTreeWriter.write("\n");
        List<String> strTrees = Util.readLines(treeFile);
        int serial = 0;
        
        for (int i = 0; i < text.size(); i++) {
            Sentence s = text.get(i);            
            int connStart = -1;
            int arg1S = -1;
            int arg1E = -1;
            int arg2S = -1;
            int arg2E = -1;
            for (int j = 0; j < s.size(); j++) {
                if (s.get(j).getTag("CONN").equals("1")) {
                    connStart = j;
                }
                if (s.get(j).getTag("ARG1S").equals("1")) {
                    arg1S = j;
                }
                if (s.get(j).getTag("ARG1E").equals("1")) {
                    arg1E = j;
                }
                if (s.get(j).getTag("ARG2S").equals("1")) {
                    arg2S = j;
                }
                if (s.get(j).getTag("ARG2E").equals("1")) {
                    arg2E = j;
                }
            }
            if (connStart< 0 || arg1S < 0 || arg1E < 0 ||arg2S<0 || arg2E < 0) continue;            
            
            int connEnd = connStart + 3;
            if (connEnd >= s.size()) connEnd = s.size() - 1;
            int bestLength = -1;
            int bestStart = -1;
            int bestEnd = -1;
            for (int j = connStart; j <= connEnd; j++) {                
                for (int k = j; k <= connEnd; k++) {
                    if ((k - j) > bestLength) {
                        String conn = s.toString(j, k).toLowerCase();
                        if (connAnalyzer.isBaseConnective(conn)) {
                            bestLength = (k - j);
                            bestStart = j;
                            bestEnd = k;
                        }
                    }
                }
            }
            if (bestLength < 0) continue;
            
            
            TreeReader tr = new PennTreeReader(new StringReader(strTrees.get(i)), tf);
            Tree root = tr.readTree();
            root.setValue(null);
            
            String conn = s.toString(bestStart, bestEnd);
            List<Tree> connGornNodes = treeAnalyzer.findGornNodes(root, bestStart, bestEnd);
            String connGornAddress = treeAnalyzer.getGornAddress(root, connGornNodes, serial);
            List<Tree> arg1GornNodes = treeAnalyzer.findGornNodes(root, arg1S, arg1E);
            String arg1GornAddress = treeAnalyzer.getGornAddress(root, arg1GornNodes, serial);
            List<Tree> arg2GornNodes = treeAnalyzer.findGornNodes(root, arg2S, arg2E);
            String arg2GornAddress = treeAnalyzer.getGornAddress(root, arg2GornNodes, serial);
            
            root.setValue("");
            //prepare relation piped list
            String columns[] = new String[48];
            columns[0] = "Explicit";
            columns[5] = conn;
            columns[4] = connGornAddress;
            columns[8] = conn;
            columns[24] = s.toString(arg1S, arg1E);
            columns[23] = arg1GornAddress;
            columns[34] = s.toString(arg2S, arg2E);
            columns[33] = arg2GornAddress;
            for (int j = 0; j < columns.length; j++) {
                if (j != 0) relnWriter.write("|");
                if (columns[j] != null) relnWriter.write(columns[j]);
            }
            relnWriter.write("\n");
            relnWriter.flush();
            
            String pennOutput = root.pennString();
            ptbTreeWriter.write(pennOutput);
            ptbTreeWriter.flush();
                
            //dep
            GrammaticalStructure gs = null;
            gs = gsf.newGrammaticalStructure(root);            
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
            depWriter.flush();
            
            serial++;
        }
        relnWriter.close();
        ptbTreeWriter.close();
        depWriter.close();
    }
    
    public String getText(SpanList spanList, String str) {
        StringBuilder sb = new StringBuilder();
        for (Span span : spanList) {
            sb.append(str.substring(span.getStart(), span.getEnd()) + " ");
        }
        return sb.toString().trim();
    }
    
    private static class DummySentence {
        int startOffset;
        int endOffset;
        String text;
        Tree root;
        Sentence s;
        public DummySentence(int startOffset, int endOffset, String text) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.text = text;
        }
        
    }
    public void createSentences(String annDirPath, String rawDirPath) throws IOException {
        File annDir = new File(annDirPath);
        File rawDir = new File(rawDirPath);
        File newDir = new File("G:\\UWO\\thesis\\public corpus\\Genia corpus\\BioDRB\\sentences");
        File[] annFiles = annDir.listFiles();
        BioDRBRelationReader relationReader = new BioDRBRelationReader();
        OSentenceBoundaryDetector boundaryDetector = new OSentenceBoundaryDetector();
        
        for (File annFile : annFiles) {
            List<BioDRBRelation> relations = relationReader.read(annFile);
            File rawFile = new File(rawDir, annFile.getName());
            String content = Util.readContent(rawFile, true);
            List<DummySentence> dummySentences = new ArrayList<DummySentence>();
            
            List<String> rawLines = Util.readLines(rawFile);
            int offset = 0;
            for (String rawLine : rawLines) {
                if (rawLine.equals("")) {
                    offset++;
                    continue;
                }
                opennlp.tools.util.Span[] spans = boundaryDetector.getSpans(rawLine);
                for (opennlp.tools.util.Span span : spans) {
                    dummySentences.add(new DummySentence(offset + span.getStart(), offset + span.getEnd(), rawLine.substring(span.getStart(), span.getEnd())));                    
                }
                offset += rawLine.length() + 1;
            }
            File newFile = new File(newDir, annFile.getName());
            FileWriter writer = new FileWriter(newFile);
            for (DummySentence dummySentence : dummySentences) {
                writer.write(dummySentence.startOffset + "\n");
                writer.write(dummySentence.endOffset + "\n");
                writer.write(dummySentence.text + "\n");
            }
            writer.close();
            
            for (BioDRBRelation relation : relations) {
                if (relation.getType().equals("Explicit")) {
                    String conn = getText(relation.getConnSpanList(), content);                    
                    
                }
            }
        }
    }
    
    List<DummySentence> readDummySentences(File sentenceFile, File parseFile) {
        List<DummySentence> dummySentences = new ArrayList<DummySentence>();
        PTBFileReader reader = new PTBFileReader();
        List<Tree> trees = reader.read(parseFile);
        List<String> lines = Util.readLines(sentenceFile);
        for (int i = 0; i < trees.size(); i++) {
            DummySentence ds = new DummySentence(Integer.parseInt(lines.get(i*3)), Integer.parseInt(lines.get(i*3 + 1)), lines.get(i*3 + 2));
            ds.root = trees.get(i);
            ds.s = new Sentence(ds.root);
            dummySentences.add(ds);            
        }
        return dummySentences;
    }
    static class Range {
        int start;
        int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
    }
    Range findDummySentencesInSpan(List<DummySentence> dummySentences, Span span) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < dummySentences.size(); i++) {
            DummySentence ds = dummySentences.get(i);
            if (start == -1) {
                if (span.getStart() >= ds.startOffset && span.getStart() <= ds.endOffset) {
                    start = i;
                }
            }
            if (span.getEnd() >= ds.startOffset && span.getEnd() <= ds.endOffset) {
                end = i;
                break;
            }
        }
        return new Range(start, end);
    }
    String getPrefix(String s, int pos) {
        if (pos == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pos; i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    int findPrefix(Tree root, String prefix, boolean extend) {
       if (prefix.equals("")) return -1;
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        int result = -2;
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            if (!leaf.value().matches("-LRB-|-RRB-|-LSB-|-RSB-")) {
                if (leaf.value().contains("-LSB-")) {
                    String modifiedValue = leaf.value().replace("-LSB-", "");
                    sb.append(getPrefix(modifiedValue, modifiedValue.length()));
                } else {
                    sb.append(getPrefix(leaf.value(), leaf.value().length()));
                }
            } 
            if (sb.toString().equals(prefix)) {
                if (!extend) return i;
                result = i;                
            } else if (sb.toString().length() > prefix.length()) {
                break;
            }
        }
        return result;
    }
    String getGornAddress(DummySentence ds, int beginIndex, int endIndex, int lineNumber) {
        String text = ds.text;
        Tree root = ds.root;
        String prefix1 = getPrefix(text, beginIndex);
        String prefix2 = getPrefix(text, endIndex);
        int leftPos = findPrefix(root, prefix1, true);
        int rightPos = findPrefix(root, prefix2, false);
        if (leftPos < -1) {
            System.out.println("Prefix1 not found!");
            System.out.println("Prefix1: " + prefix1);
            Sentence s = new Sentence(root);
            System.out.println(s.toString());
            return null;
        } else if (rightPos < -1) {
            System.out.println("Prefix2 not found!");
            System.out.println("Prefix2: " + prefix2);
            Sentence s = new Sentence(root);
            System.out.println(s.toString());
            return null;
        } else {
            List<Tree> gornNodes = TREE_ANALYZER.findGornNodes(root, leftPos + 1, rightPos);            
            if (gornNodes != null) {
                for (Tree gornNode : gornNodes) {
                    for (Tree leaf : gornNode.getLeaves()) {
                        System.out.print(leaf.value() + " ");
                    }
                }
                System.out.println("");
                String gornAddress = TREE_ANALYZER.getGornAddress(root, gornNodes, lineNumber);
                return gornAddress;
            } else {
                System.out.println("GornNodes not found!");
                return null;
            }
        }
    }
    
    String getGornAddress(List<DummySentence> dummySentences, Span span) {
        Range range = findDummySentencesInSpan(dummySentences, span);
        if (range.start < 0 || range.end < 0) {
            System.out.println("Illegal range");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = range.start; i <= range.end; i++) {
            DummySentence ds = dummySentences.get(i);
            int beginIndex = Math.max(span.getStart() - ds.startOffset, 0);            
            int endIndex = Math.min(span.getEnd()-ds.startOffset, ds.endOffset-ds.startOffset);
            String gornAddress = getGornAddress(ds, beginIndex, endIndex, i);
            if (gornAddress == null) continue;
            if (sb.length() == 0) {
                sb.append(gornAddress);
            } else {
                sb.append(";" + gornAddress);
            }
        }
        return sb.toString();
    }
    String getGornAddressList(List<DummySentence> dummySentences, SpanList spanList) {
        StringBuilder sb = new StringBuilder();
        for (Span span : spanList) {
            String gornAddress = getGornAddress(dummySentences, span);
            if (gornAddress != null) {
                if (sb.length() == 0) {
                    sb.append(gornAddress);
                } else {
                    sb.append(";" + gornAddress);
                }
            }
        }
        return sb.toString();
    }
    private void process(List<Tree> ptbTrees, File depFile) {
        System.out.println("Processing " + depFile.getAbsolutePath());
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(depFile));
            for (Tree root : ptbTrees) {
                GrammaticalStructure gs = gsf.newGrammaticalStructure(root);
                //Collection<TypedDependency> dependencies = gs.typedDependenciesCollapsed();
                //had to change this because and, or were being ingnored
                Collection<TypedDependency> dependencies = gs.typedDependencies();
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
        } catch (IOException ex) {
            Logger.getLogger(BioDRB2PDTBStyle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void createDepFiles(String biodrbRoot) throws IOException {
        File biodrbRootDir = new File(biodrbRoot);        
        File parseDir = new File(biodrbRootDir, "parses");
        File depDir = new File(biodrbRootDir, "dep");
        depDir.mkdir();
        File[] parseFiles = parseDir.listFiles();
        PTBFileReader reader = new PTBFileReader();
        for (File parseFile : parseFiles) {
            List<Tree> trees = reader.read(parseFile);
            File depFile = new File(depDir, parseFile.getName().replace(".mrg", ".dep"));
            process(trees, depFile);
        }
    }
    public void totalConversion(String biodrbRoot) throws IOException {
        File biodrbRootDir = new File(biodrbRoot);
        File sentenceDir = new File(biodrbRootDir, "sentences");
        File parseDir = new File(biodrbRootDir, "parses");
        File annDir = new File(biodrbRootDir, "GeniaAnn");
        File rawDir = new File(biodrbRootDir, "GeniaRaw");
        File newAnnDir = new File(biodrbRootDir, "newAnnTmp");
        newAnnDir.mkdir();
        File[] annFiles = annDir.listFiles();
        
        BioDRBRelationReader relationReader = new BioDRBRelationReader();
        Set<String> connSet = new HashSet<String>();
        FileWriter iobWriter = new FileWriter("biodrb_sense.iob");
        FileWriter iobTreeWriter = new FileWriter("biodrb_sense.tree");
        for (File annFile : annFiles) {
            File rawFile = new File(rawDir, annFile.getName());
            File sentenceFile = new File(sentenceDir, annFile.getName());
            File parseFile = new File(parseDir, annFile.getName().replace(".txt", ".mrg"));
            File newAnnFile = new File(newAnnDir, annFile.getName().replace(".txt", ".pipe"));
            FileWriter newAnnWriter = new FileWriter(newAnnFile);
            
            String content = Util.readContent(rawFile, true);
            List<DummySentence> dummySentences = readDummySentences(sentenceFile, parseFile);
            List<BioDRBRelation> relations = relationReader.read(annFile);
            
            
            for (BioDRBRelation relation : relations) {
                if (relation.getType().equals("Explicit")) {
                    SpanList connSpanList = relation.getConnSpanList();
                    String connGornAddressList = getGornAddressList(dummySentences, connSpanList);                    
                    String connString = getText(connSpanList, content).replace("\n", "");
                    relation.setConnRawText(connString);
                    relation.setConnectiveGornAddress(connGornAddressList);
                    //System.out.println("Connective: " + connString);
                    //System.out.println("GornAddress: " + connGornAddressList);
                    
                    connSet.add(connString.toLowerCase());
                    
                    //prepare IOB file for sense
                    for (Span span : connSpanList) {
                        Range range = findDummySentencesInSpan(dummySentences, span);
                        int index = range.start;
                        if (range.start != range.end) {
                            System.out.println("WTF");
                        }
                        String gornAddress = getGornAddress(dummySentences, span);
                        Tree root = dummySentences.get(index).root;                        
                        List<Tree> nodes = TREE_ANALYZER.getGornNodes(root, gornAddress);
                        boolean first = true;
                        for (Tree t : nodes) {
                            List<Tree> leaves = t.getLeaves();
                            for (Tree leaf : leaves) {
                                int pos = TREE_ANALYZER.getLeafPosition(root, leaf);
                                if (first) {
                                    dummySentences.get(index).s.get(pos).setTag("CONN", "B-conn");
                                    dummySentences.get(index).s.get(pos).setTag("SENSE", relation.getSense());
                                    first = false;
                                } else {
                                    dummySentences.get(index).s.get(pos).setTag("CONN", "I-conn");
                                    dummySentences.get(index).s.get(pos).setTag("SENSE", relation.getSense());
                                }
                            }
                        }
                    }
                    //end of prepare IOB
                    
                    SpanList arg1SpanList = relation.getArg1SpanList();
                    String arg1GornAddressList = getGornAddressList(dummySentences, arg1SpanList);
                    String arg1RawText = getText(arg1SpanList, content).replace("\n", "");
                    relation.setArg1RawText(arg1RawText);
                    relation.setArg1GornAddress(arg1GornAddressList);
                    //System.out.println("Arg1: " + arg1RawText);
                    //System.out.println("GornAddress: " + arg1GornAddressList);
                    
                    SpanList arg2SpanList = relation.getArg2SpanList();
                    String arg2GornAddressList = getGornAddressList(dummySentences, arg2SpanList);
                    String arg2RawText = getText(arg2SpanList, content).replace("\n", "");
                    relation.setArg2RawText(arg2RawText);
                    relation.setArg2GornAddress(arg2GornAddressList);
                    //System.out.println("Arg2: " + arg2RawText);
                    //System.out.println("GornAddress: " + arg2GornAddressList);
                    PDTBRelation pdtbRelation = relation.toPDTBRelation();
                    newAnnWriter.write(pdtbRelation.toString() + "\n");
                }
            }            
            newAnnWriter.close();
            writeIOBSentences(dummySentences, iobWriter, iobTreeWriter);
        }
        iobWriter.close();
        iobTreeWriter.close();
        File connListFile = new File(biodrbRootDir, "connectiveList.txt");
        FileWriter connListWriter = new FileWriter(connListFile);
        for (String connective : connSet) {
            connListWriter.write(connective + "\n");
        }
        connListWriter.close();
    }
    
    void writeIOBSentences(List<DummySentence> dummySentences, FileWriter iobWriter, FileWriter iobTreeWriter) throws IOException {
        for (DummySentence ds : dummySentences) {
            Sentence s = ds.s;
            for (TokWord word : s) {
                iobWriter.write(word.word() + " ");
                if (word.getTag("CONN") == null) {
                    iobWriter.write("O ");
                } else {
                    iobWriter.write(word.getTag("CONN") + " ");
                }
                if (word.getTag("SENSE") == null) {
                    iobWriter.write("O\n");
                } else {
                    iobWriter.write(word.getTag("SENSE") + "\n");
                }
            }
            iobWriter.write("\n");
            ds.root.setValue("");
            iobTreeWriter.write(TREE_ANALYZER.getPennOutput(ds.root) + "\n");
        }
    }
    public void createConnTypeList(String connListPath, String pdtbConnTypeListPath, String knottConnTypeListPath, String newListPath, String annRoot) throws IOException {
        File annDir = new File(annRoot);
        File[] files = annDir.listFiles();        
        PDTBPipedFileReader reader = new PDTBPipedFileReader();
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (File file : files) {
            System.out.println(file.getName());
            List<PDTBRelation> relations = reader.read(file);
            for (PDTBRelation relation : relations) {
                if (relation.getType().equals("Explicit")) {
                    //System.out.println(relation.getConnRawText().toLowerCase());
                    if (map.containsKey(relation.getConnRawText().toLowerCase())) {
                        map.put(relation.getConnRawText().toLowerCase(), map.get(relation.getConnRawText().toLowerCase()) + 1);
                    } else {                        
                        map.put(relation.getConnRawText().toLowerCase(), 1);
                    }
                }
            }
        }
        List<String> connectives = Util.readLines(connListPath);
        List<String> pdtbConnTypeList = Util.readLines(pdtbConnTypeListPath);
        List<String> knottConnTypeList = Util.readLines(knottConnTypeListPath);
        Map<String, String> pdtbConnTypeMap = new HashMap<String, String>();
        Map<String, String> knottConnTypeMap = new HashMap<String, String>();
        
        for (String connType : pdtbConnTypeList) {
            String tokens[] = connType.split("\t");
            pdtbConnTypeMap.put(tokens[0], tokens[1]);
        }
        for (String connType : knottConnTypeList) {
            String[] tokens = connType.split(" ::");
            knottConnTypeMap.put(tokens[0], tokens[1]);
        }
        
        List<String> types = new ArrayList<String>();
        List<String> sources = new ArrayList<String>();
        
        for (String connective : connectives) {
            String type = "**";
            String source = "**";
            for (String pdtbConn : pdtbConnTypeMap.keySet()) {
                if (connective.endsWith(pdtbConn)) {
                    type = pdtbConnTypeMap.get(pdtbConn);
                    source = "PDTB";
                    break;
                }
            }
            if (type.equals("**")) {
                for (String knottConn : knottConnTypeMap.keySet()) {
                    if (connective.endsWith(knottConn)) {
                        type = knottConnTypeMap.get(knottConn);
                        source = "KNOTT";
                        break;
                    }
                }
            }
            types.add(type);
            sources.add(source);
        }
        FileWriter writer = new FileWriter(newListPath);
        
        for (int i = 0; i < connectives.size(); i++) {
            //writer.write(connectives.get(i) + "\t" + types.get(i) + "\t" + sources.get(i) +"\n");
            //writer.write(connectives.get(i) + "\t" + types.get(i) + "\n");
            writer.write(connectives.get(i) + "\t" + types.get(i) + "\t" + map.get(connectives.get(i)) + "\n");
            if (types.get(i).equals("**")) {
                System.out.println(connectives.get(i) + "\t" + types.get(i) + "\t" + map.get(connectives.get(i)));
            }
        }        
        writer.close();
    }
    
    public void writeConnHeadList(String connListPath, String knottConnTypeListPath, String outputPath) throws IOException {
        List<String> connectives = Util.readLines(connListPath);
        List<String> lines = Util.readLines(knottConnTypeListPath);
        List<String> knottConnectives = new ArrayList<String>();
        for (String line : lines) {
            String[] tokens = line.split(" ::");
            knottConnectives.add(tokens[0]);
        }
        FileWriter writer = new FileWriter(outputPath);
        for (String connective : connectives) {
            String head = connective;
            for (String knottConnective : knottConnectives) {
                if (connective.endsWith(knottConnective)) {
                    head = knottConnective;
                    break;
                }
            }
            writer.write(connective + "\t" + head + "\n");
        }
        writer.close();
    }
    
    /**
     * Create new Annotation will corrected heads
     * @param oldRootPath
     * @param newRootPath 
     */
    public void createNewAnnotationWithHeads(String connHeadListPath, String oldRootPath, String newRootPath) throws IOException {
        List<String> lines = Util.readLines(connHeadListPath);
        Map<String, String> conn2HeadMap = new HashMap<String, String>();
        for (String line : lines) {
            String[] tokens = line.split("\t");
            conn2HeadMap.put(tokens[0], tokens[1]);
            System.out.println(tokens[0]+"-"+tokens[1]);
        }
        File oldRoot = new File(oldRootPath);
        File newRoot = new File(newRootPath);
        newRoot.mkdir();
        File[] files = oldRoot.listFiles();
        PDTBPipedFileReader reader = new PDTBPipedFileReader();
        for (File oldFile : files) {
            File newFile = new File(newRoot, oldFile.getName());
            FileWriter writer = new FileWriter(newFile);
            List<PDTBRelation> relations = reader.read(oldFile);
            for (PDTBRelation relation : relations) {
                String head = conn2HeadMap.get(relation.getConnRawText().toLowerCase());
                if (head != null) {
                    relation.setConnHead(head);
                } else {
                    System.out.println("Not found! " + relation.getConnRawText().toLowerCase());
                }
                writer.write(relation.toString() + "\n");
            }
            writer.close();
        }
    }
    public static void main(String args[]) throws IOException {
        BioDRB2PDTBStyle converter = new BioDRB2PDTBStyle();
        //converter.createConnIOBFile("./resource/ml/data/discourse/biodrb_conn_2.txt", "./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn");
        //converter.formatTreeFile("./resource/ml/data/discourse/biodrb_conn_2_parsed.txt", "./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree");
        //converter.createDepFile("./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_tree", "./resource/ml/data/pdtb/conn_id/biodrb/biodrb_conn_dep");
        //converter.createPDTBStyledFiles("./resource/ml/data/discourse/SentConnArg12_final.txt", "./resource/ml/data/discourse/SentConnArg12_final_parsed.txt");
        //converter.createSentences("G:\\UWO\\thesis\\public corpus\\Genia corpus\\BioDRB\\GeniaAnn\\Genia", "G:\\UWO\\thesis\\public corpus\\Genia corpus\\BioDRB\\GeniaRaw\\Genia");
        
        converter.totalConversion("./resource/ml/data/biodrb");
        //converter.createDepFiles("./resource/ml/data/biodrb");
        //converter.createConnTypeList("./resource/ml/data/biodrb/connectiveList.txt", "./resource/ml/data/pdtb/explicit_conn_types_category", "./resource/ml/data/biodrb/knott_conn_types.txt", "./resource/ml/data/biodrb/connective_types2", "./resource/ml/data/biodrb/newAnn");
        
        //converter.writeConnHeadList("./resource/ml/data/biodrb/connectiveList.txt", "./resource/ml/data/biodrb/knott_conn_types.txt","./resource/ml/data/biodrb/connHeadList.txt");
        //converter.createNewAnnotationWithHeads("./resource/ml/data/biodrb/connHeadList.txt", "./resource/ml/data/biodrb/newAnn", "./resource/ml/data/biodrb/newAnn2");
    }
}
