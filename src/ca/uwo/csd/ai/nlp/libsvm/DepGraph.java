/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.libsvm;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DepGraph {
    public Sentence s;
    public SimpleDepGraph depGraph;
    public int lcs;
    public int entity1;
    public int entity2;
    
    public DepGraph(Sentence s, SimpleDepGraph depGraph, int lcs, int entity1, int entity2) {
        this.s = s;
        this.depGraph = depGraph;
        this.lcs = lcs;
        this.entity1 = entity1;
        this.entity2 = entity2;
    } 
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(s.toString()).append("\n");
        sb.append(s.get(entity1)).append("-").append(s.get(entity2));
        return sb.toString();
    }
}
