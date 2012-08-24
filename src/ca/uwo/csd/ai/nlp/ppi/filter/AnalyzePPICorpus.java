package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.libsvm.DepGraph;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import libsvm.Instance;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class AnalyzePPICorpus {

    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    final private SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    final private BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();

    public void analyze(String corpusRoot) {
        List<SimpleRelationInstance> relationInstances = getRelationInstances(corpusRoot);
        int N = relationInstances.size();
        Instance[] instances = new Instance[N];
        for (int i = 0; i < N; i++) {
            double label = -1;
            if (relationInstances.get(i).interaction) {
                label = 1;
            }
            SimpleDepGraph depGraph = (SimpleDepGraph) relationInstances.get(i).obj;
            DepGraph dg = new DepGraph(relationInstances.get(i).s, depGraph, -1, relationInstances.get(i).i, relationInstances.get(i).j);
            instances[i] = new Instance(label, dg);
        }
        SimpleDepGraph dep;
        int count = 0;
        int sum = 0;
        for (Instance instance : instances) {
            if (instance.getLabel() < 0) {
                DepGraph dg = (DepGraph) instance.getData();
                List<String> backBonePath = getBackBonePath(dg);
                if (backBonePath == null) {
                    /*Sentence s = dg.s;
                    System.out.println(s);
                    System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                    System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                    //printBackbone(s, backBonePath);
                    System.out.println("");
                    count++;*/
                } else {
                    Sentence s = dg.s;
                    dep = dg.depGraph;
                    int lcs = getLCS(dg);
                    int protein = 0;
                    /*for (int i = 0; i < s.size(); i++) {
                    if (s.get(i).word().contains("PROTEIN")) {
                    protein++;
                    }
                    }
                    if (protein == 2) {
                    System.out.println(s);
                    }*/
                    if (dg.entity1 == dg.entity2 && s.get(lcs).getTag("POS").matches("VB.*")) {
                        /*count++;
                        System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");*/
                    }
                    if (Math.abs(dg.entity1 - dg.entity2) < 3) {
                        /*count++;
                        System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");*/
                    }
                    if (lcs < dg.entity1) {
                        //System.out.println("left");

                        String lcsStr = String.valueOf(lcs);
                        int dist = 0;
                        for (int i = 0; i < backBonePath.size(); i++) {
                            if (backBonePath.get(i).equals(lcsStr)) {
                                dist = i;
                                break;
                            }
                        }
                        boolean prep1 = false;
                        for (int i = 1; i < dist; i += 2) {
                            if (backBonePath.get(i).matches("-?prep.*")) {
                                prep1 = true;
                                break;
                            }
                        }
                        boolean prep2 = false;
                        for (int i = dist + 1; i < backBonePath.size(); i += 2) {
                            if (backBonePath.get(i).matches("-?prep.*")) {
                                prep2 = true;
                                break;
                            }
                        }
                        /*if (!prep1 && !prep2) {
                        count++;
                        System.out.println(dist);
                        System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");
                        //sum += dist;
                        }*/
                        if ((!prep1 && !prep2) || s.get(lcs).getTag("POS").matches("VB.*")) {
                            /*count++;
                            System.out.println(s);
                            System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                            //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                            printBackbone(s, backBonePath);
                            System.out.println("");*/
                        }
                    } else if (lcs < dg.entity2) {
                        boolean nsubj = false;
                        boolean prep = false;
                        boolean clausal = false;
                        for (int i = 1; i < backBonePath.size(); i += 2) {
                            /*if (backBonePath.get(i).contains("subj")) {
                            nsubj = true;
                            break;
                            } else if (backBonePath.get(i).contains("prep")) {
                            prep = true;
                            break;
                            }*/
                            if (backBonePath.get(i).matches("rcmod|advcl|ccomp|parataxis")) {
                                clausal = true;
                            }
                        }
                        //if (true || !nsubj && !prep) {
                        if (clausal) {
                            /*System.out.println(s);
                            System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                            //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                            printBackbone(s, backBonePath);
                            System.out.println("");
                            count++;*/
                        }
                    } else if (lcs > dg.entity2) {
                        /*List<Integer> reachableIndices = dg.depGraph.getReachableIndices(lcs, true, 10, "rcmod|conj_(and|or|but)|advcl");
                        boolean protein = false;
                        for (int i : reachableIndices) {
                        if (i > lcs && i != dg.entity1 && i != dg.entity2 && s.get(i).word().contains("PROTEIN")) {
                        protein = true;
                        break;
                        }
                        }
                        if (protein) {
                        System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");
                        count++;
                        }*/
                    }
                    boolean domain = false;
                    for (int i = 0; i < backBonePath.size(); i += 2) {
                        int index = Integer.parseInt(backBonePath.get(i));
                        if (s.get(index).getTag("DOMAIN") != null) {
                            domain = true;
                            break;
                        } else {
                            for (SimpleDependency dependency : dep.getDepDependencies(index)) {
                                int gov = dependency.gov();
                                if (s.get(gov).getTag("DOMAIN") != null) {
                                    domain = true;
                                    break;
                                }
                            }
                            for (SimpleDependency dependency : dep.getGovDependencies(index)) {
                                int dependent = dependency.dep();
                                if (s.get(dependent).getTag("DOMAIN") != null) {
                                    domain = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (domain) {
                        System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");
                        count++;
                    }
                    boolean neg = false;
                    for (int i = 0; i < backBonePath.size(); i += 2) {
                        int index = Integer.parseInt(backBonePath.get(i));
                        for (SimpleDependency dependency : dep.getGovDependencies(index)) {
                            String reln = dependency.reln();
                            if (reln.matches("neg")) {
                                neg = true;
                                break;
                            }
                        }
                    }
                    if (neg) {
                        /*System.out.println(s);
                        System.out.println(s.get(dg.entity1) + "-" + s.get(dg.entity2));
                        //System.out.println(dg.depGraph.getPath(dg.entity1, dg.entity2));
                        printBackbone(s, backBonePath);
                        System.out.println("");
                        count++;*/
                    }
                }
            }
        }
        System.out.println(count);
    }

    private void printBackbone(Sentence s, List<String> backBonePath) {
        for (int i = 0; i < backBonePath.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.valueOf(backBonePath.get(i));
                System.out.print(s.get(index));
            } else {
                System.out.print(":" + backBonePath.get(i) + ":");
            }
        }
        System.out.println("");
    }

    public List<SimpleRelationInstance> getRelationInstances(String corpusRoot) {
        ArrayList<SimpleRelationInstance> relationInstances = new ArrayList<SimpleRelationInstance>();

        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            //File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");

            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            //List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);

            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                s = domainAnnotator.annotate(s);
                Tree root = trees.get(i);
                s.setParseTree(root);
                TREE_ANALYZER.assignPOS(s);
                //s.setParseTree(null);
                root = null;

                //SimpleDepGraph depGraph = deps.get(i);
                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                //s.setParseTree(root);
                //relationInstances.addAll(getRelationInstances(s, depGraph));
                relationInstances.addAll(getRelationInstances(s, ccDepGraph));
            }
        }
        return relationInstances;
    }

    private List<SimpleRelationInstance> getRelationInstances(Sentence s, SimpleDepGraph depGraph) {
        List<SimpleRelationInstance> instances = new ArrayList<SimpleRelationInstance>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();
        Map<String, Integer> n1Map = new HashMap<String, Integer>();

        //find P1 and N1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token : tokens) {
                    p1Map.put(token, i);
                }
            }
            if (!word.getTag("N1").equals("O")) {
                String[] tokens = word.getTag("N1").split(", ");
                for (String token : tokens) {
                    n1Map.put(token, i);
                }
            }
        }

        //find P2 and N2 and match with P1 and N2
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token : tokens) {
                    Integer entity1 = p1Map.get(token);
                    //instances.add(new PPIPair(entity1, i, true));
                    //instances.add(new RelationInstance(s.getParseTree(), s, entity1, i, true));
                    instances.add(new SimpleRelationInstance(depGraph, s, entity1, i, true));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token : tokens) {
                    Integer entity1 = n1Map.get(token);
                    //instances.add(new PPIPair(entity1, i, false));
                    //instances.add(new RelationInstance(s.getParseTree(), s, entity1, i, false));
                    instances.add(new SimpleRelationInstance(depGraph, s, entity1, i, false));
                }
            }
        }
        return instances;
    }

    private void addAncestors(List<Integer> ancestors, Sentence s, SimpleDepGraph depGraph, int node, boolean onlyRelTerms) {
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(node);
        boolean[] visited = new boolean[s.size()];

        while (!queue.isEmpty()) {
            int next = queue.poll();
            if (visited[next]) {
                continue;
            }
            visited[next] = true;
            List<SimpleDependency> deps = depGraph.getDepDependencies(next);
            for (SimpleDependency dependency : deps) {
                int gov = dependency.gov();
                String reln = dependency.reln();
                if (reln.matches("conj_(and|or)")) {
                    continue;
                }
                if (onlyRelTerms) {
                    if (s.get(gov).getTag("DOMAIN") != null) {
                        ancestors.add(gov);
                    }
                } else {
                    ancestors.add(gov);
                }
                queue.add(gov);
            }
        }
    }

    private int getLCS(DepGraph dg) {
        SimpleDepGraph depGraph = dg.depGraph;
        Sentence s = dg.s;

        int entity1 = dg.entity1;
        int entity2 = dg.entity2;

        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1, false);

        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2, false);

        int lcs = -1;
        for (Integer ancestor : ancestors2) {
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }
        return lcs;
    }

    public List<String> getBackBonePath(DepGraph dg) {
        SimpleDepGraph depGraph = dg.depGraph;
        Sentence s = dg.s;

        int entity1 = dg.entity1;
        int entity2 = dg.entity2;

        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1, false);

        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2, false);

        int lcs = -1;
        for (Integer ancestor : ancestors2) {
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }

        if (lcs == -1) {    //path = e1 ---- e2 or e1 ++++ e2
            return null;
            /*List<SimpleDependency> relations = depGraph.getPathAsRelnList(entity1, entity2, false);
            if (relations == null) {
            return null;
            }
            List<String> path = new ArrayList<String>();            
            
            boolean rightDirection = (depGraph.getDependency(relations.get(0).gov(), relations.get(0).dep()) != null);
            for (int i = 0; i < relations.size(); i++) {
            int gov = relations.get(i).gov();                
            path.add(String.valueOf(gov));
            String reln = relations.get(i).reln();
            if (rightDirection) {
            path.add(reln);
            } else {
            path.add("-"+reln);
            }                                  
            }
            path.add(String.valueOf(relations.get(relations.size() - 1).dep()));*/
            /*path.add(String.valueOf(entity2));
            System.out.println(s);
            System.out.println(s.get(entity1) + "-" + s.get(entity2));
            System.out.println(depGraph.getPath(entity1, entity2));
            System.out.println(relations);
            System.out.println("path: "+path);*/
            //return path;
        }
        return getPath(dg, lcs);
    }

    public List<String> getBackBonePathOld(DepGraph dg) {
        SimpleDepGraph depGraph = dg.depGraph;
        Sentence s = dg.s;

        int entity1 = dg.entity1;
        int entity2 = dg.entity2;

        //get entity1's ancestors
        List<Integer> ancestors1 = new ArrayList<Integer>();
        addAncestors(ancestors1, s, depGraph, entity1, false);

        //get entity2's ancestors
        List<Integer> ancestors2 = new ArrayList<Integer>();
        addAncestors(ancestors2, s, depGraph, entity2, false);

        int lcs = -1;
        for (Integer ancestor : ancestors2) {
            if (ancestor != entity1 && ancestors1.contains(ancestor)) {
                lcs = ancestor; //path = e1 ---- lcs ++++ e2
                break;
            }
        }

        if (lcs == -1) {    //path = e1 ---- e2 or e1 ++++ e2
            return null;

        }
        return getPath(dg, lcs);
    }

    private List<String> getPath(DepGraph dg, int lcs) {
        SimpleDepGraph depGraph = dg.depGraph;
        int entity1 = dg.entity1;
        int entity2 = dg.entity2;

        List<SimpleDependency> path1 = depGraph.getPathAsRelnList(lcs, entity1, true);
        List<SimpleDependency> path2 = depGraph.getPathAsRelnList(lcs, entity2, true);

        ArrayList<String> path = new ArrayList<String>();
        for (int i = path1.size() - 1; i >= 0; i--) {
            int dep = path1.get(i).dep();
            path.add(String.valueOf(dep));
            path.add("-" + path1.get(i).reln());
        }
        for (int i = 0; i < path2.size(); i++) {
            int gov = path2.get(i).gov();
            path.add(String.valueOf(gov));
            path.add(path2.get(i).reln());
        }
        path.add(String.valueOf(entity2));
        return path;
    }

    public static void main(String[] args) {
        AnalyzePPICorpus analyzer = new AnalyzePPICorpus();
        analyzer.analyze("./resource/relation/PPI4/AIMed");
        //analyzer.analyze("./resource/relation/PPI2/LLL");
        //analyzer.analyze("./resource/relation/PPI3/BioInfer");
        //analyzer.analyze("./resource/relation/PPI2/IEPA");
        //analyzer.analyze("./resource/relation/PPI2/HPRD50");

        //analyzer.analyzeText("./resource/relation/PPI2/AIMed");
        //analyzer.analyzeText("./resource/relation/PPI2/BioInfer");
    }

    public void analyzeText(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        int totalSentence = 0;
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            //File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");

            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            //List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);

            totalSentence += text.size();
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                s = domainAnnotator.annotate(s);
                Tree root = trees.get(i);
                s.setParseTree(root);
                TREE_ANALYZER.assignPOS(s);

                //SimpleDepGraph depGraph = deps.get(i);
                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                //s.setParseTree(root);
                //analyzeSentence(s, ccDepGraph);
                if (i == 0) {
                    System.out.println(s);
                }
            }
        }
        System.out.println("Total sentence #:" + totalSentence);
    }

    private void analyzeSentence(Sentence s, SimpleDepGraph depGraph) {
        String prefix = getPrefix(s, 0, Math.min(4, s.size() - 1));
        if (prefix.matches("We#.*#VB.*#that#.*")) {
            System.out.println(s);
        } else if (prefix.matches("Our#.*#VB.*#that#.*")) {
            System.out.println(s);
        }
    }

    private String getPrefix(Sentence s, int begin, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = begin; i <= end; i++) {
            sb.append(s.get(i).word());
            sb.append("#");
            sb.append(s.get(i).getTag("POS"));
            sb.append("#");
        }
        return sb.toString();
    }
}

class SimpleRelationInstance {

    public Object obj;
    public Sentence s;
    public int i;
    public int j;
    public boolean interaction;

    public SimpleRelationInstance(Object obj, Sentence s, int i, int j, boolean interaction) {
        this.obj = obj;
        this.s = s;
        this.i = i;
        this.j = j;
        this.interaction = interaction;
    }
}