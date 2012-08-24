/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class JuxtaposFilter implements Filter {

    @Override
    public String getName() {
        return "JUXTAPOS_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            
            List<String> backBonePath = instance.path;
            if (apply(instance)) {
                newInstances.add(instance);
            } else {
                instance.filterVerdict = getName();
            }
        }
        return newInstances;
    }

    public boolean apply(RelationInstance instance) {
        if (instance.entity1 == instance.entity2) {
            Sentence s = instance.s;
            int lcs = instance.lcs;
            String lcsPOS = s.get(lcs).getTag("POS");
            if (lcsPOS.matches("N.*") && lcs > instance.entity2) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
