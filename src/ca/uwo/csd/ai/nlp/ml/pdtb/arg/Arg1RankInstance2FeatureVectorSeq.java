/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.DependencyTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.classify.RankMaxEnt;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import edu.stanford.nlp.trees.Tree;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Arg1RankInstance2FeatureVectorSeq extends Pipe {
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    private static final HeadAnalyzer headAnalyzer = new HeadAnalyzer();
    //private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    //biodrb
    //private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer("./resource/ml/data/biodrb/connective_types");    
    private static final DependencyTreeAnalyzer depAnalyzer = new DependencyTreeAnalyzer();
    private static final String[] attributiveVerb = new String[] {"says?|said","accept(s|ed)?","assume(s|d)?","contend(s|ed)?","explain(s|ed)?","note(s|d)?","reveal(s|ed)?","account(s|ed)? for","believe(s|d)?","content(s|ed)?","expresse(s|d)?","object(s|ed)?","see(s)?","acknowledge(s|ed)?","categorize(s|d)?","criticize(s|d)?","find(s|ed)?","observe(s|d)?","show(s|ed)?","addresse(s|d)?","challenge(s|d)?","deal(s|t)? with","grant(s|ed)?","offer(s|ed)?","speculate(s|d)?","add(s|ed)?","charge(s|d)?","decide(s|d)?","hypothesize(s|d)?","oppose(s|d)?","state(s|d)?","admit(s|ted)?","cite(s|d)?","declare(s|d)?","illustrate(s|d)?","point(s|ed)? out","suggest(s|ed)?","advise(s|d)?","claim(s|ed)?","define(s|d)?","implie(s|d)?","propose(s|d)?","support(s|ed)?","affirm(s|ed)?","comment(s|ed)?","denie(s|d)?","indicate(s|d)?","question(s|ed)?","suppose(s|d)?","agree(s|d)?","compare(s|d)?","describe(s|d)?","insinuate(s|d)?","realize(s|d)?","think(s|ed)?","allege(s|d)?","complain(s|ed)?","disagree(s|d)?","insist(s|ed)?","reason(s|ed)?","use(s|d)?","allow(s|ed)?","concede(s|d)?","discusse(s|d)?","interpret(s|ed)?","refute(s|d)?","utilize(s|d)?","analyze(s|d)?","conclude(s|ed)?","dispute(s|d)?","introduce(s|d)?","reject(s|ed)?","verifie(s|d)?","answer(s|ed)?","concur(s|red)?","emphasize(s|d)?","list(s|ed)?","remark(s|ed)?","whine(s|d)?","argue(s|d)?","confesse(s|d)?","emphasize(s|d)?","maintain(s|ed)?","replie(s|ed)?","write(s)?","ask(s|ed)?","confirm(s|ed)?","endorse(s|d)?","mention(s|ed)?","report(s|ed)?","assert(s|ed)?","consider(s|ed)?","exclaim(s|ed)?","mumble(s|ed)?","respond(s|ed)?"};
    
    private ConnectiveAnalyzer connAnalyzer;
    
    public Arg1RankInstance2FeatureVectorSeq() {
        super(new Alphabet(), new LabelAlphabet());
        connAnalyzer = new ConnectiveAnalyzer();
    }
    
    public Arg1RankInstance2FeatureVectorSeq(String connLexiconPath) {
        super(new Alphabet(), new LabelAlphabet());
        connAnalyzer = new ConnectiveAnalyzer(connLexiconPath);
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        Arg1RankInstance instance = (Arg1RankInstance) carrier;
        
        Document document = (Document) instance.getData();
        List<Pair<Integer, Integer>> candidates = instance.getCandidates();
        int connStart = instance.getConnStart();
        int connEnd = instance.getConnEnd();
        int arg2Line = instance.getArg2Line();
        int arg2HeadPos = instance.getArg2HeadPos();
        
        FeatureVector fvs[] = new FeatureVector[candidates.size()];
        
        for (int i = 0; i < candidates.size(); i++) {            
            Pair<Integer, Integer> candidate = candidates.get(i);
            PropertyList pl = null;
            pl = addBaselineFeatures(pl, document, candidate, arg2Line, arg2HeadPos, connStart, connEnd);
            pl = addConstituentFeatures(pl, document, candidate, arg2Line, arg2HeadPos, connStart, connEnd);
            pl = addDependencyFeatures(pl, document, candidate, arg2Line, arg2HeadPos, connStart, connEnd);
            //pl = addLexicoSyntacticFeatures(pl, document, candidate, arg2Line, arg2HeadPos, connStart, connEnd);
            
            fvs[i] = new FeatureVector(getDataAlphabet(), pl, true, true);
        }

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(String.valueOf(instance.getTrueArg1Candidate())));
        carrier.setData(new FeatureVectorSequence(fvs));
        
        return carrier;
    }
    
    private PropertyList addBaselineFeatures(PropertyList pl, Document doc, Pair<Integer, Integer> candidate, int arg2Line, int arg2HeadPos, int connStart, int connEnd) {
        Sentence arg2Sentence = doc.getSentence(arg2Line);
        int arg1Line = candidate.first();
        Sentence arg1Sentence = doc.getSentence(arg1Line);
        int arg1HeadPos = candidate.second();
        String conn = arg2Sentence.toString(connStart, connEnd);
        
        //R-connective type
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("R="+category, 1.0, pl);
        
        //A-position of the connective
        String position = "Medial";
        if (connStart < 4) position = "Initial";
        else if (connEnd >= (arg1Sentence.size() - 3)) position = "Terminal";
        pl = PropertyList.add("A="+position, 1.0, pl);
        
        //S-A & R
        pl = PropertyList.add("S="+position+"&"+category, 1.0, pl);
        
        //C-connective phrase
        pl = PropertyList.add("C="+conn, 1.0, pl);
        
        //D-downcase conn phrase
        pl = PropertyList.add("D="+conn.toLowerCase(), 1.0, pl);
        
        //E-argument head word
        pl = PropertyList.add("E="+arg1Sentence.get(arg1HeadPos).word(), 1.0, pl);
        
        //B-same sentence or not
        pl = PropertyList.add("B="+(arg1Line == arg2Line), 1.0, pl);
        //G-A&B
        pl = PropertyList.add("A="+position+"&"+"B="+(arg1Line == arg2Line), 1.0, pl);
        
        //F-arg1 head prior or after conn
        if (arg1Line < arg2Line || arg1HeadPos < connStart) {
            pl = PropertyList.add("F=<", 1.0, pl);
        } else {
            pl = PropertyList.add("F=>", 1.0, pl);
        }
        
        //if (1 < 2) return pl;
        //Z1-relative position of arg1-conn-arg2
        String z = null;
        if (arg1Line < arg2Line) {
            if (arg2HeadPos < connStart) z = "ARG1-ARG2-CONN";
            else z = "ARG1-CONN-ARG2";
        } else if (arg1HeadPos < connStart) {
            if (arg2HeadPos < arg1HeadPos) z = "ARG2-ARG1-CONN";
            else if (arg2HeadPos < connStart) z = "ARG1-ARG2-CONN";
            else z = "ARG1-CONN-ARG2";
        } else if (arg2HeadPos < connStart) {
            z = "ARG2-CONN-ARG1";
        } else if (arg2HeadPos < arg1HeadPos) {
            z = "CONN-ARG2-ARG1";
        } else {
            z = "CONN-ARG1-ARG2";
        }
        pl = PropertyList.add("Z="+z, 1.0, pl);
        
        //Z2-Conn&Z1
        pl = PropertyList.add("CONN="+conn+"&"+"Z="+z, 1.0, pl);
        
        return pl;
    }
    
    private PropertyList addConstituentFeatures(PropertyList pl, Document doc, Pair<Integer, Integer> candidate, int arg2Line, int arg2HeadPos, int connStart, int connEnd) {
        Sentence arg2Sentence = doc.getSentence(arg2Line);
        String conn = arg2Sentence.toString(connStart, connEnd);
        int connHeadPos = connAnalyzer.getHeadWord(arg2Sentence.getParseTree(), connStart, connEnd);
        
        int arg1Line = candidate.first();
        Tree arg1Tree = doc.getTree(arg1Line);
        int arg1HeadPos = candidate.second();        
        
        List<String> path = new ArrayList<String>();
        List<String> pathWithoutPOS = new ArrayList<String>();
        
        if (arg1Line == arg2Line) {
            Tree root = arg1Tree;
            List<Tree> leaves = root.getLeaves();
            List<Tree> treePath = root.pathNodeToNode(leaves.get(connHeadPos), leaves.get(arg1HeadPos));
            if (treePath != null) {
                for (Tree t : treePath) {
                    if (!t.isLeaf()) {
                        path.add(t.value());
                        if (!t.isPreTerminal()) {
                            pathWithoutPOS.add(t.value());
                        }
                    }
                }
            }
        } else {
            Tree arg2Root = arg2Sentence.getParseTree();
            Tree mainHead = headAnalyzer.getCollinsHead(arg2Root.getChild(0));
            List<Tree> leaves = arg2Root.getLeaves();
            int mainHeadPos = treeAnalyzer.getLeafPosition(arg2Root, mainHead);
            if (mainHeadPos != -1) {
                List<Tree> treePath = arg2Root.pathNodeToNode(leaves.get(connHeadPos), leaves.get(mainHeadPos));
                if (treePath != null) {
                    for (Tree t : treePath) {
                        if (!t.isLeaf()) {
                            path.add(t.value());
                            if (!t.isPreTerminal()) {
                                pathWithoutPOS.add(t.value());
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < Math.abs(arg1Line - arg2Line); i++) {
                path.add("SENT");
                pathWithoutPOS.add("SENT");
            }
            Tree arg1Root = arg1Tree;
            mainHead = headAnalyzer.getCollinsHead(arg1Root.getChild(0));
            leaves = arg1Root.getLeaves();
            mainHeadPos = treeAnalyzer.getLeafPosition(arg1Root, mainHead);
            if (mainHeadPos != -1) {
                List<Tree> treePath = arg1Root.pathNodeToNode(leaves.get(mainHeadPos), leaves.get(arg1HeadPos));
                if (treePath != null) {
                    for (Tree t : treePath) {
                        if (!t.isLeaf()) {
                            path.add(t.value());
                            if (!t.isPreTerminal()) {
                                pathWithoutPOS.add(t.value());
                            }
                        }
                    }
                }
            }
        }
        //H-full path
        //L-C&H
        StringBuilder fullPath = new StringBuilder();
        for (String node : path) {
            fullPath.append(node).append(":");
        }
        pl = PropertyList.add("H="+fullPath.toString(), 1.0, pl);
        pl = PropertyList.add("L=CONN-"+conn+"&"+"H-"+fullPath.toString(), 1.0, pl);
        
        //I-length of path
        pl = PropertyList.add("I="+path.size(), 1.0, pl);
        
        //J-collapsed path without part of speech
        //K-collapsed path without repititions
        fullPath = new StringBuilder();
        StringBuilder collapsedPath = new StringBuilder();
        String prev = "";
        for (String node : pathWithoutPOS) {
            fullPath.append(node).append(":");
            if (!node.equals(prev)) {
                collapsedPath.append(node).append(":");
            }
            prev = node;
        }
        pl = PropertyList.add("J="+fullPath.toString(), 1.0, pl);
        pl = PropertyList.add("K="+collapsedPath.toString(), 1.0, pl);
        
        return pl;
    }
    
    private PropertyList addDependencyFeatures(PropertyList pl, Document doc, Pair<Integer, Integer> candidate, int arg2Line, int arg2HeadPos, int connStart, int connEnd) {
        Sentence arg2Sentence = doc.getSentence(arg2Line);
        String conn = arg2Sentence.toString(connStart, connEnd);
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        int connHeadPos = connAnalyzer.getHeadWord(arg2Sentence.getParseTree(), connStart, connEnd);
        
        int arg1Line = candidate.first();
        Tree arg1Tree = doc.getTree(arg1Line);
        int arg1HeadPos = candidate.second();        
        
        List<String> path = new ArrayList<String>();
        if (arg1Line == arg2Line) {
            SimpleDepGraph depGraph = doc.getDepGraph(arg1Line);
            List<String> tmpPath = depGraph.getPathAsList(connHeadPos, arg1HeadPos, false);            
            if (tmpPath != null) {
                path.addAll(tmpPath);
            } else {
                path.add("null");
            }
        } else {
            Tree arg2Root = arg2Sentence.getParseTree();
            Tree mainHead = headAnalyzer.getCollinsHead(arg2Root.getChild(0));            
            int mainHeadPos = treeAnalyzer.getLeafPosition(arg2Root, mainHead);
            if (mainHeadPos != -1) {
                SimpleDepGraph depGraph = doc.getDepGraph(arg2Line);
                List<String> tmpPath = depGraph.getPathAsList(connHeadPos, mainHeadPos, false);
                if (tmpPath != null) {
                    path.addAll(tmpPath);
                } else {
                    path.add("null");
                }
            }
            for (int i = 0; i < Math.abs(arg1Line - arg2Line); i++) {                
                path.add("SENT");                
            }
            
            Tree arg1Root = arg1Tree;
            mainHead = headAnalyzer.getCollinsHead(arg1Root.getChild(0));            
            mainHeadPos = treeAnalyzer.getLeafPosition(arg1Root, mainHead);
            if (mainHeadPos != -1) {
                SimpleDepGraph depGraph = doc.getDepGraph(arg1Line);
                List<String> tmpPath = depGraph.getPathAsList(mainHeadPos, arg1HeadPos, false);
                if (tmpPath != null) {
                    path.addAll(tmpPath);
                } else {
                    path.add("null");
                }
            }
        }
                
        StringBuilder sbPath = new StringBuilder();
        StringBuilder sbPathWithoutCC = new StringBuilder();
        StringBuilder sbPathWithoutRep = new StringBuilder();

        String prev = "";
        for (String node : path) {
            sbPath.append(node).append(":");
            if (!node.matches("cc|-cc")) {
                sbPathWithoutCC.append(node).append(":");
            }
            if (!node.equals(prev)) {
                sbPathWithoutRep.append(node).append(":");
            }
            prev = node;
        }
        //M-dependency path
        pl = PropertyList.add("M="+sbPath.toString(), 1.0, pl);
        //Q-M&C
        pl = PropertyList.add("Q="+"CONN-"+conn+'&'+"M-"+sbPath.toString(), 1.0, pl);
        //T-M&R
        pl = PropertyList.add("T="+"CAT-"+category+'&'+"M-"+sbPath.toString(), 1.0, pl);
        //O-collapsed path without cc
        pl = PropertyList.add("O="+sbPathWithoutCC.toString(), 1.0, pl);
        //P-collapsed path without repetition
        pl = PropertyList.add("P="+sbPathWithoutRep.toString(), 1.0, pl);
        
        
        return pl;
    }
    
    private PropertyList addLexicoSyntacticFeatures(PropertyList pl, Document doc, Pair<Integer, Integer> candidate, int arg2Line, int arg2HeadPos, int connStart, int connEnd) {        
        
        int arg1Line = candidate.first();
        Tree root = doc.getTree(arg1Line);
        int arg1HeadPos = candidate.second();        
        
        boolean attributive = false;
        String head = root.getLeaves().get(arg1HeadPos).value();
        for (String verb : attributiveVerb) {
            if (head.matches(verb)) {
                attributive = true;
                break;
            }
        }
        
        pl = PropertyList.add("U="+attributive, 1.0, pl);
        SimpleDepGraph depGraph = doc.getDepGraph(arg1Line);
        
        boolean hasClausalComp = false;
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(arg1HeadPos);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().equals("ccomp")) {
                hasClausalComp = true;
                break;
            }
        }
        
        pl = PropertyList.add("V="+hasClausalComp, 1.0, pl);
        pl = PropertyList.add("W="+attributive+"&"+hasClausalComp, 1.0, pl);
        
        boolean isClausalComp = false;
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(arg1HeadPos);
        SimpleDependency clausalComp  = null;
        for (SimpleDependency dep : depDependencies) {
            if (dep.reln().equals("ccomp")) {
                isClausalComp = true;
                clausalComp = dep;
                break;
            }
        }
        
        pl = PropertyList.add("X="+isClausalComp, 1.0, pl);
        if (isClausalComp) {            
            int gov = clausalComp.gov();
            String govWord = root.getLeaves().get(gov).value();
            boolean isGovAttributive = false;
            for (String verb : attributiveVerb) {
                if (govWord.matches(verb)) {
                    isGovAttributive = true;
                    break;
                }
            }
            pl = PropertyList.add("Y="+isClausalComp+"&"+isGovAttributive, 1.0, pl);
        }
        
        return pl;
    }
}