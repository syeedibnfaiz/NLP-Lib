/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PrintFeatureVector extends Pipe {

    @Override
    public Instance pipe(Instance carrier) {        
        FeatureVector fv = (FeatureVector) carrier.getData();
        System.out.println(carrier.getTarget()+" :"+fv.toString(true));
        return carrier;
    }
}
