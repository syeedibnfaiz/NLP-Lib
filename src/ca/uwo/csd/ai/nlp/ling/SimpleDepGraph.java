/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.SimpleTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.awt.Desktop;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.Pattern;
import sun.misc.Perf.GetPerfAction;

/**
 * TODO: need to make retrieval efficient.
 * @author Syeed Ibn Faiz
 */
public class SimpleDepGraph extends ArrayList<SimpleDependency> implements Serializable {

    /**
     * Reads a list of dependencies which follows the following format:
     * nn(Mr.-1, John-2), nsubj(tried-3, John-2),...
     * @param dependencyList 
     */    
    
    public SimpleDepGraph(String dependencyList) {
        super();
        String tokens[] = dependencyList.split("\t");
        if (tokens != null) {
            for (String dependency : tokens) {
                if (!dependency.equals("")) {
                    add(new SimpleDependency(dependency));
                }
            }
        }
    }
    public SimpleDepGraph(Collection<TypedDependency> dependencies) {
        super();
        for (TypedDependency td : dependencies) {
            add(new SimpleDependency(td.toString()));
        }
    }
    public SimpleDepGraph(Tree root, String type) {
        super();
        TreebankLanguagePack TLP = new PennTreebankLanguagePack();
        GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
        GrammaticalStructure gs = GSF.newGrammaticalStructure(root);
        Collection<TypedDependency> dependencies;
        
        if (type == null) type = "";
                
        if (type.equalsIgnoreCase("CCProcessed")) {
            dependencies = gs.typedDependenciesCCprocessed();
        } else if (type.equalsIgnoreCase("collapsed")) {
            dependencies = gs.typedDependenciesCollapsed();
        } else if (type.equalsIgnoreCase("collapsedTree")) {
            dependencies = gs.typedDependenciesCollapsedTree();
        } else {
            dependencies = gs.typedDependencies();
        }
        
        for (TypedDependency td : dependencies) {
            add(new SimpleDependency(td.toString()));
        }
        //display
        /*try {
            com.chaoticity.dependensee.Main.writeImage(root, dependencies, "tmp.png", 3);
            Desktop.getDesktop().open(new File("tmp.png"));
        } catch (Exception ex) {
            System.err.println("Could not produce dependency pic!");
        }*/
    }
    @Deprecated
    public SimpleDepGraph(Tree root) {
        super();
        assignGovernerLinks(root);
    }
    
