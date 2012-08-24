/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.SentenceInstance;
import cc.mallet.fst.CRF;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CRFEvaluator {
    CRF crf;
    Pipe pipe;

    public CRFEvaluator(String path2CRFModel) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2CRFModel));
            crf = (CRF) ois.readObject();
            pipe = crf.getInputPipe();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public CRFEvaluator(CRF crf) {
        this.crf = crf;
        this.pipe = crf.getInputPipe();
    }

    public void evaluate(Text text, String tag) {
        Alphabet alphabet = pipe.getTargetAlphabet();
        Object labels[] = new Object[alphabet.size()];
        alphabet.toArray(labels);
        int tp = 0;
        int fp = 0;
        for (Sentence s : text) {
            if (s.length() >= 1024) continue;
            Instance instance = pipe.instanceFrom(new SentenceInstance(s));
            Sequence seq = (Sequence) instance.getData();
            Sequence output = crf.transduce(seq);

            ArrayList<String> tags = new ArrayList<String>();
            for (int i = 0; i < output.size(); i++) {
                //tags.add(output.get(i).toString());
                if (!s.get(i).getTag(tag).equals("O")) {
                    if (s.get(i).getTag(tag).equals(output.get(i).toString())) {
                        tp++;
                    } else {
                        fp++;
                    }
                }
            }
        }
        System.out.println("Precision: " + 1.0*tp/(tp+fp));
    }

    public static void main(String args[]) {
        GenericTextReader textReader = new GenericTextReader("\n", "\\s+", null, null);
        Text text = textReader.read(new File(".\\resource\\ml\\data\\discourse\\biodrb_conn.txt"));
        for (Sentence s : text) {
            for (TokWord tokWord : s) {
                String str = tokWord.word();
                int where = str.lastIndexOf('|');
                String word;
                String tag;
                if (where == -1 || where == 0 || where == (str.length() - 1)) {
                    word = str;
                    tag = "O";
                } else {
                    word = str.substring(0, where);
                    tag = str.substring(where + 1);
                }
                tokWord.setWord(word);
                tokWord.setTag("_CONN", tag);
            }
        }
        CRFEvaluator evaluator = new CRFEvaluator(".\\resource\\ml\\models\\disc_conn.model");
        evaluator.evaluate(text, "_CONN");
    }
}
