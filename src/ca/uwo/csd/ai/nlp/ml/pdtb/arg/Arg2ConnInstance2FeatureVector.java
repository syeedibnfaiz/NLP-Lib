/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.DependencyTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import edu.stanford.nlp.trees.Tree;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Arg2ConnInstance2FeatureVector extends Pipe {
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    private static final DependencyTreeAnalyzer depAnalyzer = new DependencyTreeAnalyzer();
    private static final String[] attributiveVerb = new String[] {"says?|said","accept(s|ed)?","assume(s|d)?","contend(s|ed)?","explain(s|ed)?","note(s|d)?","reveal(s|ed)?","account(s|ed)? for","believe(s|d)?","content(s|ed)?","expresse(s|d)?","object(s|ed)?","see(s)?","acknowledge(s|ed)?","categorize(s|d)?","criticize(s|d)?","find(s|ed)?","observe(s|d)?","show(s|ed)?","addresse(s|d)?","challenge(s|d)?","deal(s|t)? with","grant(s|ed)?","offer(s|ed)?","speculate(s|d)?","add(s|ed)?","charge(s|d)?","decide(s|d)?","hypothesize(s|d)?","oppose(s|d)?","state(s|d)?","admit(s|ted)?","cite(s|d)?","declare(s|d)?","illustrate(s|d)?","point(s|ed)? out","suggest(s|ed)?","advise(s|d)?","claim(s|ed)?","define(s|d)?","implie(s|d)?","propose(s|d)?","support(s|ed)?","affirm(s|ed)?","comment(s|ed)?","denie(s|d)?","indicate(s|d)?","question(s|ed)?","suppose(s|d)?","agree(s|d)?","compare(s|d)?","describe(s|d)?","insinuate(s|d)?","realize(s|d)?","think(s|ed)?","allege(s|d)?","complain(s|ed)?","disagree(s|d)?","insist(s|ed)?","reason(s|ed)?","use(s|d)?","allow(s|ed)?","concede(s|d)?","discusse(s|d)?","interpret(s|ed)?","refute(s|d)?","utilize(s|d)?","analyze(s|d)?","conclude(s|ed)?","dispute(s|d)?","introduce(s|d)?","reject(s|ed)?","verifie(s|d)?","answer(s|ed)?","concur(s|red)?","emphasize(s|d)?","list(s|ed)?","remark(s|ed)?","whine(s|d)?","argue(s|d)?","confesse(s|d)?","emphasize(s|d)?","maintain(s|ed)?","replie(s|ed)?","write(s)?","ask(s|ed)?","confirm(s|ed)?","endorse(s|d)?","mention(s|ed)?","report(s|ed)?","assert(s|ed)?","consider(s|ed)?","exclaim(s|ed)?","mumble(s|ed)?","respond(s|ed)?"};
    
    public Arg2ConnInstance2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        Arg2ConnInstance instance = (Arg2ConnInstance) carrier;
        Tree root;
        try {
            root = treeAnalyzer.getPennTree((String) instance.getData());            
        } catch (IOException ex) {
            Logger.getLogger(Arg2ConnInstance2FeatureVector.class.getName()).log(Level.SEVERE, null, ex);
            root = null;
        }
        SimpleDepGraph depGraph = instance.getDepGraph();
        int connStart = instance.getConnStart();
        int connEnd = instance.getConnEnd();
        int headPos = instance.getHeadPos();
        
        PropertyList pl = null;
        pl = addBaselineFeatures(pl, root, connStart, connEnd, headPos);
        pl = addSyntaxFeatures(pl, root, connStart, connEnd, headPos);
        pl = addDependencyFeatures(pl, root, depGraph, connStart, connEnd, headPos);
        pl = addLexicoSyntacticFeatures(pl, root, depGraph, connStart, connEnd, headPos);
        //pl = addClauseFeatures(pl, root, depGraph, connStart, connEnd, headPos);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(instance.getLabel().toString()));        
        carrier.setData(fv);
        
        return carrier;
    }
    private PropertyList addBaselineFeatures(PropertyList pl, Tree root, int connStart, int connEnd, int headPos) {        
        String conn = getConnString(root, connStart, connEnd);
        List<Tree> leaves = root.getLeaves();
        
        //R-connective type
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("R="+category, 1.0, pl);
        
        //A-position of the connective
        String position = "Medial";
        if (connStart < 4) position = "Initial";
        else if (connEnd == (leaves.size() - 1)) position = "Terminal";
        pl = PropertyList.add("A="+position, 1.0, pl);
        
        //S-A & R
        pl = PropertyList.add("S="+position+"&"+category, 1.0, pl);
        
        //C-connective phrase
        pl = PropertyList.add("C="+conn, 1.0, pl);
        
        //D-downcase conn phrase
        pl = PropertyList.add("D="+conn.toLowerCase(), 1.0, pl);
        
        //E-argument head word
        pl = PropertyList.add("E="+leaves.get(headPos).value(), 1.0, pl);
        Tree headParent = leaves.get(headPos).parent(root);
        pl = PropertyList.add("Conn&E-type="+conn.toLowerCase()+"&"+headParent.value()/*.substring(0, 2)*/, 1.0, pl);
        
        //F-head after or before connective
        if (headPos < connStart) {
            pl = PropertyList.add("F="+"before", 1.0, pl);
            pl = PropertyList.add("F'="+category+"&"+"before", 1.0, pl);
        }
        else {
            pl = PropertyList.add("F="+"after", 1.0, pl);
            pl = PropertyList.add("F'="+category+"&"+"after", 1.0, pl);
        }
        
        //distance from conn to argument head
        pl = PropertyList.add("DIST="+(headPos - connStart)/2, 1.0, pl);
        
        return pl;
    }
    
    private PropertyList addSyntaxFeatures(PropertyList pl, Tree root, int connStart, int connEnd, int headPos) {        
        String conn = getConnString(root, connStart, connEnd);
        List<Tree> leaves = root.getLeaves();
        
        //H-path from argHead to connective
        //I-length of path
        List<Tree> path = root.pathNodeToNode(leaves.get(headPos), leaves.get(connStart));
        if (path != null) {
            StringBuilder sb = new StringBuilder();
            int clauseCount = 0;
            
            for (Tree t : path) {
                if (t.isLeaf()) continue;
                sb.append(t.value()).append(":");
                if (t.value().startsWith("S")) {    //count clauses
                    clauseCount++;
                }
            }
            pl = PropertyList.add("H="+sb.toString(), 1.0, pl);
            pl = PropertyList.add("L="+conn+"&"+sb.toString(), 1.0, pl);
            pl = PropertyList.add("I="+path.size()/4, 1.0, pl);
            
            //clause feature-whether the conn and head belong to the same clause
            pl = PropertyList.add("CLAUSE#="+(clauseCount>1), 1.0, pl);
            
            
            //J-collapsed path without parts of speech
            sb = new StringBuilder();
            for (Tree t : path) {
                if (t.isLeaf() || t.isPreTerminal()) continue;
                sb.append(t.value() + ":");
            }
            pl = PropertyList.add("J="+sb.toString(), 1.0, pl);
            
            //K-collapsed path without repititions
            String prev = "";
            sb = new StringBuilder();
            for (Tree t : path) {
                if (t.isLeaf() || t.isPreTerminal() || t.value().equals(prev)) continue;
                sb.append(t.value() + ":");
                prev = t.value();
            }
            pl = PropertyList.add("K="+sb.toString(), 1.0, pl);
        } else {
            pl = PropertyList.add("H="+"null", 1.0, pl);
            /*pl = PropertyList.add("I="+"null", 1.0, pl);
            pl = PropertyList.add("J="+"null", 1.0, pl);
            pl = PropertyList.add("K="+"null", 1.0, pl);
            pl = PropertyList.add("L="+"null", 1.0, pl);*/
        }
        
        
        return pl;
    }
    
    private PropertyList addDependencyFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int headPos) { 
        String conn = getConnString(root, connStart, connEnd);
        //List<Tree> leaves = root.getLeaves();
        //String depPath = depAnalyzer.getPath(root, headPos, connStart);
        String depPath = depGraph.getPath(headPos, connStart);
        
        //M-dependency path from argument to connective
        pl = PropertyList.add("M="+depPath, 1.0, pl);
        //T- M & R
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("T="+category+"&"+depPath, 1.0, pl);
        
        //Q-C & M
        pl = PropertyList.add("Q="+conn+"&"+depPath, 1.0, pl);
        //O-collapsed path removing coordination links
        //p-collapsed path removing repitition of links
        if (depPath != null) {
            String nodes[] = depPath.split(":");
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            String prev = "";
            for (String node : nodes) {
                if (!node.matches("cc|-cc")) {
                    sb1.append(node).append(":");
                    if (!node.equals(prev)) {
                        sb2.append(node).append(":");
                        prev = node;
                    }
                }
            }
            pl = PropertyList.add("O="+sb1.toString(), 1.0, pl);
            pl = PropertyList.add("P="+sb2.toString(), 1.0, pl);
            //pl = PropertyList.add("LEN="+nodes.length, 1.0, pl);
        }
        /*List<Integer> connDependents = depGraph.getDependents(connStart);
        if (!connDependents.isEmpty()) {
            int minDep = connDependents.get(0);
            for (Integer dep : connDependents) {
                if (dep < minDep) minDep = dep;
            }
            String firstHead = root.getLeaves().get(minDep).value();
            pl = PropertyList.add("N="+depPath + "&"+firstHead, 1.0, pl);
        }*/
        return pl;
    }
    private PropertyList addClauseFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int headPos) { 
        List<Tree> leaves = root.getLeaves();
        Tree arg2Head = leaves.get(headPos);
        Tree connLeaf = leaves.get(connStart);
        List<Tree> dominationPath = root.dominationPath(connLeaf);
        boolean sameClause = false;
        for (int i = dominationPath.size() - 2; i >= 0; i--) {
            Tree node = dominationPath.get(i);
            if (node.value().startsWith("S")) {
                if (node.dominates(arg2Head)) {
                    sameClause = true;
                }
                break;
            }            
        }
        pl = PropertyList.add("SAME_CLS="+sameClause, 1.0, pl);
        
        return pl;        
    }
    private void traverseTree(Tree t, Tree root, int clsStartCount[], int clsEndCount[]) {
        if (!t.isPhrasal()) return;
        if (t.value() != null && t.value().startsWith("S")) {
            List<Tree> tmpLeaves = t.getLeaves();
            if (tmpLeaves != null && !tmpLeaves.isEmpty()) {
                Tree firstLeaf = tmpLeaves.get(0);
                Tree lastLeaf = tmpLeaves.get(tmpLeaves.size() - 1);
                int firstPos = treeAnalyzer.getLeafPosition(root, firstLeaf);
                int lastPos = treeAnalyzer.getLeafPosition(root, lastLeaf);
                //clsStart[firstPos] = true;
                //clsEnd[lastPos] = true;
                clsStartCount[firstPos]++;
                clsEndCount[lastPos]++;
            }
        }
        List<Tree> children = t.getChildrenAsList();
        for (Tree child : children) {
            traverseTree(child, root, clsStartCount, clsEndCount);
        }
    }
    private PropertyList addLexicoSyntacticFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int headPos) { 
        boolean attributive = false;
        String head = root.getLeaves().get(headPos).value();
        for (String verb : attributiveVerb) {
            if (head.matches(verb)) {
                attributive = true;
                break;
            }
        }
        pl = PropertyList.add("U="+attributive, 1.0, pl);
        
        boolean hasClausalComp = false;
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(headPos);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().equals("ccomp")) {
                hasClausalComp = true;
                break;
            }
        }
        
        pl = PropertyList.add("V="+hasClausalComp, 1.0, pl);
        pl = PropertyList.add("W="+attributive+"&"+hasClausalComp, 1.0, pl);
        
        boolean isClausalComp = false;
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(headPos);
        SimpleDependency clausalComp  = null;
        for (SimpleDependency dep : depDependencies) {
            if (dep.reln().equals("ccomp")) {
                isClausalComp = true;
                clausalComp = dep;
                break;
            }
        }
        
        if (isClausalComp) {
            pl = PropertyList.add("X="+isClausalComp, 1.0, pl);
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
    private String getConnString(Tree root, int connStart, int connEnd) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        for (int i = connStart; i <= connEnd; i++) {
            Tree leaf = leaves.get(i);
            if (sb.length() != 0) sb.append(" ");
            sb.append(leaf.value());
        }
        return sb.toString();
    }
}
