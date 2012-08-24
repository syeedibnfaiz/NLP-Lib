/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LLLDataReader {
    
    public static List<LLLDataInstance> readTrainingData(String trainingFile) {
        List<LLLDataInstance> dataInstances = new ArrayList<LLLDataInstance>();
        List<String> lines = Util.readLines(trainingFile);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("%") || line.length() == 0) {
                if (count == 6) {
                    dataInstances.add(new LLLDataInstance(sb.toString()));
                    sb = new StringBuilder();
                    count = 0;                    
                }
                continue;
            }
            sb.append(line + "\n");
            count++;
        }
        
        /*for (LLLDataInstance instance : dataInstances) {
            System.out.println(instance);
        }*/
        return dataInstances;
    }
    
    public static void main(String args[]) {
        LLLDataReader.readTrainingData("./resource/relation/LLL/genic_interaction_data.txt");
        
    }
}
