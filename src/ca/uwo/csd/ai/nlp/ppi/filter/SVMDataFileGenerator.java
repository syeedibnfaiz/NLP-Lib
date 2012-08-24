/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import kernel.ds.SparseVector;

/**
 *
 * @author tonatuni
 */
public class SVMDataFileGenerator {

    HashMap<String, Integer> map;
    int count;

    public SVMDataFileGenerator() {
        map = new HashMap<String, Integer>();
    }

    public int getIndex(String key) {
        Integer value = map.get(key);
        if (value != null) {
            return value;
        } else {
            map.put(key, count);
            count++;
            return count - 1;
        }
    }

    public void generateCLData(String ppiCorporaBase, String corpusName, String trainingFile, String testingFile) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        List<RelationInstance> trainingRelationInstances = new ArrayList<RelationInstance>();
        List<RelationInstance> testingRelationInstances = new ArrayList<RelationInstance>();

        CorpusReader corpusReader = new CorpusReader();
        for (int i = 0; i < corpora.length; i++) {
            File corpusDir = new File(ppiCorporaBase, corpora[i]);
            List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusDir.getAbsolutePath());
            if (corpora[i].equals(corpusName)) {
                testingRelationInstances.addAll(relationInstances);
            } else {
                trainingRelationInstances.addAll(relationInstances);
            }
        }
        System.out.println("Before: " + trainingRelationInstances.size());
        trainingRelationInstances = doFiltering(trainingRelationInstances);
        System.out.println("After: " + trainingRelationInstances.size());
        testingRelationInstances = doFiltering(testingRelationInstances);

        writeFile(trainingRelationInstances, new File(trainingFile));
        writeFile(testingRelationInstances, new File(testingFile));
    }

    public void showCLResults(String ppiCorporaBase, String corpusName, String outputFile) {
        CorpusReader corpusReader = new CorpusReader();
        File corpusDir = new File(ppiCorporaBase, corpusName);

        List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusDir.getAbsolutePath());
        int totalPositive = countPositive(relationInstances);
        relationInstances = doFiltering(relationInstances);

        evaluate(relationInstances, outputFile, totalPositive);
    }

    private void writeFile(List<RelationInstance> instances, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (RelationInstance instance : instances) {
            writeInstance(instance, writer);
        }
        writer.close();
    }

    private void writeInstance(RelationInstance instance, BufferedWriter writer) throws IOException {
        int lcs = getLCSIndexInPath(instance);
        //SparseVector vector = makeVector(instance);
        SparseVector vector = makeRBFVector(instance, lcs);
        if (instance.interaction) {
            writer.write("+1 ");
        } else {
            writer.write("-1 ");
        }
        writer.write(vector.toString() + "\n");
    }

    private List<RelationInstance> doFiltering(List<RelationInstance> relationInstances) {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PathFilter());
        filters.add(new DomainFilter());
        filters.add(new NegativeFilter());
        filters.add(new LeftPatternFilter());
        filters.add(new RightPatternFilter());
        filters.add(new MiddlePatternFilter());
        filters.add(new JuxtaposFilter());

        RelationFilterer filterer = new RelationFilterer();
        return filterer.applyFilters(relationInstances, filters);
    }

    private SparseVector makeVector(RelationInstance instance) {
        SparseVector vector = new SparseVector();
        Sentence s = instance.s;
        List<String> path = instance.path;
        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < path.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String word = s.get(index).word().toLowerCase();
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                }
                if (!set.contains(word)) {
                    vector.add(getIndex(word), 1.0);
                    set.add(word);
                }

                if (s.get(index).getTag("DOMAIN") != null) {
                    char pos = s.get(index).getTag("POS").charAt(0);
                    if (!set.contains("REL-" + pos)) {
                        vector.add(getIndex("REL-" + pos), 1.0);
                        set.add("REL-" + pos);
                    }
                }

            } else {
                String reln = path.get(i);
                if (reln.startsWith("prep")) {
                    if (!set.contains("prep")) {
                        vector.add(getIndex("prep"), 1.0);
                        set.add("prep");
                    }
                } else if (reln.startsWith("-prep")) {
                    if (!set.contains("-prep")) {
                        vector.add(getIndex("-prep"), 1.0);
                        set.add("-prep");
                    }
                }

                if (!set.contains(path.get(i))) {
                    vector.add(getIndex(path.get(i)), 1.0);
                    set.add(path.get(i));
                }
            }
        }
        vector.sortByIndices();
        return vector;
    }

    private void evaluate(List<RelationInstance> testingInstances, String outputFile, int totalPositive) {
        List<String> outputs = Util.readLines(outputFile);
        int tp = 0;
        int fp = 0;
        for (int i = 0; i < testingInstances.size(); i++) {
            String output = outputs.get(i);
            if (testingInstances.get(i).interaction) {
                if (output.equals("-1")) {
                    fp++;
                } else {
                    tp++;
                }
            }
        }
        int fn = totalPositive - tp;
        double precision = tp * 1.0 / (tp + fp);
        double recall = tp * 1.0 / (tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);

        System.out.print("TP: " + tp);
        System.out.print(", FP: " + fp);
        System.out.println(", FN: " + fn);
        System.out.println("Precision: " + String.format("%.2f", precision * 100) + "%");
        System.out.println("Recall: " + String.format("%.2f", recall * 100) + "%");
        System.out.println("Fscore: " + String.format("%.2f", fscore * 100) + "%");
    }

    private int countPositive(List<RelationInstance> relationInstances) {
        int count = 0;
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction) {
                count++;
            }
        }
        return count;
    }

    private SparseVector makeRBFVector(RelationInstance instance, int lcsIndex) {
        SparseVector vector = new SparseVector();
        Sentence s = instance.s;
        HashSet<String> set = new HashSet<String>();
        List<String> path = instance.path;
        for (int i = 1; i < lcsIndex; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = "W1=" + s.get(index).getTag("BASE");
                if (!set.contains(lemma)) {
                    vector.add(getIndex(lemma), 1.0);
                    set.add(lemma);
                }
                String pos = "POS1=" + s.get(index).getTag("POS");
                if (!set.contains(pos)) {
                    vector.add(getIndex(pos), 1.0);
                    set.add(pos);
                }
            } else {
                String reln = path.get(i);
                if (!set.contains(reln)) {
                    vector.add(getIndex(reln), 1.0);
                    set.add(reln);
                }
            }
        }
        vector.add(getIndex("H1="), lcsIndex / 2.0);
        vector.add(getIndex("D1="), Math.abs(instance.entity1 - instance.lcs));

        vector.add(getIndex(s.get(lcsIndex).word()), 1.0);

        set = new HashSet<String>();
        for (int i = lcsIndex + 1; i < path.size() - 1; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String lemma = "W2=" + s.get(index).getTag("BASE");
                if (!set.contains(lemma)) {
                    vector.add(getIndex(lemma), 1.0);
                    set.add(lemma);
                }
                String pos = "POS2=" + s.get(index).getTag("POS");
                if (!set.contains(pos)) {
                    vector.add(getIndex(pos), 1.0);
                    set.add(pos);
                }
            } else {
                String reln = path.get(i);
                if (!set.contains(reln)) {
                    vector.add(getIndex(reln), 1.0);
                    set.add(reln);
                }
            }
        }
        vector.add(getIndex("H2="), (path.size() - lcsIndex) / 2.0);
        vector.add(getIndex("D2="), Math.abs(instance.entity2 - instance.lcs));

        int protein = 0;
        int rel = 0;
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                protein++;
            }
            if (s.get(i).getTag("DOMAIN") != null) {
                rel++;
            }
        }

        vector.add(getIndex("C1="), protein);
        vector.add(getIndex("C2="), rel);

        vector.sortByIndices();
        return vector;
    }

    private int getLCSIndexInPath(RelationInstance instance) {
        int lcs = instance.lcs;
        List<String> backBonePath = instance.path;
        String lcsStr = String.valueOf(lcs);
        int dist = 0;
        for (int i = 0; i < backBonePath.size(); i++) {
            if (backBonePath.get(i).equals(lcsStr)) {
                dist = i;
                break;
            }
        }
        return dist;
    }

    public static void main(String[] args) throws IOException {
        SVMDataFileGenerator generator = new SVMDataFileGenerator();
        generator.generateCLData("./resource/relation/PPI4", "BioInfer", "bioinfer_cl.train", "bioinfer_cl.test");
        //generator.showCLResults("./resource/relation/PPI4", "LLL", "lll_cl.out");
    }
}
