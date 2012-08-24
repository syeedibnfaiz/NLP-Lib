/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class HigherOrderRelationAnalyzer {
    private static final DiscourseAnnotator DISCOURSE_ANNOTATOR = new DiscourseAnnotator();
    private static final RelationAnalyzer RELATION_ANALYZER = new RelationAnalyzer();
    private static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public void showRelations(Sentence s, SimpleDepGraph depGraph) {
        Tree root = s.getParseTree();
        
        System.out.println(s.toString(new String[]{"DIS_CONN", "ARG1", "ARG2"}));
        System.out.println("===============================\n");
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("DIS_CONN").equals("O")) {
                String pos = String.valueOf(i);
                int arg1 = -1;
                int arg2 = -1;
                for (int j = 0; j < s.size(); j++) {
                    if (s.get(j).getTag("ARG1").equals(pos)) {
                        arg1 = j;
                    }
                    if (s.get(j).getTag("ARG2").equals(pos)) {
                        arg2 = j;
                    }
                }
                if (arg1 == -1) {
                    System.out.println("Arg1:- null");
                } else {
                    System.out.print("Arg1:-" + s.get(arg1).word());
                }
                String conn = word.word();
                for (int j = i + 1; j < s.size() && s.get(j).getTag("DIS_CONN").startsWith("I"); j++) {
                    conn += " " + s.get(j).word();
                }
                System.out.print(" :: " + conn + " :: ");
                System.out.println("Arg2:-" + s.get(arg2).word());
                
                if (arg1 != -1) {
                    System.out.println("----------ARG1 Relations-----------");
                    showRelations(root, depGraph, arg1);
                }
                System.out.println("----------ARG2 Relations-----------");
                showRelations(root, depGraph, arg2);                
                System.out.println("===============================");
            }
        }
        
    }
    
    private void showRelations(Tree root, SimpleDepGraph depGraph, int arg) {
        List<Tree> leaves = root.getLeaves();
        Tree leaf = root.getLeaves().get(arg);
        Tree parent = leaf.parent(root);

        if (!parent.value().matches("VB.*")) {
            List<SimpleDependency> dependencies = depGraph.getDepDependencies(arg);
            SimpleDependency tDep = dependencies.get(0);
            if (!tDep.reln().matches("advcl|ccomp")) {
                arg = tDep.gov();
            } else {
                return;
            }
        }

        List<SimpleDependency> dependencies = depGraph.getGovDependencies(arg);
        SimpleDependency seed = null;
        for (SimpleDependency dep : dependencies) {
            if (dep.reln().matches("nsubj|nsubjpass")) {
                seed = dep;
                break;
            }
        }
        if (seed == null) return;
        
        List<SimpleDependency> tmpList = new ArrayList<SimpleDependency>();
        List<List<SimpleDependency>> relations = new ArrayList<List<SimpleDependency>>();
        tmpList.add(seed);
        RELATION_ANALYZER.exploreSPORelation(root, depGraph, arg, tmpList, relations);


        for (List<SimpleDependency> relation : relations) {
            for (int i = 0; i < relation.size(); i++) {
                if (i == 0) {
                    int dep = relation.get(i).dep();
                    Tree chunk = TREE_ANALYZER.getNPChunk(root, dep);
                    System.out.println("Subj: " + TREE_ANALYZER.toString(chunk));
                    System.out.print(leaves.get(dep).value() + "--[" + relation.get(i).reln() + "]-- ");
                } else {
                    int gov = relation.get(i).gov();
                    System.out.print(leaves.get(gov).value() + "--[" + relation.get(i).reln() + "]-- ");
                    if (i == (relation.size() - 1)) {
                        int dep = relation.get(i).dep();
                        System.out.println(leaves.get(dep).value());
                        Tree chunk = TREE_ANALYZER.getNPChunk(root, dep);
                        System.out.println("Obj: " + TREE_ANALYZER.toString(chunk));
                    }
                }
            }
            System.out.println("");
        }
    }
    public static void main(String args[]) {
        TreebankLanguagePack TLP = new PennTreebankLanguagePack();
        GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();        
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        SimpleSentReader sentReader = new SimpleSentReader();
        ParserAnnotator annotator = new ParserAnnotator();        
        DiscourseAnnotator discAnnotator = new DiscourseAnnotator();
        HigherOrderRelationAnalyzer hRelAnalyzer = new HigherOrderRelationAnalyzer();
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            s = discAnnotator.annotate(s);
            Tree root = s.getParseTree();
            root.pennPrint();
            List<Tree> leaves = root.getLeaves();
            GrammaticalStructure gs = GSF.newGrammaticalStructure(root);
            Collection<TypedDependency> dependencies = gs.typedDependenciesCCprocessed();
            System.out.println(dependencies);
            SimpleDepGraph depGraph = new SimpleDepGraph(dependencies);
            hRelAnalyzer.showRelations(s, depGraph);
        }
    }
}
