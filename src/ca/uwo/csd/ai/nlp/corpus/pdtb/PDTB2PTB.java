/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.utils.BLLIPClient;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PDTB2PTB {
    String rawRoot;
    String annRoot;
    static BLLIPClient bLLIPClient = new BLLIPClient();
    static TreeFactory treeFactory = new LabeledScoredTreeFactory();
    
    public PDTB2PTB(String rawRoot, String annRoot) {
        this.rawRoot = rawRoot;
        this.annRoot = annRoot;
        if (!new File(rawRoot).isDirectory() || !new File(annRoot).isDirectory()) {
            throw new IllegalArgumentException("Both rawRoot and annRoot should be directories.");
        }
    }
    
    public void generatePTB(String ptbRoot) {
        File rawDir = new File(rawRoot);        
        File ptbDir = new File(ptbRoot);
        if (!ptbDir.exists()) {
            ptbDir.mkdirs();
        }
        File[] dirs = rawDir.listFiles(new FileFilter() {
                               public boolean accept(File file) {
                                   return file.isDirectory();
                               }
                           }); 
        HashSet<String> doneList = new HashSet<String>();
        doneList.add("05");doneList.add("09");doneList.add("16");doneList.add("06");doneList.add("02");
        for (File dir : dirs) {
            if (doneList.contains(dir.getName())) continue;
            System.out.println("Current directory: " + dir.getAbsolutePath());
            File ptbSubDir = new File(ptbDir, dir.getName());
            if (!ptbSubDir.exists()) ptbSubDir.mkdirs();
            File[] files = dir.listFiles();
            for (File file : files) {
                System.out.println("\tCurrent file: " + file.getAbsolutePath());
                File ptbFile = new File(ptbSubDir,file.getName() + ".mrg");
                try {
                    processFile(file, ptbFile);
                } catch (IOException ex) {
                    System.out.println("Error at: file="+file.getAbsolutePath());
                    Logger.getLogger(PDTB2PTB.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RuntimeException ex) {
                    File errorLog = new File(ptbSubDir, "errorlog");
                    try {
                        FileWriter fileWriter = new FileWriter(errorLog, true);
                        fileWriter.write("Could not parse file: " + file.getAbsolutePath()+"\n");
                        fileWriter.write(ex.getMessage()+"\n");
                        fileWriter.close();
                        System.err.println(ex.getMessage());
                    } catch (IOException ex1) {
                        Logger.getLogger(PDTB2PTB.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
        }
        
    }
    private void processFile(File rawFile, File mrgFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(rawFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(mrgFile));
        
        String line;
        line = reader.readLine();   //.START
        while ((line = reader.readLine()) != null) {
            if (line.equals("")) continue;
            String parsed = null;
            try {
                parsed = bLLIPClient.parse(line);
            } catch (RuntimeException ex) {
                writer.close();
                reader.close();
                throw ex;
            }
            TreeReader tr = new PennTreeReader(new StringReader(parsed), treeFactory);
            Tree t = tr.readTree();
            t.setValue("");
            String pennString = t.pennString();
            writer.write("\n"+pennString.substring(0, pennString.length()-1));            
        }
        writer.close();
        reader.close();
    }
    
    public static void main(String args[]) {
        PDTB2PTB pdtb = new PDTB2PTB("/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/RAW/WSJ", "/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/data");
        pdtb.generatePTB("/home/mibnfaiz/UWO/thesis/corpus/pdtb_v2/ptb");
    }
}
