/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.io.SentReader;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import cc.mallet.classify.AdaBoostTrainer;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.DecisionTreeTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntGETrainer;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netlib.util.doubleW;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ClauseCandidateTrainer {
    private GenericTextReader textReader;
    private Text trainingText;
    private Text testingText;
    private MyClassifierTrainer trainer;
    private Pipe pipe;
    private ClauseBoundaryAnnotator annotator;
    int tmpCount = 0;
    public ClauseCandidateTrainer() {
        trainer = new MyClassifierTrainer();
        //openBracketClassifier = new MyClassifierTrainer(new AdaBoostTrainer(new DecisionTreeTrainer(10)));
        textReader = new GenericTextReader("\n\n", "\n", " ", new String[]{"Word","POS", "CHUNK", "CLS_S#", "CLS_E#"});
        annotator = new ClauseBoundaryAnnotator(false);
    }

    public void train(String trainingFile, String testingFile) {
        trainingText = textReader.read(new File(trainingFile));
        System.out.println(trainingText.size());
        //System.out.println(trainingText);

        if (pipe == null) pipe = defaultPipe();

        InstanceList trainingInstanceList = new InstanceList(pipe);

        for (Sentence sentence : trainingText) {
            addSentenceThruPipe(sentence, trainingInstanceList);
        }
        System.out.println("Total number of positive candidate in training data: " + tmpCount);
        
        InstanceList testingInstanceList = null;
        if (testingFile != null) {
            testingText = textReader.read(new File(testingFile));
            System.out.println(testingText.size());
            testingInstanceList = new InstanceList(pipe);
            for (Sentence sentence : testingText) {
                addSentenceThruPipe(sentence, testingInstanceList);
            }
        }
        Classifier classifier = trainer.train(trainingInstanceList, testingInstanceList);
        /*MaxEnt m = (MaxEnt) classifier;
        try {
            m.printExtremeFeatures(new PrintWriter("extreme.log"), 1);
            m.printRank(new PrintWriter("rank.log"));
            //m.print(new PrintWriter("out.log"));
            System.out.println("# of Features: " + m.getAlphabet().size());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ClauseSplitTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    private void addSentenceThruPipe(Sentence sentence, InstanceList instanceList) {
        String clsStartPos[] = new String[sentence.size()];
        String clsEndPos[] = new String[sentence.size()];
        //using the learned classifiers for detecting S and E's
        //alternatively we could also use the information from the dataset
        //annotation. but it may affect final annotation (clauseAnnotator)
        //using dataset gives F-score of 72.6 at the final stage
        //using learned classifiers give F-score of 73.5 at the final stage
        sentence = annotator.annotate(sentence);
        for (int i = 0; i < sentence.size(); i++) {
            clsStartPos[i] = sentence.get(i).getTag("CLS_S#");
            /*if (!clsStartPos[i].equals("0")) {
                sentence.get(i).setTag("CLS_BN_S", "S");
            } else {
                sentence.get(i).setTag("CLS_BN_S", "X");
            }*/

            clsEndPos[i] = sentence.get(i).getTag("CLS_E#");
            /*if (!clsEndPos[i].equals("0")) {
                sentence.get(i).setTag("CLS_BN_E", "E");
            } else {
                sentence.get(i).setTag("CLS_BN_E", "X");
            }*/
        }

        for (int i = 0; i < clsStartPos.length; i++) {
            if (!clsStartPos[i].equals("0")) {
                String sPos[] = clsStartPos[i].split(":");
                for (int j = i; j < clsEndPos.length; j++) {
                    if (!clsEndPos[j].equals("0")) {
                        String ePos[] = clsEndPos[j].split(":");
                        boolean found = false;
                        for (int m = 0; m < ePos.length; m++) {
                            for (int n = 0; n < sPos.length; n++) {
                                if (ePos[m].equals(sPos[n])) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                break;
                            }
                        }                        
                        instanceList.addThruPipe(new ClauseCandidateInstance(sentence, i, j, found));
                        if (found) tmpCount++;
                    }
                }
            }
        }
    }

    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new ClauseCandidate2FeatureVector());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }

    public void save(String fileName) {
        trainer.saveModel(fileName);
    }

    public static void train() {
        ClauseCandidateTrainer trainer = new ClauseCandidateTrainer();
        trainer.train(".\\resource\\ml\\data\\clause_data\\train3_cls_cnddt", ".\\resource\\ml\\data\\clause_data\\testb3_cls_cnddt");
        //trainer.save(".\\resource\\ml\\models\\clause_cnddt_maxent.model");
    }

    public static void test() {
        Classifier classifier = null;
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(".\\resource\\ml\\models\\clause_cnddt_maxent.model"));
            classifier =  (Classifier) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Scanner in = new Scanner(System.in);
        GeniaTagger tagger = new GeniaTagger();
        ClauseBoundaryAnnotator annotator = new ClauseBoundaryAnnotator(false);
        SentReader reader = new LPSentReader();
        Pipe pipe = classifier.getInstancePipe();
        String line;
        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            s = tagger.annotate(s);
            s = annotator.annotate(s);
            String[] sTags = s.getTags("CLS_BN_S");
            String[] eTags = s.getTags("CLS_BN_E");
            for (int i = 0; i < sTags.length; i++) {
                if (sTags[i].equals("S")) {
                    for (int j = i; j < eTags.length; j++) {
                        if (eTags[j].equals("E")) {
                            Instance instance = pipe.instanceFrom(new ClauseCandidateInstance(s, i, j));
                            Classification classification = classifier.classify(instance);
                            Labeling labeling = classification.getLabeling();
                            String tag = labeling.labelAtLocation(labeling.getBestIndex()).toString();
                            System.out.println(s.toString(i, j)+"="+tag);
                            MaxEnt m = (MaxEnt) classifier;
                            double scores[] = new double[2];
                            m.getClassificationScores(instance, scores);
                            System.out.println(scores[0]+":"+scores[1]);                            
                        }
                    }
                }
            }
        }
    }
        
    public static void main(String args[]) {
        train();
        //test();
    }
}
