/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class PrepareBiomedicalTerms {
    public static void main(String[] args) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        List<String> lines = Util.readLines("./resource/relation/Rel_Word_TAG.txt");
        process(map, lines, '|');
        lines = Util.readLines("./resource/relation/Rel_Pos.txt");
        process(map, lines, '_');
        lines = Util.readLines("./resource/relation/RelWord.txt");
        process(map, lines, '_');
        write(map, "./resource/relation/biomedical_terms_tag.txt");
    }
    
    private static void write(HashMap<String, String> map, String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
        List<String> keyList = new ArrayList<String>(map.keySet());
        Collections.sort(keyList);
        for (String term : keyList) {
            writer.write(term + "|" + map.get(term) + "\n");
        }
        writer.close();
    }
    
    private static void process(HashMap<String, String> map, List<String> lines, char delimiter) {
        for (String word : lines) {
            int index = word.lastIndexOf(delimiter);
            if (index == -1) {
                String prevTag = map.get(word);
                if (prevTag == null) {
                    map.put(word, "*");
                }
            } else {
                String term = word.substring(0, index);
                String tag = word.substring(index + 1);
                map.put(term, tag);
            }
        }
    }
}
