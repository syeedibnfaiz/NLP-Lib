/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public interface Rule {
    Set<Pair<Integer, Integer>> getCandidates(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph);
}
