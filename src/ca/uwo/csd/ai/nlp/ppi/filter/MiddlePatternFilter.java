/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class MiddlePatternFilter implements Filter {

    @Override
    public String getName() {
        return "MIDDLE_PATTERN_FILTER";
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
        if (instance.lcs > instance.entity1 && instance.lcs < instance.entity2) {
            List<String> backBonePath = instance.path;
            boolean clausal = false;
            for (int i = 1; i < backBonePath.size(); i += 2) {
                //if (backBonePath.get(i).matches("(rcmod|advcl|ccomp|parataxis)")) {
                if (backBonePath.get(i).matches("(advcl|ccomp|parataxis)")) { //rcmod is ok
                    clausal = true;
                }
            }
            if (!clausal) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
