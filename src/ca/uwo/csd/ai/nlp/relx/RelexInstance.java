/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelexInstance extends Instance{

    int entity1;
    int entity2;
    SimpleDepGraph depGraph;    //typed
    SimpleDepGraph depGraphCC; //CCProcessed
    Sentence s;
    boolean relation;
    public RelexInstance(Object data, Object target, Object name, Object source) {
        super(data, target, name, source);
    }

    public RelexInstance(LLLDataInstance dataInstance, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph depGraphCC, int entity1, int entity2, boolean target) {
        super(s, target, null, dataInstance);
        this.s = s;
        this.depGraph = depGraph;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.depGraphCC = depGraphCC;
        this.relation = target;
    }

    public SimpleDepGraph getDepGraph() {
        return depGraph;
    }

    public int getEntity1() {
        return entity1;
    }

    public int getEntity2() {
        return entity2;
    }

    public SimpleDepGraph getDepGraphCC() {
        return depGraphCC;
    }

    public Sentence getS() {
        return s;
    }
    
    
    @Override
    public String toString() {
        //return s.get(entity1).word() + "-" + s.get(entity2).word();
        return depGraph.getPath(entity1, entity2);
    }
}
