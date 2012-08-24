package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RightPatternFilter implements Filter {

    @Override
    public String getName() {
        return "RIGHT_PATTERN_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            List<String> backBonePath = instance.path;
            if (backBonePath == null) {
                continue;
            }
            if (apply(instance)) {
                newInstances.add(instance);
            } else {
                instance.filterVerdict = getName();
            }

        }
        return newInstances;
    }
    public boolean apply(RelationInstance instance) {
        if (instance.lcs > instance.entity2) {
                Sentence s = instance.s;
                SimpleDepGraph depGraph = instance.depGraph;
                int lcs = instance.lcs;
                String lcsPOS = s.get(lcs).getTag("POS");
                String lcsWord = s.get(lcs).word();
                int entity1 = instance.entity1;
                int entity2 = instance.entity2;
                List<Integer> reachableIndices = depGraph.getReachableIndices(lcs, true, 10, "rcmod|conj_(and|or|but)|advcl");
                boolean protein = false;
                for (int i : reachableIndices) {
                    if (i > lcs && i != entity1 && i != entity2 && s.get(i).word().contains("PROTEIN")) {
                        protein = true;
                        break;
                    }
                }
                if (!protein || lcsPOS.matches("N.*")) {
                    return true;
                } else {                    
                    return false;
                }
            } else {
                return true;
            }
    }
}
