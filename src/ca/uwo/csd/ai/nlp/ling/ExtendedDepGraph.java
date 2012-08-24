/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class ExtendedDepGraph extends SimpleDepGraph {

    List<SimpleDependency> graph[][];
    public ExtendedDepGraph(Tree root, String type) {
        super(root, type);
        initializeGraph();
    }

    public ExtendedDepGraph(Collection<TypedDependency> dependencies) {
        super(dependencies);
        initializeGraph();
    }
    
    private void initializeGraph() {
        int maxIndex = 0;
        for (SimpleDependency sDep : this) {
            if (sDep.dep() > maxIndex) {
                maxIndex = sDep.dep();
            }
            if (sDep.gov() > maxIndex) {
                maxIndex = sDep.gov();
            }
        }
        graph = new List[maxIndex + 1][maxIndex + 1];
        for (SimpleDependency sDep : this) {
            int gov = sDep.gov();
            int dep = sDep.dep();
            if (graph[gov][dep] == null) {
                graph[gov][dep] = new ArrayList<SimpleDependency>();
            }
            graph[gov][dep].add(sDep);
        }
    }
    
    @Override
    public List<SimpleDependency> getGovDependencies(int gov) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (int dep = 0; dep < graph[gov].length; dep++) {
            if (graph[gov][dep] != null) {
                dependencies.addAll(graph[gov][dep]);
            }
        }
        return dependencies;
    }
    
    @Override
    public List<SimpleDependency> getDepDependencies(int dep) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (int gov = 0; gov < graph[dep].length; gov++) {
            if (graph[gov][dep] != null) {
                dependencies.addAll(graph[gov][dep]);
            }
        }
        return dependencies;
    }        
    
    @Override
    public SimpleDependency getDependency(int gov, int dep) {
        if (graph[gov][dep] == null) return null;
        return graph[gov][dep].get(0);
    }        
    
    @Override
    public List<SimpleDependency> getGovDependencies(int gov, Pattern pattern) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (int dep = 0; dep < graph[gov].length; dep++) {
            if (graph[gov][dep] != null) {
                for (SimpleDependency sDep : graph[gov][dep]) {
                    if (pattern.matcher(sDep.reln()).matches()) {
                        dependencies.add(sDep);
                    }
                }
            }
        }
        return dependencies;
    }
    
    @Override
    public List<SimpleDependency> getDepDependencies(int dep, Pattern pattern) {
        List<SimpleDependency> dependencies = new ArrayList<SimpleDependency>();
        for (int gov = 0; gov < graph[dep].length; gov++) {
            if (graph[gov][dep] != null) {
                for (SimpleDependency sDep : graph[gov][dep]) {
                    if (pattern.matcher(sDep.reln()).matches()) {
                        dependencies.add(sDep);
                    }
                }
            }
        }
        return dependencies;
    }
}
