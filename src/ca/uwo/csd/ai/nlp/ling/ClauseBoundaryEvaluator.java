/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling;

import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ml.ChunkInstance;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierEvaluator;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.ling.Word;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ClauseBoundaryEvaluator {
    private Classifier classifier;
    private DiscourseMarkerAnnotator discourseMarkerAnnotator;
    private ClauseBoundaryAnnotator clauseBoundaryAnnotator;
    MyClassifierEvaluator evaluator;
    
    public ClauseBoundaryEvaluator(String path2Classifier) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2Classifier));
            classifier =  (Classifier) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        discourseMarkerAnnotator = new DiscourseMarkerAnnotator(false);
        clauseBoundaryAnnotator = new ClauseBoundaryAnnotator(ClauseBoundaryAnnotator.defaultPath2StartModel);
        evaluator = new MyClassifierEvaluator(classifier);
    }

    public void evaluate(String testingFile, boolean startingBoundary) {

        InstanceList testingInstanceList;
        Pipe pipe = classifier.getInstancePipe();

        Text testingText = new CONLL01TextReader().read(new File(testingFile));
        System.out.println(testingText.size());

        testingInstanceList = new InstanceList(pipe);
        for (Sentence sentence : testingText) {
            sentence = discourseMarkerAnnotator.annotate(sentence);
            if (!startingBoundary) {
                sentence = clauseBoundaryAnnotator.annotate(sentence);
            }
            ArrayList<Chunk> chunks = sentence.getChunks();
            for (Chunk chunk : chunks) {
                testingInstanceList.addThruPipe(new ChunkInstance(chunk));
            }
        }

        evaluator.evaluate(testingInstanceList);
    }

    public static void prepareOfficialTestFile(String testingFile, String fieldName) throws IOException {
        ClauseBoundaryAnnotator annotator = new ClauseBoundaryAnnotator();
        Text testingText = new CONLL01TextReader().read(new File(testingFile));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\clause_data\\bin\\out");        
        for (Sentence sentence : testingText) {
            sentence = annotator.annotate(sentence);
            for (TokWord word : sentence) {
                writer.write(word.word() + " " + word.getTag("POS") + " " + word.getTag("CHUNK") + " " + word.getTag("CLS_BN") + " ");                
                writer.write(word.getTag(fieldName) + "\n");
            }
            writer.write("\n");            
            
        }
        writer.close();        
    }
    public static void main(String args[]) throws IOException {
        //ClauseBoundaryEvaluator evaluator = new ClauseBoundaryEvaluator(".\\resource\\ml\\models\\clause_end_maxent.model");
        //evaluator.evaluate(".\\resource\\ml\\data\\discourse\\biodrb_clause_ann_manual_e.txt", false);
        prepareOfficialTestFile(".\\resource\\ml\\data\\clause_data\\testb2", "CLS_BN_E");
    }
}
