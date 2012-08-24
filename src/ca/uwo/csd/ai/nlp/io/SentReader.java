/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;

/**
 *
 * @author Syeed Ibn Faiz
 */
public interface SentReader {

    public Sentence read(String line);
}
