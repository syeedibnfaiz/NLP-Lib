/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonatuni
 */
public class ParsedTextReader implements TextReader {
    TextReader textReader;
    String parsedFile;
    BufferedReader bufferedReader;
    private static TreeFactory tf = new LabeledScoredTreeFactory();
    
    public ParsedTextReader(TextReader textReader, String parsedFile) {
        this.textReader = textReader;
        this.parsedFile = parsedFile;
    }

    public void setParsedFile(String parsedFile) {
        this.parsedFile = parsedFile;
    }
    
    public Text read(File plainTextFile) {
        try {

            Text text = textReader.read(plainTextFile);
            bufferedReader = new BufferedReader(new FileReader(parsedFile));
            int count = 0;
            for (Sentence s : text) {
                ++count;
                String line = bufferedReader.readLine();
                TreeReader tr = new PennTreeReader(new StringReader(line), tf);
                Tree t = tr.readTree();
                if (t == null) {
                    System.err.println("Null tree at " + count);
                    return null;
                } else if (t.getLeaves().size() != s.size()) {
                    System.err.println("Syntax tree mismatched with plain text at " + count);
                    System.err.println("# of words: " + s.size());
                    System.err.println("# of leves: " + t.getLeaves().size());
                    return null;
                }
                s.setParseTree(t);
                s.markAnnotation("PARSED");
            }
            return text;
        } catch (IOException ex) {
            Logger.getLogger(ParsedTextReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Text read(String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void main(String args[]) {
        GenericTextReader gTextReader = new GenericTextReader("\n\n", "\n", "\\t", args);
        ParsedTextReader pTextReader = new ParsedTextReader(gTextReader, ".\\resource\\ml\\data\\discourse\\SentConnArg12_final_parsed.txt");
        Text text = pTextReader.read(new File(".\\resource\\ml\\data\\discourse\\SentConnArg12_final.txt"));
    }
}