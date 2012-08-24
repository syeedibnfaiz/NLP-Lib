/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;



/**
 * Generate raw text files from GS PTB files
 * @author tonatuni
 */
public class GSPTB2Raw {
    String ptbRoot;
    String rawRoot;
    PTBFileReader reader;
    
    public GSPTB2Raw(String ptbRoot, String rawRoot) {
        this.ptbRoot = ptbRoot;
        this.rawRoot = rawRoot;
        reader = new PTBFileReader();
    }
    
    public void generate() throws IOException {
        File ptbRootDir = new File(ptbRoot);
        File rawRootDir = new File(rawRoot);
        File[] ptbSections = ptbRootDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        for (File ptbSectionDir : ptbSections) {
            File rawSectionDir = new File(rawRootDir, ptbSectionDir.getName());
            rawSectionDir.mkdir();
            File[] files = ptbSectionDir.listFiles();
            
            for (File ptbFile : files) {
                File rawFile = new File(rawSectionDir, ptbFile.getName().replace(".mrg", ""));
                write(ptbFile, rawFile);
            }
        }
    }
    private void write(File ptbFile, File rawFile) throws IOException {
        FileWriter writer = new FileWriter(rawFile);
        List<Tree> trees = reader.read(ptbFile);
        for (Tree root : trees) {
            List<Tree> leaves = root.getLeaves();
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            for (Tree leaf : leaves) {
                Tree parent = leaf.parent(root);
                if (!parent.value().equals("-NONE-")) {
                    if (first == true) {
                        first = false;
                    } else {
                        sb.append(" ");
                    }
                    sb.append(leaf.value());
                }
            }
            writer.write(sb.toString() + "\n");
        }
        writer.close();
    }
    
    public static void main(String args[]) throws IOException {
        GSPTB2Raw conv = new GSPTB2Raw("./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/my_raw");
        conv.generate();
    }
}
