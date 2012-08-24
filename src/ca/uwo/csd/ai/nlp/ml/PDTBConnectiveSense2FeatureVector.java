/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.OChunker;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBConnectiveSense2FeatureVector extends Pipe {

    private static Pattern pat = Pattern.compile("CC|TO|IN|PDT|[,.;:?!-+()]");
    private static TreeFactory tf = new LabeledScoredTreeFactory();
    //private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    //private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer("./resource/ml/data/biodrb/connective_types");
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    private static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    private static final OChunker CHUNKER = new OChunker();
    private ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    String target = null;
    public PDTBConnectiveSense2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
        connAnalyzer = new ConnectiveAnalyzer();
    }
    
    public PDTBConnectiveSense2FeatureVector(String connLexiconPath) {
        super(new Alphabet(), new LabelAlphabet());
        connAnalyzer = new ConnectiveAnalyzer(connLexiconPath);
    }


    @Override
    public Instance pipe(Instance carrier) {
        PDTBConnectiveSenseInstance instance = (PDTBConnectiveSenseInstance) carrier;        
                        
        Sentence sentence = (Sentence) instance.getData();                
        int start = instance.getS();
        int end = instance.getE();
        int arg2Head = instance.getArg2Head();
        
        Tree root = sentence.getParseTree();
        
        SimpleDepGraph depGraph = instance.getDepGraph();
        PropertyList pl = null;                
        
        target = instance.getLabel().toString();
        pl = addIdentityFeatures(pl, sentence, start, end);
        pl = addPennFeatures(pl, sentence, root, start, end);
        
        //pl = addSyntaxTreeFeatures(pl, sentence, root, start, end);
        //pl = addMySyntacticFeatures(pl, sentence, root, start, end);
        //pl = addMyCustomFeatures(pl, sentence, root, depGraph, start, end);
        //pl = addClauseFeatures(pl, sentence, root, start, end);
        //pl = addTemporalFeatures(pl, sentence, root, start, end);
        //pl = addPathFeatures(pl, sentence, root, start, end);
        //pl = addConstituentFeatures(pl, root, depGraph, start, end);
        //pl = addDependencyFeatures(pl, root, depGraph, start, end);
        
        pl = addSurfaceFeatures(pl, sentence, root, depGraph, start, end);
        pl = addHeadFeatures(pl, sentence, root, depGraph, start, end, arg2Head);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);
        
        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
        //boolean connective = instance.isConnective();
        //carrier.setTarget(ldict.lookupLabel(String.valueOf(connective)));
        carrier.setTarget(ldict.lookupLabel(instance.getLabel().toString()));
        //System.out.println(connective+" : " + sentence.toString(start, end));
        carrier.setData(fv);
        return carrier;
    }
    private PropertyList addHeadFeatures(PropertyList pl, Sentence s, Tree root, SimpleDepGraph depGraph, int start, int end, int arg2Head) {
        String conn = getConnString(root, start, end).toLowerCase();
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        /*if (arg2Head < start) {
            pl = PropertyList.add("HEAD_ORDER=LEFT&CONN="+conn, 1.0, pl);
        } else {
            pl = PropertyList.add("HEAD_ORDER=RIGHT&CONN="+conn, 1.0, pl);
        }*/
        pl = PropertyList.add("CONN="+conn+"&HEAD_POS="+root.getLeaves().get(arg2Head).parent(root).value(), 1.0, pl);
        //pl = PropertyList.add("CONN="+conn+"&HEAD="+s.get(arg2Head).word(), 1.0, pl);
        pl = PropertyList.add("HEAD="+s.get(arg2Head).word(), 1.0, pl);
        
        return pl;
    }
    private PropertyList addSurfaceFeatures(PropertyList pl, Sentence s, Tree root, SimpleDepGraph depGraph, int start, int end) {        
        String conn = getConnString(root, start, end);
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("CAT="+category, 1.0, pl);        
        /*if (start < 4) {
            pl = PropertyList.add("CONN-POSITION=INIT", 1.0, pl);
        } else {
            pl = PropertyList.add("CONN-POSITION=MEDIAL", 1.0, pl);
        }*/
        List<Tree> leaves = root.getLeaves();
        if (start > 0) {
            //pl = PropertyList.add("PREVW="+s.get(start-1).word(), 1.0, pl);
            //pl = PropertyList.add("CONN&PREVW="+conn+"&"+s.get(start-1).getTag("POS"), 1.0, pl);
            pl = PropertyList.add("CONN&PREVW="+conn+"&"+s.get(start-1).word(), 1.0, pl);
            Tree leftLeaf = leaves.get(start-1);
            String chunk = leftLeaf.value();
            if (!chunk.matches("[,;:.]")) {
                chunk = leftLeaf.ancestor(2, root).value();
            }
            chunk = removeReferenceTag(chunk).substring(0, 1);            
            //pl = PropertyList.add("CONN&PREV-CHUNK="+conn+"&"+chunk, 1.0, pl);            
            pl = PropertyList.add("CONN&PREV-CHUNK="+chunk, 1.0, pl);
        }  else {
            //pl = PropertyList.add("PREVW=NONE", 1.0, pl);
            pl = PropertyList.add("CONN&PREVW="+conn+"&NONE", 1.0, pl);
            //pl = PropertyList.add("CONN&PREV-CHUNK="+conn+"&"+"NONE", 1.0, pl);            
            pl = PropertyList.add("CONN&PREV-CHUNK="+"NONE", 1.0, pl);
        }
        
        if (end < (s.size()-1)) {
            //pl = PropertyList.add("NEXTW="+s.get(end + 1).word(), 1.0, pl);
            //pl = PropertyList.add("CONN&NEXTW="+conn+"&"+s.get(end+1).getTag("POS"), 1.0, pl);
            pl = PropertyList.add("CONN&NEXTW="+conn+"&"+s.get(end+1).word(), 1.0, pl);
            Tree rightLeaf = leaves.get(end+1);
            String chunk = rightLeaf.value();
            if (!chunk.matches("[,;:.]")) {
                chunk = rightLeaf.ancestor(2, root).value();
            }
            chunk = removeReferenceTag(chunk).substring(0, 1);            
            //pl = PropertyList.add("CONN&NEXT-CHUNK="+conn+"&"+chunk, 1.0, pl);
            //pl = PropertyList.add("CONN&NEXT-CHUNK="+conn+"&"+chunk, 1.0, pl);
            pl = PropertyList.add("CONN&NEXT-CHUNK="+chunk, 1.0, pl);
        }
        return pl;
    }
    
    private PropertyList addIdentityFeatures(PropertyList pl, Sentence s, int start, int end) {
        pl = PropertyList.add(s.toString(start, end), 1.0, pl);
        //pl = PropertyList.add("LC="+s.toString(start, end).toLowerCase(), 1.0, pl);
        return pl;
    }
    private PropertyList addMyCustomFeatures(PropertyList pl, Sentence s, Tree root, SimpleDepGraph depGraph, int start, int end) {        
        String conn = getConnString(root, start, end);
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add("CAT="+category, 1.0, pl);
        List<Tree> leaves = root.getLeaves();
        
        int connHeadPos = connAnalyzer.getHeadWord(root, start, end);                        
        
        String leftChunk = null;
        String rightChunk = null;
        if (start > 0) {
            pl = PropertyList.add("PREVW="+s.get(start-1).word(), 1.0, pl);
            pl = PropertyList.add("CONN&PREVW="+conn+"&"+s.get(start-1).word(), 1.0, pl);
            //pl = PropertyList.add("PREV-POS="+s.get(start-1).getTag("POS"), 1.0, pl);
            //pl = PropertyList.add("CONN&PREV-POS="+conn+"&"+s.get(start-1).getTag("POS"), 1.0, pl);
            Tree leftLeaf = leaves.get(start-1);
            String chunk = leftLeaf.value();
            if (!chunk.matches("[,;:.]")) {
                chunk = leftLeaf.ancestor(2, root).value();
            }
            chunk = removeReferenceTag(chunk);
            //chunk = s.get(start - 1).getTag("CHUNK");
            //chunk = getChunk4Leaf(leftLeaf, root);
            leftChunk = chunk;
            
            //pl = PropertyList.add("PREV-CHUNK="+chunk, 1.0, pl);
            pl = PropertyList.add("CONN&PREV-CHUNK="+conn+"&"+chunk, 1.0, pl);
            Tree leftLca = treeAnalyzer.getLCA(root, start - 1, start);
            if (leftLca != null) {
                pl = PropertyList.add("CAT&LEFT-LCA="+category+"&"+leftLca.value(), 1.0, pl);
            }
        } else {
            pl = PropertyList.add("PREVW=NONE", 1.0, pl);
            pl = PropertyList.add("CONN&PREVW="+conn+"&NONE", 1.0, pl);
        }
        if (end < (s.size()-1)) {
            pl = PropertyList.add("NEXTW="+s.get(end + 1).word(), 1.0, pl);
            pl = PropertyList.add("CONN&NEXTW="+conn+"&"+s.get(end+1).word(), 1.0, pl);
            //pl = PropertyList.add("NEXT-POS="+s.get(end+1).getTag("POS"), 1.0, pl);
            //pl = PropertyList.add("CONN&NEXT-POS="+conn+"&"+s.get(end+1).getTag("POS"), 1.0, pl);
            Tree rightLeaf = leaves.get(end+1);
            String chunk = rightLeaf.value();
            if (!chunk.matches("[,;:.]")) {
                chunk = rightLeaf.ancestor(2, root).value();
            }
            chunk = removeReferenceTag(chunk);
            //chunk = s.get(end + 1).getTag("CHUNK");
            //chunk = getChunk4Leaf(rightLeaf, root);
            rightChunk = chunk;
            //pl = PropertyList.add("NEXT-CHUNK="+chunk, 1.0, pl);
            pl = PropertyList.add("CONN&NEXT-CHUNK="+conn+"&"+chunk, 1.0, pl);
        }        
        //pl = PropertyList.add("PREV-CHUNK="+leftChunk+"&CONN="+conn+"&NEXT-CHUNK="+rightChunk, 1.0, pl);
        
        //if (1 < 2) return pl;
        //path from root to lca
        Tree lca = treeAnalyzer.getLCA(root, start, end);
        Tree self = lca;
        if (self != null) {            
            Tree parent = self.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                self = parent;
                parent = self.parent(root);
            }
        }
        if (self != null) {
            List<Tree> dominationPath = root.dominationPath(self);
            int sz = dominationPath.size();
            StringBuilder sb = new StringBuilder();
            String prev = "";
            for (int i = 1; i <= sz - 1; i++) {
                Tree t = dominationPath.get(i);
                if (t.value() != null) {
                    String tag = t.value();
                    tag = removeReferenceTag(tag);
                    if (i < (sz-2)) {
                        pl = PropertyList.add("ANC@"+(sz - i)+"="+tag, 1.0, pl);
                    }
                    //pl = PropertyList.add(category+"&ANC@"+(sz - i)+"="+tag, 1.0, pl);
                    if ((sz - i) < 2) {
                        Tree leftSibling = treeAnalyzer.getSibling(root, t, -1);
                        Tree rightSibling = treeAnalyzer.getSibling(root, t, 1);
                        if (leftSibling != null) {
                            //pl = PropertyList.add("LEFT@" + (sz - i) + "=" + leftSibling.value(), 1.0, pl);
                            String value = removeReferenceTag(leftSibling.value());
                            //pl = PropertyList.add(category+"&LEFT@" + (sz - i) + "=" + value, 1.0, pl);
                        } else {
                            //pl = PropertyList.add("LEFT@" + (sz - i) + "=" + "NONE", 1.0, pl);
                            //pl = PropertyList.add(category+"&LEFT@" + (sz - i) + "=" + "NONE", 1.0, pl);
                        }
                        if (rightSibling != null) {
                            //pl = PropertyList.add("RIGHT@" + (sz - i) + "=" + rightSibling.value(), 1.0, pl);
                            //pl = PropertyList.add(category+"&RIGHT@" + (sz - i) + "=" + rightSibling.value(), 1.0, pl);
                            /*boolean containsVP = false;
                            for (Tree leaf : rightSibling.getLeaves()) {
                            Tree parent = leaf.parent(root);
                            if (parent.value().matches("(V|A|M).*")) {
                            containsVP = true;
                            break;
                            }
                            }
                            pl = PropertyList.add("RIGHT@" + (sz - i) + "hasVP=" + containsVP, 1.0, pl);
                            pl = PropertyList.add(conn+"&RIGHT@" + (sz - i) + "hasVP=" + containsVP, 1.0, pl);*/
                            Tree head = HEAD_ANALYZER.getCollinsHead(rightSibling);                            
                            if (head != null) {
                                Tree headParent = head.parent(root);
                                //pl = PropertyList.add("RIGHT-HEAD@" + (sz - i) + "=" + headParent.value(), 1.0, pl);
                                pl = PropertyList.add(category +"&RIGHT-HEAD@" + (sz - i) + "=" + headParent.value(), 1.0, pl);
                            }
                        } else {
                            //pl = PropertyList.add("RIGHT@" + (sz - i) + "=" + "NONE", 1.0, pl);
                            //pl = PropertyList.add(category+"&RIGHT@" + (sz - i) + "=" + "NONE", 1.0, pl);
                            /*pl = PropertyList.add("RIGHT@" + (sz - i) + "hasVP=" + false, 1.0, pl);
                            pl = PropertyList.add(conn+"&RIGHT@" + (sz - i) + "hasVP=" + false, 1.0, pl);*/
                        }
                    }
                    
                    //pl = PropertyList.add("CONN="+conn.toLowerCase()+"&ANCESTOR@"+(sz - i)+"="+t.value(), 1.0, pl);
                    /*String value = processTag(t.value());
                    if (!value.equals(prev)) {
                        sb.append(value + ":");
                        prev = value;
                    }*/
                }
            }
            //pl = PropertyList.add("CPATH=" + sb.toString(), 1.0, pl);
        }
        
        
        return pl;
    }
        
    private PropertyList addDependencyFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int start, int end) {
        
        List<Tree> leaves = root.getLeaves();
        String conn = getConnString(root, start, end);
        
        //contextual features
        String prevWord = "NONE";
        String prevPOS = "NONE";
        String nextWord = "NONE";
        String nextPOS = "NONE";
        
        if (start > 0) {
            Tree leftLeaf = leaves.get(start - 1);
            Tree preTerminal = leftLeaf.parent(root);
            prevWord = leftLeaf.value();
            prevPOS = preTerminal.value();
        }
        if (end < (leaves.size() - 1)) {
            Tree rightLeaf = leaves.get(end + 1);
            Tree preTerminal = rightLeaf.parent(root);
            nextWord = rightLeaf.value();
            nextPOS = preTerminal.value();
        }
        pl = PropertyList.add("PREVWORD="+prevWord, 1.0, pl);
        pl = PropertyList.add("PREVPOS="+prevPOS, 1.0, pl);
        pl = PropertyList.add("CONN&PREVW="+conn+"&"+prevWord, 1.0, pl);
        pl = PropertyList.add("CONN&PREVP="+conn+"&"+prevPOS, 1.0, pl);
        pl = PropertyList.add("NXTWORD="+nextWord, 1.0, pl);
        pl = PropertyList.add("NXTPOS="+nextPOS, 1.0, pl);
        pl = PropertyList.add("CONN&NXTW="+conn+"&"+nextWord, 1.0, pl);
        pl = PropertyList.add("CONN&NXTP="+conn+"&"+nextPOS, 1.0, pl);
        
        //syntactic features
        int connHeadPos = connAnalyzer.getHeadWord(root, start, end);
        Tree connLeaf = leaves.get(connHeadPos);
        String connPOS = connLeaf.parent(root).value();
        List<SimpleDependency> dependencies = depGraph.getDepDependencies(connHeadPos);
        if (!dependencies.isEmpty()) {
            boolean parentHasSubj = false;
            boolean siblingHasSubj = false;
            
            int parentPos = dependencies.get(0).gov();
            String parentPOS = leaves.get(parentPos).parent(root).value();
            dependencies = depGraph.getGovDependencies(parentPos);
                        
            
            for (SimpleDependency sd : dependencies) {
                int dep = sd.dep();
                if (dep == connHeadPos) continue;
                
                String reln = sd.reln();
                if (reln.matches(".*subj.*")) parentHasSubj = true;
                
                String siblingPOS = leaves.get(dep).parent(root).value();
                //1
                pl = PropertyList.add("POSTRIPLE="+connPOS+"&"+parentPOS+"&"+siblingPOS, 1.0, pl);
                //2
                pl = PropertyList.add("POSTRIPLE&RELN="+connPOS+"&"+parentPOS+"&"+siblingPOS+"-"+reln, 1.0, pl);
                //3
                pl = PropertyList.add("POSTRIPLE'&RELN="+connPOS+"&"+parentPOS+"-"+reln, 1.0, pl);
                //4
                pl = PropertyList.add("POSTRIPLE'&RELN="+connPOS.substring(0, 1) +"&"+parentPOS.substring(0, 1) +"-"+reln, 1.0, pl);
                
                if (!siblingHasSubj) {
                    List<SimpleDependency> siblingDependencies = depGraph.getGovDependencies(dep);
                    for (SimpleDependency sd1 : siblingDependencies) {
                        if (sd1.reln().matches(".*subj.*")) {
                            siblingHasSubj = true;
                            break;
                        }
                    }
                }                                
            }
            pl = PropertyList.add("PSUBJ="+parentHasSubj, 1.0, pl);
            pl = PropertyList.add("SSUBJ="+siblingHasSubj, 1.0, pl);
        }
        
        return pl;
    }
    
    private PropertyList addConstituentFeatures(PropertyList pl, Tree root, SimpleDepGraph depGraph, int start, int end) {        
        List<Tree> leaves = root.getLeaves();
        String conn = getConnString(root, start, end);
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        
        //connective features
        pl = PropertyList.add("CONN="+conn, 1.0, pl);
        pl = PropertyList.add("LCONN="+conn.toLowerCase(), 1.0, pl);
        pl = PropertyList.add("CAT="+category, 1.0, pl);
        pl = PropertyList.add("CONN&CAT="+conn+"&"+category, 1.0, pl);
        
        int connHeadPos = connAnalyzer.getHeadWord(root, start, end);
        Tree connHead = leaves.get(connHeadPos);
        List<Tree> path = root.dominationPath(connHead);
        
        //path features;
        
        StringBuilder collapsedPathBuilder = new StringBuilder();
        
        String prev = "";
        int size = path.size();
        String n1 = null;
        String n2 = null;
        String n3 = null;
        for (int i = size - 3; i > 0; i--) {
            Tree node = path.get(i);
            if (i >= (size - 5)) {                
                if (i == (size-3)) n1 = node.value();
                else if (i == (size-4)) n2 = node.value();
                else n3 = node.value();
            }
            if (!node.value().equals(prev)) {
                collapsedPathBuilder.append(node.value()+":");
            }
            prev = node.value();
        }
        /*pl = PropertyList.add("N1="+n1, 1.0, pl);
        pl = PropertyList.add("N2="+n2, 1.0, pl);
        pl = PropertyList.add("N3="+n3, 1.0, pl);*/
        pl = PropertyList.add("N1N2="+n1+n2, 1.0, pl);
        pl = PropertyList.add("N2N3="+n2+n3, 1.0, pl);
        pl = PropertyList.add("N1N2N3="+n1+n2+n3, 1.0, pl);
        
        //n2, n1n2, n1n3 X conn,cat
        pl = PropertyList.add("CONN-N2="+conn+n2, 1.0, pl);
        pl = PropertyList.add("CONN-N1N2="+conn+n1+n2, 1.0, pl);
        pl = PropertyList.add("CONN-N1N3="+conn+n1+n3, 1.0, pl);
        pl = PropertyList.add("CAT-N2="+category+n2, 1.0, pl);
        pl = PropertyList.add("CAT-N1N2="+category+n1+n2, 1.0, pl);
        pl = PropertyList.add("CAT-N1N3="+category+n1+n3, 1.0, pl);
        
        
        pl = PropertyList.add("CPATH="+collapsedPathBuilder.toString(), 1.0, pl);
        
        //syntactic context features
        int countNP = 0;
        int countVP = 0;
        int countSBAR = 0;
        boolean NPb4VP = false;
        boolean VPb4NP = false;
        boolean SBARb4NP = false;
        
        for (Tree node : path) {
            String value = node.value();
            if (value == null) continue;
            if (value.matches("NP.*")) {
                countNP++;
                if (countVP == 0) NPb4VP = true;
            } else if (value.matches("VP.*")) {
                countVP++;
                if (countNP == 0) VPb4NP = true;
            } else if (value.matches("SBAR.*")) {
                countSBAR++;
                if (countNP == 0) SBARb4NP = true;
            }
        }
        //simpleNP-if NPb4VP and #SBAR=0
        pl = PropertyList.add("SMPLNP="+(NPb4VP && countSBAR==0), 1.0, pl);
        //complexNP = simpleNP-if NPb4VP and #SBAR>0
        pl = PropertyList.add("CMPLXNP="+(NPb4VP && SBARb4NP), 1.0, pl);
        //SOnly = #NP == #VP == 0
        pl = PropertyList.add("SONLY="+(countNP==0 && countVP==0), 1.0, pl);
        //VPb4NP
        pl = PropertyList.add("VPB4NP="+(VPb4NP), 1.0, pl);
        //SBARONLY = #NP == #VP == 0 && #SBAR > 0
        pl = PropertyList.add("SONLY="+(countNP==0 && countVP==0 && countSBAR>0), 1.0, pl);
        
        //VPCord = #sister(VP)of parent >= 2
        Tree parentHead = connHead.parent(root);
        Tree parent = parentHead.parent(root);
        int count = 0;
        for (int i = 1; ; i++) {
            Tree sibling = treeAnalyzer.getSibling(root, parent, i);
            if (sibling != null && sibling.value().equals("VP")) count++;
            sibling = treeAnalyzer.getSibling(root, parentHead, -i);
            if (sibling != null && sibling.value().equals("VP")) count++;
            if (sibling == null) break;
        }
        pl = PropertyList.add("VPCOORD="+(count >= 2), 1.0, pl);
        
        
        
        return pl;
    }
    private PropertyList addSyntaxTreeFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {        
        if (root == null) return pl;
        List<Tree> leaves = root.getLeaves();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        String conn = sentence.toString(start, end).toLowerCase();
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        
        ArrayList<String> features = new ArrayList<String>();
        Tree lca = analyzer.getLCA(root, leaves.subList(start, end+1));
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }
            features.add("SELF="+lca.value());            
        } else {            
            features.add("SELF=<NONE>");
        }

        Tree parent = lca.parent(root);
        String parentStr = null;
        if (parent != null) {            
            features.add("PARENT="+parent.value());            
        } else {            
            features.add("PARENT=<NONE>");
        }

        Tree left = analyzer.getSibling(root, lca, -1);
        
        if (left != null) {               
            //if (left.value().matches("RB|ADVP|NP|DT|PP|,")) {
            /*if (left.value().matches("[,:;]")) {
                Tree left2Left = analyzer.getSibling(root, lca, -2);
                if (left2Left != null) left = left2Left;
            }*/
            
            
            
            features.add("LEFT@-1-"+left.value());            
        } else {
            features.add("LEFT@-1-<NONE>");
        }
        

        Tree right = analyzer.getSibling(root, lca, 1);
        if (right != null) {
            
            features.add("RIGHT@+1-"+right.value());
            
            if (!right.isLeaf()) {
                if (right.value().matches("[,:;]")) {
                    Tree tmp = analyzer.getSibling(root, right, 1);
                    if (tmp != null) {
                        right = tmp;
                    }
                }
                List<Tree> children = right.getChildrenAsList();                
                
                boolean containsVP = false;                
                Queue<Tree> queue = new LinkedList<Tree>(children);
                while (!queue.isEmpty()) {
                    Tree node = queue.remove();
                    if (node.value().equals("VP")) {
                        containsVP = true;
                        break;
                    } else if (node.isPhrasal()) {
                        queue.addAll(node.getChildrenAsList());
                    }
                }                                
                if (containsVP || right.value().equals("VP")) {                    
                    features.add("RIGHT@+1-has-VP");
                } else {
                    features.add("RIGHT@+1-has-no-VP");
                }           
            }
            Tree head = HEAD_ANALYZER.getCollinsHead(right);
            if (head != null) {
                //pl = PropertyList.add("RIGHT-HEAD@-1-"+head.value(), 1.0, pl);
                String rightHead = head.parent(root).value();
                if (rightHead.matches("[,;:]")) {
                    Tree sibling = treeAnalyzer.getSibling(root, head.parent(root), 1);
                    if (sibling != null) {
                        rightHead = sibling.value();
                        System.out.println("**Righthead changed to: " + rightHead);
                    }
                }
                pl = PropertyList.add("RIGHT-HEAD@-1-"+rightHead, 1.0, pl);
                //pl = PropertyList.add(features.get(3) +"&RIGHT-HEAD@-1-"+rightHead, 1.0, pl);                
                pl = PropertyList.add("CAT="+category +"&RIGHT-HEAD@-1-"+rightHead, 1.0, pl);                
            }
        } else {            
            features.add("RIGHT@+1-<NONE>");
        }
                        
        for (int i = 0; i < features.size(); i++) {
            pl = PropertyList.add(features.get(i), 1.0, pl);
            pl = PropertyList.add(conn+"&"+features.get(i), 1.0, pl);            
        }        
                
        //manual combination, syn-syn
        final int SELF=0, PARENT=1, LEFT=2, RIGHT=3, RIGHT_VP = 4;
        pl = PropertyList.add(features.get(LEFT) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(LEFT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(PARENT) +"&"+features.get(LEFT), 1.0, pl);
        pl = PropertyList.add(features.get(PARENT) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(PARENT), 1.0, pl);
        if (features.size() > 4) {
            pl = PropertyList.add(features.get(RIGHT) +"&"+features.get(RIGHT_VP), 1.0, pl);            
        }
        
        pl = PropertyList.add("CAT="+category, 1.0, pl);
        pl = PropertyList.add(features.get(PARENT)+"&"+category, 1.0, pl);
        
        if (1 < 2) return pl;
        //chunk features
        Tree leftChunk = null;
        if (start > 0) {
            Tree leftLeaf = leaves.get(start - 1);
            leftChunk = leftLeaf;
            if (!leftLeaf.value().matches("[,;:]")) {
                leftChunk = leftLeaf.ancestor(2, root);
            }
        }
        Tree rightChunk = null;
        if ((end + 1) < sentence.size()) {
            Tree rightLeaf = leaves.get(end + 1);
            rightChunk = rightLeaf;
            if (!rightLeaf.value().matches("[,;:]")) {
                rightChunk = rightLeaf.ancestor(2, root);
            }
            
            //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
            Tree rightSibling = analyzer.getSibling(root, rightChunk, 1);
            if (rightSibling != null) {
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&"+rightSibling.value()+"@+2", 1.0, pl);
            } else {
                //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&Null", 1.0, pl);
            }
        }
        
        if (leftChunk != null && rightChunk != null) {
            pl = PropertyList.add(leftChunk.value()+"@-1&"+conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
        }
        
        
        
        
        return pl;
    }
    private PropertyList addClauseFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {
        String conn = sentence.toString(start, end).toLowerCase();
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        List<Tree> leaves = root.getLeaves();
        //boolean clsStart[] = new boolean[leaves.size()];
        //boolean clsEnd[] = new boolean[leaves.size()];
        int clsStartCount[] = new int[leaves.size()];
        int clsEndCount[] = new int[leaves.size()];
        
        traverseTree(root, root, clsStartCount, clsEndCount);
        
        String leftPattern = "";
        for (int i = 0; i < start; i++) {
            if (clsStartCount[i] > 0) leftPattern += "S:";
            if (clsEndCount[i] > 0) leftPattern += "E:";            
        }
        String rightPattern = "";
        for (int i = start; i < leaves.size(); i++) {
            if (clsStartCount[i] > 0) rightPattern += "S:";
            if (clsEndCount[i] > 0) rightPattern += "E:";            
        }
        /*while (leftPattern.contains("S:S:E:E:")) {
            leftPattern = leftPattern.replace("S:S:E:E:", "S:E:");
        }
        while (rightPattern.contains("S:S:E:E:")) {
            rightPattern = rightPattern.replace("S:S:E:E:", "S:E:");
        }*/
        //pl = PropertyList.add("CAT&CLS-"+category+"&"+leftPattern, 1.0, pl);
        pl = PropertyList.add("CLS-"+leftPattern, 1.0, pl);
        //pl = PropertyList.add("CONN&CLS-"+conn+"&"+leftPattern, 1.0, pl);
        //pl = PropertyList.add("CAT&CLS+"+category+"&"+rightPattern, 1.0, pl);
        pl = PropertyList.add("CLS+"+rightPattern, 1.0, pl);
        //pl = PropertyList.add("CONN&CLS+"+conn+"&"+rightPattern, 1.0, pl);
        //pl = PropertyList.add("CAT&CLS-&CLS+"+category+"&"+leftPattern+"&"+rightPattern, 1.0, pl);
        
        /*String fullPattern = "";
        fullPattern = leftPattern;
        for (int i = start; i >= 0; i--) {
            if (clsStartCount[i] > 0 || clsEndCount[i] > 0) {
                fullPattern += (start - i)/4+":";
                break;
            }
        }
        fullPattern += "C:";
        for (int i = start; i < leaves.size(); i++) {
            if (clsStartCount[i] > 0 || clsEndCount[i] > 0) {
                fullPattern += (i - start)/4+":";
                break;
            }
        }
        fullPattern += rightPattern;        
        //pl = PropertyList.add("CLS="+category+"&"+fullPattern, 1.0, pl);*/
        
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
    private PropertyList addPathFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {
        List<Tree> leaves = root.getLeaves();
        List<Tree> path = root.pathNodeToNode(leaves.get(0), leaves.get(start));
        if (path == null) return pl;
        
        String prev = "";
        StringBuilder sb = new StringBuilder();        
        
        for (Tree t: path) {
            if (t.isLeaf() || t.isPreTerminal()) continue;
            if (!t.value().equals(prev)) {
                prev = t.value();
                sb.append(t.value() + ":");
            }
        }
        pl = PropertyList.add("L-PATH="+sb.toString(), 1.0, pl);
        
        path = root.pathNodeToNode(leaves.get(start), leaves.get(leaves.size() - 1));
        if (path == null) return pl;
        
        prev = "";
        sb = new StringBuilder();        
        
        for (Tree t: path) {
            if (t.isLeaf() || t.isPreTerminal()) continue;
            if (!t.value().equals(prev)) {
                prev = t.value();
                sb.append(t.value() + ":");
            }
        }
        pl = PropertyList.add("R-PATH="+sb.toString(), 1.0, pl);
        return pl;
    }
    private PropertyList addTemporalFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {
        String conn = sentence.toString(start, end).toLowerCase();
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        //if (category == null || !category.startsWith("Sub") || !conn.matches("when|since|after|before")) {
        if (category == null || !category.startsWith("Sub") || !conn.matches("when")) {
            return pl;
        }
        List<Tree> mainLeaves = root.getLeaves();
        //if (!conn.toLowerCase().matches("when|since")) return pl;
        String year = "(18|19|20)[0-9][0-9]s?|year";
        String months = "january|february|march|april|may|june|july|august|september|october|november|december|month";
        //String ordinal = "first|last|second|third|fourth|fifth|sixth|seventh|eighth|nineth|tenth";
        String seasons = "summer|spring|fall|winter|season|week";
        String others = "decade|century";                
                                        
        Tree lca = treeAnalyzer.getLCA(root, start, end);
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }               
        }

        Tree parent = lca.parent(root);
        
        if (parent != null) {
            Tree prevParent = parent;
            parent = parent.parent(root);
            if (parent != null) {
                List<Tree> leaves = parent.getLeaves();
                int connPos = treeAnalyzer.getLeafPosition(parent, mainLeaves.get(start));
                if (connPos == -1) {
                    System.out.println("*\n****\n***\n**");
                }
                boolean flag = false;
                for (int i = 0; i < connPos; i++) {
                    Tree leaf = leaves.get(i);
                    String word = leaf.value().toLowerCase();
                    if (word.matches(year)) {
                        flag = true;
                        pl = PropertyList.add(conn + "&" + parent.value() +"&L-TMP", 1.0, pl);
                    } else if (word.matches(months)) {
                        flag = true;
                        pl = PropertyList.add(conn + "&" + parent.value() +"&L-TMP", 1.0, pl);
                    } else if (word.matches(seasons)) {
                        flag = true;
                        pl = PropertyList.add(conn + "&" + parent.value() +"&L-TMP", 1.0, pl);
                    } else if (word.matches(others)) {
                        flag = true;
                        pl = PropertyList.add(conn + "&" + parent.value() +"&L-TMP", 1.0, pl);
                    }
                }
                if (flag && target.equals("true")) {
                    System.out.println(sentence);
                }
            }
        }
        return pl;
    }
    
    private PropertyList addPennFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {        
        if (root == null) {
            System.out.println("root is null");
            return pl;
        }
        List<Tree> leaves = root.getLeaves();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        String conn = sentence.toString(start, end).toLowerCase();
        //String category = connAnalyzer.getCategory(conn.toLowerCase());
        
        ArrayList<String> features = new ArrayList<String>();
        Tree lca = analyzer.getLCA(root, leaves.subList(start, end+1));
        
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }
            features.add("SELF="+removeReferenceTag(lca.value()));            
            //features.add("SELF="+lca.value());
        } else {            
            features.add("SELF=<NONE>");            
        }

        Tree parent = lca.parent(root);        
        if (parent != null) {            
            features.add("PARENT="+removeReferenceTag(parent.value()));            
            //features.add("PARENT="+parent.value());
        } else {            
            features.add("PARENT=<NONE>");
        }

        Tree left = analyzer.getSibling(root, lca, -1);
        
        if (left != null) {                           
            features.add("LEFT@-1-"+removeReferenceTag(left.value()));            
            //features.add("LEFT@-1-"+left.value());
        } else {
            features.add("LEFT@-1-<NONE>");
        }
        

        Tree right = analyzer.getSibling(root, lca, 1);
        if (right != null) {
            features.add("RIGHT@+1-"+removeReferenceTag(right.value()));
            //features.add("RIGHT@+1-"+right.value());
            if (!right.isLeaf()) {                
                List<Tree> children = right.getChildrenAsList();                
                
                boolean containsVP = false;                
                Queue<Tree> queue = new LinkedList<Tree>(children);
                while (!queue.isEmpty()) {
                    Tree node = queue.remove();
                    if (node.value().matches("VP.*")) {
                        containsVP = true;
                        break;
                    } else if (node.isPhrasal()) {
                        queue.addAll(node.getChildrenAsList());
                    }
                }                                
                if (containsVP || right.value().matches("VP.*")) {                    
                    features.add("RIGHT@+1-has-VP");
                } else {
                    features.add("RIGHT@+1-has-no-VP");
                }
                List<Tree> rightLeaves = right.getLeaves();
                boolean containsTrace = false;
                for (Tree leaf : rightLeaves) {
                    if (leaf.value().startsWith("*T*")) {
                        containsTrace = true;
                        break;
                    }
                }
                /*if (containsTrace) {
                    features.add("RIGHT@+1-has-trace");
                } else {
                    features.add("RIGHT@+1-has-no-trace");
                }*/
            }            
        } else {            
            features.add("RIGHT@+1-<NONE>");
        }
                        
        for (int i = 0; i < features.size(); i++) {
            pl = PropertyList.add(features.get(i), 1.0, pl);
            pl = PropertyList.add(conn+"&"+features.get(i), 1.0, pl);            
        }        
        
        if (1 < 2) return pl;
        //manual combination, syn-syn
        final int SELF=0, PARENT=1, LEFT=2, RIGHT=3, RIGHT_VP = 4, RIGHT_TRACE = 5;
        pl = PropertyList.add(features.get(LEFT) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(LEFT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(PARENT) +"&"+features.get(LEFT), 1.0, pl);
        pl = PropertyList.add(features.get(PARENT) +"&"+features.get(RIGHT), 1.0, pl);
        pl = PropertyList.add(features.get(SELF) +"&"+features.get(PARENT), 1.0, pl);
        if (features.size() > 4) {
            pl = PropertyList.add(features.get(RIGHT) +"&"+features.get(RIGHT_VP), 1.0, pl);
            pl = PropertyList.add(features.get(PARENT) +"&"+features.get(RIGHT_VP), 1.0, pl);
        }
        if (features.size() > 5) {
            pl = PropertyList.add(features.get(RIGHT) +"&"+features.get(RIGHT_TRACE), 1.0, pl);
            pl = PropertyList.add(features.get(PARENT) +"&"+features.get(RIGHT_TRACE), 1.0, pl);
        }
        /*
        //chunk features
        Tree leftChunk = null;
        if (start > 0) {
            Tree leftLeaf = leaves.get(start - 1);
            leftChunk = leftLeaf;
            if (!leftLeaf.value().matches("[,;:]")) {
                leftChunk = leftLeaf.ancestor(2, root);
            }
        }
        Tree rightChunk = null;
        if ((end + 1) < sentence.size()) {
            Tree rightLeaf = leaves.get(end + 1);
            rightChunk = rightLeaf;
            if (!rightLeaf.value().matches("[,;:]")) {
                rightChunk = rightLeaf.ancestor(2, root);
            }
            
            //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
            Tree rightSibling = analyzer.getSibling(root, rightChunk, 1);
            if (rightSibling != null) {
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&"+rightSibling.value()+"@+2", 1.0, pl);
            } else {
                //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&Null", 1.0, pl);
            }
        }
        
        if (leftChunk != null && rightChunk != null) {
            pl = PropertyList.add(leftChunk.value()+"@-1&"+conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
        }*/
        return pl;
    }
    
    private PropertyList addMySyntacticFeatures(PropertyList pl, Sentence sentence, Tree root, int start, int end) {        
        if (root == null) return pl;
        List<Tree> leaves = root.getLeaves();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        String conn = sentence.toString(start, end).toLowerCase();
        String cat = connAnalyzer.getCategory(conn.toLowerCase());
        
        String selfFeat, parentFeat;
        Tree lca = analyzer.getLCA(root, leaves.subList(start, end+1));
        if (lca != null) {            
            Tree parent = lca.parent(root);
            while (parent != null && parent.isUnaryRewrite()) {
                lca = parent;
                parent = lca.parent(root);
            }
            selfFeat = "SELF="+lca.value();            
        } else {            
            selfFeat = "SELF=<NONE>";
        }

        Tree parent = lca.parent(root);        
        if (parent != null) {            
            parentFeat = "PARENT="+parent.value(); 
            List<Tree> children = parent.getChildrenAsList();            
                        
            int lcaPos = -1;
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i) == lca) {
                    lcaPos = i;
                    break;
                }
            }
            for (int i = 0; i < children.size(); i++) {                
                Tree child = children.get(i);
                
                pl = PropertyList.add("Sibling@" + (i - lcaPos) + "=" + child.value(), 1.0, pl);
                pl = PropertyList.add(conn + "&Sibling@" + (i - lcaPos) + "=" + child.value(), 1.0, pl);

            }
            //contains VP
            boolean vp = false;
            for (int i = lcaPos + 1; i < children.size(); i++) {
                Tree child = children.get(i);
                if (containsVP(child)) {
                    vp = true;
                    //break;
                }
                Tree head = HEAD_ANALYZER.getCollinsHead(child);
                if (head != null) {
                    pl = PropertyList.add("head@" + (i - lcaPos) + "=" + head.value(), 1.0, pl);
                    pl = PropertyList.add(conn + "&head@" + (i - lcaPos) + "=" + head.value(), 1.0, pl);
                }
            }
            pl = PropertyList.add("VP=" + vp, 1.0, pl);
            pl = PropertyList.add(conn+"&VP=" + vp, 1.0, pl);
        } else {            
            parentFeat = "PARENT=<NONE>";
        }
        
        pl = PropertyList.add(parentFeat, 1.0, pl);
        pl = PropertyList.add(cat + "&" + parentFeat, 1.0, pl);
        pl = PropertyList.add(conn+"&"+parentFeat, 1.0, pl);
        pl = PropertyList.add(parentFeat +"&"+ selfFeat, 1.0, pl);
        
        //chunk features
        Tree leftChunk = null;
        if (start > 0) {
            Tree leftLeaf = leaves.get(start - 1);
            leftChunk = leftLeaf;
            if (!leftLeaf.value().matches("[,;:]")) {
                leftChunk = leftLeaf.ancestor(2, root);
            }
        }
        Tree rightChunk = null;
        if ((end + 1) < sentence.size()) {
            Tree rightLeaf = leaves.get(end + 1);
            rightChunk = rightLeaf;
            if (!rightLeaf.value().matches("[,;:]")) {
                rightChunk = rightLeaf.ancestor(2, root);
            }
            
            //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
            Tree rightSibling = analyzer.getSibling(root, rightChunk, 1);
            if (rightSibling != null) {
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&"+rightSibling.value()+"@+2", 1.0, pl);
            } else {
                //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&Null", 1.0, pl);
            }
        }
        if (leftChunk != null && rightChunk != null) {
            pl = PropertyList.add(leftChunk.value() + "@-1&" + conn + "&" + rightChunk.value() + "@+1", 1.0, pl);
        }
        return pl;
    }
    
    boolean containsVP(Tree tree) {
        if (tree.isLeaf()) {
            return false;
        } else if (tree.value().equals("VP")) {
            return true;
        }
        
        boolean containsVP = false;
        List<Tree> children = tree.getChildrenAsList();        
        Queue<Tree> queue = new LinkedList<Tree>(children);
        
        while (!queue.isEmpty()) {
            Tree node = queue.remove();
            if (node.value().equals("VP")) {
                containsVP = true;
                break;
            } else if (node.isPhrasal()) {
                queue.addAll(node.getChildrenAsList());
            }
        }
        if (containsVP) {
            return true;
        } else {
            return false;
        }
    }
    private Sentence assignFakePOSTags(Sentence s, Tree root) {
        if (root != null) {
            List<Tree> leaves = root.getLeaves();
            for (int i = 0; i < leaves.size(); i++) {
                Tree parent = leaves.get(i).parent(root);
                s.get(i).setTag("POS", parent.value());
            }
        }
        return s;
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
    private String removeReferenceTag(String tag) {
        if (tag == null || !tag.contains("-")) {
            return tag;
        }
        String tokens[] = tag.split("-");
        int len = tokens.length;
        if (!tokens[len-1].matches("[0-9]+")) {
            return tag;
        }
        StringBuilder sb = new StringBuilder(tokens[0]);
        for (int i = 1; i < len - 1; i++) {
            sb.append("-");
            sb.append(tokens[i]);
        }
        return sb.toString();
    }
    
    private String getChunk4Leaf(Tree leaf, Tree root) {
        Tree anc = leaf.ancestor(2, root);
        Tree parent = leaf.parent(root);
        if (anc.value().endsWith("P")) {
            return anc.value().split("-")[0];
        } else if (parent.value().startsWith("N")) {
            return "NP";  
        } else if (parent.value().startsWith("V")) {
            return "VP";
        } else if (parent.value().matches("IN|TO")) {
            return "PP";
        } else if (parent.value().startsWith("R")) {
            return "ADVP";
        } else if (parent.value().startsWith("A")) {
            return "ADJP";
        }
        return "O";
    }    
}

