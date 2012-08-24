/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
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
public class RelationAnalyzer {
    
    public List< List<SimpleDependency> > getSPORelations(Tree root, SimpleDepGraph depGraph) {
        List< List<SimpleDependency> > relations = new ArrayList< List<SimpleDependency> > ();        
        for (SimpleDependency dep : depGraph) {
            if (dep.reln().matches("nsubj|nsubjpass")) {
                List<SimpleDependency> tmpList = new ArrayList<SimpleDependency>();
                tmpList.add(dep);
                exploreSPORelation(root, depGraph, dep.gov(), tmpList, relations);
            }
        }
        return relations;
    }
    
    public void exploreSPORelation(Tree root, SimpleDepGraph depGraph, int i, List<SimpleDependency> tmpList, List<List<SimpleDependency>> relations) {
        Tree leaf = root.getLeaves().get(i);
        Tree ancestor = leaf.ancestor(2, root);
        if (ancestor.value().equals("NP")) {
            List<SimpleDependency> newList = new ArrayList<SimpleDependency>(tmpList);
            relations.add(newList);
        }
        List<SimpleDependency> dependents = depGraph.getGovDependencies(i);
        for (SimpleDependency dep : dependents) {
            String reln = dep.reln();
            if (reln.matches("dobj|iobj|pobj|prep.*|xcomp") /*|| (leaf.value().matches("VB.*") && reln.matches("prep.*"))*/) {
                tmpList.add(dep);
                exploreSPORelation(root, depGraph, dep.dep(), tmpList, relations);
                tmpList.remove(tmpList.size() - 1);
            }
        }
    }
    
    public static void main(String args[]) {
        TreebankLanguagePack TLP = new PennTreebankLanguagePack();
        GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
        RelationAnalyzer relationAnalyzer = new RelationAnalyzer();
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        SimpleSentReader sentReader = new SimpleSentReader();
        ParserAnnotator annotator = new ParserAnnotator();        
        Scanner in = new Scanner(System.in);
        String line;
        while (true) {
            System.out.println("Enter a line: ");
            line = in.nextLine();
            Sentence s = sentReader.read(line);
            s = annotator.annotate(s);
            Tree root = s.getParseTree();
            root.pennPrint();
            List<Tree> leaves = root.getLeaves();
            GrammaticalStructure gs = GSF.newGrammaticalStructure(root);
            Collection<TypedDependency> dependencies = gs.typedDependenciesCCprocessed();
            System.out.println(dependencies);
            SimpleDepGraph depGraph = new SimpleDepGraph(dependencies);
            System.out.println(depGraph);
            List<List<SimpleDependency>> relations = relationAnalyzer.getSPORelations(root, depGraph);
            for (List<SimpleDependency> relation : relations) {                
                for (int i = 0; i < relation.size(); i++) {                    
                    if (i == 0) {
                        int dep = relation.get(i).dep();
                        Tree chunk = treeAnalyzer.getNPChunk(root, dep);
                        System.out.println("Subj: " + treeAnalyzer.toString(chunk));
                        System.out.print(leaves.get(dep).value() + "--[" + relation.get(i).reln() + "]-- ");
                    } else {                         
                        int gov = relation.get(i).gov();
                        System.out.print(leaves.get(gov).value() + "--[" + relation.get(i).reln() + "]-- ");
                        if ( i == (relation.size()-1)) {
                            int dep = relation.get(i).dep();
                            System.out.println(leaves.get(dep).value());
                            Tree chunk = treeAnalyzer.getNPChunk(root, dep);
                            System.out.println("Obj: " + treeAnalyzer.toString(chunk));
                        }
                    }
                }                
                //System.out.println("");
            }
        }
    }
}
