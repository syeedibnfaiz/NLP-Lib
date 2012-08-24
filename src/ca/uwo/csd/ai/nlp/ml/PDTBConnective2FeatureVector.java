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
public class PDTBConnective2FeatureVector extends Pipe {

    private static Pattern pat = Pattern.compile("CC|TO|IN|PDT|[,.;:?!-+()]");
    private static TreeFactory tf = new LabeledScoredTreeFactory();
    private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    
    public PDTBConnective2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
    }


    @Override
    public Instance pipe(Instance carrier) {
        PDTBConnectiveInstance instance = (PDTBConnectiveInstance) carrier;        
                        
        Sentence sentence = (Sentence) instance.getData();
        String syntax = sentence.getProperty("parse_tree");
        
        int start = instance.getS();
        int end = instance.getE();
        
        
        Tree root = null;
        try {
            root = treeAnalyzer.getPennTree(syntax); 
        } catch (IOException ex) {
            Logger.getLogger(NewConnective2FeatureVector.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        SimpleDepGraph depGraph = instance.getDepGraph();
        PropertyList pl = null;        
        pl = PropertyList.add(sentence.toString(start, end)/*.toLowerCase()*/, 1.0, pl);
        pl = PropertyList.add("LC="+sentence.toString(start, end).toLowerCase(), 1.0, pl);
        
        pl = addSyntaxTreeFeatures(pl, sentence, root, start, end);
        //pl = addClauseFeatures(pl, sentence, root, start, end);
        //pl = addTemporalFeatures(pl, sentence, root, start, end);
        //pl = addPathFeatures(pl, sentence, root, start, end);
        //pl = addConstituentFeatures(pl, root, depGraph, start, end);
        pl = addDependencyFeatures(pl, root, depGraph, start, end);
        
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
        pl = PropertyList.add("N1="+n1, 1.0, pl);
        pl = PropertyList.add("N2="+n2, 1.0, pl);
        pl = PropertyList.add("N3="+n3, 1.0, pl);
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
            if (value.equals("NP")) {
                countNP++;
                if (countVP == 0) NPb4VP = true;
            } else if (value.equals("VP")) {
                countVP++;
                if (countNP == 0) VPb4NP = true;
            } else if (value.equals("SBAR")) {
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
        if (parent != null) {            
            features.add("PARENT="+parent.value());            
        } else {            
            features.add("PARENT=<NONE>");
        }

        Tree left = analyzer.getSibling(root, lca, -1);
        if (left != null) {           
            //if left sibling is an RB then there is a big chance that it is a modifier
            if (left.value().matches("RB|ADVP|NP|DT|PP|,")) {
            //if (left.value().matches("RB|ADVP")) {
                Tree left2Left = analyzer.getSibling(root, lca, -2);
                if (left2Left != null) left = left2Left;
            }
            features.add("LEFT@-1-"+left.value());
        } else {
            features.add("LEFT@-1-<NONE>");
        }
        

        Tree right = analyzer.getSibling(root, lca, 1);
        if (right != null) {
            
            features.add("RIGHT@+1-"+right.value());
            
            if (!right.isLeaf()) {
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
        } else {            
            features.add("RIGHT@+1-<NONE>");
        }
                        
        for (int i = 0; i < features.size(); i++) {
            pl = PropertyList.add(features.get(i), 1.0, pl);
            pl = PropertyList.add(conn+"&"+features.get(i), 1.0, pl);
            /*for (int j = i + 1; j < features.size(); j++) {                
                pl = PropertyList.add(features.get(i) +"&"+features.get(j), 1.0, pl);
            }*/
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
            //pl = PropertyList.add(features.get(PARENT) +"&"+features.get(RIGHT_VP), 1.0, pl);
        }
        
        //if (1 < 2) return pl;
        //add right chunk info
        if ((start-1) >= 0 && (end + 1) < sentence.size()) {
            Tree rightLeaf = leaves.get(end + 1);
            Tree rightChunk = rightLeaf;
            if (!rightLeaf.value().matches("[,;:]")) {
                rightChunk = rightLeaf.ancestor(2, root);
            }
            Tree leftLeaf = leaves.get(start - 1);
            Tree leftChunk = leftLeaf;
            if (!leftLeaf.value().matches("[,;:]")) {
                leftChunk = leftLeaf.ancestor(2, root);
            }
            //pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
            Tree rightSibling = analyzer.getSibling(root, rightChunk, 1);
            if (rightSibling != null) {
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&"+rightSibling.value()+"@+2", 1.0, pl);
            } else {
                pl = PropertyList.add(conn+"&"+rightChunk.value()+"@+1&Null", 1.0, pl);
            }
            pl = PropertyList.add(leftChunk.value()+"@-1&"+conn+"&"+rightChunk.value()+"@+1", 1.0, pl);
            //pl = PropertyList.add(leftChunk.value()+"@-1&"+conn, 1.0, pl);
        }
        String category = connAnalyzer.getCategory(conn.toLowerCase());
        pl = PropertyList.add(features.get(PARENT)+"&"+category, 1.0, pl);
        
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
        pl = PropertyList.add("CAT&CLS-"+category+"&"+leftPattern, 1.0, pl);
        //pl = PropertyList.add("CONN&CLS-"+conn+"&"+leftPattern, 1.0, pl);
        pl = PropertyList.add("CAT&CLS+"+category+"&"+rightPattern, 1.0, pl);
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
        String conn = sentence.toString(start, end)/*.toLowerCase()*/;
        //if (!conn.toLowerCase().matches("when|since")) return pl;
        String year = "(18|19|20)[0-9][0-9]|year";
        String months = "january|february|march|april|may|june|july|august|september|october|november|december";
        //String ordinal = "first|last|second|third|fourth|fifth|sixth|seventh|eighth|nineth|tenth";
        String seasons = "summer|spring|fall|winter|season";
        String others = "decade|century|earlier|later|month|hour|before|after|day";
        
        boolean prev = false;
        for (int i = 0; i < start; i++) {
            String word = sentence.get(i).word().toLowerCase();
            if (word.matches(year)||word.matches(months)/*||word.matches(ordinal)*/||word.matches(seasons)) {
                pl = PropertyList.add(conn+"&L-TEMPORAL", 1.0, pl);
                prev = true;
                break;
            }            
        }
        for (int i = end + 1; i < sentence.size(); i++) {
            String word = sentence.get(i).word().toLowerCase();
            if (word.matches(year)||word.matches(months)/*||word.matches(ordinal)*/||word.matches(seasons)) {
                pl = PropertyList.add(conn+"&R-TEMPORAL", 1.0, pl);
                if (prev) {
                    pl = PropertyList.add("L-TEMPORAL&"+conn+"&R-TEMPORAL", 1.0, pl);
                }
                break;
            }
        }
        
        /*List<Tree> leaves = root.getLeaves();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        Tree connTree = leaves.get(start);
        Tree parent = connTree.parent(root);
        while (parent != null) {

            if (parent.value() != null && parent.value().startsWith("S")) {
                break;
            }
            connTree = parent;
            parent = connTree.parent(root);
        }
        if (parent != null && parent.value().startsWith("S")) {
            leaves = parent.getLeaves();
            for (Tree leaf : leaves) {
                String word = leaf.value().toLowerCase();
                if (word.matches(year) || word.matches(months) || word.matches(ordinal) || word.matches(seasons)) {
                    pl = PropertyList.add(conn + "&R-TEMPORAL", 1.0, pl);
                    break;
                }
            }
        }*/
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
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
    }
}
