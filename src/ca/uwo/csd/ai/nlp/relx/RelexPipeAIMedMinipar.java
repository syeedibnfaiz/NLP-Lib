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
import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelexPipeAIMedMinipar extends Pipe {

    //static final LexSynAnnotator LEX_SYN_ANNOTATOR = new LexSynAnnotator("./resource/relation/LLL/dictionary_data.txt");
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    static final String[] restrictionTerms = new String[]{"abolish","abrogat","acceler","accelerat","accumul","acetylat","acquir","act","activ","activat","adapt","add","addit","adhe","adher","affect","aggregat","agoni","alter","amplif","antagoni","apparat","assembl","assist","associat","attach","attack","attenuat","augment","autophosphorylat","autoregulat","bind","block","bound","carboxyl","cataly","cleav","cluster","co-operat","co-precipit","co-purifi","coactivat","coexist","coexpres","colocaliz","compet","complex","component","compris","conjugat","contact","contain","control","convers","convert","cooperat","coprecipit","copurifi","correlat","counteract","coupl","cross-link","cross-talk","crosslink","crosstalk","deacetylat","declin","decreas","degrad","depend","dephosphorylat","deplet","deposi","depress","deriv","destruct","dimer","diminish","dissociat","down-regulat","downregulat","effect","elevat","encod","enhanc","enrich","exert","exhibit","expos","express","form","functio","fuse","generat","glucosyl","glycosyl","heterodimer","hydrol","hyperexpr","imitat","immuno-precipit","immunoprecipit","import","improv","inactivat","includ","increas","increment","induc","influenc","inhibit","initiat","interact","interfer","interrupt","ligand","mediat","migrat","mobili","moderat","modif","modulat","neutrali","obstruct","operat","oppos","overexpress","overproduc","oxidis","oxidiz","phosphorylat","potentiat","prevent","process","produc","prohibit","promot","react","recogni","recruit","reduc","regulat","releas","remov","replac","repress","requir","respond","respons","result","secret","sever","signal","splice","stabili","stimulat","subunit","suppress","suspend","synthesis","target","transactivat","transcri","transduc","translat","translocat","transport","transregulat","trigger","up-regulat","upregulat","us","utilis","utiliz","yield"};    
    
    public RelexPipeAIMedMinipar() {
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
        
        //entity1 = getEntityHeadPosition(s, depGraph, entity1, "nn|amod|det|lex-mod");
        //entity2 = getEntityHeadPosition(s, depGraph, entity2, "nn|amod|det|lex-mod");
        if (entity1 > entity2) {
            int tmp = entity1;
            entity1 = entity2;
            entity2 = tmp;
        }
        PropertyList pl = null;
        pl = addBaselineFeatures(pl, s, depGraph, entity1, entity2);
        pl = addRule1Features(pl, s, depGraph, depGraphCC, entity1, entity2);
        pl = addRule2Features(pl, s, depGraph, depGraphCC, entity1, entity2);
        pl = addSurfaceFeatures(pl, s, depGraph, entity1, entity2);
        pl = addSyntacticFeatures(pl, s, depGraph, entity1, entity2);
        //pl = addDependencyFeatures(pl, s, depGraphCC, entity1, entity2);
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);

        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(instance.getTarget().toString()));        
        carrier.setData(fv);
        return carrier;
    }
    
    PropertyList addRule1Features(PropertyList pl, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph depGraphCC, int entity1, int entity2) {
        List<String> pathList = depGraph.getPathAsList(entity1, entity2, false);
        List<Integer> pathIndexList = depGraph.getPathAsIndexList(entity1, entity2, false);
        
        if (pathList == null) {
            System.out.println("pathList is null!!");
            return pl;
        }
        
        /*String subjFeature = "SBJ=NONE";
        String partmodFeature = "PMOD=NONE";
        String rcmodFeature = "RCMOD=NONE";        
        String apposFeature = "APPOS=NONE";
        
        for (int i = 0; i < pathList.size(); i++) {
            //String prefix = "";            
            String relation = pathList.get(i);
            String prefix = (relation.startsWith("-")?"-":"");
            if (relation.contains("nsubj")) {
                int index = pathIndexList.get(i);
                //String prefix = (relation.startsWith("-")?"-":"");
                subjFeature = "SBJ=" + prefix + s.get(index).word();                
                subjFeature = "SBJ=" + prefix + isDomainTerm(s.get(index).word());
            } else if (relation.contains("partmod")) {
                int index = pathIndexList.get(i);
                //String prefix = (relation.startsWith("-")?"-":"");
                partmodFeature = "PMOD=" + prefix + s.get(index).word();        
                partmodFeature = "PMOD=" + prefix + isDomainTerm(s.get(index).word());
            } else if (relation.contains("rcmod")) {
                int index = pathIndexList.get(i);
                //String prefix = (relation.startsWith("-")?"-":"");
                rcmodFeature = "RCMOD=" + prefix + s.get(index).word();   
                rcmodFeature = "RCMOD=" + prefix + isDomainTerm(s.get(index).word());   
            } else if (relation.contains("appos")) {
                int index = pathIndexList.get(i);
                //String prefix = (relation.startsWith("-")?"-":"");
                apposFeature = "APPOS=YES" + prefix;;
            }
        }
        
        pl = PropertyList.add(subjFeature, 1.0, pl);
        pl = PropertyList.add(partmodFeature, 1.0, pl);
        pl = PropertyList.add(rcmodFeature, 1.0, pl);
        pl = PropertyList.add(apposFeature, 1.0, pl);*/
        
        
        String pathType = "NONE";        
        for (int i = 0; i < pathList.size(); i++) {
            String reln = pathList.get(i);
            if (reln.matches("-?(s|subj|rel)")) {
                pathType = "reln";
                break;
            }
        }        
        pl = PropertyList.add("PTYPE=" + pathType, 1.0, pl);
        
        StringBuilder collapsedPath = new StringBuilder();
        StringBuilder collapsedPathWord = new StringBuilder();
        String last = "";
        for (int i = 0; i < pathList.size(); i++) {            
            String relation = pathList.get(i);
            
            if (!relation.matches("-?(nn|mod)") /*&& !relation.equals(last)*/) {
            //if (relation.matches("-?(nsubj.*|rcmod|partmod|dobj|appos|conj|dep|xcomp)") /*&& !relation.equals(last)*/) {
                int index = pathIndexList.get(i);
                String prefix = (relation.startsWith("-")?"-":"");
                if (relation.matches("-?(subj|rel)")) {
                    //collapsedPathWord = new StringBuilder();
                    collapsedPathWord.append(prefix + s.get(index).word()).append(":");
                    collapsedPath.append(relation).append(":");
                } else if (relation.matches("-?(pcomp.*)")) {                    
                    collapsedPathWord.append(prefix + s.get(index).word()).append(":");
                    //collapsedPath.append(relation).append(":");
                    collapsedPath.append(prefix + s.get(index).word()).append(":");
                } else {
                    collapsedPathWord.append(relation).append(":");
                    collapsedPath.append(relation).append(":");
                }
                
                last = relation;
            }
        }        
        pl = PropertyList.add("CPATH="+collapsedPath.toString(), 1.0, pl);
        //pl = PropertyList.add("CPATHW="+collapsedPathWord.toString(), 1.0, pl);  
        
        return pl;
    }
    
    PropertyList addBaselineFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
                
        int lcs = depGraph.getLCS(entity1, entity2);
        if (lcs == -1) {
            pl = PropertyList.add("LCS=NONE", 1.0, pl);
        } else {
            pl = PropertyList.add("LCS="+s.get(lcs), 1.0, pl);            
        }        
                        
        int root1 = getRoot(s, depGraph, entity1);
        pl = PropertyList.add("ROOT1="+s.get(root1).word(), 1.0, pl);
       
        int root2 = getRoot(s, depGraph, entity2);
        pl = PropertyList.add("ROOT2="+s.get(root2).word(), 1.0, pl);
        
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
    
        //if (1 < 2) return pl;
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
                    if (sDep.reln().matches("nn|mod|appo|pcomp.*")) {
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
    
    PropertyList addRule2Features(PropertyList pl, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph depGraphCC, int entity1, int entity2) {
        
        Set<Integer> reachables1 = getPPReachables(s, depGraphCC, entity1);
        Set<Integer> reachables2 = getPPReachables(s, depGraphCC, entity2);
        
        boolean ppChain = false;
        List<Integer> commonReachables = new ArrayList<Integer>();
        
        for (Integer r : reachables2) {
            if (reachables1.contains(r)) {
                ppChain = true;
                System.out.println("YESS");
                //System.out.println(s);
                commonReachables.add(r);                                       
            }
        }
        if (ppChain) {                   
            //use depGraph
            for (Integer common : commonReachables) {
                List<String> pathList = depGraph.getPathAsList(common, entity1, false);
                List<Integer> pathIndexList = depGraph.getPathAsIndexList(common, entity1, false);
                String pp1 = null;
                for (int i = pathList.size()-1; i >= 0 ; i--) {
                    if (pathList.get(i).matches("mod|pcomp.*")) {
                        pp1 = s.get(pathIndexList.get(i)).word();
                        break;
                    }
                }
                //second chain
                pathList = depGraph.getPathAsList(common, entity2, false);
                pathIndexList = depGraph.getPathAsIndexList(common, entity2, false);
                String pp2 = null;
                for (int i = pathList.size()-1; i >= 0 ; i--) {
                    if (pathList.get(i).matches("mod|pcomp.*")) {
                        pp2 = s.get(pathIndexList.get(i)).word();
                        break;
                    }
                }
                pl = PropertyList.add("PP="+s.get(common).word(), 1.0, pl);
                pl = PropertyList.add("PP="+s.get(common).getTag("POS") + "&" + pp1 + "&" + pp2, 1.0, pl);
                                
            }
        } else {
            pl = PropertyList.add("PP=NONE", 1.0, pl);
        }
        
        //Only PP in path
        boolean PPPath = true;
        int count = 0;
        List<String> pathList = depGraph.getPathAsList(entity1, entity2, false);
        if (pathList != null) {
            for (String reln : pathList) {
                if (!reln.matches("-?(pcomp.*|lex-mod|nn|mod)")) {
                    //PPPath = false;
                    count++;
                    //break;
                }
            }
            if ((1.0*count/pathList.size()) > 0.3) {
                PPPath = false;
            }
            pl = PropertyList.add("PP_PATH=" + PPPath, 1.0, pl);
        } else {
            System.out.println("NULL_PATH*******************");
        }
        return pl;
    }
    PropertyList addSurfaceFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {                
        StringBuilder subSent = new StringBuilder();
        String firstVerb = "";
        String lastPrep = "";
        for (int i = entity1 + 1; i < entity2; i++) {
            if (i == entity1) {
                //subSent.append("*:");
            } else if (s.get(i).getTag("POS").matches("VB.*")) {
                //subSent.append(s.get(i).word() + ":");
                subSent.append(s.get(i).getTag("POS") + ":");                
                if (firstVerb.equals("")) {
                    firstVerb = s.get(i).word();
                }                
            } else if (s.get(i).getTag("POS").matches("IN")) {
                subSent.append(s.get(i).word() + ":");
                lastPrep = s.get(i).word();
            }    
        }
        //subSent.append("*:");
        pl = PropertyList.add("SUBS=" + subSent.toString(), 1.0, pl);
        pl = PropertyList.add("CSUBS=" + firstVerb + "-" + lastPrep, 1.0, pl);        
    
        boolean domain = false;
        String sent = s.toString(entity1, entity2);
        for (int i = 0; i < restrictionTerms.length; i++) {
            if (sent.contains(restrictionTerms[i])) {
                //pl = PropertyList.add("DOMAIN=" + restrictionTerms[i], 1.0, pl);
                domain = true;
                break;
            }
        }
        pl = PropertyList.add("DOMAIN=" + domain, 1.0, pl);
        return pl;
    }
    
    PropertyList addSyntacticFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        Tree lca = TREE_ANALYZER.getLCA(s.getParseTree(), entity1, entity2);
        if (lca != null) {
            //pl = PropertyList.add("LCA=" + lca.value(), 1.0, pl);
            /*while (!lca.value().startsWith("S")) {
                Tree parent = lca.parent(s.getParseTree());
                if (parent != null) {
                    lca = parent;
                } else {
                    break;
                }
            }*/            

            Tree head = HEAD_ANALYZER.getCollinsHead(lca);
            String headValue = "NONE";            
            if (head != null) {
                int headIndex = TREE_ANALYZER.getLeafPosition(s.getParseTree(), head);
                if (headIndex != -1) {
                    headValue = s.get(headIndex).word();
                }                
            }
            pl = PropertyList.add("HEAD=" + headValue, 1.0, pl);            
        } else {
            //pl = PropertyList.add("LCA=NONE", 1.0, pl);
        }
        
        return pl;
    }
    
    PropertyList addDependencyFeatures(PropertyList pl, Sentence s, SimpleDepGraph depGraphCC, int entity1, int entity2) {
        SimpleDependency dependency = depGraphCC.getDependency(entity1, entity2);
        if (dependency == null) {
            dependency = depGraphCC.getDependency(entity2, entity1);
        }
        if (dependency == null) {
            pl = PropertyList.add("DEP=NONE", 1.0, pl);
        } else {
            pl = PropertyList.add("DEP=" + dependency.reln(), 1.0, pl);
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
    int getEntityHeadPosition(Sentence s, SimpleDepGraph depGraph, int entity, String pattern) {
        /*int endPos = startPos;
        for (int i = startPos + 1; i < s.size(); i++) {
        if (s.get(i).getTag("LEXE").equals("I")) {
        endPos = i;
        } else {
        break;
        }
        }
         return endPos;*/
        boolean visited[] = new boolean[s.size()];
        int dep = entity;
        while (true) {
            visited[dep] = true;
            List<SimpleDependency> deps = depGraph.getDepDependencies(dep, Pattern.compile(pattern));
            if (deps.isEmpty()) return dep;
            dep = deps.get(0).gov();
            if (visited[dep]) {
                return entity;
            }
        }
        
        
        /*Tree root = s.getParseTree();        
        Tree lca = TREE_ANALYZER.getLCA(root, startPos, endPos);
        Tree head = HEAD_ANALYZER.getCollinsHead(lca);
        int pos = TREE_ANALYZER.getLeafPosition(root, head);*/
        
        //return pos;
    }
}
