package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LeftPatternFilter implements Filter {

    @Override
    public String getName() {
        return "LEFT_PATTERN_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            if (apply(instance)) {
                newInstances.add(instance);
            } else {
                instance.filterVerdict = getName();
            }
        }
        return newInstances;
    }

    public boolean apply(RelationInstance instance) {
        List<String> backBonePath = instance.path;
        if (backBonePath == null) {
            return false;
        }
        if (instance.lcs < instance.entity1) {
            Sentence s = instance.s;
            int lcs = instance.lcs;
            int lcsIndex = instance.lcsIndex;

            HashSet<String> prep1Set = new HashSet<String>();
            for (int i = lcsIndex - 1; i >= 1; i -= 2) {
                if (backBonePath.get(i).matches("-prep.*")) {
                    prep1Set.add(backBonePath.get(i));
                } else if (backBonePath.get(i).matches("-agent")) {
                    prep1Set.add("-prep_by");
                }
            }
            HashSet<String> prep2Set = new HashSet<String>();
            for (int i = lcsIndex + 1; i < backBonePath.size(); i += 2) {
                if (backBonePath.get(i).matches("prep.*")) {
                    prep2Set.add(backBonePath.get(i));
                } else if (backBonePath.get(i).matches("agent")) {
                    prep2Set.add("prep_by");
                }
            }

            String lcsPOS = s.get(lcs).getTag("POS");
            String lcsWord = s.get(lcs).word();
            if (prep1Set.isEmpty() && prep2Set.isEmpty()) {
                return false;
            } /*else if ((lcsPOS.matches("VB(P|Z|G|D)?") && !lcsWord.matches("(consist|combin|compos).*") && !prep2Set.contains("prep_by"))) {
                return false;
            } else if ((lcsPOS.matches("VBN") && !lcsWord.matches("(consist|combin|compos).*") && !(prep1Set.contains("-prep_between") && prep2Set.contains("prep_between")))) {
                return false;
            }*/ else {
                return true;
            }
        } else {
            return true;
        }
    }
}
