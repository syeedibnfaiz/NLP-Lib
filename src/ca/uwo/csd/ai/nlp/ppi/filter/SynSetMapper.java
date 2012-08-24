/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class SynSetMapper {
    HashMap<String, HashSet<Integer>> map;

    public SynSetMapper() {
        this("./resource/relation/synset.txt");
    }

    
    public SynSetMapper(String filePath) {
        List<String> lines = Util.readLines(filePath);
        map = new HashMap<String, HashSet<Integer>>();
        
        for (int i = 0; i < lines.size(); i++) {
            String key = lines.get(i);
            
            int j = i + 1;
            HashSet<Integer> set = new HashSet<Integer>();
            while (!lines.get(j).equals("")) {
                int synset = Integer.parseInt(lines.get(j).substring(1, lines.get(j).length() - 1));
                set.add(synset);
                j++;
            }
            if (set.size() > 0) {
                map.put(key, set);
            }
            
            i = j;
        }
    }
    
    public HashSet<Integer> getSynSet(String word) {
        return map.get(word.toLowerCase());
    }
    
    public boolean synMatch(String word1, String word2) {
        HashSet<Integer> synSet1 = getSynSet(word1);
        HashSet<Integer> synSet2 = getSynSet(word2);
        
        if (synSet1 == null || synSet2 == null) {
            return false;
        }
        
        return doesIntersect(synSet1, synSet2);
    }
    
    public boolean doesIntersect(HashSet<Integer> synSet1, HashSet<Integer> synSet2) {
        for (Integer i : synSet1) {
            if (synSet2.contains(i)) {
                return true;
            }
        }
        return false;
    }
    
    public static void main(String[] args) {
        SynSetMapper mapper = new SynSetMapper();
        System.out.println("start");
        System.out.println(mapper.getSynSet("start"));
        System.out.println("begins");
        System.out.println(mapper.getSynSet("begins"));
        System.out.print("match: " + mapper.synMatch("start", "beginning"));
    }
}
