/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LexSynAnnotator extends LexAnnotator {
    Map<String, String> synonymMap;
    
    public LexSynAnnotator(List<String> terms) {
        super();
        synonymMap = new HashMap<String, String>();
        
        if (terms != null) {
            for (String term : terms) {
                if (term.startsWith("%") || term.length() == 0) {   //skip comment or empty lines
                    continue;
                }
                String[] tokens = term.split("\t");
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    if (CASE_SENSITIVE) {
                        dictionary.add(token);
                    } else {
                        dictionary.add(token.toLowerCase());
                    }
                    this.maxLen = Math.max(this.maxLen, token.split("\\s+").length);
                }
                if (tokens.length > 1) {
                    for (int i = 1; i < tokens.length; i++) {
                        if (CASE_SENSITIVE) {
                            synonymMap.put(tokens[i], tokens[0]);
                        } else {
                            synonymMap.put(tokens[i].toLowerCase(), tokens[0]);
                        }
                    }
                }

            }
        }
    }

    public LexSynAnnotator(String fileName) {
        this(new File(fileName));
    }

    public LexSynAnnotator(File termFile) {
        this(Util.readLines(termFile));
    }
    
    public String hasSynonym(String key) {
        if (CASE_SENSITIVE) {
            return synonymMap.get(key);
        } else {
            return synonymMap.get(key.toLowerCase());
        }
    }
    
    public static void main(String args[]) {
        LexSynAnnotator lexSynAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
        lexSynAnnotator.printTerms();
    }
}