    private void assignGovernerLinks(Tree root) {
        SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
        HeadAnalyzer headAnalyzer = new HeadAnalyzer();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            int dep = i;
            List<Tree> dominationPath = root.dominationPath(leaf);
            for (int j = 1; j < dominationPath.size() - 1; j++) {
                Tree node = dominationPath.get(j);
                Tree syntacticHead = headAnalyzer.getSyntacticHead(node);
                if (syntacticHead == leaf) {
                    Tree parentOfNode = node.parent(root);
                    Tree parentHead = headAnalyzer.getSyntacticHead(parentOfNode);
                    if (parentHead != null) {
                        int gov = treeAnalyzer.getLeafPosition(root, parentHead);
                        String label = labelLinks(root, leaf, node, parentOfNode);
                        this.add(new SimpleDependency(label, gov, dep));
                        break;
                    }
                }
            }
        }
    }
    /**
     * 
     * @param root
     * @param w the dependent leaf/word
     * @param c the highest node with c as its head
     * @param p parent of c
     * @return 
     */
    private String labelLinks(Tree root, Tree w, Tree c, Tree p) {
        String cVal = c.value();
        String pVal = p.value();
        String wVal = w.value();
        if (c == root) return "root";
        else if (cVal.matches("N-(ADV|DIR|EXT|LGS|LOC|MNR|PRD|SBJ|TMP)")) {
            return cVal.substring(2).toLowerCase();
        } else if (cVal.matches("NP") && pVal.matches("VP|PP")) {
            if (pVal.matches("VP")) return "dobj";
            else return "pobj";
        } else if (cVal.matches("PRN")) {
            return "prn";
        } else if (cVal.matches("RRC|SBAR") && pVal.matches("NP")) {
            return "rcmod";
        } else if (wVal.matches("[.,;:?!]")) {
            return "p";
        } else if (cVal.matches("PP|ADVP|SBAR") && pVal.matches("VP|S.*")) {
            return "adv";
        } else if (cVal.matches("PRT")) {
            return "prt";
        } else if (isCoordinated(p)) {
            if (cVal.matches("CC")) return "conj";
            else return "coord";
        } else if (cVal.matches("VP") && pVal.matches("VP|SQ|SINV|SBAR|S|SBARQ")) {
            return "vc";
        } else if (cVal.matches("S.*") && pVal.matches("VP")) {
            return "ccomp"; 
        } else if (pVal.matches("S.*|VP")) {
            if (cVal.matches("CC")) return "conj";
            else if (cVal.matches("DT")) return "nmod";
            else if (cVal.matches("NP|WHNP.*")) return "sbj"; //changed from WHNP.* -> NP|WHNP.*
            else return "vmod";
        } else if (pVal.matches("UCP")) {
            if (cVal.matches("CC")) return "conj";
            else return "coord";
        } else if (pVal.matches("NP|QP")) {
            return "nmod";
        } else if (pVal.matches("ADJP|ADVP|WHADJP|WHADVP")) {
            return "amod";
        } else if (pVal.matches("PP|WHPP")) {
            return "pmod";
        } else {
            return "dep";
        }
    }
    private boolean isCoordinated(Tree t) {
        if (t == null || t.isLeaf()) return false;
        List<Tree> children = t.getChildrenAsList();
        for (Tree child : children) {
            if (child.value().equals("CC")) return true;
        }
        return false;
    }
    /**
     * 
     * @param srcIndex
     * @param destIndex
     * @return null if no path exists
     */
    public String getPath(int srcIndex, int destIndex) {
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) size = sd.gov();
            if (sd.dep() > size) size = sd.dep();
        }
        if (srcIndex > size || destIndex > size || srcIndex < 0 || destIndex < 0) {
            return null;
        }
        size++;
        int parent[] = new int[size];
        boolean visited[] = new boolean[size];
        String[][] graph = new String[size][size];
        
        for (SimpleDependency sd : this) {
            graph[sd.gov()][sd.dep()] = sd.reln();
            graph[sd.dep()][sd.gov()] = "-"+sd.reln();
        }
        
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(destIndex);
        
        while (!queue.isEmpty()) {
            int v = queue.remove();
            visited[v] = true;            

            if (v == srcIndex) {
                break;
            }
            for (int i = 0; i < size; i++) {
                if (!visited[i]) {
                    if (graph[v][i] != null || graph[i][v] != null) {
                        parent[i] = v + 1;
                        queue.add(i);
                    }
                }                
            }
        }
        
        if (parent[srcIndex] != 0) {
            StringBuilder sb = new StringBuilder();
            int v = srcIndex;
            int p = parent[v] - 1;
            while (v != destIndex) {                                                                
                sb.append(graph[p][v]).append(":");                
                v = p;
                p = parent[v] - 1;
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    
    public List<Integer> getDependents(int index) {
        List<Integer> dependents = new ArrayList<Integer>();
        for (SimpleDependency sDep : this) {
            if (sDep.gov() == index) {
                dependents.add(sDep.dep());
            }
        }
        return dependents;
    }
    public List<SimpleDependency> getGovDependencies(int gov) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (SimpleDependency sDep : this) {
            if (sDep.gov() == gov) {
                dependencies.add(sDep);
            }
        }
        return dependencies;
    }
    public List<SimpleDependency> getDepDependencies(int dep) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (SimpleDependency sDep : this) {
            if (sDep.dep() == dep) {
                dependencies.add(sDep);
            }
        }
        return dependencies;
    }
    
    public int getParent(int dep) {
        for (SimpleDependency sDep : this) {
            if (sDep.dep() == dep) {
                return sDep.gov();
            }
        }
        return -1;
    }
    
    public SimpleDependency getDependency(int gov, int dep) {
        for (SimpleDependency sDep : this) {
            if (sDep.gov() == gov && sDep.dep() == dep) {
                return sDep;
            }
        }
        return null;
    }
    
    public List<SimpleDependency> getDependencies(String relnPat) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (SimpleDependency sDep : this) {
            if (sDep.reln().matches(relnPat)) {
                dependencies.add(sDep);
            }
        }
        return dependencies;
    }
    
    public List<SimpleDependency> getGovDependencies(int gov, Pattern pattern) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (SimpleDependency sDep : this) {
            if (sDep.gov() == gov && pattern.matcher(sDep.reln()).matches()) {
                dependencies.add(sDep);
            }
        }
        return dependencies;
    }
    public List<SimpleDependency> getDepDependencies(int dep, Pattern pattern) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (SimpleDependency sDep : this) {
            if (sDep.dep() == dep && pattern.matcher(sDep.reln()).matches()) {
                dependencies.add(sDep);
            }
        }
        return dependencies;
    }
    
    public List<Integer> getReachableIndices(int srcIndex, boolean directed, int maxDistance) {
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) {
                size = sd.gov();
            }
            if (sd.dep() > size) {
                size = sd.dep();
            }
        }
        if (srcIndex > size || srcIndex < 0) {
            return null;
        }
        size++;
        int distance[] = new int[size];
        boolean visited[] = new boolean[size];
        String[][] graph = new String[size][size];

        for (SimpleDependency sd : this) {
            graph[sd.gov()][sd.dep()] = sd.reln();
            if (!directed) {
                graph[sd.dep()][sd.gov()] = "-" + sd.reln();
            }
        }

        Queue<Integer> queue = new LinkedList<Integer>();
        distance[srcIndex] = 0;
        queue.add(srcIndex);
        List<Integer> reachable = new ArrayList<Integer>();
        
        while (!queue.isEmpty()) {
            int v = queue.remove();
            visited[v] = true;
            reachable.add(v);
            
            if (distance[v] == maxDistance) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                if (!visited[i]) {
                    if (graph[v][i] != null || (!directed && graph[i][v] != null)) {
                        distance[i] = distance[v] + 1;
                        queue.add(i);
                    }
                }
            }
        }
        return reachable;
    }
    public List<Integer> getReachableIndices(int srcIndex, boolean directed, int maxDistance, String relnFilter) {
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) {
                size = sd.gov();
            }
            if (sd.dep() > size) {
                size = sd.dep();
            }
        }
        if (srcIndex > size || srcIndex < 0) {
            return null;
        }
        size++;
        int distance[] = new int[size];
        boolean visited[] = new boolean[size];
        String[][] graph = new String[size][size];

        for (SimpleDependency sd : this) {
            if (sd.reln().matches(relnFilter)) continue; //filter path
            
            graph[sd.gov()][sd.dep()] = sd.reln();                        
            if (!directed) {
                graph[sd.dep()][sd.gov()] = "-" + sd.reln();
            }
        }

        Queue<Integer> queue = new LinkedList<Integer>();
        distance[srcIndex] = 0;
        queue.add(srcIndex);
        List<Integer> reachable = new ArrayList<Integer>();
        
        while (!queue.isEmpty()) {
            int v = queue.remove();
            visited[v] = true;
            reachable.add(v);
            
            if (distance[v] == maxDistance) {
                continue;
            }
            for (int i = 0; i < size; i++) {
                if (!visited[i]) {
                    if (graph[v][i] != null || (!directed && graph[i][v] != null)) {
                        distance[i] = distance[v] + 1;
                        queue.add(i);
                    }
                }
            }
        }
        return reachable;
    }
    
    public List<String> getPathAsList(int srcIndex, int destIndex, boolean directed) {
        
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) size = sd.gov();
            if (sd.dep() > size) size = sd.dep();
        }
        if (srcIndex > size || destIndex > size || srcIndex < 0 || destIndex < 0) {
            return null;
        }
        size++;
        
        int parent[] = new int[size];   //initialize to 0
        boolean visited[] = new boolean[size];
        String[][] graph = new String[size][size];
        
        for (SimpleDependency sd : this) {
            graph[sd.gov()][sd.dep()] = sd.reln();
            if (!directed) {
                graph[sd.dep()][sd.gov()] = "-"+sd.reln();
            }
        }
        
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(srcIndex);
        
        while (!queue.isEmpty()) {
            int v = queue.remove();
            if (visited[v]) continue;
            
            visited[v] = true;            

            if (v == destIndex) {
                break;
            }            
            for (int i = 0; i < size; i++) {
                if (!visited[i]) {
                    if (graph[v][i] != null || (!directed && graph[i][v] != null)) {
                        parent[i] = v + 1;  //+1 because 0 means uninitialized
                        queue.add(i);
                    }
                }                
            }
        }
                
        List<String> path = new ArrayList<String>();        
        
        if (parent[destIndex] != 0) {            
            int v = destIndex;
            int p = parent[v] - 1;  //need to get rid of that 1
            
            while (v != srcIndex) {
                path.add(0, graph[p][v]);
                v = p;
                p = parent[v] - 1;                
            }            
            
            return path;
        } else {
            return null;
        }
    }
    
    public List<SimpleDependency> getPathAsRelnList(int srcIndex, int destIndex, boolean directed) {
        
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) size = sd.gov();
            if (sd.dep() > size) size = sd.dep();
        }
        if (srcIndex > size || destIndex > size || srcIndex < 0 || destIndex < 0) {
            return null;
        }
        size++;
        
        int parent[] = new int[size];   //initialize to 0
        boolean visited[] = new boolean[size];
        boolean marked[] = new boolean[size];
        SimpleDependency[][] graph = new SimpleDependency[size][size];
        
        for (SimpleDependency sd : this) {
            graph[sd.gov()][sd.dep()] = sd;
            if (!directed) {
                graph[sd.dep()][sd.gov()] = new SimpleDependency(sd.reln(), sd.dep(), sd.gov());
            }
        }
        
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(srcIndex);
        marked[srcIndex] = true;
        while (!queue.isEmpty()) {
            int v = queue.remove();
            if (visited[v]) continue;
            //System.out.println("Visiting: " + v);
            visited[v] = true;            

            if (v == destIndex) {
                break;
            }            
            for (int i = 0; i < size; i++) {
                if (!visited[i] && !marked[i]) {
                    if (graph[v][i] != null || (!directed && graph[i][v] != null)) {
                        parent[i] = v + 1;  //+1 because 0 means uninitialized
                        queue.add(i);       
                        marked[i] = true;
                    }
                }                
            }
        }
                
        List<SimpleDependency> path = new ArrayList<SimpleDependency>();        
        
        if (parent[destIndex] != 0) {            
            int v = destIndex;
            int p = parent[v] - 1;  //need to get rid of that 1
            
            while (v != srcIndex) {
                path.add(0, graph[p][v]);
                v = p;
                p = parent[v] - 1;                
            }            
            
            return path;
        } else {
            return null;
        }
    }
    
    public List<Integer> getPathAsIndexList(int srcIndex, int destIndex, boolean directed) {
        
        int size = 0;
        for (SimpleDependency sd : this) {
            if (sd.gov() > size) size = sd.gov();
            if (sd.dep() > size) size = sd.dep();
        }
        if (srcIndex > size || destIndex > size || srcIndex < 0 || destIndex < 0) {
            return null;
        }
        size++;
        
        int parent[] = new int[size];   //initialize to 0
        boolean visited[] = new boolean[size];
        String[][] graph = new String[size][size];
        
        for (SimpleDependency sd : this) {
            graph[sd.gov()][sd.dep()] = sd.reln();
            if (!directed) {
                graph[sd.dep()][sd.gov()] = "-"+sd.reln();
            }
        }
        
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(srcIndex);
        
        while (!queue.isEmpty()) {
            int v = queue.remove();
            if (visited[v]) continue;
            
            visited[v] = true;            

            if (v == destIndex) {
                break;
            }            
            for (int i = 0; i < size; i++) {
                if (!visited[i]) {
                    if (graph[v][i] != null || (!directed && graph[i][v] != null)) {
                        parent[i] = v + 1;  //+1 because 0 means uninitialized
                        queue.add(i);
                    }
                }                
            }
        }
                
        List<Integer> path = new ArrayList<Integer>();        
        
        if (parent[destIndex] != 0) {            
            int v = destIndex;
            int p = parent[v] - 1;  //need to get rid of that 1
            
            while (v != srcIndex) {
                if (graph[p][v].startsWith("-")) {
                    if (graph[p][v].matches("-(rcmod|partmod|dobj)")) {
                        path.add(0, p);
                    } else {
                        path.add(0, v);
                    }
                } else {
                    if (graph[p][v].matches("(rcmod|partmod|dobj)")) {
                        path.add(0, v);
                    } else {
                        path.add(0, p);
                    }
                }
                v = p;
                p = parent[v] - 1;                
            }            
            
            return path;
        } else {
            return null;
        }
    }
    public String toString(Tree root) {
        StringBuilder sb = new StringBuilder();
        List<Tree> leaves = root.getLeaves();
        for (SimpleDependency sDep : this) {
            Tree gov = leaves.get(sDep.gov());
            Tree dep = leaves.get(sDep.dep());
            sb.append(sDep.reln() + "(" + gov.value() + "-" + sDep.gov() + ", " + dep.value() + "-" + sDep.dep() + ")" + "\n");
        }
        return sb.toString();
    }
    
    /**
     * Returns Least Common Subsumer (like LCA) of two words in the dependency tree
     * @param word1
     * @param word2
     * @return 
     */
    public int getLCS(int word1, int word2) {
        List<Integer> path1 = new LinkedList<Integer>();
        List<Integer> path2 = new LinkedList<Integer>();
                
        int dep = word1;
        while (true) {
            if (path1.contains(dep)) break; //need for CCGraph
            path1.add(0, dep);
            List<SimpleDependency> depDependencies = getDepDependencies(dep);
            if (depDependencies.isEmpty()) break;            
            dep = depDependencies.get(0).gov();
        }
        
        dep = word2;
        while (true) {
            if (path2.contains(dep)) break; //need for CCGraph
            path2.add(0, dep);
            List<SimpleDependency> depDependencies = getDepDependencies(dep);
            if (depDependencies.isEmpty()) break;            
            dep = depDependencies.get(0).gov();
        }
        
        int lcs = -1;
        for (int i = 0; i < path1.size() && i < path2.size(); i++) {
            if (path1.get(i) == path2.get(i)) {
                lcs = path1.get(i);
            } else {
                break;
            }
        }
        return lcs;
    }

    
    
    
    public static void main(String args[]) {
        /*String s = "nn(Vinken-2, Mr.-1)	nsubj(chairman-4, Vinken-2)	cop(chairman-4, is-3)	nn(N.V.-7, Elsevier-6)	prep_of(chairman-4, N.V.-7)	det(group-12, the-9)	amod(group-12, Dutch-10)	nn(group-12, publishing-11)	appos(N.V.-7, group-12)";
        SimpleDepGraph sdg = new SimpleDepGraph(s);
        System.out.println(sdg);
        System.out.println(sdg.getPath(0, 11));
        System.out.println(sdg.getPath(1, 12));*/
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
            
            TreebankLanguagePack tlp = new PennTreebankLanguagePack();
            GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
            TreePrint tp = new TreePrint("oneline");
            
            GrammaticalStructure gs = gsf.newGrammaticalStructure(root);                       
            System.out.println(gs.typedDependencies());
            Collection<TypedDependency> dependencies = gs.typedDependencies();
            for (TypedDependency td : dependencies) {
                System.out.println(td);
            }
            System.out.println("");
            tp.printTree(root);
            System.out.println("----------------");
            SimpleDepGraph sdg = new SimpleDepGraph(dependencies);
            //System.out.println(sdg);
            for (SimpleDependency sd : sdg) {
                int gov = sd.gov();
                int dep = sd.dep();
                String reln = sd.reln();
                System.out.println(reln+"("+root.getLeaves().get(gov).value()+"-"+gov+", "+root.getLeaves().get(dep).value()+"-"+dep+")");
            }
            System.out.println(sdg.getReachableIndices(0, true, 2));
            System.out.println(sdg.getPathAsList(0, 2, false));
        }
    }
}
