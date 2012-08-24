/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.utils.BLLIPClient;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonatuni
 */
public class BLLIPParser implements Annotator {
    static final BLLIPClient CLIENT = new BLLIPClient();
    static final TreeFactory TF = new LabeledScoredTreeFactory();
    
    @Override
    public Sentence annotate(Sentence s) {
        String parse = CLIENT.parse(s.toString());
        StringReader sr = new StringReader(parse);
        TreeReader tr = new PennTreeReader(sr, TF);
        try {
            Tree root = tr.readTree();
            s.setParseTree(root);
            s.markAnnotation(getFieldNames());
            List<Tree> leaves = root.getLeaves();
            for (int i = 0; i < leaves.size(); i++) {
                Tree leaf = leaves.get(i);
                Tree parent = leaf.parent(root);
                s.get(i).setTag(parent.value());
            }
        } catch (IOException ex) {
            Logger.getLogger(BLLIPParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"POS","PARSED"};
    }
    
}
