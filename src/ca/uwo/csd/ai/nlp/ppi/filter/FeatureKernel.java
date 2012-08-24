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
public class FeatureKernel implements CustomKernel {

    public static FeatureSet2Vector featureSet2Vector = new FeatureSet2Vector();

    @Override
    public double evaluate(svm_node x, svm_node y) {
        RelationInstance instance1 = (RelationInstance) x.data;
        RelationInstance instance2 = (RelationInstance) y.data;
        if (instance1.type != instance2.type) {
            return 0;
        }
               
        double score = instance1.leftVector.normDot(instance2.leftVector);
        score += instance1.rightVector.normDot(instance2.rightVector);
        //score += instance1.fullVector.normDot(instance2.fullVector);
        return score;
    }

    public static SparseVector getLeftVector(RelationInstance instance) {
        HashSet<String> featureSet = new HashSet<String>();
        Sentence s = instance.s;
        List<String> path = instance.path;
        if (path == null) {
            return null;
        }

        int size = path.size();
        String prevReln = "null";
        int lcsIndex = instance.lcsIndex;
        if (lcsIndex < (size - 1)) {
            prevReln = path.get(lcsIndex + 1);
        }

        for (int i = lcsIndex; i >= 0; i -= 2) {
            int index = Integer.parseInt(path.get(i));
            String word = s.get(index).word().toLowerCase();
            String base = s.get(index).getTag("BASE").toLowerCase();
            char position = getRelativePosition(instance, index);
            if (word.contains("-")) {
                word = word.substring(word.lastIndexOf('-') + 1);
            }
            if (base.contains("-")) {
                base = base.substring(base.lastIndexOf('-') + 1);
            }

            String reln = null;
            if (i > 0 /*&& i < lcsIndex*/) {
                reln = path.get(i - 1);
            }

            if (s.get(index).getTag("DOMAIN") != null) {
                char pos = s.get(index).getTag("POS").charAt(0);
                featureSet.add("REL-" + pos);
                
                featureSet.add(word);
                featureSet.add(base);
                featureSet.add(word + position);

                featureSet.add(word + reln);
                featureSet.add(word + reln + position);
                
                if (index == instance.key) {
                    featureSet.add("Key-" + s.get(index).getTag("DOMAIN"));            
                }
            } else {
                featureSet.add(word + position);
            }

            if (reln != null) {
                featureSet.add(reln);
            }
            if (reln != null && reln.startsWith("prep")) {
                featureSet.add("prep");
            } else if (reln != null && reln.startsWith("-prep")) {
                featureSet.add("-prep");
            }
            featureSet.add(prevReln + reln);
            featureSet.add(prevReln + base + reln);
            prevReln = reln;
        }        

        for (int i = Math.max(instance.entity1 - 4, 0); i <= Math.min(instance.entity1 + 4, s.size() - 1); i++) {
            if (s.get(i).getTag("DOMAIN") != null) {
                String base = s.get(i).getTag("BASE");
                featureSet.add("S-" + base + getRelativePosition(instance, i));
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

        int size = path.size();
        int lcsIndex = instance.lcsIndex;
        String prevReln = "null";
        if (lcsIndex > 0) {
            prevReln = path.get(lcsIndex - 1);
        }

        for (int i = lcsIndex; i < size; i += 2) {
            int index = Integer.parseInt(path.get(i));
            String word = s.get(index).word().toLowerCase();
            String base = s.get(index).getTag("BASE").toLowerCase();
            char position = getRelativePosition(instance, index);
            if (word.contains("-")) {
                word = word.substring(word.lastIndexOf('-') + 1);
            }
            if (base.contains("-")) {
                base = base.substring(base.lastIndexOf('-') + 1);
            }

            String reln = null;
            if (i < (size - 1)) {
                reln = path.get(i + 1);
            }

            if (s.get(index).getTag("DOMAIN") != null) {
                char pos = s.get(index).getTag("POS").charAt(0);
                featureSet.add("REL-" + pos);
                featureSet.add(word);
                featureSet.add(base);
                featureSet.add(word + position);

                featureSet.add(word + reln);
                featureSet.add(word + reln + position);                
                
                if (index == instance.key) {
                    featureSet.add("Key-" + s.get(index).getTag("DOMAIN"));                    
                }
            } else {
                featureSet.add(word + position);
            }

            if (reln != null) {
                featureSet.add(reln);
            }
            if (reln != null && reln.startsWith("prep")) {
                featureSet.add("prep");
            } else if (reln != null && reln.startsWith("-prep")) {
                featureSet.add("-prep");
            }
            featureSet.add(prevReln + reln);
            featureSet.add(prevReln + base + reln);
            prevReln = reln;
        }       

        for (int i = Math.max(instance.entity2 - 4, 0); i <= Math.min(instance.entity2 + 4, s.size() - 1); i++) {
            if (s.get(i).getTag("DOMAIN") != null) {
                String base = s.get(i).getTag("BASE");
                featureSet.add("S-" + base + getRelativePosition(instance, i));
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
        vector.sortByIndices();
        return vector;
        /*HashSet<String> featureSet = new HashSet<String>();
        Sentence s = instance.s;
        List<String> path = instance.path;
        if (path == null) {
            return null;
        }
        for (int i = Math.max(instance.entity1 - 4, 0); i <= Math.min(instance.entity1 + 4, s.size() - 1); i++) {
            if (s.get(i).getTag("DOMAIN") != null) {
                String base = s.get(i).getTag("BASE");
                featureSet.add("S-" + base + getRelativePosition(instance, i));
            }
        }
        for (int i = Math.max(instance.entity2 - 4, 0); i <= Math.min(instance.entity2 + 4, s.size() - 1); i++) {
            if (s.get(i).getTag("DOMAIN") != null) {
                String base = s.get(i).getTag("BASE");
                featureSet.add("S-" + base + getRelativePosition(instance, i));
            }
        }
        featureSet.add(s.get(instance.key).word().toLowerCase());
        featureSet.add(s.get(instance.key).word().toLowerCase() + getRelativePosition(instance, instance.key));
        return featureSet2Vector.getVector(featureSet);*/
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
