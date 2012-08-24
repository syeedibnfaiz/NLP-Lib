package ca.uwo.csd.ai.nlp.ml;


import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class ClauseBnTaggedSentence2TokenSequence extends Pipe implements Serializable {

    DiscourseMarkerAnnotator discourseMarkerAnnotator;
    public ClauseBnTaggedSentence2TokenSequence() {
        super(null, new LabelAlphabet());
    }

    private void init() {
        discourseMarkerAnnotator = new DiscourseMarkerAnnotator(false);
    }

    @Override
    public Instance pipe(Instance carrier) {
        if (discourseMarkerAnnotator == null) {
            init();
        }
        Sentence sentence = (Sentence) carrier.getData();
        TokenSequence data = new TokenSequence(sentence.size());
        LabelSequence target = new LabelSequence ((LabelAlphabet)getTargetAlphabet(), sentence.size());

        sentence = discourseMarkerAnnotator.annotate(sentence);
        for (int i = 0; i < sentence.size(); i++) {
            TokWord tokWord = sentence.get(i);            
            String word = tokWord.word();

            Token token = new Token(word);
            token.setFeatureValue("W="+word.toLowerCase(), 1.0);
            token.setFeatureValue(tokWord.getTag("POS"), 1.0);
            token.setFeatureValue(tokWord.getTag("CHUNK"), 1.0);

            String discTag = tokWord.getTag("DIS_CON");
            token.setFeatureValue("DIS_CON=" + discTag.charAt(0), 1.0);

            data.add(token);
            if (tokWord.getTag("CLS_BN") != null) target.add(tokWord.getTag("CLS_BN"));
            else target.add("O");
        }

        carrier.setData(data);
        carrier.setTarget(target);
        carrier.setSource(sentence.toString());
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
