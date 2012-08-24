/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ExtendedTrial extends Trial{
    Classifier classifier;
    InstanceList instanceList;
    public ExtendedTrial(Classifier c, InstanceList ilist) {
        super(c, ilist);
        classifier = c;
        instanceList = ilist;
    }

    public double getPrecision() {
        LabelAlphabet labels = classifier.getLabelAlphabet();
        Iterator iterator = labels.iterator();
        int numCorrect = 0;
	int numInstances = 0;

        while (iterator.hasNext()) {
            Object label = iterator.next();

            int index;
            if (label instanceof Labeling) {
                index = ((Labeling) label).getBestIndex();
            } else {
                index = labels.lookupIndex(label, false);
            }

            int trueLabel, classLabel;
            for (int i = 0; i < this.size(); i++) {
                trueLabel = this.get(i).getInstance().getLabeling().getBestIndex();
                classLabel = this.get(i).getLabeling().getBestIndex();
                if (classLabel == index) {
                    numInstances++;
                    if (trueLabel == index) {
                        numCorrect++;
                    }
                }
            }
        }
        if (numInstances == 0) {
            assert (numCorrect == 0);
            return 1;
        }

        return ((double) numCorrect / (double) numInstances);
    }

    public double getRecall() {
        LabelAlphabet labels = classifier.getLabelAlphabet();
        Iterator iterator = labels.iterator();
        int numCorrect = 0;
	int numInstances = 0;

        while (iterator.hasNext()) {
            Object label = iterator.next();

            int index;
            if (label instanceof Labeling) {
                index = ((Labeling) label).getBestIndex();
            } else {
                index = labels.lookupIndex(label, false);
            }

            int trueLabel, classLabel;
            for (int i = 0; i < this.size(); i++) {
                trueLabel = this.get(i).getInstance().getLabeling().getBestIndex();
                classLabel = this.get(i).getLabeling().getBestIndex();
                if (trueLabel == index) {
                    numInstances++;
                    if (classLabel == index) {
                        numCorrect++;
                    }
                }
            }
        }
        if (numInstances == 0) {
            assert (numCorrect == 0);
            return 1;
        }

        return ((double) numCorrect / (double) numInstances);
    }

    public double getF1() {
        double precision = getPrecision();
        double recall = getRecall();

        // gdruck@cs.umass.edu
        // When both precision and recall are 0, F1 is 0.
        if (precision == 0.0 && recall == 0.0) {
            return 0;
        }

        return 2 * precision * recall / (precision + recall);
    }

    public double getFallOut() {
        LabelAlphabet labels = classifier.getLabelAlphabet();
        Iterator iterator = labels.iterator();
        int numCorrect = 0;
	int numInstances = 0;

        while (iterator.hasNext()) {
            Object label = iterator.next();

            int index;
            if (label instanceof Labeling) {
                index = ((Labeling) label).getBestIndex();
            } else {
                index = labels.lookupIndex(label, false);
            }

            int trueLabel, classLabel;
            for (int i = 0; i < this.size(); i++) {
                trueLabel = this.get(i).getInstance().getLabeling().getBestIndex();
                classLabel = this.get(i).getLabeling().getBestIndex();
                if (trueLabel != index) {
                    numInstances++;
                    if (classLabel == index) {
                        numCorrect++;
                    }
                }
            }
        }
        if (numInstances == 0) {
            assert (numCorrect == 0);
            return 1;
        }

        return ((double) numCorrect / (double) numInstances);
    }

    public void showErrors() {
        HashMap<String, Pair> connMap = new HashMap<String, Pair>();
        for (Instance instance : instanceList) {
            Classification classification = classifier.classify(instance);
            if (!classification.bestLabelIsCorrect()) {
                String s = instance.toString();
                if (!connMap.containsKey(s)) {
                    connMap.put(s, new Pair(s, 1));
                } else {
                    Pair p = connMap.get(s);
                    p.setCount(p.getCount() + 1);
                }
            }
        }
        Set<Entry<String, Pair>> entrySet = connMap.entrySet();
        List<Pair> list = new ArrayList<Pair>();
        for (Entry e : entrySet) {
            list.add((Pair) e.getValue());
        }
        Collections.sort(list);
        for (Pair p : list) {
            System.out.println(p);
        }
    }
}

class Pair implements Comparable<Pair>{
    String s;
    int count;

    public Pair(String s, int count) {
        this.s = s;
        this.count = count;
    }

    public int compareTo(Pair o) {
        if (count > o.count) return -1;
        else if (count < o.count) return 1;
        else return 0;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return s + "-" + count;
    }
}
