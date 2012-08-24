package ca.uwo.csd.ai.nlp.corpus.biodrb;

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
public class BioDRBRelationReader {
    
    public List<BioDRBRelation> read(File file) {
        List<BioDRBRelation> list = new ArrayList<BioDRBRelation>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(new BioDRBRelation(line));
            }
        } catch (IOException ex) {
            Logger.getLogger(BioDRBRelationReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list;
    }
}
