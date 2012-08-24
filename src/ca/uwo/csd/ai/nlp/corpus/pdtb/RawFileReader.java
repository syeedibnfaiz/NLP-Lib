/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class RawFileReader {
    public List<RawSentence> read(File file) {
        List<RawSentence> list = new ArrayList<RawSentence>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            int offset = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.equals("") && !line.equals(".START ")) {
                    list.add(new RawSentence(offset, line));
                }
                offset += line.length() + 1;    //1 for \n
            }
        } catch (IOException ex) {
            Logger.getLogger(RawFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }
}
