/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ExtendedDepGraph;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.relx.LLLDataInstance.InteractionPair;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ExtendedRelex {
    private HashSet<String> restrictionTerms;

    public ExtendedRelex() {
        restrictionTerms = new HashSet<String>();
        List<String> lines = Util.readLines("./resource/relation/LLL/relex_restriction_terms.txt");
        for (String line : lines) {
            int pos = line.indexOf(':');
            String tokens[] = line.substring(pos + 1).split("\\|");
            for (String token : tokens) {
                restrictionTerms.add(token);
            }
        }
    }
    
    public Sentence correctTokenization(Sentence s) {
        Sentence newS = new Sentence();
        for (int i = 0; i < s.size(); i++) {
            String word = s.get(i).word();
            if (word.startsWith("(") && word.endsWith(")")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else if (word.startsWith("(") && !word.equals("(")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1)));
            } else if (word.endsWith(")") && !word.equals(")") && !word.contains("(")) {                
                newS.add(new TokWord(word.substring(0, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else {
                newS.add(new TokWord(word));
            }
        }
        return newS;
    }
    public List<InteractionPair> getInteractions(Sentence s) {
        List<InteractionPair> interactions = new ArrayList<InteractionPair>();
        SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule1(restrictionTerms));
        rules.add(new Rule2(restrictionTerms));
        rules.add(new Rule4());
        List<Relation> relations = new ArrayList<Relation>();
        for (Rule rule : rules) {
            relations.addAll(rule.findRelations(s, depGraph));
        }
        boolean marked[][] = new boolean[s.size()][s.size()];
        for (Relation relation : relations) {
            if (!marked[relation.agent()][relation.target()]) {
                interactions.add(new InteractionPair(relation.agent(), relation.target()));
                marked[relation.agent()][relation.target()] = true;
            }
        }
        return interactions;
    }
    
    String getLexChunk(Sentence s, int pos) {
        
        while (pos >0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            return null;
        }
        int start = pos;
        int end = start;
        pos++;
        while (pos < s.size() && s.get(pos).getTag("LEXE").equals("I")) {
            end = pos;
            pos++;
        }
        return s.toString(start, end);
    }
    
    public void testLLLTrainingset(String filePath) {
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        LexSynAnnotator lexSynAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
        List<LLLDataInstance> dataInstances = LLLDataReader.readTrainingData(filePath);
        for (LLLDataInstance dataInstance : dataInstances) {
            Sentence s = dataInstance.s;
            s = correctTokenization(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            s = lexSynAnnotator.annotate(s);
            
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            
            List<InteractionPair> interactions = getInteractions(s);
            List<InteractionPair> goldenInteractions = dataInstance.interactions;
    
            
            System.out.println("--------------");
            //System.out.println(s.toString("LEX"));
            System.out.println(s.toString("LEXE"));
            System.out.println(s);
            boolean found[] = new boolean[goldenInteractions.size()];
            for (InteractionPair ip : interactions) {
                //String ipAgent = s.get(ip.agent).word();
                String ipAgent = getLexChunk(s, ip.agent);
                //String ipTarget = s.get(ip.target).word();
                String ipTarget = getLexChunk(s, ip.target);
                if (ipAgent.contains("-")) {
                    int pos = ipAgent.indexOf('-');
                    String first = ipAgent.substring(0, pos);
                    if (lexSynAnnotator.contains(first)) ipAgent = first;
                }
                if (ipTarget.contains("-")) {
                    int pos = ipTarget.indexOf('-');
                    String first = ipTarget.substring(0, pos);
                    if (lexSynAnnotator.contains(first)) ipTarget = first;
                }
                boolean correct = false;
                for (int i = 0; i < goldenInteractions.size(); i++) {
                    if (found[i]) continue;
                    InteractionPair gip = goldenInteractions.get(i);
                    String gipAgent = dataInstance.words.get(gip.agent);
                    String gipTarget = dataInstance.words.get(gip.target);
                    if (gipAgent.equals(ipAgent) && gipTarget.equals(ipTarget)) {
                        correct = true;
                        found[i] = true;
                        break;
                    } else if (gipAgent.equals(ipTarget) && gipTarget.equals(ipAgent)) {
                        /*correct = true;
                        found[i] = true;
                        break;*/
                        System.out.println("REVERSE!");
                    }
                }
                if (correct) {
                    System.out.println("TP: " + getLexChunk(s, ip.agent) + "-" + getLexChunk(s, ip.target));
                } else {
                    System.out.println("FP: " + getLexChunk(s, ip.agent) + "-" + getLexChunk(s, ip.target));
                }
            }
            
            for (int i = 0; i < goldenInteractions.size(); i++) {
                InteractionPair ip = goldenInteractions.get(i);
                if (!found[i]) {
                    System.out.println("FN: " + dataInstance.words.get(ip.agent) + "-" + dataInstance.words.get(ip.target));
                    //showGoldenInteractionPath(ip, dataInstance.words.get(ip.agent), dataInstance.words.get(ip.target), s, depGraph);
                }
            }
            System.out.println("--------------");
        }
    }
    public static void testInput() {
        LPSentReader sentReader = new LPSentReader("(/|~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");
        GeniaTagger geniaTagger = new GeniaTagger();
        ParserAnnotator parserAnnotator = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
        Scanner in = new Scanner(System.in);
        String line;
        ExtendedRelex relex = new ExtendedRelex();        
        LexSynAnnotator lexAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            //System.out.println(s);
            s = relex.correctTokenization(s);
            //System.out.println(s);
            s = geniaTagger.annotate(s);
            s = parserAnnotator.annotate(s);
            s = lexAnnotator.annotate(s);
            System.out.println(s.toString("LEXE"));
            s.getParseTree().pennPrint();
            //SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            ExtendedDepGraph depGraph = new ExtendedDepGraph(s.getParseTree(), "CCProcessed");
            System.out.println(depGraph.toString(s.getParseTree()));
            
            List<InteractionPair> interactions = relex.getInteractions(s);
            for (InteractionPair ip : interactions) {
                System.out.println(relex.getLexChunk(s, ip.agent) +"-"+relex.getLexChunk(s, ip.target));
            }
        }    
    }
    public static void main(String args[]) {
        testInput();
        ExtendedRelex relex = new ExtendedRelex();
        relex.testLLLTrainingset("./resource/relation/LLL/genic_interaction_data.txt");
    }
}
