/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Text;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LPTextReader implements TextReader {

    private static String regex = "(\\+|-|'|\\d|\\p{L})+|\\S";
    private static TokenizerFactory TOKENIZER_FACTORY;
    static final SentenceModel SENTENCE_MODEL  = new MedlineSentenceModel();
    LPSentReader sentReader;

    public LPTextReader() {
        this(regex);
    }

    public LPTextReader(String regex) {
        TOKENIZER_FACTORY = new RegExTokenizerFactory(regex);
        sentReader = new LPSentReader(regex);
    }


    public Text read(String text) {
        Text sentences = new Text();        
        
        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(text.toCharArray(), 0, text.length());
        List<String> tokenList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        tokenizer.tokenize(tokenList, whiteList);

        String tokens[] = new String[tokenList.size()];
        String whites[] = new String[whiteList.size()];
        tokenList.toArray(tokens);
        whiteList.toArray(whites);

        int sentenceBoundaries[] = SENTENCE_MODEL.boundaryIndices(tokens, whites);
        
        int sentStartToken = 0;
        int sentEndToken = 0;
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            sentEndToken = sentenceBoundaries[i];
            String sentence = "";
            for (int j = sentStartToken; j <= sentEndToken; j++) {
                sentence += tokens[j] + " " + whites[j + 1];
            }
            sentences.add(sentReader.read(sentence));
            sentStartToken = sentEndToken+1;
        }
        
        return sentences;
    }

    public Text read(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line + "\n");
            }
            reader.close();
            
            return this.read(content.toString());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.err.println("Cant not read from: " + file.getAbsolutePath());
        }
        
        return null;
    }

    public static void main(String args[]) {
        LPTextReader textReader = new LPTextReader();
        //GeniaTagger tagger = new GeniaTagger();
        Scanner in = new Scanner(System.in);
        String line;

        while ((line = in.nextLine()) != null) {
            Text text = textReader.read(line);
            //for (Sentence s : text) tagger.annotate(s);
            System.out.println(text);
        }
    }
}
