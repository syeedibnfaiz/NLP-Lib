/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
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
public class NewConnective2FeatureVector extends Pipe {

    private static Pattern pat = Pattern.compile("CC|TO|IN|PDT|[,.;:?!-+()]");
    private static TreeFactory tf = new LabeledScoredTreeFactory();
    private static final ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
    
    public NewConnective2FeatureVector() {
        super(new Alphabet(), new LabelAlphabet());
    }


    @Override
    public Instance pipe(Instance carrier) {
        ConnectiveInstance instance = (ConnectiveInstance) carrier;
        Sentence sentence = (Sentence) instance.getData();
        String syntax = sentence.getProperty("parse_tree");
        
        int start = instance.getS();
        int end = instance.getE();
        
        TreeReader tr = new PennTreeReader(new StringReader(syntax), tf);
        Tree root = null;
        try {
            root = tr.readTree();
        } catch (IOException ex) {
            Logger.getLogger(NewConnective2FeatureVector.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PropertyList pl = null;        
        pl = PropertyList.add(sentence.toString(start, end)/*.toLowerCase()*/, 1.0, pl);
        pl = PropertyList.add("LC="+sentence.toString(start, end).toLowerCase(), 1.0, pl);
        
        pl = addSyntaxTreeFeatures(pl, sentence, root, start, end);
        pl = addTemporalFeatures(pl, sentence, root, start, end);
        pl = addPathFeatures(pl, sentence, root, start, end);
        
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
