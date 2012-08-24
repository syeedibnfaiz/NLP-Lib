/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling;

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
 * @author Syeed Ibn Faiz
 */
public class SimpleDepFileReader {
    
    public List<SimpleDepGraph> read(File file) {
        List<SimpleDepGraph> list = new ArrayList<SimpleDepGraph>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                //line = line.trim();
                try {
                    list.add(new SimpleDepGraph(line));
                } catch (IllegalArgumentException ex) {
                    System.out.println(line);
                    System.out.println(file.getAbsolutePath());
                    throw ex;
                }
            }
        } catch(IOException ex) {
            Logger.getLogger(SimpleDepFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return list;
    }
}
