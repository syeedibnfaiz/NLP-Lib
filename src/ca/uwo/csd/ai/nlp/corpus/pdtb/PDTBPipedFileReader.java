/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PDTBPipedFileReader {
    
    public List<PDTBRelation> read(File file) {
        List<PDTBRelation> list = new ArrayList<PDTBRelation>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(new PDTBRelation(line));
            }
        } catch (IOException ex) {
            Logger.getLogger(PDTBPipedFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list;
    }
}
