/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Text;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public interface TextReader {

    public Text read(String text);
    public Text read(File file);
}
