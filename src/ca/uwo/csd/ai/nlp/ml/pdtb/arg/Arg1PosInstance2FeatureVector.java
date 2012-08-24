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
public class Arg1PosInstance2FeatureVector extends Pipe {
    
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    private static final DependencyTreeAnalyzer depAnalyzer = new DependencyTreeAnalyzer();
    private static final String[] attributiveVerb = new String[] {"says?|said","accept(s|ed)?","assume(s|d)?","contend(s|ed)?","explain(s|ed)?","note(s|d)?","reveal(s|ed)?","account(s|ed)? for","believe(s|d)?","content(s|ed)?","expresse(s|d)?","object(s|ed)?","see(s)?","acknowledge(s|ed)?","categorize(s|d)?","criticize(s|d)?","find(s|ed)?","observe(s|d)?","show(s|ed)?","addresse(s|d)?","challenge(s|d)?","deal(s|t)? with","grant(s|ed)?","offer(s|ed)?","speculate(s|d)?","add(s|ed)?","charge(s|d)?","decide(s|d)?","hypothesize(s|d)?","oppose(s|d)?","state(s|d)?","admit(s|ted)?","cite(s|d)?","declare(s|d)?","illustrate(s|d)?","point(s|ed)? out","suggest(s|ed)?","advise(s|d)?","claim(s|ed)?","define(s|d)?","implie(s|d)?","propose(s|d)?","support(s|ed)?","affirm(s|ed)?","comment(s|ed)?","denie(s|d)?","indicate(s|d)?","question(s|ed)?","suppose(s|d)?","agree(s|d)?","compare(s|d)?","describe(s|d)?","insinuate(s|d)?","realize(s|d)?","think(s|ed)?","allege(s|d)?","complain(s|ed)?","disagree(s|d)?","insist(s|ed)?","reason(s|ed)?","use(s|d)?","allow(s|ed)?","concede(s|d)?","discusse(s|d)?","interpret(s|ed)?","refute(s|d)?","utilize(s|d)?","analyze(s|d)?","conclude(s|ed)?","dispute(s|d)?","introduce(s|d)?","reject(s|ed)?","verifie(s|d)?","answer(s|ed)?","concur(s|red)?","emphasize(s|d)?","list(s|ed)?","remark(s|ed)?","whine(s|d)?","argue(s|d)?","confesse(s|d)?","emphasize(s|d)?","maintain(s|ed)?","replie(s|ed)?","write(s)?","ask(s|ed)?","confirm(s|ed)?","endorse(s|d)?","mention(s|ed)?","report(s|ed)?","assert(s|ed)?","consider(s|ed)?","exclaim(s|ed)?","mumble(s|ed)?","respond(s|ed)?"};
    
    public Arg1PosInstance2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        Arg1PosInstance instance = (Arg1PosInstance) carrier;
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
        
        
        PropertyList pl = null;
        pl = addBaselineFeatures(pl, root, connStart, connEnd);
        //pl = addSyntaxFeatures(pl, root, connStart, connEnd, arg1HeadPos, arg2HeadPos);
        //pl = addDependencyFeatures(pl, root, depGraph, connStart, connEnd, arg1HeadPos, arg2HeadPos);
        //pl = addLexicoSyntacticFeatures(pl, root, depGraph, connStart, connEnd, arg1HeadPos);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(String.valueOf(instance.isSentential())));        
        carrier.setData(fv);
        
