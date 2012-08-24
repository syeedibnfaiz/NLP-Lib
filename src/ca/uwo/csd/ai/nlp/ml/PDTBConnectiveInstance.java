/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import cc.mallet.types.Instance;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBConnectiveInstance extends Instance implements Serializable {
    int s;
    int e;
    //boolean connective;
    Sentence sentence;
    Object label;    
    SimpleDepGraph depGraph;
    
    public PDTBConnectiveInstance(Sentence sentence, int s, int e, Object label, SimpleDepGraph depGraph) {        
        super(sentence, label, null, null);
        this.s = s;
        this.e = e;
        //this.connective = connective;
        this.label = label;
        this.sentence = sentence;
        this.depGraph = depGraph;
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

    public SimpleDepGraph getDepGraph() {
        return depGraph;
    }
    
    @Override
    public String toString() {
        return sentence.toString(s, e);
    }
    
}
