/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.utils.CharniakClient;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Syeed Ibn Faiz
 */
public class CharniakParser implements Annotator {
    public static CharniakClient charniakClient = new CharniakClient();
    public static TreeFactory treeFactory = new LabeledScoredTreeFactory();
    
    public Sentence annotate(Sentence s) {
        String parseStr = charniakClient.parse(s.toString());
        StringReader sr = new StringReader(parseStr);
        TreeReader tr = new PennTreeReader(sr, treeFactory);
        try {
            s.setParseTree(tr.readTree());
            s.markAnnotation(getFieldNames());
        } catch (IOException ex) {
            Logger.getLogger(CharniakParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }

    public String[] getFieldNames() {
        return new String[]{"PARSED"};
    }
    
}
