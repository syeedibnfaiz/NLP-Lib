/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.HashSet;
import java.util.List;
import kernel.CustomKernel;
import kernel.ds.SparseVector;
import libsvm.svm_node;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SimpleFeatureKernel implements CustomKernel {

    public static FeatureSet2Vector featureSet2Vector = new FeatureSet2Vector();
    public double s1, s2, s3;
    int count = 0;
    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;

        if (instance1.type != instance2.type) {
            return 0;
        }

        double score1 = instance1.leftVector.normDot(instance2.leftVector);
        double score2 = instance1.rightVector.normDot(instance2.rightVector);
        double score3 = instance1.fullVector.normDot(instance2.fullVector);

        s1 += score1;
        s2 += score2;
        s3 += score3;
        count++;
        
        return score1 + score2 + score3;
    }

    public static SparseVector getLeftVector(RelationInstance instance) {
        HashSet<String> featureSet = new HashSet<String>();
        Sentence s = instance.s;
        List<String> path = instance.path;
        if (path == null) {
            return null;
        }
        int size = path.size();
        int lcsIndex = instance.lcsIndex;
        String prevReln = null;
        if (lcsIndex < (size - 1)) {
            prevReln = path.get(lcsIndex + 1);
        }
        for (int i = lcsIndex; i >= 0; i--) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                //char position = getRelativePosition(instance, index);
                String word = s.get(index).word();
                char pos = s.get(index).getTag("POS").charAt(0);
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                }        
                
                boolean domain = false;
                if (s.get(index).getTag("DOMAIN") != null) {
                    featureSet.add("REL-" + pos);                                                                                
                    domain = true;
                }   
                //featureSet.add(word.toLowerCase());                                
                if (domain) {
                    featureSet.add(word.toLowerCase());
                } else if (index == instance.entity1) {
                    //featureSet.add("E1");
                } else if (index == instance.entity2) {
                    //featureSet.add("E2");
                } else if (word.startsWith("PROTEIN")) {
                    featureSet.add("PROT");
                } else {
                    featureSet.add(word.toLowerCase());                    
                }
            } else {
                String reln = path.get(i);
                if (reln.startsWith("-prep")) {
                    featureSet.add("-prep");
                }
                /*if (reln.equals("nsubjpass")) {
                    reln = "nsubj";
                } else if (reln.equals("-nsubjpass")) {
                    reln = "-nsubj";
                }*/
                //featureSet.add(reln);
                featureSet.add(reln + (lcsIndex - i)/3);
                //featureSet.add(prevReln + reln);
                prevReln = reln;
            }
        }

        return featureSet2Vector.getVector(featureSet);
    }

    public static SparseVector getRightVector(RelationInstance instance) {
        HashSet<String> featureSet = new HashSet<String>();
        Sentence s = instance.s;
        List<String> path = instance.path;
        if (path == null) {
            return null;
        }
        int lcsIndex = instance.lcsIndex;
        String prevReln = null;
        if (lcsIndex > 0) {
            prevReln = path.get(lcsIndex - 1);
        }
        for (int i = lcsIndex; i < path.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));                
                String word = s.get(index).word();
                char pos = s.get(index).getTag("POS").charAt(0);
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                }   
                boolean domain = false;
                if (s.get(index).getTag("DOMAIN") != null) {                                                      
                    featureSet.add("REL-" + pos);
                    domain = true;
                }   
                //featureSet.add(word.toLowerCase());                
                if (domain) {
                    featureSet.add(word.toLowerCase());
                } else if (index == instance.entity1) {
                    //featureSet.add("E1");
                } else if (index == instance.entity2) {
                    //featureSet.add("E2");
                } else if (word.startsWith("PROTEIN")) {
                    featureSet.add("PROT");
                } else {
                    featureSet.add(word.toLowerCase());                    
                }
            } else {
                String reln = path.get(i);
                if (reln.startsWith("prep")) {
                    featureSet.add("prep");
                }
                /*if (reln.equals("nsubjpass")) {
                    reln = "nsubj";
                } else if (reln.equals("-nsubjpass")) {
                    reln = "-nsubj";
                }*/
                //featureSet.add(reln);
                featureSet.add(reln + (i - lcsIndex)/3);
                //featureSet.add(prevReln + reln);
                prevReln = reln;
            }
        }

        return featureSet2Vector.getVector(featureSet);
    }

    public static SparseVector getFullVector(RelationInstance instance, SparseVector vector1, SparseVector vector2) {
        SparseVector vector = new SparseVector();

        if (vector1 != null) {
            for (int i = 0; i < vector1.size(); i++) {
                vector.add(vector1.get(i));
            }
        }
        if (vector2 != null) {
            for (int i = 0; i < vector2.size(); i++) {
                vector.add(vector2.get(i));
            }
        }

        HashSet<String> featureSet = new HashSet<String>();
        int lcsIndex = instance.lcsIndex;
        if (instance.path != null && lcsIndex > 0 && lcsIndex < (instance.path.size() - 1)) {
            featureSet.add(instance.path.get(lcsIndex - 1) + instance.s.get(instance.lcs).getTag("POS").charAt(0) + instance.path.get(lcsIndex + 1));
        }        

        /*int key = instance.key;
        if (key != -1) {
        if (instance.s.get(key).getTag("DOMAIN") != null) {
        featureSet.add("Key-" + instance.s.get(key).word());
        }
        }*/
        SparseVector vector3 = featureSet2Vector.getVector(featureSet);
        if (vector3 != null) {
            for (int i = 0; i < vector3.size(); i++) {
                vector.add(vector3.get(i));
            }
        }

        vector.sortByIndices();
        return vector;
    }

    private static char getRelativePosition(RelationInstance instance, int i) {
        if (i < instance.entity1) {
            return 'L';
        } else if (i <= instance.entity2) {
            return 'M';
        } else {
            return 'R';
        }
    }
}
