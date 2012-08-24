/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PPIInstance extends Instance {
    public Sentence s;    
    public SimpleDepGraph depGraph;
    public SimpleDepGraph ccDepGraph;
    public String docId;
    public int lineNum;
    public int entity1;
    public int entity2;
    public boolean interact;
    
    public PPIInstance(Object data, Object target, Object name, Object source) {
        super(data, target, name, source);
    }

    public PPIInstance(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String docId, int lineNum, int entity1, int entity2, boolean interact) {
        super(s, interact, docId + "_" + lineNum, null);
        this.s = s;
        this.depGraph = depGraph;
        this.ccDepGraph = ccDepGraph;
        this.docId = docId;
        this.lineNum = lineNum;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.interact = interact;                
    }        
}
