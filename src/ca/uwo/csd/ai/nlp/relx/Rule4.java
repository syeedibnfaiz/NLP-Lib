/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author mibnfaiz
 */
public class Rule4 extends Rule {

    @Override
    public List<Relation> findRelations(Sentence s, SimpleDepGraph depGraph) {
        List<Relation> relations = new ArrayList<Relation>();
        List<SimpleDependency> amodDependencies = depGraph.getDependencies("amod");
        for (SimpleDependency sDep : amodDependencies) {
            int gov = sDep.gov();
            int dep = sDep.dep();
            String depWord = s.get(dep).word();
            if (s.get(dep).getTag("LEXE").equals("B") && depWord.endsWith("-dependent")) {
                List<Integer> entities = getEntitiesFromNP(gov, s, depGraph, Pattern.compile("nn|amod"));
                for (Integer e : entities) {
                    relations.add(new Relation(dep, e));
                }
            }
        }
        return relations;
    }
    
}
