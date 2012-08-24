/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling;

import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.TaggedWord;
import java.util.HashMap;

/**
 * A <code>TokWord</code> object contains a word and its various tags,
 * like POS tag, chunk tag, discourse tag, concept tag, clausal tag etc.
 * @author Syeed Ibn Faiz
 */
public class TokWord extends TaggedWord implements HasTag {
    
    private HashMap<String, String> tagMap;

    /**
     * Create a new <code>TokWord</code>.
     * It will have <code>null</code> in its content fields.
     */
    public TokWord() {
        super();
        tagMap = new HashMap<String, String>(4);
    }
    /**
     * Create a new <code>TokWord</code>.     
     * @param word The word, which will have <code>null</code> POS tag.
     */
    public TokWord(String word) {
        super(word);
        this.tagMap = new HashMap<String, String>(4);
    }

    /**
     * Create a new <code>TokWord</code>.
     * @param word The word
     * @param tag The POS tag
     */
    public TokWord(String word, String tag) {
        super(word);
        this.tagMap = new HashMap<String, String>(4);
        this.setTag(tag);
    }

    /**
     * Set tag for a given field, like POS, NER, CONCEPT etc.
     * @param tagName The field name
     * @param tag The tag
     */
    public void setTag(String tagName, String tag) {
        tagMap.put(tagName, tag);
    }

    @Override
    public String tag() {
        return tagMap.get("POS");
    }
    
    @Override
    public final void setTag(String tag) {
        tagMap.put("POS", tag);
    }

    /**
     * Get tag for a given field name.
     * @param fieldName The field name
     * @return The tag assigned for <code>fieldName</code>
     */
    public String getTag(String fieldName) {
        return tagMap.get(fieldName);
    }

    @Override
    public String toString() {
        return word();
    }

    @Override
    public String toString(String fieldName) {
        return toString(new String[]{fieldName});
    }

    public String toString(String fieldNames[]) {
        return toString(fieldNames, "/");
    }
    public String toString(String fieldNames[], String separator) {
        String tmp = word();
        for (String fieldName : fieldNames) {
            tmp += separator + getTag(fieldName);
        }
        return tmp;
    }
}
