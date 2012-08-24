/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import java.util.HashMap;
import java.util.HashSet;
import kernel.ds.SparseVector;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class FeatureSet2Vector {
    HashMap<String, Integer> map;
    HashMap<Integer, String> rmap;
    int count = 0;
    
    public FeatureSet2Vector() {
        map = new HashMap<String, Integer>();
        rmap = new HashMap<Integer, String>();
    }
    
    public SparseVector getVector(HashSet<String> set) {
        SparseVector vector = new SparseVector();
        for (String key : set) {
            vector.add(getIndex(key), 1.0);
        }
        vector.sortByIndices();
        return vector;
    }
    
    private int getIndex(String key) {
        Integer v = map.get(key);
        if (v != null) {
            return v;
        } else {
            map.put(key, count);
            rmap.put(count, key);            
            count++;
            return count - 1;
        }
    }
    
    public String toString(SparseVector vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.size(); i++) {
            sb.append(rmap.get(vector.get(i).index)).append(" ");
        }
        return sb.toString();
    }
}
