/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.lll;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.relx.LLLDataInstance;
import ca.uwo.csd.ai.nlp.relx.LLLDataReader;
import ca.uwo.csd.ai.nlp.relx.LexSynAnnotator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tonatuni
 */
public class AnalyzeLLL {
    final static GeniaTagger TAGGER = new GeniaTagger();
    //final static ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
    final static CharniakParser PARSER_ANNOTATOR = new CharniakParser();    
    
    public void analyze(String dataFile) {
        List<LLLDataInstance> dataInstances = LLLDataReader.readTrainingData(dataFile);
        LexSynAnnotator lexSynAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");        
        
        for (LLLDataInstance dataInstance : dataInstances) {            
            Sentence s = dataInstance.getS();
            s = correctTokenization(s);
            s = TAGGER.annotate(s);
            s = PARSER_ANNOTATOR.annotate(s);
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), null);            
            s = lexSynAnnotator.annotate(s);
            analyze(dataInstance, s, depGraph);
        }
    }
    
    void analyze(LLLDataInstance dataInstance, Sentence s, SimpleDepGraph depGraph) {        
        Map<Integer, Integer> mapping = getMapping(dataInstance);
        
        for (LLLDataInstance.InteractionPair ip : dataInstance.getInteractions()) {            
            int agent = mapping.get(ip.getAgent());
            if (agent == -1) {
                System.out.println(dataInstance.getWords().get(ip.getAgent()) + " is not found!");
                continue;
            }
            int target = mapping.get(ip.getTarget());
            if (target == -1) {
                System.out.println(dataInstance.getWords().get(ip.getTarget()) + " is not found!");
                continue;
            }
            int agentStart = getEntityStartPos(s, agent);
            int targetStart = getEntityStartPos(s, target);
            
            //don't bother about direction of interaction now
            if (agentStart > targetStart) {
                int tmp = agentStart;
                agentStart = targetStart;
                targetStart = tmp;
            }
            
            String path = depGraph.getPath(agentStart, targetStart);
            if (path != null) {
                if (path.contains("subj")) {
                    System.out.println("subj");
                }
                if (path.contains("rcmod")) {
                    System.out.println("rcmod");
                }
                if (path.contains("partmod")) {
                    System.out.println("partmod");
                }
                if (path.contains("prep")) {
                    System.out.println("prep");
                }
                if (path.contains("dep")) {
                    System.out.println("dep");
                }
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
    Map<Integer, Integer> getMapping(LLLDataInstance dataInstance) {
        Sentence s = dataInstance.getS();
        List<String> words = dataInstance.getWords();
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        int curIndex = 0;
        for (int i = 0; i < words.size(); i++) {
            boolean found = false;
            for (int j = curIndex; j < s.size(); j++) {
                if (s.get(j).word().equals(words.get(i))) {
                    map.put(i, j);
                    curIndex++;
                    found = true;
                    break;
                } else if (s.get(j).word().startsWith(words.get(i)) && s.get(j).word().contains("-")) {
                    map.put(i, j);
                    //curIndex++;
                    found = true;
                    break;
                } else if (s.get(j).word().endsWith(words.get(i)) && s.get(j).word().contains("-")) {
                    map.put(i, j);
                    curIndex++;
                    found = true;
                    break;
                } else if (words.get(i).startsWith(s.get(j).word()) && (j < (s.size() - 1) && words.get(i).contains(s.get(j+1).word()))) {
                    map.put(i, j);
                    curIndex++;
                    found = true;
                    break;
                }
            }
            if (!found) {
                map.put(i, -1);
            }
        }
        return map;
    }
    
    private int getEntityStartPos(Sentence s, int pos) {        
        while (pos > 0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            System.out.println("Returning -1!");
            return -1;
        }
        return pos;
    }
    
    public static void main(String args[]) {
        AnalyzeLLL analyzer = new AnalyzeLLL();
        analyzer.analyze("./resource/relation/LLL/genic_interaction_data_all.txt");
    }
}
