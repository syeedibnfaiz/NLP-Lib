/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.PropertyList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class RelexPipe extends Pipe {

    //static final LexSynAnnotator LEX_SYN_ANNOTATOR = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    
    public RelexPipe() {
        super(new Alphabet(), new LabelAlphabet());
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        RelexInstance instance = (RelexInstance) carrier;
        Sentence s = (Sentence) instance.getData();
        int entity1 = instance.getEntity1();
        int entity2 = instance.getEntity2();
        SimpleDepGraph depGraph = instance.getDepGraph();
        SimpleDepGraph depGraphCC = instance.getDepGraphCC();       //CCProcessed
        
        PropertyList pl = null;
        pl = addBaselineFeatures(pl, s, depGraph, entity1, entity2);
        pl = addPPChainFeatures(pl, s, depGraphCC, entity1, entity2);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(instance.getTarget().toString()));        
        carrier.setData(fv);
        return carrier;
    }
    
    PropertyList addBaselineFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        //pl = PropertyList.add("Entities="+getEntity(s, entity1) + "&" + getEntity(s, entity2), 1.0, pl);        
        
        entity1 = getEntityHeadPosition(s, depGraph, entity1);
        entity2 = getEntityHeadPosition(s, depGraph, entity2);
                
        int lcs = depGraph.getLCS(entity1, entity2);
        if (lcs == -1) {
            pl = PropertyList.add("LCS=NONE", 1.0, pl);
        } else {
            pl = PropertyList.add("LCS="+s.get(lcs), 1.0, pl);
            //pl = PropertyList.add("LCS-POS="+s.get(lcs).getTag("POS"), 1.0, pl);
        }        
        
        List<String> pathList = depGraph.getPathAsList(entity1, entity2, false);
        if (pathList == null) {
            System.out.println("pathList is null!!");
            return pl;
        }
        
        String path = pathList.toString();
        if (path.contains("nsubj")) {
            pl = PropertyList.add("SBJ=TRUE", 1.0, pl);
        } else {
            pl = PropertyList.add("SBJ=FALSE", 1.0, pl);
        }
        
        
        boolean prepChain = true;
        StringBuilder collapsedPath = new StringBuilder();
        String last = "";
        for (String reln : pathList) {
            if (!reln.matches("-?(prep|pobj|amod|nn|conj)")) {
                prepChain = false;
                //break;
            }
            if (!reln.matches("-?(pobj|amod|nn|conj)") && !reln.equals(last)) {
                collapsedPath.append(reln).append(":");
                last = reln;
            }
        }
        pl = PropertyList.add("CPATH="+collapsedPath.toString(), 1.0, pl);
        
        //if (1 < 2) return pl;
        pl = PropertyList.add("PPChain?="+prepChain, 1.0, pl);
        
        int root = getRoot(s, depGraph, entity1);
        pl = PropertyList.add("ROOT="+s.get(root).word(), 1.0, pl);
       
        int parent1 = getParent(s, depGraph, entity1);
        int parent2 = getParent(s, depGraph, entity2);
        
        if (parent1 == -1) {
            pl = PropertyList.add("P1=NONE", 1.0, pl);
        } else {
            pl = PropertyList.add("P1=" + s.get(parent1)+"-" + depGraph.getDependency(parent1, entity1).reln(), 1.0, pl);
        }
        
        if (parent2 == -1) {
            pl = PropertyList.add("P2=NONE", 1.0, pl);
        } else {
            pl = PropertyList.add("P2=" + s.get(parent2)+"-" + depGraph.getDependency(parent2, entity2).reln(), 1.0, pl);
        }
        
        List<SimpleDependency> govDeps1 = depGraph.getGovDependencies(entity1);
        for (int i = 0; i < 2 && i < govDeps1.size(); i++) {
            int dep = govDeps1.get(i).dep();
            if (dep < s.size()) {
                pl = PropertyList.add("C1"+i+"="+s.get(dep).word(), 1.0, pl);
            } else {
                System.out.println("How??");
            }
        }
        List<SimpleDependency> govDeps2 = depGraph.getGovDependencies(entity2);
        for (int i = 0; i < 2 && i < govDeps2.size(); i++) {
            int dep = govDeps2.get(i).dep();
            if (dep < s.size()) {
                pl = PropertyList.add("C2"+i+"="+s.get(dep).word(), 1.0, pl);
            } else {
                System.out.println("How??");                                
            }
        }
        return pl;
    }
    
    int getParent(Sentence s, SimpleDepGraph depGraph, int pos) {
        List<SimpleDependency> deps = depGraph.getDepDependencies(pos);
        if (deps.isEmpty()) {
            return -1;
        } else {            
            return deps.get(0).gov();
        }
    }
    Set<Integer> getPPReachables(Sentence s, SimpleDepGraph depGraphCC, int dep) {
        Set<Integer> reachables = new HashSet<Integer>();
        Queue<Integer> queue = new LinkedList<Integer>();                
        boolean visited[] = new boolean[s.size()];
        queue.add(dep);
        
        while (!queue.isEmpty()) {
            int top = queue.poll();
            
            if (!visited[top]) {
                visited[top] = true;
                List<SimpleDependency> depDependencies = depGraphCC.getDepDependencies(top);
                for (SimpleDependency sDep : depDependencies) {
                    int parent = sDep.gov();
                    if (sDep.reln().matches("nn|amod|appos|prep.*|pobj")) {
                        queue.add(parent);
                        if (sDep.reln().matches("prep.*")) {
                            reachables.add(parent);
                        }
                    }
                }
            }
        }
        
        return reachables;
    }
    
    PropertyList addPPChainFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraphCC, int entity1, int entity2) {
        
        Set<Integer> reachables1 = getPPReachables(s, depGraphCC, entity1);
        Set<Integer> reachables2 = getPPReachables(s, depGraphCC, entity2);
        
        boolean ppChain = false;
        int common = -1;
        for (Integer r : reachables2) {
            if (reachables1.contains(r)) {
                ppChain = true;
                System.out.println("YESS");
                System.out.println(s);
                common = r;
                break;
            }
        }
        if (ppChain) {            
            pl = PropertyList.add("PP="+s.get(common).word(), 1.0, pl);            
        } else {
            pl = PropertyList.add("PP=NONE", 1.0, pl);
        }
        
        return pl;
    }
    
    int getRoot(Sentence s, SimpleDepGraph depGraph, int pos) {
        boolean visited[] = new boolean[s.size()];
        while (true) {
            List<SimpleDependency> deps = depGraph.getDepDependencies(pos);
            if (deps.isEmpty()) break;
            pos = deps.get(0).gov();
            if (visited[pos]) break;
            visited[pos] = true;
        }
        return pos;
    }
    
    int getEntityHeadPosition(Sentence s, SimpleDepGraph depGraph, int startPos) {
        /*int endPos = startPos;
        for (int i = startPos + 1; i < s.size(); i++) {
        if (s.get(i).getTag("LEXE").equals("I")) {
        endPos = i;
        } else {
        break;
        }
        }
         return endPos;*/
        List<SimpleDependency> deps = depGraph.getDepDependencies(startPos, Pattern.compile("nn|amod"));
        if (deps.isEmpty()) return startPos;
        
        return getEntityHeadPosition(s, depGraph, deps.get(0).gov());
        
        
        /*Tree root = s.getParseTree();        
        Tree lca = TREE_ANALYZER.getLCA(root, startPos, endPos);
        Tree head = HEAD_ANALYZER.getCollinsHead(lca);
        int pos = TREE_ANALYZER.getLeafPosition(root, head);*/
        
        //return pos;
    }
    private String getEntity(Sentence s, int pos) {        
        while (pos >0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            return null;
        }
        int start = pos;
        int end = start;
        pos++;
        while (pos < s.size() && s.get(pos).getTag("LEXE").equals("I")) {
            end = pos;
            pos++;
        }
        return s.toString(start, end);
    }
}
