/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author mibnfaiz
 */
public class RelexRule4 extends RelexRule {

    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {                
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        String path = ccDepGraph.getPath(i, j);
                        if (path == null) {
                            relations.add(new Pair<Integer, Integer>(i, j));
                        } else if (path.matches("-?(appos|nn):")) {
                            relations.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                }
            }            
        }
        return relations;
    }
    
}
