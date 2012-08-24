/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.ling.Sentence;


/**
 * A <code>Annotator</code> assigns tags to tokens in a sentence.
 * It can also assign tag/mark to the sentence only (in that case,
 * we call it sentential annotation)
 * @author Syeed Ibn Faiz
 */
public interface Annotator{

    /**
     * Annotate a sentence
     * @param s The sentence to annotate
     */
    public Sentence annotate(Sentence s);

    /**
     * Give the name of the fields that this annotator uses
     * @return list of field names (like POS, CHUNK, NE and so on)
     */
    public String[] getFieldNames();
}
