/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PPIPipe extends Pipe{
    
    //static final String[] domainTerms = {"abolish","abrogat","acceler","acceptor","accompanied","accompanies","accompany","accumul","acetylat","acquir","act","adapt","add","adhe","affect","aggregat","agoni","alter","amplif","antagoni","apparat","assembl","assist","associat","attach","attack","attenuat","augment","autophosphorylat","autoregulat","bind","block","bound","carbamoylated","carbamoylation","carboxyl","cataly","cause","causing","change","changing","cleav","cluster","co-immunoprecipitate","co-immunoprecipitating","co-immunoprecipitation","co-operat","co-precipit","co-purifi","co-stimulate","co-stimulating","coactivat","coexist","coexpres","coimmunoprecipitate","coimmunoprecipitating","coimmunoprecipitation","colocaliz","compet","complex","component","compris","concentration","conjugat","contact","contain","contribute","contributing","control","convers","convert","cooperat","coprecipit","copurifi","correlat","costimulate","costimulating","counteract","coupl","cripple","crippling","cross-link","cross-react","cross-talk","crosslink","crosstalk","deacetylat","deaminated","deamination","decarboxylated","decarboxylates","decarboxylation","declin","decreas","degrad","dehydrated","dehydrogenated","dehydrogenation","depend","dephosphorylat","deplet","deposi","depress","deriv","destruct","determine","determining","dimer","diminish","direct","disrupt","dissociat","dock","down-regulat","downregulat","drive","driving","effect","elavating","elevat","eliminate","eliminating","encod","engage","engaging","enhanc","enrich","exert","exhibit","expos","express","facilitate","facilitating","faciliteted","follow","form","functio","fuse","fusing","generat","glucosyl","glycosyl","govern","heterodimer","homodimer","hydrol","hyperexpr","imitat","immuno-precipit","immunoprecipit","impact","impair","import","improv","inactivat","inactive","includ","increas","increment","induc","influenc","inhibit","initiat","interact","interfer","interrupt","involve","involving","isomerization","isomerize","isomerizing","lead","led","ligand","ligate","ligating","ligation","limit","link","mediat","methylate","methylating","methylation","migrat","mobili","moderat","modif","modulat","neutrali","obstruct","operat","oppos","overexpress","overproduc","oxidis","oxidiz","pair","peroxidizing","perturb","phosphoryates","phosphorylat","potentiat","prducing","precede","preceding","prevent","process","produc","prohibit","promot","raise","raising","react","recogni","recruit","reduc","regulat","releas","remov","replac","repress","requir","respond","respons","result","secret","sever","signal","splice","stabili","stimulat","subunit","suppress","suspend","synergise","synergising","synergize","synergizing","synthesis","target","terminate","terminating","tether","trans-activate","trans-activating","transactivat","transamination","transcri","transduc","transform","translat","translocat","transport","transregulat","trigger","ubiquitinate","ubiquitinating","ubiquitination","up-regulat","upregulat","us","utilis","utiliz","yield"};
    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final HeadAnalyzer HEAD_ANALYZER = new HeadAnalyzer();
    static final RuleExtractor RULE_EXTRACTOR = new RuleExtractor();
    
    public PPIPipe() {
        super(new Alphabet(), new LabelAlphabet());
    }
    
    @Override
    public Instance pipe(Instance carrier) {
        PPIInstance instance = (PPIInstance) carrier;
        Sentence s = instance.s;
        int entity1 = instance.entity1;
        int entity2 = instance.entity2;        
        SimpleDepGraph depGraph = instance.depGraph;
        SimpleDepGraph ccDepGraph = instance.ccDepGraph;
        
        
        Map<String, String> featureMap = new HashMap<String, String>();
        addFormTypeFeatures(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        addSimpleFeatures(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        addRule1Features(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        addRule2Features(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        addRule3Features(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        addSurfaceFeatures(featureMap, s, depGraph, entity1, entity2);
        addSyntacticFeatures(featureMap, s, depGraph, entity1, entity2);
        addDependencyFeatures(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        
        PropertyList pl = null;
        pl = makePropertyList(featureMap, pl);
        
        FeatureVector fv = new FeatureVector(getDataAlphabet(), pl, true, true);
        //set target label
        LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();        
        carrier.setTarget(ldict.lookupLabel(instance.getTarget().toString()));        
        carrier.setData(fv);
        return carrier;
    }
    
    PropertyList makePropertyList(Map<String, String> featureMap, PropertyList pl) {
        String form = featureMap.get("FORM-TYPE");
        
        for (Entry<String, String> entry : featureMap.entrySet()) {
            /*if (entry.getKey().equals("FORM-TYPE")) {
                pl = PropertyList.add(entry.getKey() + "=" + entry.getValue(), 1.0, pl);
            } else {
                pl = PropertyList.add("Form="+form+"&"+entry.getKey() + "=" + entry.getValue(), 1.0, pl);
            }*/
            pl = PropertyList.add(entry.getKey() + "=" + entry.getValue(), 1.0, pl);
        }
        return pl;
    }
    
    void addSimpleFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        addLCSFeatures(featureMap, s, depGraph, ccDepGraph, entity1, entity2);
        //addRootFeatures(featureMap, s, depGraph, entity1, entity2);
        //addParentFeatures(featureMap, s, depGraph, entity1, entity2);        
    }
    
    void addSurfaceFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        StringBuilder subSent = new StringBuilder();
        String firstVerb = "NONE";
        String lastPrep = "NONE";        
        String firstNoun = "NONE";
        for (int i = entity1 + 1; i < entity2; i++) {        
            if (s.get(i).getTag("POS").matches("VB.*") && s.get(i).getTag("DOMAIN") != null) {
                subSent.append(s.get(i).getTag("POS") + ":");                
                if (firstVerb.equals("NONE")) {
                    firstVerb = s.get(i).getTag("DOMAIN");
                }                
            }else if (s.get(i).getTag("POS").matches("N.*") && s.get(i).getTag("DOMAIN") != null) {
                subSent.append(s.get(i).getTag("POS") + ":");                
                if (firstNoun.equals("NONE")) {
                    firstNoun = s.get(i).getTag("DOMAIN");
                }                
            } else if (s.get(i).getTag("POS").matches("IN")) {
                subSent.append(s.get(i).word() + ":");
                lastPrep = s.get(i).word();
            }    
        }                
        //featureMap.put("SUBS", subSent.toString());        
        featureMap.put("CSUBS", firstVerb + "-" + lastPrep);
        featureMap.put("VERB", firstVerb);
        featureMap.put("NOUN", firstNoun);
        
        int windowLen = 4;
        for (int i = entity1-windowLen; i <= entity1+windowLen; i++) {
            if (i >= 0 && i < entity1) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    featureMap.put("1LWIN" + s.get(i).getTag("DOMAIN"), String.valueOf(true));
                }
            } else if (i > entity1 && i < entity2) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    featureMap.put("1RWIN" + s.get(i).getTag("DOMAIN"), String.valueOf(true));
                }
            }            
        }
        for (int i = entity2-windowLen; i <= entity2+windowLen; i++) {
            if (i > entity1 && i < entity2) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    featureMap.put("2LWIN" + s.get(i).getTag("DOMAIN"), String.valueOf(true));
                }
            } else if (i > entity2 && i < s.size()) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    featureMap.put("2RWIN" + s.get(i).getTag("DOMAIN"), String.valueOf(true));
                }
            }            
        }
        
        //same token
        if (entity1 == entity2) {
            featureMap.put("SAME", String.valueOf(true));
            int parent = depGraph.getParent(entity1);
            if (entity2 < (s.size() - 1) && s.get(entity2 + 1).getTag("DOMAIN") != null) {
                featureMap.put("SAME-DOM", String.valueOf(true));
            } else if (parent != -1 && s.get(parent).getTag("DOMAIN") != null) {
                featureMap.put("SAME-DOM", String.valueOf(true));
            }            
        }
    }
    
    void addSyntacticFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {        
        Tree lca = TREE_ANALYZER.getLCA(s.getParseTree(), entity1, entity2);
        if (lca != null) {            
            Tree head = HEAD_ANALYZER.getCollinsHead(lca);                        
            if (head != null) {
                int headIndex = TREE_ANALYZER.getLeafPosition(s.getParseTree(), head);
                if (headIndex != -1) {
                    if (headIndex == entity1) {
                        featureMap.put("HEAD", "E1");
                    } else if (headIndex == entity2) {
                        featureMap.put("HEAD", "E2");
                    } else {
                        featureMap.put("HEAD", s.get(headIndex).getTag("DOMAIN"));
                    }                    
                }
            }
            
            addPOSListFeatures(featureMap, s, lca, entity1, entity2);
        } else {
            //pl = PropertyList.add("LCA=NONE", 1.0, pl);
        }
    }
    
    void addPOSListFeatures(Map<String, String> featureMap, Sentence s, Tree lca, int entity1, int entity2) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        Tree entityTree1 = leaves.get(entity1);
        Tree entityTree2 = leaves.get(entity2);
        
        List<Tree> dominationPath1 = lca.dominationPath(entityTree1);
        List<Tree> dominationPath2 = lca.dominationPath(entityTree2);
        
        for (int i = 1; i < dominationPath1.size()-1 && i <= 3; i++) {
            Tree t = dominationPath1.get(i);
            featureMap.put("POS1_" + i, t.value());
        }
        
        for (int i = 1; i < dominationPath2.size()-1 && i <= 3; i++) {
            Tree t = dominationPath2.get(i);
            featureMap.put("POS2_" + i, t.value());
        }
    }
    void addDependencyFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        List<SimpleDependency> pathAsRelnList = ccDepGraph.getPathAsRelnList(entity1, entity2, false);
        if (pathAsRelnList == null) return;
        for (SimpleDependency sd : pathAsRelnList) {
            int gov = sd.gov();            
            String reln = sd.reln();
            if (s.get(gov).getTag("DOMAIN") != null) {
                featureMap.put(reln, s.get(gov).getTag("DOMAIN"));
            }
        }
    }
    void addRule1Features(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        List<String> pathList = ccDepGraph.getPathAsList(entity1, entity2, false);
        List<Integer> pathIndexList = ccDepGraph.getPathAsIndexList(entity1, entity2, false);
        
        if (pathList == null) {
            System.out.println("pathList is null!!");
            return;
        }
        
        addSubjFeatures(featureMap, s, depGraph, pathList, pathIndexList, entity1, entity2);
        addPathFeatures(featureMap, s, pathList, pathIndexList, entity1, entity2);
    }

    void addFormTypeFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        int form = RULE_EXTRACTOR.check(s, depGraph, ccDepGraph, entity1, entity2);
        featureMap.put("FORM-TYPE", String.valueOf(form));
    }
    void addSubjFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, List<String> pathList, List<Integer> pathIndexList, int entity1, int entity2) {
        boolean domain = false;
        for (int i = 0; i < pathList.size(); i++) {            
            String relation = pathList.get(i);
            String prefix = (relation.startsWith("-")?"-":"");
            int index = pathIndexList.get(i);
            if (relation.contains("nsubj")) {
                /*if (isNegated(index, depGraph)) {
                    //featureMap.put("NEG", "TRUE");
                    featureMap.put("NSBJ-NEG", relation);
                } else {
                    featureMap.put("NSBJ", relation);
                }*/
                //featureMap.put("SBJ", prefix + s.get(index).getTag("DOMAIN"));
                if (s.get(index).getTag("DOMAIN") != null) {
                    //featureMap.put("SBJ-DOM", "DOMAIN");
                    featureMap.put("SBJ", s.get(index).getTag("DOMAIN"));
                } else {
                    featureMap.put("SBJ", "NONE");
                }
                break;
            } else if (relation.matches("rcmod")) {                           
                /*if (isNegated(index, depGraph)) {
                    featureMap.put("RCMOD-NEG", "TRUE");
                } else {
                    featureMap.put("RCMOD", relation);
                }*/
                //featureMap.put("RCMOD", prefix + s.get(index).getTag("DOMAIN"));
                if (s.get(index).getTag("DOMAIN") != null) {
                    //featureMap.put("RCMOD-DOM", "DOMAIN");
                    featureMap.put("RCMOD", s.get(index).getTag("DOMAIN"));
                } else {
                    featureMap.put("RCMOD", "NONE");
                }
            }
            
            if (s.get(index).getTag("DOMAIN") != null) {
                domain = true;
            }
        }
        featureMap.put("DOMAIN", String.valueOf(domain));
    }
    void addPathFeatures(Map<String, String> featureMap, Sentence s, List<String> pathList, List<Integer> pathIndexList, int entity1, int entity2) {
        StringBuilder collapsedPath = new StringBuilder();
        
        String prev = "";
        for (int i = 0; i < pathList.size(); i++) {            
            String relation = pathList.get(i);            
            /*if (!relation.matches("-?(amod|nn|prep|conj|dep|appos|abbrev)")) {            
                int index = pathIndexList.get(i);
                //String prefix = (relation.startsWith("-")?"-":"");
                String prefix = "";
                if (relation.matches("-?(nsubj|nsubjpass|partmod|rcmod)")) {
                    //collapsedPath.append(relation).append(":");
                    collapsedPath.append("subj").append(":");
                } else if (relation.matches("-?(pobj)") && s.get(index).word().matches("by|through|in|of|to|between")) {                    
                    collapsedPath.append(prefix + s.get(index).word()).append(":");
                }                                
            }*/
            int index = pathIndexList.get(i);
            String cur = null;
            if (relation.matches("-?(amod|nn|prep|conj|dep|appos|abbrev)")) {
                cur = "*";
            } else if (relation.matches("-?(nsubj|nsubjpass|partmod|rcmod)")) {
                cur = "subj";
            } else if (relation.matches("-?(pobj)") && s.get(index).word().matches("by|through|in|of|to|between")) {
                cur = "prep";
            } else if (relation.matches("-?prep_.*")) {
                cur = "prep";
            } else {
                cur = "o";
            }
            if (cur != null && !cur.equals(prev)) {
                collapsedPath.append(cur + ":");
                prev = cur;
            }
        }        
        if (collapsedPath.length() == 0) {
            collapsedPath.append("NONE");
        }        
        featureMap.put("CPATH", collapsedPath.toString());
    }
    
    void addRule2Features(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        Set<Integer> reachables1 = getPPReachables(s, ccDepGraph, entity1);
        Set<Integer> reachables2 = getPPReachables(s, ccDepGraph, entity2);
        
        boolean ppChain = false;
        List<Integer> commonReachables = new ArrayList<Integer>();
        
        for (Integer r : reachables2) {
            if (reachables1.contains(r)) {
                ppChain = true;                
                commonReachables.add(r);                                       
            }
        }
        if (ppChain) {                   
            //use depGraph
            for (Integer common : commonReachables) {
                List<String> pathList = depGraph.getPathAsList(common, entity1, false);
                if (pathList == null) continue;
                List<Integer> pathIndexList = depGraph.getPathAsIndexList(common, entity1, false);
                String pp1 = null;
                for (int i = pathList.size()-1; i >= 0 ; i--) {
                    if (pathList.get(i).contains("pobj")) {
                        pp1 = s.get(pathIndexList.get(i)).word();
                        break;
                    }
                }
                //second chain
                pathList = depGraph.getPathAsList(common, entity2, false);
                if (pathList == null) continue;
                
                pathIndexList = depGraph.getPathAsIndexList(common, entity2, false);
                String pp2 = null;
                for (int i = pathList.size()-1; i >= 0 ; i--) {
                    if (pathList.get(i).contains("pobj")) {
                        pp2 = s.get(pathIndexList.get(i)).word();
                        break;
                    }
                }                
                //featureMap.put("PP", s.get(common).getTag("DOMAIN"));
                if (s.get(common).getTag("DOMAIN") != null) {
                    featureMap.put("PP", "DOMAIN");
                    featureMap.put("PP_PAT", s.get(common).getTag("POS") + "&" + pp1 + "&" + pp2);
                } else if (!featureMap.containsKey("PP")) {
                    featureMap.put("PP", "NON-DOMAIN");
                    //featureMap.put("PP_PAT", s.get(common).getTag("POS") + "&" + pp1 + "&" + pp2);
                    featureMap.put("PP_PAT1", s.get(common).getTag("POS") + "&" + pp1);
                    featureMap.put("PP_PAT2", s.get(common).getTag("POS") + "&" + pp2);
                }
                
            }
        } else {            
            featureMap.put("PP", "NONE");
        }
        
        //Only PP in path
        boolean PPPath = true;
        int count = 0;
        List<String> pathList = depGraph.getPathAsList(entity1, entity2, false);
        if (pathList != null) {
            for (String reln : pathList) {
                if (!reln.matches("-?(prep|pobj|nn|amod)")) {                    
                    count++;                    
                }
            }
            if ((1.0*count/pathList.size()) > 0.3) {
                PPPath = false;
            }            
            featureMap.put("PP_PATH", String.valueOf(PPPath));
        } else {
            System.out.println("NULL_PATH*******************");
        }
    }
    void addRule3Features(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        List<String> pathList = ccDepGraph.getPathAsList(entity1, entity2, false);
        //List<Integer> pathIndices = ccDepGraph.getPathAsIndexList(entity1, entity2, false);
        String prepPath = "";
        if (pathList != null) {
            for (int k = 1; k < pathList.size(); k++) {
                if (pathList.get(k).matches("-?prep_between") && pathList.get(k - 1).matches("-?prep_between")) {
                    featureMap.put("PREP_BETWEEN=", "TRUE");
                    break;
                } else if (pathList.get(k).matches("-?prep.*") && pathList.get(k - 1).matches("-?prep.*")) {
                    featureMap.put("PREP_PREP=", "TRUE");
                    break;
                }
            }
            for (int k = 0; k < pathList.size(); k++) {
                if (pathList.get(k).matches("-?prep_.*")) {
                    prepPath += pathList.get(k) + ":";
                }
            }
            featureMap.put("PREP_PATH", prepPath);
        }
    }
    void addLCSFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        //int lcs = depGraph.getLCS(entity1, entity2);
        int lcs = ccDepGraph.getLCS(entity1, entity2);
        String form = featureMap.get("FORM-TYPE");
        if (lcs == -1) {            
            featureMap.put("LCS", form+"-NONE");
        } else {            
            String dt = s.get(lcs).getTag("DOMAIN");
            if (dt != null) {                
                featureMap.put("LCS", form + "-" + dt);
            } else if (lcs == entity1){                
                featureMap.put("LCS", form + "-" + "E1");
            } else if (lcs == entity2){
                featureMap.put("LCS", form + "-" + "E2");
            } else {                
                featureMap.put("LCS", form + "-" + "O");                
            }            
        }        
    }
    
    void addRootFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        int root = getRoot(s, depGraph, entity1);        
        String dt = s.get(root).getTag("DOMAIN");
        if (dt != null) {            
            featureMap.put("ROOT", dt);
        } else {            
            featureMap.put("ROOT", "O");
        }        
    }
    
    void addParentFeatures(Map<String, String> featureMap, Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        int parent1 = getParent(s, depGraph, entity1);
        int parent2 = getParent(s, depGraph, entity2);            
        
        if (parent1 == -1) {            
            featureMap.put("P1", "NONE");
        } else {            
            //featureMap.put("P1", s.get(parent1).word()+"-" + depGraph.getDependency(parent1, entity1).reln());
            featureMap.put("P1", depGraph.getDependency(parent1, entity1).reln());
        }
        
        if (parent2 == -1) {            
            featureMap.put("P2", "NONE");
        } else {            
            //featureMap.put("P2", s.get(parent2).word()+"-" + depGraph.getDependency(parent2, entity2).reln());
            featureMap.put("P2", depGraph.getDependency(parent2, entity2).reln());
        }        
    }
    
    private Set<Integer> getPPReachables(Sentence s, SimpleDepGraph ccDepGraph, int dep) {
        Set<Integer> reachables = new HashSet<Integer>();
        Queue<Integer> queue = new LinkedList<Integer>();                
        boolean visited[] = new boolean[s.size()];
        queue.add(dep);
        
        while (!queue.isEmpty()) {
            int top = queue.poll();
            
            if (!visited[top]) {
                visited[top] = true;
                List<SimpleDependency> depDependencies = ccDepGraph.getDepDependencies(top);
                for (SimpleDependency sDep : depDependencies) {
                    int parent = sDep.gov();
                    if (sDep.reln().matches("nn|amod|appos|prep.*|pobj|agent|abbrev")) {
                        queue.add(parent);
                        //if (sDep.reln().matches("prep.*|agent")) {
                        if (s.get(parent).getTag("DOMAIN") != null) {
                            reachables.add(parent);
                        }
                    }
                }
            }
        }
        
        return reachables;
    }
    private int getRoot(Sentence s, SimpleDepGraph depGraph, int pos) {
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
    
    private int getParent(Sentence s, SimpleDepGraph depGraph, int pos) {
        List<SimpleDependency> deps = depGraph.getDepDependencies(pos);
        if (deps.isEmpty()) {
            return -1;
        } else {            
            return deps.get(0).gov();
        }
    }
    
    boolean isNegated(int index, SimpleDepGraph depGraph) {
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(index);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().endsWith("neg")) return true;
        }
        return false;
    }
}