        return carrier;
    }
    private PropertyList addBaselineFeatures(PropertyList pl, Tree root, int connStart, int connEnd) {        
        String conn = getConnString(root, connStart, connEnd);
        List<Tree> leaves = root.getLeaves();
        
        //R-connective type
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("R="+category, 1.0, pl);
        
        //A-position of the connective
        String position = "Medial";
        if (connStart < 4) position = "Initial";
        else if (connEnd > (leaves.size() - 4)) position = "Terminal";
        pl = PropertyList.add("A="+position, 1.0, pl);
        
        //S-A & R
        pl = PropertyList.add("S="+position+"&"+category, 1.0, pl);
        
        //C-connective phrase
        pl = PropertyList.add("C="+conn, 1.0, pl);
        
        //D-downcase conn phrase
        pl = PropertyList.add("D="+conn.toLowerCase(), 1.0, pl);
        
        //previous word
        if (connStart > 0) {
            pl = PropertyList.add("prev="+leaves.get(connStart - 1).value(), 1.0, pl);
        } else {
            pl = PropertyList.add("prev=NONE", 1.0, pl);
        }
        return pl;
    }
    
    /**
     * returns the position of the first leaf in the range whose value matches with a given value
     * @param leaves
     * @param s
     * @param e
     * @param v
     * @return 
     */
    private int findValue(List<Tree> leaves, int s, int e, String v) {
        if (s < 0 || e < 0 || s >= leaves.size() || e >= leaves.size()) return -1;
        else if (s > e) {
            for (int i = s; i >= e; i--) {
                if (leaves.get(i).value().equals(v)) {
                    return i;
                }
            }
        } else {
            for (int i = s; i <= e; i++) {
                if (leaves.get(i).value().equals(v)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private PropertyList addSyntaxFeatures(PropertyList pl, Tree root, int connStart, int connEnd, int arg1HeadPos, int arg2HeadPos) {        
        String conn = getConnString(root, connStart, connEnd);
        List<Tree> leaves = root.getLeaves();
        
        //H-path from argHead to connective
        //I-length of path
        List<Tree> path = root.pathNodeToNode(leaves.get(arg1HeadPos), leaves.get(connStart));
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
            pl = PropertyList.add("I="+path.size()/2, 1.0, pl);
            
            //clause feature-whether the conn and head belong to the same clause
            //pl = PropertyList.add("CLAUSE#="+(clauseCount>1), 1.0, pl);
            
            
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
            pl = PropertyList.add("I="+"null", 1.0, pl);
            pl = PropertyList.add("J="+"null", 1.0, pl);
            pl = PropertyList.add("K="+"null", 1.0, pl);
            pl = PropertyList.add("L="+"null", 1.0, pl);
        }
                
        //path from arg1 to arg2
        List<Tree> arg1ToArg2Path = root.pathNodeToNode(leaves.get(arg1HeadPos), leaves.get(arg2HeadPos));
        if (arg1ToArg2Path != null) {
            int clause = 0;
            for (Tree t : arg1ToArg2Path) {
                if (t.isLeaf()) continue;
                if (t.value().startsWith("S")) clause++;
            }
            pl = PropertyList.add("ARG_PATH_LEN="+arg1ToArg2Path.size()/4, 1.0, pl);
            pl = PropertyList.add("ARG_CLS="+clause, 1.0, pl);
        }
        return pl;
    }
    
    private PropertyList addDependencyFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int arg1HeadPos, int arg2HeadPos) { 
        String conn = getConnString(root, connStart, connEnd);
        //List<Tree> leaves = root.getLeaves();
        //String depPath = depAnalyzer.getPath(root, headPos, connStart);
        String depPath = depGraph.getPath(arg1HeadPos, connStart);
        
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
                if (!node.equals("cc")) {
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
                
        
        String arg1H2Arg2H = depGraph.getPath(arg1HeadPos, arg2HeadPos);
        pl = PropertyList.add("A1H2A2H="+arg1H2Arg2H, 1.0, pl);
        
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
    
    private PropertyList addLexicoSyntacticFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int connStart, int connEnd, int arg1HeadPos) { 
        boolean attributive = false;
        String head = root.getLeaves().get(arg1HeadPos).value();
        for (String verb : attributiveVerb) {
            if (head.matches(verb)) {
                attributive = true;
                break;
            }
        }
        pl = PropertyList.add("U="+attributive, 1.0, pl);
        
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
        for (SimpleDependency dep : depDependencies) {
            if (dep.reln().equals("ccomp")) {
                isClausalComp = true;
                break;
            }
        }
        pl = PropertyList.add("X="+isClausalComp, 1.0, pl);
        return pl;
    }
    private String getConnString(Tree root, int connStart, int connEnd) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        if ((connEnd - connStart) <= 4) {
            for (int i = connStart; i <= connEnd; i++) {
                Tree leaf = leaves.get(i);
                if (sb.length() != 0) {
                    sb.append(" ");
                }
                sb.append(leaf.value());
            }
        } else { //handle if..then, etc.
            sb.append(leaves.get(connStart));
            sb.append("-");
            sb.append(leaves.get(connEnd));
        }
        return sb.toString();
    }
}
