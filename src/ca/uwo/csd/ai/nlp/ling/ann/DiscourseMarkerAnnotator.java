/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ml.crf.CRFSeqTagger;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DiscourseMarkerAnnotator implements Annotator {

    private CRFSeqTagger seqTagger;
    private boolean annotateScope;   //annotate sentential/nonsential connective (head only)
    ParserAnnotator parserAnnotator;
    
    public DiscourseMarkerAnnotator() {
        this(true);
    }
    public DiscourseMarkerAnnotator(boolean markScope) {
        //this(".\\resource\\ml\\models\\disc_conn.model", markScope);
        this("resource"+File.separator+"ml"+File.separator+"models"+File.separator+"disc_conn.model", markScope);
    }
    public DiscourseMarkerAnnotator(String path2CRFModel) {
        this(path2CRFModel, true);
    }

    public DiscourseMarkerAnnotator(String path2CRFModel, boolean  markScope) {
        seqTagger = new CRFSeqTagger(path2CRFModel);
        this.annotateScope = markScope;
    }
    
    public Sentence annotate(Sentence s) {
        ArrayList<String> seqTags = seqTagger.doTagging(s);
        for (int i = 0; i < seqTags.size(); i++) {
            s.get(i).setTag("DIS_CON", seqTags.get(i));
        }
        if (annotateScope) {
            for (int i = 0; i < s.size(); i++) {
                if (s.get(i).getTag("DIS_CON").startsWith("B-")) {
                    annotateScope(s, i);
                } else {
                    s.get(i).setTag("DIS_CON_TYP", "O");
                }
            }
        }
        s.markAnnotation(getFieldNames());
        return s;
    }

    private void annotateScope(Sentence s, int start) {
        if (!s.isAnnotatedBy("PARSED")) {
            if (parserAnnotator == null) parserAnnotator = new ParserAnnotator();
            s = parserAnnotator.annotate(s);
        }
        int end = start + 1;
        while (end < s.size() && s.get(end).getTag("DIS_CON").startsWith("I-")) {
            end++;
        }
        end--;
        String connective = s.toString(start, end);
        String commAnc = getCommonAncestor(s, start, end);

        if (isSentential(s, connective, commAnc, start, end)) {
            s.get(start).setTag("DIS_CON_SCP", "S");
        } else {
            s.get(start).setTag("DIS_CON_SCP", "NS");
        }
    }
    private boolean isSentential(Sentence s, String conn, String commAnc, int start, int end) {
        boolean sentential = false;
        String subPat = "IN|SBAR|TO|WRB";
        String coPat = "CC|CONJP";
        String advOrPrepPat = "PP|RB|ADVP|VBG";

        if (commAnc.matches(subPat)) {
            System.out.println("Subordinate rule.");
            s.get(start).setTag("DIS_CON_TYP", "S");
            return true;
        }
        else if (commAnc.matches(coPat) && (end >= 1)) {
            System.out.println("Coordinate rule.");
            s.get(start).setTag("DIS_CON_TYP", "C");
            return true;
        }
        else if (commAnc.matches(advOrPrepPat) && (end >= 5 || conn.matches(".*\\sto|.*\\sof|.*\\son"))) {
            System.out.println("PP/ADVP rule.");
            s.get(start).setTag("DIS_CON_TYP", "P");
            return true;
        }
        return sentential;
    }

    private String getCommonAncestor(Sentence s, int start, int end) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        ArrayList<ArrayList<String>> paths = new ArrayList<ArrayList<String>>();
        for (int i = start; i <= end; i++) {
            ArrayList<String> path = new ArrayList<String>();
            findPath2Root(root, leaves.get(i).parent(root), path);
            paths.add(path);
        }
        //wrong idea, why should the ancestor be in the same distance from all words in the conn?
        String commAnc = null;
        boolean flg = true;
        for (int j = 0; flg ; j++) {
            String val = null;
            for (int i = 0; i < paths.size(); i++) {
                if (j >= paths.get(i).size()) {
                    flg = false;
                    break;
                } else if (val == null) {
                    val = paths.get(i).get(j);
                }
                else if (!val.equals(paths.get(i).get(j))) {
                    flg = false;
                    break;
                }
            }
            if (flg) commAnc = val;
        }
        if (commAnc.matches("S|VP")) {
            commAnc = leaves.get(start).parent(root).value();
        }
        return commAnc;
    }

    private void findPath2Root(Tree root, Tree t, ArrayList<String> path) {        
        path.add(0, t.value());
        if (!t.value().equals("ROOT")) {
            findPath2Root(root, t.parent(root), path);

        }
    }
    public String[] getFieldNames() {
        //DIS_CON       = discourse connective
        //DIS_CON_TYP   = discourse connective type (S/NS)
        if (annotateScope) return new String[]{"DIS_CON", "DIS_CONN_SCP","DIS_CON_TYP"};
        else return new String[]{"DIS_CON"};
    }

    public static void main(String args[]) {
        DiscourseMarkerAnnotator annotator = new DiscourseMarkerAnnotator();
        GeniaTagger tagger = new GeniaTagger();
        LPSentReader sentReader = new LPSentReader();
        Scanner in = new Scanner(System.in);
        String line;
        while ((line = in.nextLine()) != null) {
            Sentence sentence = sentReader.read(line);
            sentence = annotator.annotate(sentence);
            sentence = tagger.annotate(sentence);
            System.out.println(sentence.toString(annotator.getFieldNames()));
        }
    }
}
