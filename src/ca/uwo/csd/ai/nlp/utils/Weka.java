/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Weka {
    /**
     * Writes an ARFF (Weka) representation of an instanceList
     * @param instances
     * @param pWriter
     * @param description 
     */
    public static void convert2ARFF(InstanceList instances, PrintWriter pWriter, String description) {
        Alphabet dataAlphabet = instances.getDataAlphabet();
        Alphabet targetAlphabet = instances.getTargetAlphabet();
        
        pWriter.write("@Relation \"" + description + "\"\n\n");
        
        int size = dataAlphabet.size();
        for (int i = 0; i < size; i++) {
            pWriter.write("@attribute \""+dataAlphabet.lookupObject(i).toString().replaceAll("\\s+", "_") +"\" {0, 1}\n");
        }
        
        pWriter.write("@attribute target {");
        for (int i = 0; i < targetAlphabet.size(); i++) {
            if (i != 0) pWriter.write(",");
            pWriter.write(targetAlphabet.lookupObject(i).toString().replace(",", ";"));
        }
        pWriter.write("}\n\n@data\n");
                
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = instances.get(i);
            pWriter.write("{");
            FeatureVector fv = (FeatureVector) instance.getData();
            int[] indices = fv.getIndices();
            boolean[] attrFlag = new boolean[size];
            
            for (int j = 0; j < indices.length; j++) {
                attrFlag[indices[j]] = true;
            }            
            for (int j = 0; j < attrFlag.length; j++) {                
                if (attrFlag[j]) {                    
                    pWriter.write(j + " 1, ");                    
                }
                //else pWriter.write("0,");                
            }
            pWriter.write(attrFlag.length+" " +instance.getTarget().toString().replace(",", ";"));
            pWriter.write("}\n");            
        }
    }
    
    public static void convert2ARFF(InstanceList instances, String outputFile, String description) {
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            convert2ARFF(instances, writer, description);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Weka.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
