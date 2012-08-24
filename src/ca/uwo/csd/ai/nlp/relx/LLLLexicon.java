/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LLLLexicon implements Annotator {
    HashSet<String> dictionary;
    HashMap<String, String> synonymMap;
    int maxLen;
    
    public LLLLexicon(String lexiconFile) {
        dictionary = new HashSet<String>();
        synonymMap = new HashMap<String, String>();
        int max = 0;
        List<String> lines = Util.readLines(lexiconFile);
        for (String line : lines) {
            if (line.startsWith("%") || line.length() == 0) {
                continue;
            }
            String[] tokens = line.split("\t");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                /*token = token.replace("(", " ( ");
                token = token.replace(")", " ) ");
                token = token.replaceAll("\\s+", " ");
                tokens[i] = token;*/
                dictionary.add(token.toLowerCase());
                
                max = Math.max(max, token.split("\\s+").length);                
            }
            if (tokens.length > 1) {
                for (int i = 1; i < tokens.length; i++) {
                    synonymMap.put(tokens[i].toLowerCase(), tokens[0]);
                }
            }
        }
        this.maxLen = max;
    }
    
    public boolean contains(String s) {
        return dictionary.contains(s.toLowerCase());
    }

    public String hasSynonym(String key) {
        return synonymMap.get(key.toLowerCase());
    }
    @Override
    public Sentence annotate(Sentence s) {
        for (TokWord word : s) {
            if (contains(word.word())) {
                word.setTag("LEX", "YES");
            } else {
                if (word.word().contains("-")) {
                    int pos = word.word().indexOf('-');
                    String first = word.word().substring(0, pos);
                    if (contains(first)) {
                        word.setTag("LEX", "YES");
                    } else {
                        word.setTag("LEX", "NO");
                    }
                } else {
                    word.setTag("LEX", "NO");
                }
            }
        }
        for (int i = 0; i < s.size(); i++) {
            int j = Math.min(s.size()-1, i + this.maxLen - 1);
            while (j >= i) {
                String phrase = s.toString(i, j);
                if (contains(phrase)) {
                    break;
                } else if (s.get(j).word().contains("-")) {
                    int pos = phrase.lastIndexOf('-');
                    phrase = phrase.substring(0, pos);
                    if (contains(phrase)) break;
                }
                j--;
            }
            
            if (j >= i) {                
                for (int k = i; k <= j; k++) {
                    if (k == i) {
                        s.get(k).setTag("LEXE", "B");
                    } else {
                        s.get(k).setTag("LEXE", "I");
                    }
                }
                i = j;
            } else {
                s.get(i).setTag("LEXE", "O");
            }
        }
        s.markAnnotation(getFieldNames());
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[] {"LEX"};
    }
    
    public static void main(String args[]) {
        LLLLexicon lexicon = new LLLLexicon("./resource/relation/LLL/dictionary_data.txt");
        System.out.println(lexicon.maxLen);
        String s = "sigma(E)";
        String[] tokens = s.split("\\s+|\\(|\\)");
        for (String token : tokens) {
            System.out.println(token);
        }
    }
}
