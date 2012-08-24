/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class WordNetSimilarity {

    private final static GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2", "BASE"});
    private final static BioDomainAnnotator DOMAIN_ANNOTATOR = new BioDomainAnnotator();

    public static void main(String[] args) throws IOException {
        HashSet<String> set = new HashSet<String>();
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        String ppiBase = "./resource/relation/PPI4/";
        CorpusReader corpusReader = new CorpusReader();
        for (String corpus : corpora) {
            String corpusRoot = ppiBase + corpus;
            List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusRoot);
            addWords(relationInstances, set);
        }
        System.out.println("Total words: " + set.size());
        System.out.println("Total pairs: " + set.size() * (set.size() + 1) / 2.0);
        //writePairs(set, "leftPairs.txt");
        writeWords(set, "words.txt");
    }

    private static void writeWords(HashSet<String> set, String outputFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        ArrayList<String> list = new ArrayList<String>(set);
        set.clear();
        Collections.sort(list);
        for (int i = 0; i < list.size(); i++) {
            writer.write(list.get(i) + "\n");
        }
        writer.close();
    }

    private static void writePairs(HashSet<String> set, String outputFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        ArrayList<String> list = new ArrayList<String>(set);
        set.clear();
        Collections.sort(list);
        for (int i = 0; i < list.size(); i++) {
            char pos1 = list.get(i).charAt(list.get(i).length() - 1);
            String word1 = list.get(i).substring(0, list.get(i).length() - 2);
            for (int j = i; j < list.size(); j++) {
                char pos2 = list.get(j).charAt(list.get(j).length() - 1);
                String word2 = list.get(j).substring(0, list.get(j).length() - 2);
                if (pos1 == pos2) {
                    writer.write(word1);
                    writer.write(" ");
                    writer.write(word2);
                    writer.write("\n");
                }
            }
        }
        writer.close();
    }

    private static void addWords(List<RelationInstance> relationInstances, HashSet<String> set) {
        for (RelationInstance instance : relationInstances) {
            for (int i = 0; i < instance.s.size(); i++) {
                String word = instance.s.get(i).word().toLowerCase();
                if (word.contains("-")) {
                    word = word.substring(word.lastIndexOf('-') + 1);
                }
                if (word.contains("PROTEIN") || word.length() < 3) {
                    continue;
                }
                if (word.matches("[a-z]+")) {
                    char pos = instance.s.get(i).getTag("POS").toLowerCase().charAt(0);
                    if (pos == 'v' || pos == 'n' || pos == 'j' || pos == 'r') {
                        set.add(word);
                    }
                }
            }
        }
    }
}
