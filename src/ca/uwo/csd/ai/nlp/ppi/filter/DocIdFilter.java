/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Used to create folds following Ariola's division 
 * for k-fold cross-validation
 * @author Syeed Ibn Faiz
 */
public class DocIdFilter implements Filter {

    private HashSet<String> docIds;

    public DocIdFilter(List<String> docIds) {
        this.docIds = new HashSet<String>(docIds);
    }
    
    @Override
    public String getName() {
        return "DOCID_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            if (docIds.contains(instance.docId)) {
                newInstances.add(instance);
            }
        }
        return newInstances;
    }
    
}
