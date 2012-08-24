/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.analyzers;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SyntaxTreeAnalyzer implements Serializable {

    private static TreeFactory tf = new LabeledScoredTreeFactory();
    
    public Tree getPennTree(String s) throws IOException {
        TreeReader tr = new PennTreeReader(new StringReader(s), tf);
        return tr.readTree();
    }
    public Tree getLCA(Tree root, int start, int end) {
        return getLCA(root, root.getLeaves().subList(start, end+1));
    }
    public Tree getLCA(Tree root, List<Tree> trees) {
        if (trees.size() == 1) {
            return trees.get(0).ancestor(1, root);
        }
        for (int i = 1; ; i++) {
            Tree anc = trees.get(0).ancestor(i, root);
            if (anc == null) break;
            boolean flg = true;
            for (int j = 1; j < trees.size(); j++) {
                Tree leaf = trees.get(j);
                if (!anc.dominates(leaf)) {
                    flg = false;
                    break;
                }
            }
            if (flg) {
                return anc;
            }
        }
        return null;
    }
    
    public Tree getLCA2(Tree root, List<Tree> trees) {
        if (trees.size() == 1) {
            return trees.get(0).ancestor(1, root);
        }
        List<List<Tree>> paths = new ArrayList<List<Tree>>();
        for (Tree t : trees) {
            paths.add(root.dominationPath(t));
        }
        Tree lca = null;
        int j = 0;
        while (true) {
            Tree dummy = null;
            boolean mismatch = false;
            for (int i = 0; i < paths.size(); i++) {
                if (j >= paths.get(i).size()) {
                    mismatch = true;
                    break;
                }
                if (dummy == null) {
                    dummy = paths.get(i).get(j);
                } else if (paths.get(i).get(j) != dummy){
                    mismatch = true;
                    break;
                }
            }
            if (mismatch) break;
            else lca = dummy;
            j++;
        }
        return lca;
    }
    
    public Tree getSibling(Tree root, Tree t, int pos) {
        Tree parent = t.parent(root);
        if (parent == null) return null;

        List<Tree> siblings = parent.getChildrenAsList();
        /*int index = parent.indexOf(t);
        if (index == -1 || (index+pos) < 0 || (index+pos) >= siblings.size()) return null;
        return siblings.get(index + pos);*/
        int tPos = -1;
        for (int i = 0; i < siblings.size(); i++) {
            Tree sib = siblings.get(i);
            if (sib == t) {
                tPos = i;
                break;
            }
        }        
        if (tPos == -1 || (tPos + pos) < 0 || (tPos + pos) >= siblings.size()) {
            return null;
        }
        return siblings.get(tPos + pos);
    }
    
    public void write(Text text, File file) {
        try {
            PrintWriter writer = new PrintWriter(file);
            for (Sentence s : text) {
                Tree t = s.getParseTree();
                if (t != null) {
                    print(t, writer);
                    writer.write("\n");
                } else {
                    writer.write("\n");
                }
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(SyntaxTreeAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void print(Tree t, PrintWriter writer) {
        if (t.depth() == 1) {
            Tree child = t.getChild(0);
            writer.write("(" + t.value() + " " + child.value() + ")");
        } else {
            writer.write("(" + t.value());
            List<Tree> children = t.getChildrenAsList();
            for (Tree child : children) {
                writer.write(" ");
                print(child, writer);
            }
            writer.write(")");
            writer.flush();
        }
    }
    
    public String getPennOutput(Tree t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        print(t, writer);
        return stringWriter.toString();

    }
    
    public String getPath(Tree root, Tree sTree, Tree dTree) {
        List<Tree> dominationPath;
        String path = "";        
        
        if ((dominationPath = sTree.dominationPath(dTree)) != null) {            
            for (int i = 0; i < dominationPath.size(); i++) {
                if (i != 0) path += ":";
                path += dominationPath.get(i).value();
            }
        } else if ((dominationPath = dTree.dominationPath(sTree)) != null) {            
            for (int i = 0; i < dominationPath.size(); i++) {
                if (i != 0) path = ":" + path;
                path = dominationPath.get(i).value() + path;
            }
            
        } else {
            List<Tree> tmpList = new ArrayList<Tree>();
            tmpList.add(sTree);
            tmpList.add(dTree);
            Tree lca = getLCA(root, tmpList);            
            dominationPath = lca.dominationPath(sTree);
            for (int i = 0; i < dominationPath.size(); i++) {
                if (i != 0) path = ":" + path;
                path = dominationPath.get(i).value() + path;
            }
            dominationPath = lca.dominationPath(dTree);
            for (int i = 0; i < dominationPath.size(); i++) {             
                path += ":" + dominationPath.get(i).value();
            }
        }
        return path;
    }
    
    /**
     * Finds the prefix in sequence of leaves
     * @param root
     * @param prefix
     * @return the index of the leaf at the end of the matched sequence, or -1 if not matched
     */
    public int findPrefix(Tree root, String prefix) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder leavesSequence = new StringBuilder();
        for (int i = 0; i < leaves.size(); i++) {
            String word = leaves.get(i).value();
            if (word.matches("-LRB-|-RRB-|-LCB-|-RCB-")) {
                continue; //3 new characters L/R, R, B would hamper the matching process            
            }
            for (int j = 0; j < word.length(); j++) {
                if (Character.isLetterOrDigit(word.charAt(j))) {
                    leavesSequence.append(word.charAt(j));
                }
            }
            if (prefix.equals(leavesSequence.toString())) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Finds the set of nodes that dominates all the leaves form start-end
     * @param root
     * @param start
     * @param end
     * @return a list of those dominant nodes
     */
    public List<Tree> findGornNodes(Tree root, int start, int end) {
        List<Tree> list = new ArrayList<Tree>();
        for (int i = start; i <= end; i++) {            
            int k = i;                     
            for (int j = end; j > i; j--) {
                Tree lca = getLCA(root, i, j);
                if (lca.getLeaves().size() == (j - i + 1)) {
                    k = j;
                    break;
                }
            }
            list.add(getLCA(root, i, k));
            i = k;
        }
        return list;
    }
    
    public String getGornAddress(Tree root, List<Tree> trees, int n) {
        StringBuilder address = new StringBuilder();
        for (int i = 0; i < trees.size(); i++) {
            Tree t = trees.get(i);
            Tree parent = null;
            List<Integer> values = new ArrayList<Integer>();
            while (true) {
                parent = t.parent(root);
                if (parent == null || parent.value() == null || parent.value().equals("ROOT")) {
                    values.add(n);
                    break;
                }
                values.add(parent.indexOf(t));
                t = parent;
            }
            if (i != 0) address.append(";");
            for (int j = values.size() - 1; j >= 0; j--) {
                if (j < (values.size()-1)) address.append(",");
                address.append(values.get(j));
            }
        }
        return address.toString();
    }
    
    public Tree getGornNode(Tree root, String gornAddress) {
        String[] tokens = gornAddress.split(",");
        //the first token is line lumber, skip it
        Tree node = root.getChild(0);
        for (int i = 1; i < tokens.length; i++) {
            int j = Integer.parseInt(tokens[i]);
            List<Tree> childrenAsList = node.getChildrenAsList();
            if (j < childrenAsList.size()) {
                node = node.getChildrenAsList().get(j);
            }
            //node = node.getChildrenAsList().get(j);
            
        }
        return node;
    }
    
    public List<Tree> getGornNodes(Tree root, String gornAddresses) {
        String addresses[] = gornAddresses.split(";");
        List<Tree> nodes = new ArrayList<Tree>();
        for (String address : addresses) {
            nodes.add(getGornNode(root, address));
        }
        return nodes;
    }
        
    /**
     * Finds the position of the given leaf in the subtree rooted at root
     * @param root
     * @param leaf
     * @return position of the leaf in the leaves list
     */
    
    public int getLeafPosition(Tree root, Tree leaf) {
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            //if (leaves.get(i).equals(leaf)) return i;
            //TODO: major change!!!! reference should be checked not equality
            //other wise two words like "in" can become equal!!!
            if (leaves.get(i) == leaf) return i;
        }
        return -1;
    }
    /**
     * Returns the sentence rooted at root
     * @param root
     * @return 
     */
    public String toString(Tree root) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leaves.size(); i++) {
            if (i != 0) sb.append(" ");
            sb.append(leaves.get(i).value());
        }
        return sb.toString();
    }
    /**
     * returns chunk type (NP/VP/O..) for a leaf
     * @param root
     * @param i
     * @return 
     */
    public String getChunkType(Tree root, int i) {
        String type = "O";
        Tree leaf = root.getLeaves().get(i);
        Tree ancestor = leaf.ancestor(2, root);
        if (ancestor.value().endsWith("P")) {
            type = ancestor.value();
        }
        return type;
    }
    
    public Tree getNPChunk(Tree root, int i) {
        if (!getChunkType(root, i).equals("NP")) {
            throw new IllegalArgumentException("Chunk type is not NP");
        }
        Tree leaf = root.getLeaves().get(i);
        Tree ancestor = leaf.ancestor(2, root);
        int h = 3;
        while (true) {
            Tree tmp = leaf.ancestor(h, root);
            if (tmp.value().equals("NP")) {
                ancestor = tmp;
            } else {
                break;
            }
            h++;
        }
        
        return ancestor;
    }
    
    public static void main(String args[]) {
        LPSentReader sentReader = new LPSentReader("\\S+");
        ParserAnnotator annotator = new ParserAnnotator();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            Tree root = s.getParseTree();
            root.pennPrint();
            
            System.out.println("Enter prefix: ");
            line = in.nextLine();
            int end = analyzer.findPrefix(root, line);
            List<Tree> gornNodes = analyzer.findGornNodes(root, 0, end);
            for (Tree t : gornNodes) {
                t.pennPrint();
            }
            System.out.println(analyzer.getGornAddress(root, gornNodes, 0));
            
            System.out.println("Enter gorn address");
            line = in.nextLine();
            Tree t = analyzer.getGornNode(root, line);
            System.out.println(analyzer.getPennOutput(t));
            
            if (t.isLeaf()) {
                int pos = analyzer.getLeafPosition(root, t);
                System.out.println("pos: " + pos);
            }
            List<Tree> pathNodeToNode = root.pathNodeToNode(root.getLeaves().get(0), root.getLeaves().get(2));
            
            for (Tree tree : pathNodeToNode) {
                System.out.print(tree.value()+",");
            }
            System.out.println("");
        }
    }
    
    public void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    public void assignPOS(Sentence s, Tree root) {        
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
}
