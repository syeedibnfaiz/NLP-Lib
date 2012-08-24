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
public class PDTBConnectiveSenseInstance extends Instance implements Serializable {
    int s;
    int e;
    int arg2Head;    
    Sentence sentence;
    Object label;    
    SimpleDepGraph depGraph;
    PDTBRelation relation;
    
    public PDTBConnectiveSenseInstance(Sentence sentence, int s, int e, int arg2Head, Object label, SimpleDepGraph depGraph, PDTBRelation relation) {        
        super(sentence, null, null, relation.getSense2());
        this.s = s;
        this.e = e;
        this.arg2Head = arg2Head;        
        this.label = label;
        this.sentence = sentence;
        this.depGraph = depGraph;
        this.relation = relation;
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

    public int getArg2Head() {
        return arg2Head;
    }

    public PDTBRelation getRelation() {
        return relation;
    }
    
    
    @Override
    public String toString() {
        return sentence.toString(s, e);
    }
    
}
