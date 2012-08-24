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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class RelexRule6 extends RelexRule {

    static final Pattern PATTERN = Pattern.compile("PROT1W?(PUNC|CONJ|PROT)+W?PROT2W?REL");
    
    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                Set<Integer> setI = getGovIndices(i, ccDepGraph, "nsubj|nsubjpass");
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        Set<Integer> setJ = getGovIndices(j, ccDepGraph, "nsubj|nsubjpass");
                        if (isIntersect(s, ccDepGraph, setI, setJ) && (j - i) < 5) {
                            relations.add(new Pair<Integer, Integer>(i, j));
                        }
                        String sentPattern = getSentPattern(s, i, j);
                        if (PATTERN.matcher(sentPattern).find()) {
                            relations.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                }
            }            
        }
        return relations;
    }
    Set<Integer> getGovIndices(int index, SimpleDepGraph ccDepGraph, String pattern) {
        List<SimpleDependency> depDependencies = ccDepGraph.getDepDependencies(index, Pattern.compile(pattern));
        Set<Integer> set = new HashSet<Integer>();
        for (SimpleDependency sd : depDependencies) {
            set.add(sd.gov());
        }
        return set;
    }
    boolean isIntersect(Sentence s, SimpleDepGraph depGraph, Set<Integer> set1, Set<Integer> set2) {
        for (int i : set2) {
            if (set1.contains(i) && s.get(i).getTag("DOMAIN") != null && !isNegated(i, depGraph)) {
                //if (s.get(i).word().matches("(form|interact).*")) return true;
                return true;
            }
        }
        return false;
    }
    boolean isNegated(int index, SimpleDepGraph depGraph) {
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(index);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().endsWith("neg")) return true;
        }
        return false;
    }
    private String getSentPattern(Sentence s, int entity1, int entity2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.size(); i++) {
            if (i == entity1) {
                sb.append("PROT1");
            } else if (i == entity2) {
                sb.append("PROT2");
            } else if (s.get(i).word().contains("PROTEIN")) {
                sb.append("PROT");
            } else if (s.get(i).getTag("DOMAIN") != null) {
                sb.append("REL" + s.get(i).getTag("POS").substring(0, 1));
            } else if (s.get(i).word().matches("[/,:;-]")) {
                sb.append("PUNC");
            } else if (s.get(i).getTag("POS").matches("IN|TO")) {
                sb.append("PREP");
            } else if (s.get(i).word().matches("and|or")) {
                sb.append("CONJ");
            } else {
                sb.append("W");
            }            
        }
        return sb.toString();
    }
}
