/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author tonatuni
 */
public class TokenSequencePOSTags extends Pipe implements Serializable {

    GeniaTagger tagger;

    public TokenSequencePOSTags() {
        super();
        init();
    }

    private void init() {
        tagger = new GeniaTagger();
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        if (tagger == null) init();
        
        TokenSequence ts = (TokenSequence) carrier.getData();
        Sentence sentence = new Sentence();
        for (Token token : ts) {
            sentence.add(new TokWord(token.getText()));
        }
        sentence = tagger.annotate(sentence);
        if (sentence.size() != ts.size()) System.err.println("Error");
        for (int i = 0; i < sentence.size(); i++) {
            TokWord word = sentence.get(i);
            Token token = ts.get(i);
            token.setFeatureValue(word.getTag("POS"), 1.0);
            //token.setFeatureValue(word.getTag("BASE"), 1.0);
            String chunkTag = word.getTag("CHUNK");
            if (chunkTag.equals("O")) token.setFeatureValue("O", 1.0);
            else token.setFeatureValue(chunkTag.substring(2), 1.0);
        }
        return carrier;
    }
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
    }
}
