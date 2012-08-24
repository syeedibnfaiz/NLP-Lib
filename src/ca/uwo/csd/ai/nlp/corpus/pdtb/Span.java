/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

/**
 *
 * @author mibnfaiz
 */
public class Span {
    int start;
    int end;

    public Span(String span) {
        if (span.equals("")) {
            this.start = -1;
            this.end = -1;
        }
        else {
            String tokens[] = span.split("\\.\\.");
            this.start = Integer.parseInt(tokens[0]);
            this.end = Integer.parseInt(tokens[1]);
        }
    }

    public int getEnd() {
        return end;
    }

    public int getStart() {
        return start;
    }
    
    @Override
    public String toString() {
        return start+".."+end;
    }
    
}
