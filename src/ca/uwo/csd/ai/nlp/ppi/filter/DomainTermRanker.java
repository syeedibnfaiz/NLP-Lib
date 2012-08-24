/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DomainTermRanker {

    public static HashMap<String, Double> rankMap;

    public static void rank(List<RelationInstance> instances) {
        rankMap = new HashMap<String, Double>();
        HashMap<String, Double> posMap = new HashMap<String, Double>();
        HashMap<String, Double> negMap = new HashMap<String, Double>();
        for (RelationInstance instance : instances) {
            Sentence s = instance.s;
            List<String> path = instance.path;
            if (path == null) continue;
            for (int i = 0; i < path.size(); i += 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null) {
                    String term = s.get(index).getTag("DOMAIN");
                    if (instance.interaction) {
                        if (posMap.containsKey(term)) {
                            posMap.put(term, posMap.get(term) + 1);
                        } else {
                            posMap.put(term, 1.0);
                        }
                    } else {
                        if (negMap.containsKey(term)) {
                            negMap.put(term, negMap.get(term) + 1);
                        } else {
                            negMap.put(term, 1.0);
                        }
                    }
                }
            }
        }
        for (String key : posMap.keySet())  {
            //double rank = posMap.get(key) + 1;
            double pos = posMap.get(key);
            double neg = 0;
            if (negMap.containsKey(key)) {
                //rank /= (negMap.get(key) + 1);
                neg = negMap.get(key);
            }
            double rank = (pos - neg)/(pos + neg);
            rankMap.put(key, rank);
        }
    }
    
    public static double getRank(String word) {
        Double rank = rankMap.get(word.toLowerCase());
        if (rank == null) {
            return 0;
        }
        return rank;
    }
    public static void print() {
        List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(rankMap.entrySet());
        Collections.sort(entryList, new Comparator<Entry<String, Double>> (){
            @Override
            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                if (o1.getValue() > o2.getValue()) return -1;
                else if (o1.getValue() < o2.getValue()) return 1;
                else return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (Entry<String, Double> entry : entryList) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
    
    public static void main(String[] args) {
        CorpusReader reader = new CorpusReader();
        List<RelationInstance> relationInstances = reader.getRelationInstances("./resource/relation/PPI4/AIMed");
        DomainTermRanker.rank(relationInstances);
        DomainTermRanker.print();
    }
}
