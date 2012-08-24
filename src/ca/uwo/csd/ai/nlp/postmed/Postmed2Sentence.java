/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.postmed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Postmed2Sentence {
    public static void main(String args[]) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"));
            
            String line;
            int pos = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("%")) {
                    pos++;
                    continue;
                }
                String[] tokens = line.split("\\s+");
                StringBuilder result = new StringBuilder();
                int sqCount = 0;
                int quoteCount = 0;
                result.append(tokens[0]);
                
                for (int i = 1; i < tokens.length; i++) {                    
                    if (tokens[i].equals("[") || tokens[i].equals("]")) {
                        sqCount++;                        
                    }
                    
                    if (tokens.equals("'")) {
                        quoteCount++;
                        if (quoteCount%2 == 1) {
                            result.append(" " + tokens[i]);
                        } else {
                            result.append(tokens[i]);
                        }
                    } else if ((!tokens[i].matches("[\\[(]") && tokens[i].matches("\\p{Punct}")) || tokens[i-1].matches("[\\[(\\/{]") || (tokens[i-1].equals(",") && ((sqCount %2) == 1)) || (tokens[i-1].equals("'") && ((quoteCount %2) == 1))) {
                        result.append(tokens[i]);
                    } else {
                        result.append(" " + tokens[i]);
                    }
                }
                //System.out.println(pos);
                //System.out.println(pos + result.length());
                //System.out.println(result.toString());
                writer.write(result.toString() + "\n");
                pos += result.length() + 1;
            }
            reader.close();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(Postmed2Sentence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
