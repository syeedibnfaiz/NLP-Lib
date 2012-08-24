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
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelexRule2b extends RelexRule {
    static final Pattern RULE2B_PATTERN = Pattern.compile("(PREP|REL|N)+(PREP)(REL|PREP|N)*PROT1(REL|N|PREP|PROT)+PROT2");
    @Override
    public List<Pair<Integer, Integer>> findRelations(Sentence s, SimpleDepGraph ccDepGraph) {
        List<Pair<Integer, Integer>> relations = new ArrayList<Pair<Integer, Integer>>();        
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        String sentPattern = getSentPattern(s, i, j);
                        if (RULE2B_PATTERN.matcher(sentPattern).find()) {
                            relations.add(new Pair<Integer, Integer>(i, j));                            
                        }
                    }
                }
            }            
        }
        return relations;
    }
    
    private String getSentPattern(Sentence s, int entity1, int entity2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= entity2; i++) {
            if (i == entity1) {
                sb.append("PROT1");
            } else if (i == entity2) {
                sb.append("PROT2");
            }  else if (s.get(i).word().contains("PROTEIN")) {
                sb.append("PROT");
            }  else if (s.get(i).getTag("DOMAIN") != null) {
                //sb.append("REL" + s.get(i).getTag("POS").substring(0, 1)); //RELV or RELN
                sb.append("REL");
            } else if (s.get(i).word().matches("[/,-]")) {
                sb.append(s.get(i).word());
            } else if (s.get(i).getTag("POS").matches("IN|TO") && s.get(i).word().matches("of|by|to|on|for|in|through|with")) {
                sb.append("PREP");
            } else if (s.get(i).word().matches("and|or")) {
                sb.append("CONJ");
            } else if (s.get(i).getTag("POS").startsWith("N")) {
                sb.append("N");
            } else {
                sb.append("W");
            }
            //sb.append(".");
        }
        return sb.toString();
    }
    
}
