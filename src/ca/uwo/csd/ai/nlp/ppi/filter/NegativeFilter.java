/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class NegativeFilter implements Filter {

    @Override
    public String getName() {
        return "NEGATIVE_FILTER";
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
        SimpleDepGraph dep = instance.depGraph;
        List<String> backBonePath = instance.path;
        boolean neg = false;
        for (int i = 0; i < backBonePath.size(); i += 2) {
            int index = Integer.parseInt(backBonePath.get(i));
            for (SimpleDependency dependency : dep.getGovDependencies(index)) {
                String reln = dependency.reln();
                if (reln.matches("neg")) {
                    neg = true;
                    break;
                }
            }

        }
        return !neg;
    }
}
