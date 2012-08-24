/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import java.util.Scanner;

/**
 * An <code>LPSentReader</code> object uses LingPipe tokenizer library to
 * tokenize a string into a <code>Sentence</code>.
 * @author Syeed Ibn Faiz
 */
public class LPSentReader implements SentReader {

    private static String regex = "(\\||\\+|-|'|\\d|\\p{L})+|\\S";
    private static TokenizerFactory TOKENIZER_FACTORY;

    public LPSentReader() {
        this(regex);
    }

    public LPSentReader(String regex) {
        TOKENIZER_FACTORY = new RegExTokenizerFactory(regex);
    }

    public Sentence read(String line) {
        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(line.toCharArray(), 0, line.length());
        String[] tokens = tokenizer.tokenize();
        return new Sentence(tokens);
    }

    public static void main(String args[]) {
        LPSentReader reader = new LPSentReader();
        Scanner in = new Scanner(System.in);
        String line;

        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            System.out.println(s);
        }
    }
}
