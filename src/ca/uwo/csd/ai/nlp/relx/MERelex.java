/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ml.PrintFeatureVector;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.InstanceList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class MERelex {
    final static GeniaTagger TAGGER = new GeniaTagger();
    //final static ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
    final static CharniakParser PARSER_ANNOTATOR = new CharniakParser();    
    MyClassifierTrainer trainer;
    Pipe pipe;

    public MERelex() {
        trainer = new MyClassifierTrainer();
    }
    
    public void train(String dataFile) {
        List<LLLDataInstance> dataInstances = LLLDataReader.readTrainingData(dataFile);
        LexSynAnnotator lexSynAnnotator = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
        InstanceList instanceList = new InstanceList(defaultPipe());
        
        for (LLLDataInstance dataInstance : dataInstances) {            
            Sentence s = dataInstance.s;
            s = correctTokenization(s);
            s = TAGGER.annotate(s);
            s = PARSER_ANNOTATOR.annotate(s);
            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), null);
            SimpleDepGraph depGraphCC = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
            s = lexSynAnnotator.annotate(s);
            addThroughPipe(instanceList, dataInstance, s, depGraph, depGraphCC);
        }        
        
        /*InstanceList[] instanceLists =
                instanceList.splitInOrder(new double[]{0.6, 0.4});
        Classifier classifier = trainer.train(instanceLists[0], instanceLists[1]);*/
        NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, instanceList, 5);
    }
    
    void addThroughPipe(InstanceList instanceList, LLLDataInstance dataInstance, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph depGraphCC) {
        Set<String> relations = new HashSet<String>();
        Map<Integer, Integer> mapping = getMapping(dataInstance);
        for (LLLDataInstance.InteractionPair ip : dataInstance.interactions) {
            String signature;
            int agent = mapping.get(ip.agent);
            if (agent == -1) {
                System.out.println(dataInstance.words.get(ip.agent) + " is not found!");
                continue;
            }
            int target = mapping.get(ip.target);
            if (target == -1) {
                System.out.println(dataInstance.words.get(ip.target) + " is not found!");
                continue;
            }
            int agentStart = getEntityStartPos(s, agent);
            int targetStart = getEntityStartPos(s, target);
            
            //don't bother about direction of interaction now
            if (agentStart < targetStart) {
                signature = agentStart + "-" + targetStart;
            } else {
                signature = targetStart + "-" + agentStart;
            }
            
            //lets bother about direction
            //signature = agentStart + "-" + targetStart;
            
            relations.add(signature);
        }
        
        //consider all pairs of known entity
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).getTag("LEXE").equals("B")) {
                for (int j = i + 1; j < s.size(); j++) {
                    //if (j == i) continue;
                    if (s.get(j).getTag("LEXE").equals("B")) {
                        String signature = i + "-" + j;
                        if (relations.contains(signature)) {
                            System.out.println("True: " + getEntity(s, i) + " - " + getEntity(s, j));
                            //System.out.println(s.toString());
                            //System.out.println(depGraph.getPath(i, j));                            
                            instanceList.addThruPipe(new RelexInstance(dataInstance, s, depGraph, depGraphCC, i, j, true));
                        } else {
                            System.out.println("False: " + getEntity(s, i) + " - " + getEntity(s, j));
                            instanceList.addThruPipe(new RelexInstance(dataInstance, s, depGraph, depGraphCC, i, j, false));
                        }
                    }
                }
            }
        }
    }
    
    Map<Integer, Integer> getMapping(LLLDataInstance dataInstance) {
        Sentence s = dataInstance.s;
        List<String> words = dataInstance.words;
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
    private String getEntity(Sentence s, int pos) {        
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
    
    Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new RelexPipeAIMed());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }    
    
    public static void main(String args[]) {
        MERelex mERelex = new MERelex();        
        //mERelex.train("./resource/relation/LLL/genic_interaction_data.txt");
        mERelex.train("./resource/relation/LLL/genic_interaction_data_all.txt");
    }
}
