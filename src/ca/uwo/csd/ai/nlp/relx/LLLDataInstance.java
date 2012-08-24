/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class LLLDataInstance {
    private static final LPSentReader SENT_READER = new LPSentReader("(/|~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");//new LPSentReader("(~|\\(|\\)|\\||\\+|'|\\d|\\p{L})+|\\S");
    //private static final LPSentReader SENT_READER = new LPSentReader("(~|\\||\\+|-|'|\\d|\\p{L})+|\\S");
    String id;
    Sentence s;
    List<String> words;
    public static class InteractionPair {
        int agent;
        int target;

        public InteractionPair(int agent, int target) {
            this.agent = agent;
            this.target = target;
        }

        public int getAgent() {
            return agent;
        }

        public int getTarget() {
            return target;
        }
        
    }
    List<InteractionPair> interactions;
    String rawData;

    public LLLDataInstance(String rawData) {
        this.interactions = new ArrayList<InteractionPair>();
        this.words = new ArrayList<String>();
        this.rawData = rawData;
        String[] lines = rawData.split("\n");
        for (String line : lines) {
            if (line.startsWith("ID")) {
                String[] tokens = line.split("\t");
                this.id = tokens[1];
            } else if (line.startsWith("sentence")) {
                String[] tokens = line.split("\t");
                this.s = SENT_READER.read(tokens[1]);
            } else if (line.startsWith("genic_interactions")) {
                String[] tokens = line.split("\t");
                for (int i = 1; i < tokens.length; i++) {
                    String token = tokens[i];
                    int whereLB = token.indexOf('(');
                    int whereRB = token.indexOf(')');
                    int whereComma = token.indexOf(',');
                    
                    int agent = Integer.parseInt(token.substring(whereLB+1, whereComma));
                    int target = Integer.parseInt(token.substring(whereComma + 1, whereRB));
                    this.interactions.add(new InteractionPair(agent, target));
                }
            } else if (line.startsWith("words")) {
                String[] tokens = line.split("\t");
                for (int i = 1; i < tokens.length; i++) {
                    String token = tokens[i];
                    int start = token.indexOf('\'');
                    int end = token.lastIndexOf('\'');
                    words.add(token.substring(start + 1, end));
                }
            }
        }
    }

    public List<InteractionPair> getInteractions() {
        return interactions;
    }

    public String getRawData() {
        return rawData;
    }

    public Sentence getS() {
        return s;
    }

    public List<String> getWords() {
        return words;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID\t").append(this.id).append("\n");
        sb.append("sentence\t").append(this.s.toString()).append("\n");
        sb.append("words\t").append(words).append("\n");
        sb.append("genic_interactions");
        for (InteractionPair ip : this.interactions) {
            sb.append("\tgenic_interaction(").append(ip.agent).append(",").append(ip.target).append(")");
        }
        sb.append("\n");
        return sb.toString();
        
    }
}
