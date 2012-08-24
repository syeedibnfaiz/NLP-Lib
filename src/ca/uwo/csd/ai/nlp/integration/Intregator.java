/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.integration;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Intregator {
    private static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    private static final PTBFileReader PTB__READER = new PTBFileReader();
    private static final SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    private static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    int totalP1 = 0;
    int totalP2 = 0;
    int totalMiss1 = 0;
    int totalMiss2 = 0;
    int totalWrong1 = 0;
    int totalWrong2 = 0;
    int totalToken1 = 0;
    int totalToken2 = 0;
    int tokenMiss1 = 0;
    int tokenMiss2 = 0;
    int tokenWrong1 = 0;
    int tokenWrong2 = 0;
    public void getScopes(SimpleDepGraph depGraph, int arg1Head, int arg2Head, List<Integer> scope1, List<Integer> scope2) {
        List<Integer> reachable1 = depGraph.getReachableIndices(arg1Head, true, 100);
        List<Integer> reachable2 = depGraph.getReachableIndices(arg2Head, true, 100);
        if (reachable1.contains(arg2Head)) {
            for (Integer i : reachable2) {
                reachable1.remove(i);
            }
        } else if (reachable2.contains(arg1Head)) {
            for (Integer i : reachable1) {
                reachable2.remove(i);
            }
        }
        scope1.addAll(reachable1);
        scope2.addAll(reachable2);
    }
    
    private boolean find(String text, String arg, List<Integer> positions) {
        String[] textTokens = text.split("\\s+");
        String[] argTokens = arg.split("\\s+");
        
        for (int i = 0; (i + argTokens.length) <= textTokens.length; i++) {
            int j = i + argTokens.length-1;
            boolean flg = true;
            for (int k = i; k <= j; k++) {
                if (!textTokens[k].equals(argTokens[k-i])) {
                    flg = false;
                    break;
                }
            }
            if (flg) {
                positions.add(i);
                positions.add(j);
                return true;
            }
        }
        return false;
    }
    
    private void measure(String corpusRoot, String text, String fileName, String lineNumber, String arg1, String arg2) {
        String fileId = fileName.replace(".mrg", "");
        List<Tree> trees = PTB__READER.read(new File(corpusRoot+"/trees", fileName));
        List<SimpleDepGraph> deps = DEP_READER.read(new File(corpusRoot + "/deps", fileId + ".dep"));
        int line = Integer.parseInt(lineNumber);
        
        List<Integer> positions1 = new ArrayList<Integer>();
        List<Integer> positions2 = new ArrayList<Integer>();
        
        if (find(text, arg1, positions1)) {
            if (find(text, arg2, positions2)) {                
                Tree root = trees.get(line);
                SimpleDepGraph depGraph = deps.get(line);
                List<Tree> nodes1 = TREE_ANALYZER.findGornNodes(root, positions1.get(0), positions1.get(1));
                List<Tree> nodes2 = TREE_ANALYZER.findGornNodes(root, positions2.get(0), positions2.get(1));
                
                Tree head1 = HEAD_ANALYZER.getSemanticHead(root, nodes1);
                Tree head2 = HEAD_ANALYZER.getSemanticHead(root, nodes2);
                int argHead1 = TREE_ANALYZER.getLeafPosition(root, head1);
                int argHead2 = TREE_ANALYZER.getLeafPosition(root, head2);
                
                //improve head
                argHead1 = getBetterHeadPos(root, depGraph, argHead1);
                argHead2 = getBetterHeadPos(root, depGraph, argHead2);
                
                List<Integer> scopes1 = new ArrayList<Integer>();
                List<Integer> scopes2 = new ArrayList<Integer>();
                getScopes(depGraph, argHead1, argHead2, scopes1, scopes2);
                
                int arg1Miss = 0;
                int arg2Miss = 0;
                Set<Integer> set1 = new HashSet<Integer>(scopes1);
                Set<Integer> set2 = new HashSet<Integer>(scopes2);
                String[] textTokens = text.split("\\s+");
                int p1 = 0;
                int p2 = 0;
                
                int m1 = 0;
                int m2 = 0;
                for (int i = positions1.get(0); i <= positions1.get(1); i++) {
                    if (textTokens[i].contains("PROTEIN") ) {
                        if (!set1.contains(i)) {
                            arg1Miss++;
                        }
                        p1++;
                    }
                    if (!set1.contains(i)) {
                        m1++;
                    }
                }
                totalToken1 += positions1.get(1) - positions1.get(0);
                for (int i = positions2.get(0); i <= positions2.get(1); i++) {
                    if (textTokens[i].contains("PROTEIN")) {
                        if (!set2.contains(i)) {
                            arg2Miss++;
                        }
                        p2++;
                    }
                    if (!set2.contains(i)) {
                        m2++;
                    }
                }
                totalToken2 += positions2.get(1) - positions2.get(0);
                tokenMiss1 += m1;
                tokenMiss2 += m2;
                
                int arg1Wrong = 0;
                int arg2Wrong = 0;
                
                int w1 = 0;
                int w2 = 0;
                for (Integer i : set1) {
                    if (i < positions1.get(0) || i > positions1.get(1)) {
                        if (textTokens[i].contains("PROTEIN")) {
                            arg1Wrong++;
                        }
                        w1++;
                    }
                }
                for (Integer i : set2) {
                    if (i < positions2.get(0) || i > positions2.get(1)) {
                        if (textTokens[i].contains("PROTEIN")) {
                            arg2Wrong++;
                        }
                        w2++;
                    }                    
                }
                tokenWrong1 += w1;
                tokenWrong2 += w2;
                
                System.out.println(text);
                System.out.println("Arg1: " + arg1);
                System.out.println("Arg2: " + arg2);
                System.out.println("Head1: " + textTokens[argHead1]);
                System.out.println("Head2: " + textTokens[argHead2]);
                System.out.print("Arg1 miss: " + arg1Miss + ", ");
                System.out.println("Arg2 miss: " + arg2Miss);
                if (arg2Miss > 0) {
                    //System.out.println(depGraph.toString(root));
                }
                System.out.println(depGraph.toString(root));
                System.out.print("Arg1 Wrong: " + arg1Wrong + ", ");
                System.out.println("Arg2 Wrong: " + arg2Wrong);
                
                System.out.println("miss token1: " + m1);
                System.out.println("miss token2: " + m2);
                System.out.println("wrong token1: " + w1);
                System.out.println("wrong token2: " + w2);

                System.out.println("----------------");
                
                
                totalP1 += p1;
                totalP2 += p2;
                totalMiss1 += arg1Miss;
                totalMiss2 += arg2Miss;
                totalWrong1 += arg1Wrong;
                totalWrong2 += arg2Wrong;
            } else {
                System.out.println("Arg2 not found for " + text);
            }
        } else {
            System.out.println("Arg1 not found for " + text);
        }
    }
    public void measure(String corpusRoot, String filePath) {
        List<String> lines = Util.readLines(filePath);
        int size = lines.size();
        int i = 0;
        int count = 0;
        while (i < size) {
            String text = lines.get(i++);
            String note = lines.get(i++);
            String fileName = lines.get(i++);
            String lineNumber = lines.get(i++);
            if (note.equals("*")) {
                String arg1 = lines.get(i++);
                String arg2 = lines.get(i++);
                measure(corpusRoot, text, fileName, lineNumber, arg1, arg2);
                count++;
            }
            i++;
        }
        System.out.println("count: " + count);
        System.out.println("Total p1: " + totalP1);
        System.out.println("Total p2: " + totalP2);
        System.out.println("Total miss1: " + totalMiss1);
        System.out.println("Total miss2: " + totalMiss2);
        System.out.println("Total wrong1: " + totalWrong1);
        System.out.println("Total wrong2: " + totalWrong2);
        System.out.println("Total tok in arg1: " + totalToken1);
        System.out.println("Total tok in arg2: " + totalToken2);
        System.out.println("Total token miss in arg1: " + tokenMiss1);
        System.out.println("Total token miss in arg2: " + tokenMiss2);
        System.out.println("Total token wrong in arg1: " + tokenWrong1);
        System.out.println("Total token wrong in arg2: " + tokenWrong2);
    }
    int getBetterHeadPos(Tree root, SimpleDepGraph depGraph, int oldPos) {
        int newPos = oldPos;
        //if (root.getLeaves().get(oldPos).parent(root).value().matches("MD")) return oldPos;
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(oldPos);
        if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("det|nn|cop|aux.*|prep.*|complm")) {
            newPos = depDependencies.get(0).gov();
        }
        return newPos;
    }
    public static void main(String args[]) {
        Intregator intregator = new Intregator();
        intregator.measure("./resource/relation/PPI2/AImed","./resource/relation/AIMed_discourse_argument_HOR.txt");
    }
}

