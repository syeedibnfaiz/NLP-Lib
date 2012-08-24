/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates new Annotation
 * @author Syeed Ibn Faiz
 */
public class PTBGS2PSPDTB {
    String pipedAnnRoot;    
    String ptbRoot;         //gold PTB root
    String psptbRoot;       //auto/pseudo TB root
    String newAnnRoot;
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public PTBGS2PSPDTB(String pipedAnnRoot, String ptbRoot, String psptbRoot, String newAnnRoot) {
        this.pipedAnnRoot = pipedAnnRoot;
        this.ptbRoot = ptbRoot;
        this.psptbRoot = psptbRoot;
        this.newAnnRoot = newAnnRoot;
    }
    
    public void createNewAnnotations() {
        
        File pipedAnnRootDir = new File(pipedAnnRoot);        
        File ptbRootDir = new File(ptbRoot);
        File autoPTBRootDir = new File(psptbRoot);
        File newAnnrootDir = new File(newAnnRoot);
        newAnnrootDir.mkdir();
        File[] annSectionDirs = pipedAnnRootDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();        
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        for (File annSectionDir : annSectionDirs) {                                
            File ptbSectionDir = new File(ptbRootDir, annSectionDir.getName());
            File autoPTBSectionDir = new File(autoPTBRootDir, annSectionDir.getName());
            File newAnnSectionDir = new File(newAnnrootDir, annSectionDir.getName());
            newAnnSectionDir.mkdir();
            
            File[] annFiles = annSectionDir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File file, String string) {
                    return string.endsWith(".pipe");
                }
            });
            for (File annFile : annFiles) {
                //System.out.println("Reading: " + annFile.getAbsolutePath());                
                File ptbFile = new File(ptbSectionDir, annFile.getName().replace(".pipe", ".mrg"));
                File autoPTBFile = new File(autoPTBSectionDir, annFile.getName().replace(".pipe", ".mrg"));
                File newAnnFile = new File(newAnnSectionDir, annFile.getName());
                
                List<PDTBRelation> relations = pipedFileReader.read(annFile);
                //printStat(relations);                
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                List<Tree> autoTrees = ptbFileReader.read(autoPTBFile);
                
                process(relations, ptbTrees, autoTrees, newAnnFile);
            }
        }                
    }
    
    private void process(List<PDTBRelation> relations, List<Tree> ptbTrees, List<Tree> autoTrees, File newAnnFile) {        
        try {
            BufferedWriter annWriter = new BufferedWriter(new FileWriter(newAnnFile));
            for (PDTBRelation relation : relations) {
                //manage conn
                if (relation.getType().equals("Explicit") && !relation.getConnRawText().isEmpty()) {                    
                    String connectiveGornAddress = relation.getConnectiveGornAddress();
                    relation.setConnectiveGornAddress(getGornAddress(relation, connectiveGornAddress, ptbTrees, autoTrees));
                                        
                    String arg1GornAddress = relation.getArg1GornAddress();
                    relation.setArg1GornAddress(getGornAddress(relation, arg1GornAddress, ptbTrees, autoTrees));
                                        
                    String arg2GornAddress = relation.getArg2GornAddress();
                    relation.setArg2GornAddress(getGornAddress(relation, arg2GornAddress, ptbTrees, autoTrees));
                }
                annWriter.write(relation.toString() + "\n");
            }
            annWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(PTBGS2PSPDTB.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    String getGornAddress(PDTBRelation relation, String gornAddress, List<Tree> ptbTrees, List<Tree> autoTrees) {
        GornAddressList gornAddressList = new GornAddressList(gornAddress);
        StringBuilder autoGornAddress = new StringBuilder();
        for (GornAddress gAddress : gornAddressList) {
            int index = gAddress.getLineNumber();            
            String address = getGornAddress(relation, ptbTrees.get(index), autoTrees.get(index), gAddress.getAddress(), index);
            
            if (address != null) {
                if (autoGornAddress.length() > 0) {
                    autoGornAddress.append(";");
                }
                autoGornAddress.append(address);
            }
        }
        return autoGornAddress.toString();
    }
    
    String getGornAddress(PDTBRelation relation, Tree gold, Tree auto, String gornAddress, int lineNumber) {
        List<Tree> goldLeaves = gold.getLeaves();
        List<Tree> autoLeaves = auto.getLeaves();
        
        Tree gornNode = TREE_ANALYZER.getGornNode(gold, gornAddress);
        List<Tree> gornLeaves = gornNode.getLeaves();
        
        Tree firstLeaf = gornLeaves.get(0);
        Tree lastLeaf = gornLeaves.get(gornLeaves.size() - 1);
        
        int firstGoldPos = TREE_ANALYZER.getLeafPosition(gold, firstLeaf);
        int lastGoldPos = TREE_ANALYZER.getLeafPosition(gold, lastLeaf);
        
        /*System.out.println("First oglden pos:" + firstGoldPos);
        System.out.println(new Sentence(gold));
        System.out.println("FirstgoldLeaf: " + firstLeaf.value());
        
        System.out.println("last gold pos: " + lastGoldPos);
        System.out.println(new Sentence(gold));
        System.out.println("LastgoldLeaf: " + lastLeaf.value());*/

        int firstAutoPos = firstGoldPos;
        int lastAutoPos = lastGoldPos;
        
        int noneCount = 0;
        for (int i = 0; i < goldLeaves.size(); i++) {
            Tree leaf = goldLeaves.get(i);
            Tree parent = leaf.parent(gold);
            if (parent.value().equals("-NONE-")) {
                noneCount++;
            }
            if (i == firstGoldPos) {
                firstAutoPos -= noneCount;
                if (parent.value().equals("-NONE-")) {
                    firstAutoPos++;                    
                }
                noneCount = 0;  //reinitialize to count none nodes following firstpos and rpeceding the potential lastpos
            }
            if (i == lastGoldPos) {
                //lastAutoPos -= noneCount;
                //update lastpos relative to first pos
                lastAutoPos = firstAutoPos + (lastGoldPos-firstGoldPos) - noneCount;
                break;
            }
        }
        if (lastAutoPos == autoLeaves.size()) {
            lastAutoPos = autoLeaves.size() - 1;
        }
        //just check
        if (firstAutoPos < 0 || firstAutoPos >= autoLeaves.size()) {
            String part = new Sentence(gornNode).toString();
            System.out.println("First pos out of range: " + part);
            return null;
        }
        if (lastAutoPos < 0 || lastAutoPos >= autoLeaves.size()) {
            String part = new Sentence(gornNode).toString();
            System.out.println("Last pos out of range: " + part);
            return null;
        }
        boolean match = check(gold, auto, firstGoldPos, lastGoldPos, firstAutoPos, lastAutoPos);
        if (!match) {
            System.out.println("Missmatch!");
        }
        
        List<Tree> autoGornNodes = TREE_ANALYZER.findGornNodes(auto, firstAutoPos, lastAutoPos);
        if (autoGornNodes == null || autoGornNodes.size() == 0) {
            System.out.println("Empty gorn nodes!");
            System.out.println("first: " + firstAutoPos);
            System.out.println("last: " + lastAutoPos);
        }
        String result = TREE_ANALYZER.getGornAddress(auto, autoGornNodes, lineNumber);        
        if (result.length() == 0) {
            System.out.println("Empty result!");
        }
        return result;
    }
    
    boolean check(Tree gold, Tree auto, int firstGoldPos, int lastGoldPos, int firstAutoPos, int lastAutoPos) {
        List<Tree> goldLeaves = gold.getLeaves();
        List<Tree> autoLeaves = auto.getLeaves();
        
        StringBuilder goldSb = new StringBuilder();
        StringBuilder autoSb = new StringBuilder();
        
        for (int i = 0; i < goldLeaves.size(); i++) {
            Tree leaf = goldLeaves.get(i);
            Tree parent = leaf.parent(gold);
            if (!parent.value().equals("-NONE-")) {
                goldSb.append(leaf.value() + " ");
            }
        }
        for (int i = 0; i < autoLeaves.size(); i++) {
            Tree leaf = autoLeaves.get(i);
            Tree parent = leaf.parent(auto);
            if (!parent.value().equals("-NONE-")) {
                autoSb.append(leaf.value() + " ");
            }
        }
        /*System.out.println(goldSb.toString());
        System.out.println(autoSb.toString());*/
        if (goldSb.toString().equals(autoSb.toString())) return true;
        return false;
    }
    
    public static void main(String args[]) {
        PTBGS2PSPDTB converter = new PTBGS2PSPDTB("./pdtb_v2/piped_data","./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/psptb", "./package/treebank_3/parsed/mrg/pspdtb");
        converter.createNewAnnotations();
    }
}
