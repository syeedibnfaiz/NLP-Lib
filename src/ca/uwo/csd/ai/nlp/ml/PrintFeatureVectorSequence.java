/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PrintFeatureVectorSequence extends Pipe {

    @Override
    public Instance pipe(Instance carrier) {        
        FeatureVectorSequence fvs = (FeatureVectorSequence) carrier.getData();
        System.out.println(carrier.getTarget()+":");
        for (int i = 0; i < fvs.size(); i++) {
            FeatureVector fv = fvs.get(i);
            System.out.println("\t"+i+": "+fv.toString(true));
        }        
        return carrier;
    }
}
