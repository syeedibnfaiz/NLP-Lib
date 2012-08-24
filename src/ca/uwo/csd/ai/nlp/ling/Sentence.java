/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling;

import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A <code>Sentence</code> object contains a list of <code>TokWord</code>s
 * that make up a sentence.
 * @author Syeed Ibn Faiz
 */
public class Sentence extends ArrayList<TokWord> {

    private HashSet<String> annotationSet;
    private Tree parseTree;
    private HashMap<String, String> propertyMap;

    /**
     * Create a new empty <code>Sentence</code>.
     */
    public Sentence() {
        super();
        annotationSet = new HashSet<String>();
        propertyMap = new HashMap<String, String>(2);
    }

    /**
     * Create a new <code>Sentence</code>.
     * @param c The collection of <code>TokWord</code>s, typically produced by a tokenizer.
     */
    public Sentence(List<TokWord> c) {
        super(c);        
        annotationSet = new HashSet<String>();
        propertyMap = new HashMap<String, String>(2);
    }

    public Sentence(String tokens[]) {
        this();        
        if (tokens != null) {
            for (String token: tokens) add(new TokWord(token));
        } else throw new IllegalArgumentException("Sentence can not be constructed from a null array.");        
    }        

    public Sentence(Tree root) {
        this();
        List<Tree> leaves = root.getLeaves();
        for (Tree leaf : leaves) {
            Tree parent = leaf.parent(root);
            add(new TokWord(leaf.value(), parent.value()));
        }
        this.parseTree = root;
        annotationSet.add("POS");
        annotationSet.add("PARSED");
    }
    
    public void markAnnotation(String fieldNames[]) {
        for (String fieldName : fieldNames) markAnnotation(fieldName);
    }
    /**
     * Mark that this sentence has been given an annotation
     * @param fieldName
     */
    public void markAnnotation(String fieldName) {
        annotationSet.add(fieldName);
    }    

    /**
     * Determine whether words have got a specific annotation/tag
     * @param fieldName The annotation to check
     * @return true if the words have been annotated on this field
     */
    @Deprecated
    public boolean hasTaggedWords(String fieldName) {
        if (this.isEmpty()) return false;
        return this.get(0).getTag(fieldName) != null;
    }

    /**
     * Return whether this sentence has been annotated by a given field
     * @param fieldName
     * @return
     */
    public boolean isAnnotatedBy(String fieldName) {
        return annotationSet.contains(fieldName);
    }
    
    public ArrayList<String> getWords() {
        ArrayList<String> words = new ArrayList<String>();
        for (TokWord word: this) {
            words.add(word.word());
        }        
        return words;
    }
    /**
     * Returns chunks from this sentence
     * @return null if this sentence is not chunked.
     */
    public ArrayList<Chunk> getChunks() {
        //if (!hasTaggedWords("CHUNK")) return null;
        if (!isAnnotatedBy("CHUNK")) return null;
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        int start = 0;
        int end = this.size();
        while (start < end) {
            if (get(start).getTag("CHUNK").startsWith("B-")) {
                chunks.add(new Chunk(this, start));
            }
            start++;
        }
        return chunks;
    }

    public Tree getParseTree() {
        return parseTree;
    }

    public void setParseTree(Tree parseTree) {
        this.parseTree = parseTree;
    }

    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i != 0) sb.append(" ");
            sb.append(get(i).word());
        }
        return sb.toString();
    }

    /**
     * Return a plain string version of the sentence with some specific tags
     * @param fieldNames The fields for which tags need to be attached
     * @return
     */
    public String toString(String fieldNames[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i != 0) sb.append(" ");
            sb.append(get(i).toString(fieldNames));
        }
        return sb.toString();
    }

    public String toString(String fieldName) {
        return toString(new String[]{fieldName});
    }
    public String toString(int i, int j) {
        if (i < 0 || j < 0 || i >= size() || j >= size()) throw new IndexOutOfBoundsException("invalid range");
        String subPart = "";
        for (int k = i; k <= j; k++) {
            if (k != i) subPart += " ";
            subPart += get(k).word();
        }
        return subPart;
    }
    public String toHTML(String fieldNames[]) {
        String html="<table border=\"1\" cellpadding=\"10\" cellspacing=\"0\"><tr><th>Word</th>";
        for (String fieldName : fieldNames) {
            html += "<th>"+fieldName+"</th>";
        }
        html += "</tr>";
        int count = 1;
        for (TokWord word : this) {
            if (count%2 == 1) {
                html += "<tr style=\"background-color:#E0F0FF;\">";
            } else {
                html += "<tr>";
            }
            html += "<td>"+word.word()+"</td>";
            for (String fieldName : fieldNames) {
                html += "<td>" + word.getTag(fieldName) + "</td>";
            }
            html += "</tr>";
            ++count;
        }
        html += "</table>";
        return html;
    }
    public int length() {
        int len = size() - 1;
        for (int i = 0; i < size(); i++) {
            len += get(i).word().length();
        }
        return len;
    }

    public String[] getTags(String fieldName) {
        String tags[] = new String[size()];
        for (int i = 0; i < size(); i++) {
            tags[i] = get(i).getTag(fieldName);
        }
        return tags;
    }
    
    public String getProperty(String name) {
        return propertyMap.get(name);
    }
    
    public void setProperty(String name, String value) {
        propertyMap.put(name, value);
    }
}
