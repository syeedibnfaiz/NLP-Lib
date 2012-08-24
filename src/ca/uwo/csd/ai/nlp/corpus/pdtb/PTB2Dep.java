/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates Dependency Graph from PTB trees
 * @author Syeed Ibn Faiz
 */
public class PTB2Dep {
    private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    private static final GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    String ptbRoot;
    String depRoot;

    public PTB2Dep(String ptbRoot, String depRoot) {
        this.ptbRoot = ptbRoot;
        this.depRoot = depRoot;
        if (!new File(depRoot).isDirectory() || !new File(ptbRoot).isDirectory()) {
            throw new IllegalArgumentException("depRoot and ptbRoot should be existing directories.");
        }
    }
    
    public void createDepFiles() {
        File depRootDir = new File(depRoot);
        File ptbRootDir = new File(ptbRoot);
        File[] ptbSectionDirs = ptbRootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        PTBFileReader ptbFileReader = new PTBFileReader();
        
        for (File ptbSectionDir : ptbSectionDirs) {
            File[] ptbFiles = ptbSectionDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String string) {
                    return string.endsWith(".mrg");
                }
            });
            File depSectionDir = new File(depRootDir, ptbSectionDir.getName());
            depSectionDir.mkdir();
            
            for (File ptbFile : ptbFiles) {
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                File depFile = new File(depSectionDir, ptbFile.getName().replace(".mrg", ".dep"));
                process(ptbTrees, depFile);
            }
        }
        
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
            Logger.getLogger(PTB2Dep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String args[]) {
        //PTB2Dep ptb2Dep = new PTB2Dep("./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep");        
        
        //auto
        PTB2Dep ptb2Dep = new PTB2Dep("./package/treebank_3/parsed/mrg/psptb", "./package/treebank_3/parsed/mrg/psdep");        
        ptb2Dep.createDepFiles();
    }
}
