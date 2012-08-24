/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import cc.mallet.types.Instance;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ConnectiveInstance extends Instance implements Serializable {
    int s;
    int e;
    //boolean connective;
    Sentence sentence;
    Object label;
    public ConnectiveInstance(Sentence sentence, int s, int e, Object label) {        
        super(sentence, null, null, null);
        this.s = s;
        this.e = e;
        //this.connective = connective;
        this.label = label;
        this.sentence = sentence;
        this.setSource(this);
    }

    /*public ConnectiveInstance(Sentence sentence, int s, int e) {
        this(sentence, s, e, false);
    }*/

    public int getE() {
        return e;
    }

    public int getS() {
        return s;
    }

    /*public boolean isConnective() {
        return connective;
    }*/

    public Object getLabel() {
        return label;
    }

    
    public Sentence getSentence() {
        return sentence;
    }

    
    @Override
    public String toString() {
        return sentence.toString(s, e);
    }
    
}
