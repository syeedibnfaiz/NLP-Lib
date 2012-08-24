/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Chunk;
import cc.mallet.types.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ChunkInstance extends Instance {

    public ChunkInstance(Chunk chunk) {
        super(chunk, null, null, chunk.toString(true));
    }


}
