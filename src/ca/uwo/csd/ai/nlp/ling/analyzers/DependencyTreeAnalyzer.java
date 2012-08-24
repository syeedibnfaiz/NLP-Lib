/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.analyzers;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DependencyTreeAnalyzer {
    private static final TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    private static final GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    
    /**
     * Traverses the collapsed dependency graph to get the path form src to dest
     * @param root
     * @param srcIndex
     * @param destIndex
     * @return 
     */
    public String getPath(Tree root, int srcIndex, int destIndex) {
        //in dep graph index starts form 1
        srcIndex = srcIndex + 1;
        destIndex = destIndex + 1;
        int numLeaves = root.getLeaves().size();
        //System.out.println("numleaves: " + numLeaves);
        int parent[] = new int[numLeaves + 1];
        boolean visited[] = new boolean[numLeaves + 1];
        
        GrammaticalStructure gs = gsf.newGrammaticalStructure(root);
        Collection<TypedDependency> dependencies = gs.typedDependenciesCollapsed();
        TypedDependency graph[][] = new TypedDependency[numLeaves + 1][numLeaves + 1];
        for (TypedDependency td : dependencies) {
            int gov = td.gov().index();
            int dep = td.dep().index();
            graph[gov][dep] = td;
            graph[dep][gov] = td;
        }        
        Queue<Integer> queue = new LinkedList<Integer>();

        queue.add(destIndex);
        while (!queue.isEmpty()) {
            int v = queue.remove();
            visited[v] = true;            

            if (v == srcIndex) {
                break;
            }
            for (int i = 1; i <= numLeaves; i++) {
                if (visited[i]) {
                    continue;
                }

                if (graph[v][i] != null || graph[i][v] != null) {
                    parent[i] = v;
                    queue.add(i);
                }
            }
        }
        if (parent[srcIndex] != 0) {
            StringBuilder sb = new StringBuilder();
            int v = srcIndex;
            int p = parent[v];
            while (v != destIndex) {                                                
                TypedDependency td = graph[p][v];
                if (td.gov().index() == p) {
                    sb.append("-").append(td.reln().getShortName()).append(":");
                } else {                    
                    sb.append(td.reln().getShortName()).append(":");
                }
                v = p;
                p = parent[v];
            }
            return sb.toString();
        } else {
            return null;
        }
        
    }
    
    public static void main(String args[]) {
        /*Treebank tb = new DiskTreebank(new TreeReaderFactory() {

            @Override
            public TreeReader newTreeReader(Reader in) {
                return new PennTreeReader(in, new LabeledScoredTreeFactory(), new NPTmpRetainingTreeNormalizer());
            }
        });
        tb.loadPath(new File("./pdtb_v2/ptb/03/wsj_0300.mrg"));*/
        LPSentReader sentReader = new LPSentReader("(~|\\(|\\)|\\||\\+|-|'|\\d|\\p{L})+|\\S");
        GeniaTagger tagger = new GeniaTagger();
        ParserAnnotator annotator = new ParserAnnotator();
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = tagger.annotate(s);
            s = annotator.annotate(s);
            Tree root = s.getParseTree();
            
            TreebankLanguagePack tlp = new PennTreebankLanguagePack();
            GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
            TreePrint tp = new TreePrint("oneline");
            
            GrammaticalStructure gs = gsf.newGrammaticalStructure(root);                       
            //System.out.println(gs.typedDependencies());
            Collection<TypedDependency> dependencies = gs.typedDependencies();
            System.out.println("Basic Dependency");
            System.out.println("-------------------------");
            for (TypedDependency td : dependencies) {
                System.out.println(td);
            }
            System.out.println("");
            //tp.printTree(root);
            System.out.println("----------------");            
            dependencies = gs.typedDependenciesCCprocessed();
            System.out.println("CCProcessed Dependency");
            System.out.println("-------------------------");
            for (TypedDependency td : dependencies) {
                System.out.println(td);
            }
            System.out.println("");
            
        }
    }
}
