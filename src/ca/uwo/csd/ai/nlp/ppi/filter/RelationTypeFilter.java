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
public class RelationTypeFilter implements Filter {
    final static int LEFT = 1;
    final static int MIDDLE = 2;
    final static int RIGHT = 3;
    private int type;

    public RelationTypeFilter(int type) {
        this.type = type;
    }
    
    @Override
    public String getName() {
        return "RELATION_TYPE_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            if (getType(instance) == type) {
                newInstances.add(instance);
            }
        }
        return newInstances;
    }
    
    private int getType(RelationInstance instance) {
        if (instance.lcs < instance.entity1) return LEFT;
        else if (instance.lcs < instance.entity2) return MIDDLE;
        else return RIGHT;
    }
}
