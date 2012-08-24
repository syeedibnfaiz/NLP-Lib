/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.pipe.Noop;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.InfoGain;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.util.BitSet;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PruneFeatureVectors {

    public static InstanceList pruneByCount(InstanceList instances, int pruneCount) {
        Alphabet alpha2 = new Alphabet();
        Noop pipe2 = new Noop(alpha2, instances.getTargetAlphabet());
        InstanceList instances2 = new InstanceList(pipe2);
        int numFeatures = instances.getDataAlphabet().size();
        double[] counts = new double[numFeatures];

        for (int ii = 0; ii < instances.size(); ii++) {
            Instance instance = instances.get(ii);
            FeatureVector fv = (FeatureVector) instance.getData();
            fv.addTo(counts);
        }

        BitSet bs = new BitSet(numFeatures);

        for (int fi = 0; fi < numFeatures; fi++) {
            if (counts[fi] > pruneCount) {
                bs.set(fi);
            }
        }

        System.out.println("Pruning " + (numFeatures - bs.cardinality()) + " features out of " + numFeatures
                + "; leaving " + (bs.cardinality()) + " features.");

        FeatureSelection fs = new FeatureSelection(instances.getDataAlphabet(), bs);

        for (int ii = 0; ii < instances.size(); ii++) {

            Instance instance = instances.get(ii);
            FeatureVector fv = (FeatureVector) instance.getData();
            FeatureVector fv2 = FeatureVector.newFeatureVector(fv, alpha2, fs);

            instances2.add(new Instance(fv2, instance.getTarget(), instance.getName(), instance.getSource()),
                    instances.getInstanceWeight(ii));
            instance.unLock();
            instance.setData(null); // So it can be freed by the garbage collector
        }
        instances = instances2;
        
        return instances;
    }
    
    public static InstanceList pruneByInfoGain(InstanceList instances, int pruneInfoGain) {
        Alphabet alpha2 = new Alphabet();
        Noop pipe2 = new Noop(alpha2, instances.getTargetAlphabet());
        InstanceList instances2 = new InstanceList(pipe2);
        InfoGain ig = new InfoGain(instances);
        FeatureSelection fs = new FeatureSelection(ig, pruneInfoGain);
        for (int ii = 0; ii < instances.size(); ii++) {
            Instance instance = instances.get(ii);
            FeatureVector fv = (FeatureVector) instance.getData();
            FeatureVector fv2 = FeatureVector.newFeatureVector(fv, alpha2, fs);
            instance.unLock();
            instance.setData(null); // So it can be freed by the garbage collector
            instances2.add(pipe2.instanceFrom(new Instance(fv2, instance.getTarget(), instance.getName(), instance.getSource())),
                    instances.getInstanceWeight(ii));
        }
        instances = instances2;
        
        return instances;
    }
}
