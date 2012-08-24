/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.io.CONLL01TextReader;
import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.ChunkInstance;
import ca.uwo.csd.ai.nlp.ml.ClauseBoundaryTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.CRFSeqTagger;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

/**
 * A <code>ClauseBoundaryAnnotator</code> object annotates clausal boundary.
 * Clausal boundaries are looked for on a chunk by chunk basis. Words at the
 * beginning of chunks are potential candidate for clause start boundary..
 * It uses two classifiers, one for each of the boundary detection tasks.
 * Both classifiers must have learned to classify chunks.
 * @author Syeed Ibn Faiz
 */
public class ClauseBoundaryAnnotator implements Annotator, Serializable {
    private Classifier classifierStart;
    private Classifier classifierEnd;
    private boolean useDiscourseMarker; //whether to analyze markers to identify more plausible boundaries
    
    //public static final String defaultPath2StartModel = ".\\resource\\ml\\models\\clause_start_maxent.model";
    //public static final String defaultPath2EndModel = ".\\resource\\ml\\models\\clause_end_maxent.model";
    public static final String defaultPath2StartModel = "resource"+File.separator+"ml"+File.separator+"models"+File.separator+"clause_start_maxent.model";
    public static final String defaultPath2EndModel = "resource"+File.separator+"ml"+File.separator+"models"+File.separator+"clause_end_maxent.model";
    public ClauseBoundaryAnnotator() {
        this(defaultPath2StartModel, defaultPath2EndModel);
    }

    public ClauseBoundaryAnnotator(boolean useDiscourseMarker) {
        this();
        this.useDiscourseMarker = useDiscourseMarker;
    }
    public ClauseBoundaryAnnotator(String path2ClassifierStart) {
        this(path2ClassifierStart, null);
    }

