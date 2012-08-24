/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import edu.stanford.nlp.trees.Tree;
import java.util.List;

/**
 *
 * @author mibnfaiz
 */
public class ParsedSentence extends Sentence {

    public ParsedSentence(Tree root) {
        List<Tree> leaves = root.getLeaves();
        for (Tree leaf : leaves) {
            Tree parent = leaf.parent(root);
            if (!parent.value().equals("-NONE-")) {
                add(new TokWord(leaf.value()));            
            } else {
                TokWord word = new TokWord(leaf.value());
                word.setTag("-NONE-", "Y");   //handle null nodes in GS PTB
                add(word);
            }
        }
        this.setParseTree(root);
        this.markAnnotation("PARSED");
    }       
    
    /**
     * returns the word position for which the given prefix matches
     * @param prefix
     * @return 
     */
    public int matchPrefix(String prefix) {
        StringBuilder tmpPrefix = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            //tmpPrefix += this.get(i).word();
            String word = this.get(i).word();
            if (word.matches("-LRB-|-RRB-|-LCB-|-RCB-")) continue; //3 new characters L/R, R, B would hamper the matching process
            //else {
                for (int j = 0; j < word.length(); j++) {
                    //|| line.charAt(i) == ',' is added for new piped version, coz , were being included in the gorn address
                    if (Character.isLetterOrDigit(word.charAt(j)) || word.charAt(j) == ',') {
                        tmpPrefix.append(word.charAt(j));
                    } /*else {
                        tmpPrefix.append("-");
                    }*/
                }
            //}
            if (prefix.equals(tmpPrefix.toString())) return i;
            else if (prefix.endsWith("fterward") && (prefix+"s").equals(tmpPrefix.toString())) {
                return i;
            }
            //System.out.println(tmpPrefix + " does not match!");
        }
        return -1;
    }
    
    /**
     * Returns sub-sentence without whitespace
     * @param start
     * @param end
     * @return 
     */
    public String getSequence(int start, int end) {
        String sequence = "";
        for (int i = start; i <= end; i++) {
            sequence += this.get(i).word();
        }
        return sequence;
    }
}
