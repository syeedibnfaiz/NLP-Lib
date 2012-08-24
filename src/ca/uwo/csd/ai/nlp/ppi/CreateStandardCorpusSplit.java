/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Creates standard corpus split by inspecting the ppi-kernel package 
 * learning format CV data
 * @author Syeed Ibn Faiz
 */
public class CreateStandardCorpusSplit {
    public static void createSplit(String corpusDirPath, String outputDirPath) throws IOException {
        File corpusDir = new File(corpusDirPath);
        File outputDir = new File(outputDirPath);        
        outputDir.mkdir();
        for (int i = 0; i < 10; i++) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputDir, i+".txt")));
            LinkedHashSet<String> fileIDSet = new LinkedHashSet<String>();
            String fileName = i + ".txt.id";
            System.out.println("Processing " + fileName);
            File file = new File(corpusDir, fileName);
            List<String> lines = Util.readLines(file);
            for (String line : lines) {
                int index = line.lastIndexOf('#');
                String pairID = line.substring(index + 1);
                String[] tokens = pairID.split("\\.");
                String fileID = tokens[0] + "_" + tokens[1];
                fileIDSet.add(fileID);
            }
            for (String fileID : fileIDSet) {
                writer.write(fileID + "\n");
            }
            writer.close();
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        String[] corpora = new String[]{"AIMed", "BioInfer", "HPRD50", "IEPA", "LLL"};
        for (int i = 0; i < corpora.length; i++) {
            System.out.println(corpora[i]);
            CreateStandardCorpusSplit.createSplit("G:/UWO/thesis/Software/10-PPI-kernel-package/ppi-benchmark1/Corpora/learning-format/CV/CUSTOM_KERNEL/"+corpora[i]+"-folds", "./resource/relation/PPI2/"+corpora[i]+"/splits");
        }
    }
}
