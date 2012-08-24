/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ml.crf.CRFSeqTagger;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class ClauseBoundaryCRFAnnotator implements Annotator {
    private CRFSeqTagger seqTagger;

    public ClauseBoundaryCRFAnnotator() {
        this(".\\resource\\ml\\models\\clause_start.model");
    }

    public ClauseBoundaryCRFAnnotator(String path2CRFModel) {
        seqTagger = new CRFSeqTagger(path2CRFModel);
    }

    
    public Sentence annotate(Sentence s) {
        //need to provide the CLS_BN tag even for testing. TODO: check this in future. need to train again with that check
        ArrayList<String> seqTags = seqTagger.doTagging(s);
        for (int i = 0; i < seqTags.size(); i++) {
            s.get(i).setTag("CLS_BN", seqTags.get(i));
        }
        return s;
    }

    public String[] getFieldNames() {
        return new String[]{"CLS_BN"};
    }
    
    public static void main(String args[]) {
        ClauseBoundaryCRFAnnotator annotator = new ClauseBoundaryCRFAnnotator();
        DiscourseMarkerAnnotator discourseMarkerAnnotator = new DiscourseMarkerAnnotator(false);
        GeniaTagger tagger = new GeniaTagger();
        
        LPSentReader sentReader = new LPSentReader();        
        Scanner in = new Scanner(System.in);
        String line;
        
        while ((line = in.nextLine()) != null) {
            Sentence sentence = sentReader.read(line);
            sentence = tagger.annotate(sentence);
            sentence = discourseMarkerAnnotator.annotate(sentence);
            sentence = annotator.annotate(sentence);            
            System.out.println(sentence.toString(new String[]{"POS","CLS_BN"}));
        }
    }

}
