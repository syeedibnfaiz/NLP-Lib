/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.PrintWriter;

/**
 *
 * @author mibnfaiz
 */
public class Utils {
    
    /**
     * Writes an ARFF (Weka) representation of an instanceList
     * @param instances
     * @param pWriter
     * @param description 
     */
    public static void convert2ARFF(InstanceList instances, PrintWriter pWriter, String description) {
        Alphabet dataAlphabet = instances.getDataAlphabet();
        Alphabet targetAlphabet = instances.getTargetAlphabet();
        
        pWriter.write("@Relation " + description + "\n\n");
        
        int size = dataAlphabet.size();
        for (int i = 0; i < size; i++) {
            pWriter.write("@attribute "+dataAlphabet.lookupObject(i).toString().replaceAll("\\s+", "_") +" {1, 0}\n");
        }
        
        //pWriter.write("@attribute target {true, false}\n\n@data\n");
        pWriter.write("@attribute target {");
        for (int i = 0; i < targetAlphabet.size(); i++) {
            if (i != 0) pWriter.write(",");
            pWriter.write(targetAlphabet.lookupObject(i).toString());
        }
        pWriter.write("}\n\n@data\n");
        
        for (Instance instance : instances) {
            FeatureVector fv = (FeatureVector) instance.getData();
            int[] indices = fv.getIndices();
            boolean[] attrFlag = new boolean[size];
            
            for (int i = 0; i < indices.length; i++) {
                attrFlag[indices[i]] = true;
            }
            for (int i = 0; i < attrFlag.length; i++) {                
                if (attrFlag[i]) pWriter.write("1,");
                else pWriter.write("0,");                
            }
            pWriter.write(instance.getTarget().toString());
            pWriter.write("\n");
        }
    }
}
