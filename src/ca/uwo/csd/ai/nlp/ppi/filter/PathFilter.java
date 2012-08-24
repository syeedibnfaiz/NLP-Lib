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
public class PathFilter implements Filter {

    @Override
    public String getName() {
        return "PATH_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            List<String> backBonePath = instance.path;
            if (backBonePath != null) {
                newInstances.add(instance);
            }
        }
        return newInstances;
    }
    
}
