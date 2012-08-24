/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SentenceInstance extends Instance {

    public SentenceInstance(Sentence sentence) {
        super(sentence, null, null, sentence.toString());
    }


}
