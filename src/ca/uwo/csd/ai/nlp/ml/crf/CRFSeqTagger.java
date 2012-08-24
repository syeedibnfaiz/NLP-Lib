/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml.crf;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ml.SentenceInstance;
import cc.mallet.fst.CRF;
import cc.mallet.fst.PerClassAccuracyEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CRFSeqTagger {

    private CRF crf;
    private Pipe pipe;

    public CRFSeqTagger(String path2CRFModel) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2CRFModel));
            crf = (CRF) ois.readObject();
            pipe = crf.getInputPipe();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public CRFSeqTagger(CRF crf) {
        this.crf = crf;
        this.pipe = crf.getInputPipe();
    }


    public ArrayList<String> doTagging(Sentence sentence) {
        Instance input = pipe.instanceFrom(new SentenceInstance(sentence));
        Sequence seq = (Sequence) input.getData();
        Sequence output = crf.transduce(seq);
        
        ArrayList<String> tags = new ArrayList<String>();
        for (int i = 0; i < output.size(); i++) {
            tags.add(output.get(i).toString());
        }
        return tags;
    }
    
    public static void main(String args[]) {
        CRFSeqTagger seqTagger = new CRFSeqTagger(".//resource//ml//models//crf.model");
        LPSentReader sentReader = new LPSentReader();
        
        Scanner in = new Scanner(System.in);
        String line;
        while ((line = in.nextLine()) != null) {
            Sentence sentence = sentReader.read(line);
            ArrayList<String> tags = seqTagger.doTagging(sentence);
            System.out.println(sentence);
            System.out.println(tags);
        }
    }
}
