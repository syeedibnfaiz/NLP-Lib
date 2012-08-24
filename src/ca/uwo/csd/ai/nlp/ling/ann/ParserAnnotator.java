/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;


import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ParserAnnotator implements Annotator {

    LexicalizedParser parser;
    
    public ParserAnnotator() {
        //this("C:\\UWO\\thesis\\Software\\grammarscope\\grammar\\englishPCFG.ser.gz");
        //this ("C:\\UWO\\thesis\\Stanford NLP\\stanford-corenlp-2011-06-08\\stanford-corenlp-models-2011-06-08\\edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz");
        //this("/home/mibnfaiz/UWO/thesis/Netbeans Project/java libs/stanford-corenlp-models-2011-06-08/edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        this("./lib/englishPCFG.ser.gz");
    }
    public ParserAnnotator(String optionFlags[]) {
        this();
        parser.setOptionFlags(optionFlags);
    }
    public ParserAnnotator(String path) {
        this.parser = new LexicalizedParser(path);
    }

    public ParserAnnotator(LexicalizedParser parser) {
        this.parser = parser;
    }    

    /**
     * <code>ParserAnnotator</code> changes '(' and ')' for stanford parser
     * @param s
     * @return
     */
    @Override
    public Sentence annotate(Sentence s) {
        s = convert2PennTagSet(s);
        parser.parse(s);
        s.setParseTree(parser.getBestParse());
        s.markAnnotation(getFieldNames());

        s = revert2Normal(s);
        return s;
    }

    /**
     * Change '(' and ')' to PennTagSet format
     * @param s
     * @return
     */
    private Sentence convert2PennTagSet(Sentence s) {
        //if (!s.isAnnotatedBy("POS")) return s;
        for (TokWord word : s) {
            if (word.word().equals("(")) {
                word.setWord("-LRB-");
                if (s.isAnnotatedBy("POS")) {
                    word.setTag("-LRB-");
                }
            }
            if (word.word().equals(")")) {
                word.setWord("-RRB-");
                if (s.isAnnotatedBy("POS")) {
                    word.setTag("-RRB-");
                }
            }
        }
        return s;
    }
    private Sentence revert2Normal(Sentence s) {
        //if (!s.isAnnotatedBy("POS")) return s;
        for (TokWord word : s) {
            if (word.word().equals("-LRB-")) {
                word.setWord("(");
                if (s.isAnnotatedBy("POS")) {
                    word.setTag("(");
                }
            }
            if (word.word().equals("-RRB-")) {
                word.setWord(")");
                if (s.isAnnotatedBy("POS")) {
                    word.setTag(")");
                }
            }
        }
        return s;
    }
    
    @Override
    public String[] getFieldNames() {
        //PARSED = sentence is parsed, sentence level annotation
        return new String[]{"PARSED"};
    }

    public static void main(String args[]) {
        ParserAnnotator annotator = new ParserAnnotator();
        GeniaTagger tagger = new GeniaTagger();
        LPSentReader sentReader = new LPSentReader();
        Scanner in = new Scanner(System.in);
        String line;
        while ((line = in.nextLine()) != null) {
            Sentence sentence = sentReader.read(line);
            sentence = tagger.annotate(sentence);
            sentence = annotator.annotate(sentence);
            Tree t = sentence.getParseTree();
            t.pennPrint();
        }
    }
}
