/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

/**
 * <code>Relation</code> stores a biomedical binary relation as 
 * an interaction pair between an agent and a target.
 * @author Syeed Ibn Faiz
 */
public class Relation {
    private int agent;
    private int target;

    public Relation(int agent, int target) {
        this.agent = agent;
        this.target = target;
    }
    
    public int agent() {
        return agent;
    }
    
    public int target() {
        return target;
    }
}
