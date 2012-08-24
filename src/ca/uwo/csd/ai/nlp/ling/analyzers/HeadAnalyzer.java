/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.analyzers;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import com.aliasi.io.FileLineReader;
import edu.stanford.nlp.ling.StringLabel;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.SimpleTree;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class HeadAnalyzer {
    private static SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    
    public Tree getCollinsHead(Tree t) {
        return getHead(t, collinsHeadrules);
    }
    /**
     * Finds head according to midified Collin's rules
     * @param t
     * @return 
     */
    public Tree getSemanticHead(Tree t) {
        return getHead(t, semanticHeadRules);
    }
    /**
     * Finds head according to Wellner's modified rules
     * @param t
     * @return 
     */
    public Tree getSyntacticHead(Tree t) {
        return getHead(t, syntacticHeadRules);
    }
    
    public Tree getSemanticHead(Tree root, List<Tree> trees) {        
        return getHead(root, trees, semanticHeadRules);
    }
    public Tree getSyntacticHead(Tree root, List<Tree> trees) {        
        return getHead(root, trees, syntacticHeadRules);
    }
    /**
     * Finds head by going through the following steps
     * 1. Construct a new tree considering only the given nodes, the lca and the intermediate nodes in the paths
     * from lca to the nodes.
     * 2. get head for the new tree
     * @param root
     * @param trees 
     */
    public Tree getHead(Tree root, List<Tree> trees, String[][] headRules) {        
        if (trees.size() == 1) return getHead(trees.get(0), headRules);
        Tree lca = treeAnalyzer.getLCA(root, trees);
        if (lca != null) {
            Tree newLca = new LabeledScoredTreeNode(new StringLabel(lca.value()));
            HashMap<String, Tree> map = new HashMap<String, Tree>();
            for (Tree t : trees) {
                List<Tree> dominationPath = lca.dominationPath(t);
                boolean success = construct(lca, dominationPath, 1, newLca, map);
                if (!success) {
                    return null;
                }
            }
            //System.out.print("newLCA: ");
            //newLca.pennPrint();
            return getHead(newLca, headRules);
        } else {            
            return null;
        }        
    }
    private boolean construct(Tree lca, List<Tree> path, int i, Tree parent, HashMap<String, Tree> map) {
        ArrayList<Tree> list = new ArrayList<Tree>();
        if (i >= path.size()) {
            return false;
        }
        list.add(path.get(i));
        String gornAddress = treeAnalyzer.getGornAddress(lca, list, -1);
        Tree t = null;
        if (i == (path.size() - 1)) {
            t = path.get(i);
        } else if (map.containsKey(gornAddress)) {
            t = map.get(gornAddress);
        } else {
            t = new LabeledScoredTreeNode(new StringLabel(path.get(i).value()));
            map.put(gornAddress, t);
        }
        parent.addChild(t);
        
        if (i < (path.size() - 1)) {
            construct(lca, path, i+1, t, map);
        }
        
        return true;
    }
    /**
     * Finds the head of the given node recursively
     * Changed on 26/12/2011 to handle GS parses
     * @param t
     * @param headRules
     * @return 
     */
    public Tree getHead(Tree t, String[][] headRules) {
        
        //System.out.print(t.value() + " --> ");
        if (t.isLeaf()) {
            return t;
        } else if (t.isPreTerminal()) {
            return t.getChild(0);
        //} else if (t.value().equals("NP")) {
          } else if (isMatch(t.value(), "NP")) {
            List<Tree> children = t.getChildrenAsList();
            int n = children.size();
            //if (children.get(n - 1).value().equals("POS")) {
            if (isMatch(children.get(n - 1).value(), "POS")) {
                return getHead(children.get(n - 1), headRules);
            } else {
                for (int i = n-1; i >= 0; i--) {
                    //if (children.get(i).value().matches("NN|NNP|NNPS|NNS|NX|POS|JJR")) {
                    if (isMatch(children.get(i).value(), "NN|NNP|NNPS|NNS|NX|POS|JJR")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                for (int i = 0; i < n; i++) {
                    //if (children.get(i).value().matches("NP")) {
                    if (isMatch(children.get(i).value(), "NP")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                for (int i = n-1; i >= 0; i--) {
                    //if (children.get(i).value().matches("\\$|ADJP|PRN")) {
                    if (isMatch(children.get(i).value(), "\\$|ADJP|PRN")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                for (int i = n-1; i >= 0; i--) {
                    //if (children.get(i).value().matches("CD")) {
                    if (isMatch(children.get(i).value(), "CD")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                for (int i = n-1; i >= 0; i--) {
                    //if (children.get(i).value().matches("JJ|JJS|RB|QP")) {
                    if (isMatch(children.get(i).value(), "JJ|JJS|RB|QP")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                //return last non-punctuation
                for (int i = n - 1; i >= 0; i-- ) {
                    if (!children.get(i).value().matches(":|,|\\.|-LRB-|-RRB-|``|''")) {
                        return getHead(children.get(i), headRules);
                    }
                }
                return getHead(children.get(n-1), headRules);
            }
            
        } else {
            int rule = -1;
            for (int i = 0; i < headRules.length; i++) {
                //if (headRules[i][0].equals(t.value())) {
                if (isMatch(t.value(), headRules[i][0])) {
                    rule = i;
                    break;
                }
            }
            if (rule != -1) {
                int direction = -1;
                if (headRules[rule][1].equals("RIGHT")) direction = 1;
                
                List<Tree> children = t.getChildrenAsList();
                if (children.isEmpty()) return null;
                
                boolean matched = false;
                int matchedIndex = -1;
                int start = 0;
                int end = children.size() - 1;
                if (direction == -1) {
                    start = children.size() - 1;
                    end = 0;
                }
                for (int i = 2; i < headRules[rule].length; i++) {
                    for (int j = start; ; j += direction) {
                        //if (children.get(j).value().matches(headRules[rule][i])) {
                        if (isMatch(children.get(j).value(), headRules[rule][i])) {
                            matched = true;
                            matchedIndex = j;
                            break;
                        }
                        if (j == end) break;
                    }
                    if (matched) break;                    
                }
                if (!matched) {
                    matchedIndex = start;
                    for (int j = start; ; j += direction) {
                        if (!children.get(j).value().matches(":|,|\\.|-LRB-|-RRB-|``|''")) {                            
                            matchedIndex = j;
                            break;
                        }                        
                        if (j == end) break;
                    }
                }
                boolean isCoordinated = false;
                for (Tree child : children) {
                    //if (child.value().equals("CC")) {
                    if (isMatch(child.value(), "CC")) {
                        isCoordinated = true;
                        break;
                    }
                }
                
                if (isCoordinated) {
                    int h = matchedIndex;
                    int n = children.size();
                    //if (h < (n-2) && children.get(h+1).value().equals("CC")) {
                    if (h < (n-2) && isMatch(children.get(h+1).value(), "CC")) {
                        return getHead(children.get(h), headRules);
                    //} else if (h >= 2 && children.get(h-1).value().equals("CC")) {
                      } else if (h >= 2 && isMatch(children.get(h-1).value(), "CC")) {
                        return getHead(children.get(h-2), headRules);
                    } else {
                        return getHead(children.get(h), headRules);
                    }
                } else {
                    return getHead(children.get(matchedIndex), headRules);
                }
            } else {
                return null;
            }
        }        
    }
    /**
     * Checks whether the value of a constituent matches against a pattern
     * Handles both GS tree and auto parses
     * @param value
     * @param pat
     * @return 
     */
    boolean isMatch(String value, String pat) {
        if (value.matches(pat)) return true;
        else if (value.split("-")[0].matches(pat)) return true;
        else if (value.split("=")[0].matches(pat)) return true;
        return false;
    }
    /**
     * test the head finding algorithm
     */
    public void test() {
        String ptbFilePath = "./pdtb_v2/ptb/03/wsj_0300.mrg";
        Scanner in = new Scanner(System.in);
        String line;
        while ((line = in.nextLine()) != null) {
            String gornAddress = line;
            String addresses[] = gornAddress.split(";");
            int lineNumber = Integer.parseInt(addresses[0].split(",")[0]);

            PTBFileReader ptbFileReader = new PTBFileReader();
            List<Tree> trees = ptbFileReader.read(new File(ptbFilePath));

            Tree root = trees.get(lineNumber);
            
            root.pennPrint();
            List<Tree> list = new ArrayList<Tree>();
            for (String address : addresses) {
                Tree node = treeAnalyzer.getGornNode(root, address);
                list.add(node);                
            }
            Tree head = getSyntacticHead(root, list);
            //System.out.println(head.value());
            //head.pennPrint();
            System.out.println(treeAnalyzer.getPennOutput(head));
        }
    }
    //constituent, direction, priorityList
    //LEFT :- from right to left
    //RIGHT :- form left to right
    private static String[][] syntacticHeadRules = new String[][]{
        {"ADJP", "LEFT", "NNS", "QP", "NN", "$", "ADVP", "JJ", "VBN", "VBG", "ADJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"},
        {"ADVP", "RIGHT", "RB", "RBR", "RBS", "FW", "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"},
        {"CONJP", "RIGHT", "CC", "RB", "IN"},
        {"FRAG", "RIGHT", "(NN|NP)", "W.*", "SBAR", "(PP|IN)", "(ADJP|JJ)", "ADVP", "PP"},
        {"INTJ", "LEFT"},
        {"LST", "RIGHT", "LS", ":"},
        {"NAC", "LEFT", "NN", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "FW"},
        {"PP", "RIGHT", "IN", "TO", "VBG", "VBN", "RP", "FW"},        
        {"WHPP", "RIGHT", "IN", "TO", "VBG", "VBN", "RP", "FW"},
        {"PRN", "RIGHT", "S.*", "N.*", "W.*", "(PP|IN)", "(ADJP|JJ)", "(ADVP|RB)"},
        {"PRT", "RIGHT", "RP"},
        {"QP", "LEFT", "(\\$|IN|NNS|NN|JJ|RB|DT|CD|NCD|QP|JJR|JJS)"},
        {"RRC", "RIGHT", "VP", "NP", "ADVP", "ADJP", "PP"},
        {"S", "RIGHT", "VP", ".*-PRD", "S", "SBAR", "ADJP", "UCP", "NP", "FRAG", "SINV", "PP"},
        {"SBAR", "RIGHT", "S", "SQ", "SINV", "SBAR", "FRAG", "IN", "DT"},
        {"SBARQ", "RIGHT", "SW", "S", "SINV", "SBARQ", "FRAG", "VP"},
        {"SINV", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", ".*-PRD", "VP", "SQ", "FRAG", "S", "SINV", "SBAR", "SBARQ"},
        {"SQ", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", ".*-PRD", "VP", "SQ", "FRAG", "S", "SINV", "SBAR", "SBARQ"},
        {"UCP", "RIGHT"},
        {"VP", "RIGHT", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", ".*-PRD", "ADJP", "NN", "NNS", "NP", "S", "SINV", "SBAR", "SBARQ", "SQ"},
        {"WHADJP", "LEFT", "CC", "WRB", "JJ", "ADJP"},
        {"WHADVP", "RIGHT", "CC", "WRB"},
        {"WHNP", "LEFT", "NN", "WDT", "WP.*", "WHADJP", "WHPP", "WHNP"},
        {"X", "LEFT"}
    };
    
    //constituent, direction, priorityList
    //LEFT :- from right to left
    //RIGHT :- form left to right
    private static String[][] semanticHeadRules = new String[][]{
        {"ADJP", "RIGHT", "NNS", "QP", "NN", "$", "ADVP", "JJ", "VBN", "VBG", "ADJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"},
        {"ADVP", "LEFT", "RB", "RBR", "RBS", "FW", "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"},
        {"CONJP", "LEFT", "CC", "RB", "IN"},
        {"FRAG", "LEFT"},
        {"INTJ", "RIGHT"},
        {"LST", "LEFT", "LS", ":"},
        {"NAC", "RIGHT", "NN", "NNS", "NNP", "NNPS", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "FW"},
        {"PP", "LEFT", "IN", "TO", "VBG", "VBN", "RP", "FW"},
        {"PRN", "RIGHT"},
        {"PRT", "LEFT", "RP"},
        {"QP", "RIGHT", "$", "IN", "NNS", "NN", "JJ", "RB", "DT", "CD", "NCD", "QP", "JJR", "JJS"},
        {"RRC", "LEFT", "VP", "NP", "ADVP", "ADJP", "PP"},
        {"S", "RIGHT", "TO", "IN", "VP", "S", "SBAR", "ADJP", "UCP", "NP"},
        {"SBAR", "RIGHT", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT", "S", "SQ", "SINV", "SBAR", "FRAG"},
        {"SBARQ", "RIGHT", "SQ", "S", "SINV", "SBARQ", "FRAG"},
        {"SINV", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "SQ"},
        {"SQ", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "SQ"},
        {"UCP", "LEFT"},
        {"VP", "RIGHT", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "ADJP", "NN", "NNS", "NP"},
        {"WHADJP", "RIGHT", "CC", "WRB", "JJ", "ADJP"},
        {"WHADVP", "LEFT", "CC", "WRB"},
        {"WHNP", "RIGHT", "WDT", "WP", "WP$", "WHADJP", "WHPP", "WHNP"},
        {"WHPP", "LEFT", "IN", "TO", "FW"}
    };

    //constituent, direction, priorityList
    //LEFT :- from right to left
    //RIGHT :- form left to right
    private static String[][] collinsHeadrules = new String[][] {
        {"ADJP", "RIGHT", "NNS", "QP", "NN", "$", "ADVP", "JJ", "VBN", "VBG", "ADJP", "JJR", "NP", "JJS", "DT", "FW", "RBR", "RBS", "SBAR", "RB"},
        {"ADVP", "LEFT", "RB", "RBR", "RBS", "FW", "ADVP", "TO", "CD", "JJR", "JJ", "IN", "NP", "JJS", "NN"},
        {"CONJP", "LEFT", "CC", "RB", "IN"},
        {"FRAG", "RIGHT", "(NN|NP)", "W.*", "SBAR", "(PP|IN)", "(ADJP|JJ)", "ADVP", "PP"},
        {"INTJ", "LEFT"},
        {"LST", "LEFT", "LS", ":"},
        {"NAC", "RIGHT", "NN", "NNS", "NNP", "NNPS", "NP", "NAC", "EX", "$", "CD", "QP", "PRP", "VBG", "JJ", "JJS", "JJR", "ADJP", "FW"},
        {"PP", "LEFT", "IN", "TO", "VBG", "VBN", "RP", "FW"},
        {"WHPP", "RIGHT", "IN", "TO", "VBG", "VBN", "RP", "FW"},
        {"PRN", "RIGHT", "S.*", "N.*", "W.*", "(PP|IN)", "(ADJP|JJ)", "(ADVP|RB)"},
        {"PRT", "RIGHT", "RP"},
        {"QP", "RIGHT", "(\\$|IN|NNS|NN|JJ|RB|DT|CD|NCD|QP|JJR|JJS)"},
        {"RRC", "LEFT", "VP", "NP", "ADVP", "ADJP", "PP"},
        {"S", "RIGHT", "TO", "IN", "VP", "S", "SBAR", "ADJP", "UCP", "NP", "FRAG", "SINV", "PP"},
        {"SBAR", "RIGHT", "WHNP", "WHPP", "WHADVP", "WHADJP", "IN", "DT", "S", "SQ", "SINV", "SBAR", "FRAG"},
        {"SBARQ", "RIGHT", "SQ", "S", "SINV", "SBARQ", "FRAG", "VP"},
        {"SINV", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", "VP", "SQ", "FRAG", "S", "SINV", "SBAR", "SBARQ"},
        {"SQ", "RIGHT", "VBZ", "VBD", "VBP", "VB", "MD", ".*-PRD", "VP", "SQ", "FRAG", "S", "SINV", "SBAR", "SBARQ"},
        {"UCP", "RIGHT"},
        {"VP", "RIGHT", "TO", "VBD", "VBN", "MD", "VBZ", "VB", "VBG", "VBP", "VP", "ADJP", "NN", "NNS", "NP", "S", "SINV", "SBAR", "SBARQ", "SQ"},
        {"WHADJP", "RIGHT", "CC", "WRB", "JJ", "ADJP"},
        {"WHADVP", "LEFT", "CC", "WRB"},
        {"WHNP", "RIGHT", "WDT", "WP.*", "WHADJP", "WHPP", "WHNP"},
		{"WHPP", "LEFT", "IN", "TO", "FW"},
        {"X", "LEFT"}
    };
    
    public static void main(String args[]) {
        LPSentReader sentReader = new LPSentReader("\\S+");
        ParserAnnotator annotator = new ParserAnnotator();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            Tree root = s.getParseTree();
            root.pennPrint();                        
            
            System.out.println("Enter gorn address: ");
            line = in.nextLine();
            List<Tree> list = analyzer.getGornNodes(root, line);                        
            //System.out.println(analyzer.getPennOutput(t));
            System.out.println("Semantic head: " + headAnalyzer.getSemanticHead(root, list));
            System.out.println("Syntactic head: " + headAnalyzer.getSyntacticHead(root, list));
            
            List<Tree> dominationPath = root.dominationPath(headAnalyzer.getSyntacticHead(root, list));
            for (Tree dt : dominationPath) {
                System.out.print(dt.value()+":");
            }
            //System.out.println("");
            /*ArrayList<Tree> list = new ArrayList<Tree>();
            list.add(t);
            System.out.println("Enter another gorn address: ");
            line = in.nextLine();
            t = analyzer.getGornNode(root, line);            
            list.add(t);
            headAnalyzer.getSemanticHead(root, list);*/
        }
        //HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        //headAnalyzer.test();
    }
}
