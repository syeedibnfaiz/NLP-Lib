/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.ClauseCandidateInstance;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.Stack;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ClauseAnnotator implements Annotator, Serializable  {
    
    private Classifier classifier;    
    
    public ClauseAnnotator() {
        //this(".\\resource\\ml\\models\\clause_cnddt_maxent.model");
        this("resource"+File.separator+"ml"+File.separator+"models"+File.separator+"clause_cnddt_maxent.model");
    }

    public ClauseAnnotator(String path2Classifier) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(path2Classifier));
            classifier =  (Classifier) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Annotates all clauses as recommended by the classifier. Does not resolve
     * clause inconsistency issues.
     * @param s
     * @return
     */
    public Sentence annotatAllCandidates(Sentence s) {
        Pipe pipe = classifier.getInstancePipe();
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
                        if (tag.equals("true")) {
                            TokWord w1 = s.get(i);
                            TokWord w2 = s.get(j);
                            if (w1.getTag("CLS_ANN") == null) {
                                w1.setTag("CLS_ANN", "(S");
                            } else {
                                if (w1.getTag("CLS_ANN").contains("*")) {
                                    w1.setTag("CLS_ANN", "(S"+w1.getTag("CLS_ANN"));
                                } else {
                                    w1.setTag("CLS_ANN", w1.getTag("CLS_ANN") + "(S");
                                }
                            }

                            if (w2.getTag("CLS_ANN") == null) {
                                w2.setTag("CLS_ANN", "*S)");
                            } else {
                                if (!w2.getTag("CLS_ANN").contains("*")) {
                                    w2.setTag("CLS_ANN", w2.getTag("CLS_ANN") + "*S)");
                                } else {
                                    w2.setTag("CLS_ANN", w2.getTag("CLS_ANN") + "S)");
                                }
                            }
                        }
                    }
                }
                if (s.get(i).getTag("CLS_ANN") != null && !s.get(i).getTag("CLS_ANN").contains("*")) {
                    s.get(i).setTag("CLS_ANN", s.get(i).getTag("CLS_ANN") + "*");
                }
            }
        }
        for (TokWord word : s) {
            if (word.getTag("CLS_ANN") == null) {
                word.setTag("CLS_ANN", "*");
            }
        }

        s.markAnnotation(this.getFieldNames());
        return s;
    }

    /**
     * Annotates clause candidates after resolving conflicts. Gives priority to
     * candidate with higher score. Requires a MaxEnt classifier.
     * @param s
     * @return
     */
    public Sentence annotateWithResolving(Sentence s) {
        Pipe pipe = classifier.getInstancePipe();
        MaxEnt maxEnt = (MaxEnt) classifier;
        String[] sTags = s.getTags("CLS_BN_S");
        String[] eTags = s.getTags("CLS_BN_E");
        ArrayList<Candidate> candidates = new ArrayList<Candidate>();

        //pick the candidates recommended by the classifier
        for (int i = 0; i < sTags.length; i++) {
            if (sTags[i].equals("S")) {                
                double maxFalseScore = 0.0;
                int f = -1;
                boolean found = false;
                for (int j = i; j < eTags.length; j++) {
                    if (eTags[j].equals("E")) {                        
                        Instance instance = pipe.instanceFrom(new ClauseCandidateInstance(s, i, j));
                        Classification classification = classifier.classify(instance);
                        Labeling labeling = classification.getLabeling();
                        String tag = labeling.labelAtLocation(labeling.getBestIndex()).toString();
                        //System.out.println(s.toString(i, j)+"="+tag);
                        if (tag.equals("true")) {
                            double score[] = new double[2];
                            maxEnt.getClassificationScores(instance, score);
                            candidates.add(new Candidate(i, j, score[1]));
                            found = true;
                        } else if (!found) {
                            double score[] = new double[2];
                            maxEnt.getClassificationScores(instance, score);
                            if (score[1] > maxFalseScore) {
                                maxFalseScore = score[1];
                                f = j;
                            }
                        }
                    }
                }
                if (!found && f != -1) {
                    //System.out.println("Here!!");
                    candidates.add(new Candidate(i, f, maxFalseScore));
                }
            }
        }
        //resolve inconsistency issues
        Collections.sort(candidates);
        boolean leave[] = new boolean[candidates.size()];
        boolean candidate[][] = new boolean[s.size()][s.size()];
        for (int i = 0; i < candidates.size(); i++) {
            if (!leave[i]) {
                for (int j = i + 1; j < candidates.size(); j++) {
                    if (!leave[j] && !candidates.get(i).isConsistentWith(candidates.get(j))) {
                        leave[j] = true;                        
                    }
                }
                candidate[candidates.get(i).getStart()][candidates.get(i).getEnd()] = true;                
            }
        }
        //set annotation
        for (int i = 0; i < s.size(); i++) {
            for (int j = i; j < s.size(); j++) {
                if (candidate[i][j]) {

                    TokWord w1 = s.get(i);
                    TokWord w2 = s.get(j);
                    if (w1.getTag("CLS_ANN") == null) {
                        w1.setTag("CLS_ANN", "(S");
                    } else {
                        if (w1.getTag("CLS_ANN").contains("*")) {
                            w1.setTag("CLS_ANN", "(S" + w1.getTag("CLS_ANN"));
                        } else {
                            w1.setTag("CLS_ANN", w1.getTag("CLS_ANN") + "(S");
                        }
                    }

                    if (w2.getTag("CLS_ANN") == null) {
                        w2.setTag("CLS_ANN", "*S)");
                    } else {
                        if (!w2.getTag("CLS_ANN").contains("*")) {
                            w2.setTag("CLS_ANN", w2.getTag("CLS_ANN") + "*S)");
                        } else {
                            w2.setTag("CLS_ANN", w2.getTag("CLS_ANN") + "S)");
                        }
                    }

                }
            }
        }
        for (TokWord word: s) {
            if (word.getTag("CLS_ANN") != null && !word.getTag("CLS_ANN").contains("*")) {
                word.setTag("CLS_ANN", word.getTag("CLS_ANN") + "*");
            } else if (word.getTag("CLS_ANN") == null) {
                word.setTag("CLS_ANN", "*");
            }
        }

        s.markAnnotation(getFieldNames());
        return s;
    }
    
    public Sentence annotate(Sentence s) {
        if (classifier instanceof MaxEnt) return annotateClauseNumbers(annotateWithResolving(s));
        return annotateClauseNumbers(annotatAllCandidates(s));
    }
    private Sentence annotateClauseNumbers(Sentence s) {
        int startCount = 1;        
        Stack<Integer> stack = new Stack<Integer>();

        for (TokWord word : s) {
            String clsAnn = word.getTag("CLS_ANN");
            if (clsAnn.equals("*")) {                
                word.setTag("CLS_S#", "0");
                word.setTag("CLS_E#", "0");
            } else {
                String sTag = "";
                String eTag = "";
                for (char ch : clsAnn.toCharArray()) {
                    if (ch == '(') {
                        sTag += startCount + ":";
                        stack.push(startCount);
                        startCount++;
                    } else if (ch == ')') {
                        if (!stack.empty()) {
                            eTag += stack.pop() + ":";
                        }
                    }
                }
                if (!sTag.equals("")) {
                    word.setTag("CLS_S#", sTag);
                } else {
                    word.setTag("CLS_S#", "0");
                }
                if (!eTag.equals("")) {
                    word.setTag("CLS_E#", eTag);
                } else {
                    word.setTag("CLS_E#", "0");
                }                
            }            
        }//end for tokWord
        return s;
    }
    public String[] getFieldNames() {
        return new String[]{"CLS_ANN", "CLS_S#", "CLS_E#"};
    }

    private static int count(String s, char ch) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }
    /**
     * Generates a file that can be tested with the official perl script of
     * CONLL 2001 shared task (Clause Identification).
     * @throws IOException
     */
    public static void evaluate() throws IOException {
        ClauseAnnotator annotator = new ClauseAnnotator();
        ClauseBoundaryAnnotator boundaryAnnotator = new ClauseBoundaryAnnotator();
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", " ", new String[]{"word","POS","CHUNK","CLS_ANN_ORG"});
        Text text = textReader.read(new File(".\\resource\\ml\\data\\clause_data\\testb3"));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\clause_data\\testb3_result");
        int correct = 0;
        int wrong = 0;
        int miss = 0;
        for (Sentence s : text) {
            s = boundaryAnnotator.annotate(s);
            s = annotator.annotate(s);
            for (TokWord word : s) {
                //writer.write(word.word()+" "+word.getTag("POS")+" "+word.getTag("CHUNK")+" "+word.getTag("CLS_ANN_ORG")+" "+word.getTag("CLS_ANN"));
                writer.write(word.getTag("CLS_ANN_ORG")+" "+word.getTag("CLS_ANN"));
                writer.write("\n");
                String org = word.getTag("CLS_ANN_ORG");
                String ann = word.getTag("CLS_ANN");
                int orgL = count(org, '(');
                int orgR = count(org, ')');
                int annL = count(ann, '(');
                int annR = count(ann, ')');
                if (orgL < annL) wrong += (annL - orgL);
                else if (orgL > annL) miss += (orgL - annL);
                correct += (annL < orgL)?annL:orgL;

                if (orgR < annR) wrong += (annR - orgR);
                else if (orgR > annR) miss += (orgR - annR);
                correct += (annR < orgR)?annR:orgR;

                if (org.equals("*") && ann.equals("*")) correct++;
            }
            writer.write("\n");
        }
        System.out.println("Precision: " + (100.0*correct)/(correct+wrong));
        System.out.println("Recall: " + (100.0*correct)/(correct+miss));
        
        writer.close();
    }
    
    public static void main(String args[]) throws IOException {
        /*GeniaTagger tagger = new GeniaTagger();
        ClauseAnnotator annotator = new ClauseAnnotator();
        ClauseBoundaryAnnotator boundaryAnnotator = new ClauseBoundaryAnnotator(false);
        DiscourseMarkerAnnotator markerAnnotator = new DiscourseMarkerAnnotator(false);
        Scanner in = new Scanner(System.in);
        LPSentReader reader = new LPSentReader();
        String line;
        while ((line = in.nextLine()) != null) {
            Sentence s = reader.read(line);
            s = tagger.annotate(s);            
            s = boundaryAnnotator.annotate(s);
            s = annotator.annotate(s);
            s = markerAnnotator.annotate(s);
            System.out.println(s.toString(new String[]{"CLS_BN_S","CLS_BN_E","CLS_ANN"}));
        }*/
        evaluate();

    }
        // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeObject(this.classifier);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        classifier = (Classifier) in.readObject();
    }
}

class Candidate implements Comparable<Candidate> {

    int start, end;
    double score;

    public Candidate(int start, int end, double score) {
        this.start = start;
        this.end = end;
        this.score = score;
    }

    public int compareTo(Candidate o) {
        if (this.score < o.score) {
            return 1;
        } else if (this.score > o.score) {
            return -1;
        } else {
            return 0;
        }
    }

    public boolean isConsistentWith(Candidate o) {
        if (o.start > this.start && o.start < this.end) {
            if (o.end > this.end) {
                return false;
            }
        } else if (o.end > this.start && o.end < this.end) {
            if (o.start < this.start) {
                return false;
            }
        }
        return true;
    }

    public int getEnd() {
        return end;
    }

    public double getScore() {
        return score;
    }

    public int getStart() {
        return start;
    }
    @Override
    public String toString() {
        return start + ":" + end + ":" + score;
    }

}
