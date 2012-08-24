/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import java.util.ArrayList;

/**
 *
 * @author mibnfaiz
 */
public class SpanList extends ArrayList<Span> {

    public SpanList(String spanList) {
        String tokens[] = spanList.split(";");
        for (String token : tokens) {
            add(new Span(token));
        }
    }
    
}
