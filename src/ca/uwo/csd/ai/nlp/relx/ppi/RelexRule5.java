/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class RelexRule5 extends RelexRule {
    private final static Pattern PATTERN = Pattern.compile("PROTEIN[0-9]+.*PROTEIN[0-9]+");

    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph depGraph) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN") && i < (s.size() - 1)) {
                if (PATTERN.matcher(s.get(i).word()).matches() && s.get(i + 1).getTag("DOMAIN") != null) {
                    relations.add(new Pair<Integer, Integer>(i, i));
                } else if (PATTERN.matcher(s.get(i).word()).matches()) {
                    for (SimpleDependency dep : depGraph.getDepDependencies(i)) {
                        int gov = dep.gov();
                        if (dep.reln().matches("nn|amod|abbrev|appos") && s.get(gov).getTag("DOMAIN") != null) {
                            relations.add(new Pair<Integer, Integer>(i, i));
                            break;
                        }
                    }
                }
            }            
        }
        return relations;
    }        
}
