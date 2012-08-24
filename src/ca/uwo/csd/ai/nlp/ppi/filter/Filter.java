/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public interface Filter {
    public String getName();
    public List<RelationInstance> apply(List<RelationInstance> relationInstances);
}
