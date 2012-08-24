/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import edu.stanford.nlp.trees.Tree;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SimpleSentReader implements SentReader {

    @Override
    public Sentence read(String line) {
        List<TokWord> words = new ArrayList<TokWord>();
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(line);
        int start = boundary.first();
        for (int end = boundary.next();
                end != BreakIterator.DONE;
                start = end, end = boundary.next()) {
            String s = line.substring(start, end);
            if (!s.matches("\\s+")) {
                words.add(new TokWord(s));
            }
        }
        return new Sentence(words);
    }
    
    public static void main(String args[]) {
        SimpleSentReader sentReader = new SimpleSentReader();
        ParserAnnotator annotator = new ParserAnnotator();
        
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            Tree root = s.getParseTree();
            root.pennPrint();    
            System.out.println(s);
        }
    }
}
