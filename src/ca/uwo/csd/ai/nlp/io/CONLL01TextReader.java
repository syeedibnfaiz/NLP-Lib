/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A <code>CONLL01TextReader<code> object reads lines from CONLL 2001 shared task
 * data file for clause segmentation. a sample line from the dataset follows:
 * Rockwell NNP B-NP S
 * International NNP I-NP X
 * Corp. NNP I-NP X
 * 's POS B-NP X
 * ...
 * @author Syeed Ibn Faiz
 */
public class CONLL01TextReader implements TextReader {

    private boolean readBothBoundary;

    public CONLL01TextReader() {
        this(false);
    }

    /**
     * We sometimes need to read a modified version of the corpus where both S and E are
     * present. Specifically required to view the clauses.
     * @param readBothBoundary
     */
    public CONLL01TextReader(boolean readBothBoundary) {
        this.readBothBoundary = readBothBoundary;
    }



    /**
     * Read from a String. Each token should be on a separate line.
     * @param text The content to be read from
     * @return
     */
    public Text read(String content) {
        Text text = new Text();
        String[] lines = content.split("\n");

        Sentence s = new Sentence();
        for (String line : lines) {
            if (line.isEmpty()) {
                if (!readBothBoundary)s.markAnnotation(new String[]{"POS","CHUNK","CLS_BN"});
                else s.markAnnotation(new String[]{"POS", "CHUNK", "CLS_BN_S", "CLS_BN_E"});
                text.add(s);                
                s = new Sentence();
            }
            else {
                s.add(getTokWord(line));
            }
        }
        return text;
    }

    /**
     * Reads from a file. Note that the resulting sentences are given annotation mark.
     * This is a special case for reading data file where a token can have multiple tags.
     * @param file
     * @return
     */
    public Text read(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            Text text = new Text();

            Sentence s = new Sentence();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (!readBothBoundary) s.markAnnotation(new String[]{"POS","CHUNK","CLS_BN"});
                    else s.markAnnotation(new String[]{"POS", "CHUNK", "CLS_BN_S", "CLS_BN_E"});
                    text.add(s);                    
                    s = new Sentence();
                } else {
                    s.add(getTokWord(line));
                }
            }
            reader.close();
            return text;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.err.println("Cant not read from: " + file.getAbsolutePath());
        }

        return null;
    }
    private TokWord getTokWord(String line) {
        String parts[] = line.split("\\s+");
        if (!readBothBoundary && parts.length < 4) throw new IllegalArgumentException(line + " doesn't have 4 fields.");
        if (readBothBoundary && parts.length < 5) throw new IllegalArgumentException(line + " doesn't have 5 fields.");
        
        parts[0] = parts[0].replaceAll("COMMA", ",");
        TokWord word = new TokWord(parts[0]);
        word.setTag("POS", parts[1]);
        word.setTag("CHUNK", parts[2]);
        if (!readBothBoundary) {
            word.setTag("CLS_BN", parts[3]);
        } else {
            word.setTag("CLS_BN_S", parts[3]);
            word.setTag("CLS_BN_E", parts[4]);
        }
        return word;
    }

    public static void main(String args[]) {
        CONLL01TextReader textReader = new CONLL01TextReader();
        //String fileName = "C:\\UWO\\thesis\\public corpus\\Clause Segmentation\\CONLL 2001\\clauses\\data\\testa1";
        String fileName = ".\\resource\\ml\\data\\discourse\\biodrb_clause_ann.txt";
        Text text = textReader.read(new File(fileName));
        System.out.println(text);
    }
}
