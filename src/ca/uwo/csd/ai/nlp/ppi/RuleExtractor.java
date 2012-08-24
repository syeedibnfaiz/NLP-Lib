/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RuleExtractor {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    final BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();
    
    private final static Pattern form1Pat = Pattern.compile("PROTEIN[0-9]+.*RELV.*PROTEIN[0-9]+");
    private final static Pattern form2Pat = Pattern.compile("PROTEIN[0-9]+.*REL.*PROTEIN[0-9]+");
    private final static Pattern form3Pat = Pattern.compile("RELN\\.PREP.*PROTEIN[0-9]+.*PROTEIN[0-9]+");
    private final static Pattern form4Pat = Pattern.compile("PROTEIN[0-9]+([\\/-])?PROTEIN[0-9]+");
    private final static Pattern form5Pat = Pattern.compile("PROTEIN[0-9]+.*PROTEIN[0-9]+.*REL");
    
    public int check(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2) {
        String sentPattern = getSentPattern(s, entity1, entity2);
        if (checkForm1(s, depGraph, ccDepGraph, sentPattern, entity1, entity2)) return 1;
        else if (checkForm2(s, depGraph, ccDepGraph, sentPattern, entity1, entity2)) return 2;
        else if (checkForm3(s, depGraph, ccDepGraph, sentPattern, entity1, entity2)) return 3;
        else if (checkForm4(s, depGraph, ccDepGraph, sentPattern, entity1, entity2)) return 4;
        else if (checkForm5(s, depGraph, ccDepGraph, sentPattern, entity1, entity2)) return 5;
        return -1;
    }
    private boolean checkForm1(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String sentPattern, int entity1, int entity2) {
        Matcher matcher = form1Pat.matcher(sentPattern);        
        
        if (matcher.find()) {
            List<SimpleDependency> path = ccDepGraph.getPathAsRelnList(entity1, entity2, false);
            if (path != null) {                
                //check dependency path
                for (int i = 0; i < path.size(); i++) {
                    SimpleDependency dependency = path.get(i);
                    if (dependency.reln().matches("-?(nsubj|nsubjpass)")) {
                        int gov = dependency.gov();
                        int dep = dependency.dep();
                        if (isNegated(gov, depGraph) || isNegated(dep, depGraph)) {
                            return false;
                        }                        
                        if (gov > entity1 && gov < entity2 && s.get(gov).getTag("DOMAIN") != null) {
                            return true;
                        } else if (dep > entity1 && dep < entity2 && s.get(dep).getTag("DOMAIN") != null) {
                            return true;
                        }
                    }                                                            
                }
            }
            /*List<SimpleDependency> dependencies = ccDepGraph.getDependencies("nsubj|nsubjpass");
            for (SimpleDependency dependency : dependencies) {
                int gov = dependency.gov();
                int dep = dependency.dep();
                if (s.get(gov).getTag("DOMAIN") != null) {
                    Set<Integer> reachables = new HashSet<Integer>();
                    getReachables(s, depGraph, reachables, gov, new boolean[s.size()], true, "*");
                    //getUpwardReachables(s, depGraph, reachables, dep, new boolean[s.size()], true, "nn|amod|abbrev|appos|conj.*|advmod");
                    if (reachables.contains(entity1) && reachables.contains(entity2)) {
                        return true;
                    }
                }
            }*/
        }
        return false;
    }
    
    private boolean checkForm2(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String sentPattern, int entity1, int entity2) {
        Matcher matcher = form2Pat.matcher(sentPattern);        
        if (matcher.find()) {
            List<SimpleDependency> path = ccDepGraph.getPathAsRelnList(entity1, entity2, false);
            if (path != null) {
                boolean flg = false;
                //check dependency path
                for (int i = 0; i < path.size(); i++) {
                    SimpleDependency dependency = path.get(i);
                    if (dependency.reln().matches("-?(nsubj|nsubjpass|rcmod)")) {
                        flg = true;
                    }
                    int gov = dependency.gov();
                    int dep = dependency.dep();
                    if (isNegated(gov, depGraph) || isNegated(dep, depGraph)) {
                        return false;
                    }
                    if (flg && gov > entity1 && gov < entity2 && s.get(gov).getTag("DOMAIN") != null) {
                        return true;
                    } else if (flg && dep > entity1 && dep < entity2 && s.get(dep).getTag("DOMAIN") != null) {
                        return true;
                    }
                }
            }
            /*List<SimpleDependency> dependencies = ccDepGraph.getDependencies("nsubj|nsubjpass");
            for (SimpleDependency dependency : dependencies) {
                int gov = dependency.gov();
                int dep = dependency.dep();
                Set<Integer> reachables = new HashSet<Integer>();
                getReachables(s, ccDepGraph, reachables, gov, new boolean[s.size()], false, "*");
                //getUpwardReachables(s, ccDepGraph, reachables, dep, new boolean[s.size()], true, "nn|amod|abbrev|appos|conj.*|advmod");
                if (reachables.contains(entity1) && reachables.contains(entity2)) {
                    return true;
                }
            }*/
        }
        return false;
    }
    private boolean checkForm3(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String sentPattern, int entity1, int entity2) {
        Matcher matcher = form3Pat.matcher(sentPattern);
        if (matcher.find()) {            
            /*int parent = depGraph.getParent(entity1);
            int dist = 0;
            while (parent != -1 && dist < 5) {
                dist++;
                if (s.get(parent).getTag("DOMAIN") != null && parent < entity1) {
                    List<String> path = depGraph.getPathAsList(parent, entity2, true);
                    if (path != null && !path.isEmpty()) {
                        return true;
                    }
                }
                parent = depGraph.getParent(parent);
            }
            Set<Integer> reachables1 = getPPReachables(s, ccDepGraph, entity1);
            Set<Integer> reachables2 = getPPReachables(s, ccDepGraph, entity2);
            List<Integer> commonReachables = new ArrayList<Integer>();

            for (Integer r : reachables2) {
                if (r < entity1 && reachables1.contains(r)) {                    
                    commonReachables.add(r);
                }
            }
            for (Integer i : commonReachables) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    return true;
                }
            }*/
            for (int i = 0; i < entity1; i++) {
                if (s.get(i).getTag("POS").matches("N.*") && s.get(i).getTag("DOMAIN") != null) {
                    Set<Integer> reachables = new HashSet<Integer>();
                    getReachables(s, ccDepGraph, reachables, i, new boolean[s.size()], true, "agent|prep.*|appos|abbrev|nn|amod|dep");
                    if (reachables.contains(entity1) && reachables.contains(entity2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void getReachables(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> targets, int index, boolean[] visited, boolean domainSpecific, String relnPattern) {
        if (visited[index]) return;
        visited[index] = true;
        if (domainSpecific && s.get(index).word().contains("PROTEIN")) {
            targets.add(index);
        }
        if (!domainSpecific && s.get(index).getTag("DOMAIN") != null) {
            domainSpecific = true;
        }
        for (SimpleDependency dependency : ccDepGraph.getGovDependencies(index)) {            
            int dep = dependency.dep();
            String reln = dependency.reln();
            if (relnPattern.equals("*") || reln.matches(relnPattern)) {
                getReachables(s, ccDepGraph, targets, dep, visited, domainSpecific, relnPattern);
            }
        }
    }
    private void getUpwardReachables(Sentence s, SimpleDepGraph ccDepGraph, Set<Integer> targets, int index, boolean[] visited, boolean domainSpecific, String relnPattern) {
        if (visited[index]) return;
        visited[index] = true;
        if (s.get(index).word().contains("PROTEIN")) {
            targets.add(index);
        }
        if (!domainSpecific && s.get(index).getTag("DOMAIN") != null) {
            domainSpecific = true;
        }
        for (SimpleDependency dependency : ccDepGraph.getDepDependencies(index)) {            
            int gov = dependency.gov();
            String reln = dependency.reln();
            if (reln.matches(relnPattern)) {
                getUpwardReachables(s, ccDepGraph, targets, gov, visited, domainSpecific, relnPattern);
            }
        }
    }
    private boolean checkForm4(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String sentPattern, int entity1, int entity2) {        
        /*if (entity1 == entity2) {
            String str = s.get(entity1).word();
            Matcher matcher = form4Pat.matcher(str);
            if (matcher.matches()) {
                return true;
            }
        }*/
        if (entity1 == entity2 && entity1 < (s.size()-1)) {
            String str = s.get(entity1).word();
            Matcher matcher = form4Pat.matcher(str);
            if (matcher.matches() && s.get(entity1 + 1).getTag("DOMAIN") != null) {
                return true;
            }
        }
        return false;
    }
    private boolean checkForm5(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, String sentPattern, int entity1, int entity2) {
        Matcher matcher = form5Pat.matcher(sentPattern);        
        if (matcher.find()) {            
            for (int i = entity1 + 1; i < s.size() && i < (entity1 + 6); i++) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    return true;
                }
            }
        }
        return false;
    }
    private String getSentPattern(Sentence s, int entity1, int entity2) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.size(); i++) {
            if (i == entity1 || i == entity2) {
                sb.append(s.get(i).word());
            } else if (s.get(i).getTag("DOMAIN") != null) {
                sb.append("REL" + s.get(i).getTag("POS").substring(0, 1)); //RELV or RELN
            } else if (s.get(i).word().matches("[/,-]")) {
                sb.append(s.get(i).word());
            } else if (s.get(i).getTag("POS").matches("IN|TO")) {
                sb.append("PREP");
            } else if (s.get(i).word().matches("and|or")) {
                sb.append("CONJ");
            } else {
                sb.append("W");
            }
            sb.append(".");
        }
        return sb.toString();
    }
    private static class PPIPair {
        int entity1, entity2;
        boolean interact;
        Sentence s;
        SimpleDepGraph depGraph;
        SimpleDepGraph ccDepGraph;
        public PPIPair(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph, int entity1, int entity2, boolean interact) {
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.interact = interact;
            this.s = s;
            this.depGraph = depGraph;
            this.ccDepGraph = ccDepGraph;
        }        
    }
    private List<PPIPair> getInteractionPairs(String corpusRoot, List<String> docIds) {
        List<PPIPair> pairs = new ArrayList<PPIPair>();
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");
            
            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                Tree root = trees.get(i);
                SimpleDepGraph depGraph = deps.get(i);
                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                s.setParseTree(root);
                assignPOS(s);
                s = domainAnnotator.annotate(s);
                pairs.addAll(getInteractionPairs(s, depGraph, ccDepGraph));
            }
        }
        return pairs;
    }
    private List<PPIPair> getInteractionPairs(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        List<PPIPair> pairs = new ArrayList<PPIPair>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();
        Map<String, Integer> n1Map = new HashMap<String, Integer>();
        
        //find P1 and N1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token: tokens) {
                    p1Map.put(token, i);
                }
            }
            if (!word.getTag("N1").equals("O")) {
                String[] tokens = word.getTag("N1").split(", ");
                for (String token: tokens) {
                    n1Map.put(token, i);
                }
            }
        }
        
        //find P2 and N2 and match with P1 and N2
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = p1Map.get(token);
                    pairs.add(new PPIPair(s, depGraph, ccDepGraph, entity1, i, true));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = n1Map.get(token);
                    pairs.add(new PPIPair(s, depGraph, ccDepGraph, entity1, i, false));
                }
            }
        }
        return pairs;
    }
    private void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    public void filter(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        List<PPIPair> pairs = getInteractionPairs(corpusRoot, docIds);
        System.out.println("total=" + pairs.size());
        int tp = 0;
        int fp = 0;
        int fn = 0;
        int tn = 0;
        for (PPIPair pair : pairs) {
            int r = check(pair.s, pair.depGraph, pair.ccDepGraph, pair.entity1, pair.entity2);
            if (pair.interact) {
                if (r == -1) {
                    fn++;
                    Sentence s = pair.s;
                    System.out.println("FN:" + s);
                    System.out.println("Pat: " + getSentPattern(s, pair.entity1, pair.entity2));
                    System.out.println("f=" + r);
                    System.out.println(pair.ccDepGraph.getPathAsList(pair.entity1, pair.entity2, false));
                    System.out.println("en1: "+s.get(pair.entity1).word());
                    System.out.println("en2: "+s.get(pair.entity2).word());
                    System.out.println("-----------");
                } else {
                    tp++;
                    Sentence s = pair.s;
                    System.out.println("TP:" + s);
                    System.out.println("Pat: " + getSentPattern(s, pair.entity1, pair.entity2));
                    System.out.println("p=" + r);
                    System.out.println("en1: "+s.get(pair.entity1).word());
                    System.out.println("en2: "+s.get(pair.entity2).word());
                    System.out.println("-----------");
                }
            } else {
                if (r == -1) {
                    tn++;
                } else {
                    fp++;
                    Sentence s = pair.s;
                    System.out.println("FP:" + s);
                    System.out.println("Pat: " + getSentPattern(s, pair.entity1, pair.entity2));
                    System.out.println("r=" + r);
                    System.out.println(pair.depGraph.getPathAsList(pair.entity1, pair.entity2, false));
                    System.out.println("en1: "+s.get(pair.entity1).word());
                    System.out.println("en2: "+s.get(pair.entity2).word());
                    System.out.println("-----------");
                }
            }
        }
        System.out.println("tp=" + tp);
        System.out.println("fp=" + fp);
        System.out.println("fn=" + fn);
        double precision = 1.0*tp/((tp + fp)*1.0);
        double recall = 1.0*tp/((tp + fn)*1.0);
        double fscore = 2*precision*recall/(precision + recall);
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("Fscore: " + fscore);        
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
                    if (sDep.reln().matches("nn|amod|appos|prep.*|pobj|dobj")) {
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
    
    public void checkCandidateExtractor(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        File treesDir = new File(corpusRoot, "trees");
        File depDir = new File(corpusRoot, "deps");
        File ccDepDir = new File(corpusRoot, "depsCC");
        File[] files = iobDir.listFiles();
        RelCandidateExtractor candidateExtractor = new RelCandidateExtractor();
        int tp = 0;
        int fp = 0;
        int fn = 0;
        
        for (File file : files) {
            Text text = TEXT_READER.read(file);
            List<Tree> trees = TREE__READER.read(new File(treesDir, file.getName().replace(".txt", ".mrg")));
            List<SimpleDepGraph> deps = DEP_READER.read(new File(depDir, file.getName().replace(".txt", ".dep")));
            List<SimpleDepGraph> ccDeps = DEP_READER.read(new File(ccDepDir, file.getName().replace(".txt", ".dep")));
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                s.setParseTree(trees.get(i));
                assignPOS(s);
                s = domainAnnotator.annotate(s);
                List<PPIPair> pairs = getInteractionPairs(s, deps.get(i), ccDeps.get(i));
                List<Pair<Integer, Integer>> candidates = candidateExtractor.getCandidates(s, deps.get(i), ccDeps.get(i));
                Map<String, Boolean> map = new HashMap<String, Boolean>();
                for (PPIPair pair : pairs) {
                    map.put(String.valueOf(pair.entity1) + "_" + String.valueOf(pair.entity2), pair.interact);
                }                
                for (Pair<Integer, Integer> candidate : candidates) {
                    String key = String.valueOf(candidate.first()) + "_" + String.valueOf(candidate.second());
                    if (map.containsKey(key)) {
                        if (/*check(s, deps.get(i), ccDeps.get(i), candidate.first(), candidate.second()) != -1 &&*/ map.get(key) == true) {
                            tp++;                            
                        }
                        else {                            
                            fp++;
                        }
                    } else {
                        System.out.println("error");
                    }
                }                
            }
        }
        System.out.println("tp = " + tp);
        System.out.println("fp = " + fp);        
    }
    boolean isNegated(int index, SimpleDepGraph depGraph) {
        List<SimpleDependency> govDependencies = depGraph.getGovDependencies(index);
        for (SimpleDependency dep : govDependencies) {
            if (dep.reln().endsWith("neg")) return true;
        }
        return false;
    }
    public static void main(String args[]) {
        RuleExtractor extractor = new RuleExtractor();
        extractor.filter("./resource/relation/PPI2/AImed");
        //extractor.checkCandidateExtractor("./resource/relation/PPI2/AImed");
    }
}
