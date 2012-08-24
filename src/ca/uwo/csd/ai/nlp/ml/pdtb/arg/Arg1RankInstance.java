/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.types.Instance;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Arg1RankInstance extends Instance {    
    List<Pair<Integer, Integer>> candidates;
    int arg2Line;
    int arg2HeadPos;
    int trueArg1Candidate;
    int connStart;
    int connEnd;
    Document document;
    
    public Arg1RankInstance(Document document, List<Pair<Integer, Integer>> candidates, int arg2Line, int arg2HeadPos, int connStart, int connEnd, int trueArg1Candidate) {
        super(document, null, null, null);        
        this.candidates = candidates;
        this.arg2Line = arg2Line;
        this.arg2HeadPos = arg2HeadPos;
        this.connStart = connStart;
        this.connEnd = connEnd;
        this.trueArg1Candidate = trueArg1Candidate;
        this.document = document;
    }

    public int getArg2HeadPos() {
        return arg2HeadPos;
    }

    public int getArg2Line() {
        return arg2Line;
    }

    public List<Pair<Integer, Integer>> getCandidates() {
        return candidates;
    }

    public int getTrueArg1Candidate() {
        return trueArg1Candidate;
    }

    public int getConnEnd() {
        return connEnd;
    }

    public int getConnStart() {
        return connStart;
    }

    public Document getDocument() {
        return document;
    }
        
}
