/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.utils.SocketUtil;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class CorpusFixer {

    private final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();

    public static void main(String[] args) throws IOException {
        CorpusFixer fixer = new CorpusFixer();
        fixer.fixCCNPs("./resource/relation/PPI6/LLL");
        fixer.fixCCNPs("./resource/relation/PPI6/HPRD50");
        fixer.fixCCNPs("./resource/relation/PPI6/IEPA");
        //fixer.fixCCNPs("./resource/relation/PPI6/AIMed");
        //fixer.fixCCNPs("./resource/relation/PPI6/BioInfer");
    }

    public void fixCCNPs(String corpusRoot) throws IOException {
        File treeDir = new File(corpusRoot, "trees");
        File depsDir = new File(corpusRoot, "depsCC");
        File[] treeFiles = treeDir.listFiles(new FileExtensionFilter("mrg"));
        SocketUtil socketUtil = new SocketUtil("localhost", 8662);
        for (File treeFile : treeFiles) {
            File depFile = new File(depsDir, treeFile.getName().replace("mrg", "dep"));            
            List<String> strTrees = Util.readLines(treeFile);
            List<String> strDeps = Util.readLines(depFile);
            boolean changed = false;
            for (int i = 0; i < strTrees.size(); i++) {
                String strTree = strTrees.get(i);
                String strDep = strDeps.get(i);
                Tree tree = treeAnalyzer.getPennTree(strTree);                
                fixTree(tree);
                String newStrTree = treeAnalyzer.getPennOutput(tree).trim();
                if (!strTree.equals(newStrTree)) {
                    socketUtil.sendLine(newStrTree + "\n");
                    String newStrDep = socketUtil.readline().trim();
                    if (!strDep.equals(newStrDep)) {
                        strTrees.set(i, newStrTree);
                        strDeps.set(i, newStrDep);
                        changed = true;
                        System.out.println("old: " + strDep);
                        System.out.println("new: " + newStrDep);
                        System.out.println("");
                    }
                }
            }
            if (changed) {
                writeListToFile(strTrees, treeFile);
                writeListToFile(strDeps, depFile);
            }
        }
        socketUtil.closeConnection();
    }

    private void writeListToFile(List<String> list, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String line : list) {
            writer.write(line + "\n");
        }
        writer.close();
    }
    
    private void fixTree(Tree t) {
        if (t.isPhrasal()) {
            if (t.value().equals("NP")) {
                boolean cc = false;
                boolean preTerminal = true;
                List<Tree> children = t.getChildrenAsList();
                for (Tree child : children) {
                    if (child.value().equals("CC")) {
                        cc = true;
                    } else if ((!child.isPrePreTerminal() && !child.isPreTerminal()) || child.getChildrenAsList().size() > 1) {
                        preTerminal = false;
                        break;
                    }
                }
                if (cc && preTerminal) {
                    for (int i = 0; i < children.size(); i++) {
                        Tree child = children.get(i);
                        if (child.isPrePreTerminal() && child.value().equals("NP")) {
                            t.setChild(i, child.getChild(0));
                        }
                    }                    
                }
            }            
            List<Tree> children = t.getChildrenAsList();
            for (Tree child : children) {
                fixTree(child);
            }            
        }        
    }
}