    public ClauseBoundaryAnnotator(String path2ClassifierStart, String path2ClassifierEnd) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2ClassifierStart));
            classifierStart =  (Classifier) ois.readObject();
            if (path2ClassifierEnd != null) {
                ois = new ObjectInputStream(new FileInputStream(path2ClassifierEnd));
                classifierEnd =  (Classifier) ois.readObject();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public Sentence annotate(Sentence s) {
        ArrayList<Chunk> chunks = s.getChunks();                
        Classification classification;
        Labeling labeling;

        //starting boundary must be at chunk beginning
        for (Chunk chunk : chunks) {
            //check clause start boundary
            classification = classifierStart.classify(chunk);
            labeling = classification.getLabeling();                        
            String tag = labeling.labelAtLocation(labeling.getBestIndex()).toString();
            s.get(chunk.getStart()).setTag("CLS_BN_S", tag);            
        }
        //set tags for the rest
        for (TokWord word : s) {
            if (word.getTag("CLS_BN_S") == null ) word.setTag("CLS_BN_S", "X");
        }
        //set tag for the first (because but, and may start a sentence!)
        s.get(0).setTag("CLS_BN_S", "S");

        if (classifierEnd != null) {
            for (Chunk chunk : chunks) {
                //check clause end boundary
                classification = classifierEnd.classify(chunk);
                labeling = classification.getLabeling();
                String tag = labeling.labelAtLocation(labeling.getBestIndex()).toString();
                s.get(chunk.getEnd()).setTag("CLS_BN_E", tag);

            }
            //set tags for the rest
            for (TokWord word : s) {
                if (word.getTag("CLS_BN_E") == null) {
                    word.setTag("CLS_BN_E", "X");
                }
            }
            //set tag for the last token if it is .,! or ?
            //if (s.get(s.size() - 1).word().matches("\\.|\\?|!")) s.get(s.size() - 1).setTag("CLS_BN_E", "E");
            s.get(s.size() - 1).setTag("CLS_BN_E", "E");    //unconditional, required because '.' does not start a chunk and therefor is not a candidate
        }
        
        if (useDiscourseMarker) s = analyzeDiscourseConnectives(s);
        
        s.markAnnotation(getFieldNames());
        return s;
    }

    /**
     * Annotate more words based on analyzing discourse connectives.
     * @param s
     * @return
     */
    private Sentence analyzeDiscourseConnectives(Sentence s) {
        String[] tags = s.getTags("POS");
        String[] discTags = s.getTags("DIS_CON");
        String[] chunkTags = s.getTags("CHUNK");
        
        for (int i = 0; i < s.size(); i++) {
            if (discTags[i].startsWith("B") && tags[i].equals("CC")) {  //if a coordinating conjunction
                for (int j = i - 1; j >= 0; j--) {                      //mark the last word of the preceding chunk with E
                    if (!chunkTags[j].equals("O")) {
                        s.get(j).setTag("CLS_BN_E", "E");
                        break;
                    }
                }
                for (int j = i + 1; j < s.size(); j++) {                 //mark the first word of the following chunk with S
                    if (!chunkTags[j].equals("O")) {
                        s.get(j).setTag("CLS_BN_S", "S");
                        break;
                    }
                }
            }
        }
        return s;
    }
    public String[] getFieldNames() {
        //CLS_BN_S = clause boundary starting
        if (classifierEnd == null) return new String[]{"CLS_BN_S"};
        return new String[]{"CLS_BN_S", "CLS_BN_E"};
    }

    public void showStat() {        
        DiscourseMarkerAnnotator discourseMarkerAnnotator = new DiscourseMarkerAnnotator(false);

        CONLL01TextReader textReader = new CONLL01TextReader();
        Text testingText = textReader.read(new File(".\\resource\\ml\\data\\clause_data\\testa1"));
        InstanceList testingInstanceList = new InstanceList(classifierStart.getInstancePipe());

        for (Sentence sentence : testingText) {
            sentence = discourseMarkerAnnotator.annotate(sentence);
            ArrayList<Chunk> chunks = sentence.getChunks();
            for (Chunk chunk : chunks) {
                testingInstanceList.addThruPipe(new ChunkInstance(chunk));
            }
        }

        System.out.println("Accuracy: " + classifierStart.getAccuracy(testingInstanceList));
        LabelAlphabet labelAlphabet = classifierStart.getLabelAlphabet();
        Iterator iterator = labelAlphabet.iterator();
        while (iterator.hasNext()) {
            Object label = iterator.next();
            double p = classifierStart.getPrecision(testingInstanceList, label);
            double r = classifierStart.getRecall(testingInstanceList, label);
            double f1 = classifierStart.getF1(testingInstanceList, label);

            System.out.println("Precision[" + label + "] = " + p);
            System.out.println("Recall[" + label + "] = " + r);
            System.out.println("F1[" + label + "] = " + f1);
            System.out.println("");
        }
        
    }
    
    public static void main(String args[]) {
        ClauseBoundaryAnnotator annotator = new ClauseBoundaryAnnotator(true);
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
            System.out.println(sentence.toString(new String[]{"POS","CLS_BN_S", "CLS_BN_E"}));
            System.out.println("===============================\n\n");

            int lCount = 0;
            int rCount = 0;
            for (TokWord word : sentence) {
                if (word.getTag("CLS_BN_S").equals("S")) lCount++;
                if (word.getTag("CLS_BN_E").equals("E")) rCount++;
            }
            for (int i = 0; i < (rCount-lCount); i++) System.out.print("(");
            for (TokWord word : sentence) {
                if (word.getTag("CLS_BN_S").equals("S")) System.out.print("(");
                System.out.print(word.toString("DIS_CON"));
                if (word.getTag("CLS_BN_E").equals("E")) System.out.print(")");
                System.out.print(" ");
            }
            for (int i = 0; i < (lCount-rCount); i++) System.out.print(")");
            System.out.println("\n");
        }

        //annotator.showStat();
    }
        // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeObject(this.classifierStart);
        out.writeObject(this.classifierEnd);
        out.writeBoolean(useDiscourseMarker);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        this.classifierStart = (Classifier) in.readObject();
        this.classifierEnd = (Classifier) in.readObject();
        this.useDiscourseMarker = in.readBoolean();
    }
}
