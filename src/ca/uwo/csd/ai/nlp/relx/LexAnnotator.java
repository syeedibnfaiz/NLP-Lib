/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LexAnnotator implements Annotator {
    Set<String> dictionary;    
    int maxLen; //maximum number of words in a dictionary term
    final boolean CASE_SENSITIVE = false;
    
    public LexAnnotator() {
        dictionary = new HashSet<String>();
        maxLen = 0;
    }

    public LexAnnotator(List<String> terms) {
        this();
        if (terms != null) {
            for (String term : terms) {
                if (CASE_SENSITIVE) {
                    dictionary.add(term);
                } else {
                    dictionary.add(term.toLowerCase());
                }
                this.maxLen = Math.max(this.maxLen, term.split("\\s+").length);
            }
        }
    }

    /**
     * Read dictionary terms from a file. Each term should be on a separate line
     * @param termFile 
     */
    public LexAnnotator(File termFile) {
        this(Util.readLines(termFile));                
    }

    public LexAnnotator(String fileName) {
        this (new File(fileName));
    }
    
    public void addTerm(String term) {
        dictionary.add(term);
    }
    
    public void deleteTerm(String term) {
        dictionary.remove(term);
    }
    
    public boolean contains(String term) {
        if (CASE_SENSITIVE) {
            if (dictionary.contains(term)) return true;
        } else {
            if (dictionary.contains(term.toLowerCase())) return true;
        }
        return false;
    }
    
    @Override
    public Sentence annotate(Sentence s) {
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
        return new String[]{"LEXE"};
    }
    
    public void printTerms() {
        for (String term : dictionary) {
            System.out.println(term);
        }
    }
}
