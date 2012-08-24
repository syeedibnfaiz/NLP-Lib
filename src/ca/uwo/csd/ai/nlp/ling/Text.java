/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A <code>Text</code> object contains a set of <code>Sentence</code>s.
 * @author Syeed Ibn Faiz
 */
public class Text extends ArrayList<Sentence> {
    private HashMap<String, String> textTagMap;

    public Text() {
        super();
        this.textTagMap = new HashMap<String, String>();
    }

    public Text(List<Sentence> sentences) {
        super(sentences);
        this.textTagMap = new HashMap<String, String>();
    }

    public Text(String text[][]) {
        super();
        if (text != null) {
            for (String row[] : text) {
                add(new Sentence(row));
            }
        } else {
            throw new IllegalArgumentException("Text can not be constructed from a null array.");
        }
    }

    public void setTextTag(String fieldName, String tag) {
        this.textTagMap.put(fieldName, tag);
    }

    public String getTextTag(String fieldName) {
        return this.textTagMap.get(fieldName);
    }


}
