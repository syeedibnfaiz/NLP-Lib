/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.libsvm;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kernel.CustomKernel;
import kernel.KernelManager;
import libsvm.Instance;
import libsvm.SVMTrainer;
import libsvm.svm_node;
import libsvm.svm_parameter;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PPI {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    final private SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    final private BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();
    
    public static void main(String[] args) throws IOException {
        PPI ppi = new PPI();
        //ppi.createLibSVMFile("./resource/relation/PPI2/LLL", "lll.svm");
        //ppi.createLibSVMFile("./resource/relation/PPI2/HPRD50", "hprd50.svm");
        //ppi.createLibSVMFile("./resource/relation/PPI2/IEPA", "iepa.svm");
        //ppi.test("./resource/relation/PPI2/LLL");
        //ppi.test("./resource/relation/PPI2/HPRD50");
        //ppi.test("./resource/relation/PPI2/IEPA");
        ppi.test("./resource/relation/PPI2/AIMed");
    }
    
    public void test(String corpusRoot) {
        List<RelationInstance> relationInstances = getRelationInstances(corpusRoot);
        int N = relationInstances.size();
        Instance[] instances = new Instance[N];
        for (int i = 0; i < N; i++) {
            //Tree t = (Tree) relationInstances.get(i).obj;
            //t = TREE_ANALYZER.getLCA(t, relationInstances.get(i).i, relationInstances.get(i).j);
            double label = -1;
            if (relationInstances.get(i).interaction) {
                label = 1;
            }
            //instances[i] = new Instance(label, t);
            
            SimpleDepGraph depGraph = (SimpleDepGraph) relationInstances.get(i).obj;
            //DepGraph dg = new DepGraph(relationInstances.get(i).s, depGraph, depGraph.getLCS(relationInstances.get(i).i, relationInstances.get(i).j), relationInstances.get(i).i, relationInstances.get(i).j);
            //DepGraph dg = new DepGraph(relationInstances.get(i).s, depGraph, -1, relationInstances.get(i).i, relationInstances.get(i).j);
            //DepGraph dg = new DepGraph(relationInstances.get(i).s, depGraph, getKeyTerm(relationInstances.get(i).s, depGraph, relationInstances.get(i).i, relationInstances.get(i).j), relationInstances.get(i).i, relationInstances.get(i).j);
            
            //DepGraph dg = new DepGraph(relationInstances.get(i).s, null, -1, relationInstances.get(i).i, relationInstances.get(i).j);
            DepGraph dg = new DepGraph(relationInstances.get(i).s, depGraph, -1, relationInstances.get(i).i, relationInstances.get(i).j);
            instances[i] = new Instance(label, dg);
        }
        
        //KernelManager.setCustomKernel(new kernel.TreeKernel());
        //KernelManager.setCustomKernel(new DependencyKernel());
        KernelManager.setCustomKernel(new LCSDepPathKernel());
        svm_parameter param = new svm_parameter();
        //param.C = 0.5;
        //param.shrinking = 0;
        param.cache_size = 1500;        
        //SVMTrainer.doCrossValidation(instances, param, 10, true);
        SVMTrainer.doInOrderCrossValidation(instances, param, 10, true);
        //testKernel(instances);
    }
    
    private void testKernel(Instance[] instances) {
        LCSDepPathKernel kernel = (LCSDepPathKernel)KernelManager.getCustomKernel();
        DepGraph dg1;
        DepGraph dg2;
        
        for (int i = 0; i < instances.length; i++) {
            if (instances[i].getLabel() > 0) {
                svm_node node1 = new svm_node(instances[i].getData());
                for (int j = i + 1; j < instances.length; j++) {
                    if (instances[j].getLabel() < 0) {
                        svm_node node2 = new svm_node(instances[j].getData());
                        double score = KernelManager.getCustomKernel().evaluate(node1, node2);
                        if (score > .7) {      
                            dg1 = (DepGraph)instances[i].getData();
                            dg2 = (DepGraph)instances[j].getData();
                            System.out.println(dg1.s);
                            List<String> path1 = kernel.getBackBonePath(dg1);
                            System.out.println(instances[i].getLabel() + "-" + path1);
                            System.out.println(dg2.s);
                            List<String> path2 = kernel.getBackBonePath(dg2);                            
                            System.out.println(instances[j].getLabel() + "-" + path2);
                            System.out.println("score: " + score + "\n");
                        }
                    }
                }
            }
        }
    }
    public void createLibSVMFile(String corpusRoot, String outputFile) throws IOException {
        List<RelationInstance> relationInstances = getRelationInstances(corpusRoot);
        int N = relationInstances.size();
        double[][] gramMatrix = getGramMatrix(relationInstances);
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        for (int i = 0; i < N; i++) {
            writer.write(relationInstances.get(i).interaction?"+1":"-1");
            writer.write(" 0:" + (i + 1));
            for (int j = 0; j < N; j++) {
                writer.write(" " + (j + 1) + ":" + String.format("%.4f", gramMatrix[i][j]));                
            }
            writer.write("\n");
        }
        writer.close();
    }
    
    public List<RelationInstance> getRelationInstances(String corpusRoot) {
        ArrayList<RelationInstance> relationInstances = new ArrayList<RelationInstance>();
        
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
                s.setParseTree(null);
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
    
    private double[][] getGramMatrix(List<RelationInstance> relationInstances) {
        double[][] matrix = new double[relationInstances.size()][relationInstances.size()];
        
        TreeKernel treeKernel = new TreeKernel();
        for (int i = 0; i < relationInstances.size(); i++) {
            Tree t = (Tree) relationInstances.get(i).obj;
            t = TREE_ANALYZER.getLCA(t, relationInstances.get(i).i, relationInstances.get(i).j);
            matrix[i][i] = treeKernel.evaluate(t, t);
        }
        for (int i = 0; i < relationInstances.size(); i++) {
            for (int j = 0; j < i; j++) {
                Tree t1 = (Tree) relationInstances.get(i).obj;
                t1 = TREE_ANALYZER.getLCA(t1, relationInstances.get(i).i, relationInstances.get(i).j);
                Tree t2 = (Tree) relationInstances.get(j).obj;
                t2 = TREE_ANALYZER.getLCA(t2, relationInstances.get(j).i, relationInstances.get(j).j);
                matrix[i][j] = treeKernel.evaluate(t1, t2)/Math.sqrt(matrix[i][i] * matrix[j][j]);
                matrix[j][i] = matrix[i][j];
            }
            
        }
        return matrix;
    }
    
    private List<RelationInstance> getRelationInstances(Sentence s, SimpleDepGraph depGraph) {
        List<RelationInstance> instances = new ArrayList<RelationInstance>();
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
                    //instances.add(new PPIPair(entity1, i, true));
                    //instances.add(new RelationInstance(s.getParseTree(), s, entity1, i, true));
                    instances.add(new RelationInstance(depGraph, s, entity1, i, true));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = n1Map.get(token);
                    //instances.add(new PPIPair(entity1, i, false));
                    //instances.add(new RelationInstance(s.getParseTree(), s, entity1, i, false));
                    instances.add(new RelationInstance(depGraph, s, entity1, i, false));
                }
            }
        }
        return instances;
    }
    
    private int getKeyTerm(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {        
        int key = -1;
        key = findKeyTerm(s, depGraph, entity1, entity2, entity1 + 1, entity2 - 1, 10, true);
        if (key == -1) {
            int lKey = findKeyTerm(s, depGraph, entity1, entity2, 0, entity1 - 1, 10, false);
            int rKey = findKeyTerm(s, depGraph, entity1, entity2, entity2 + 1, s.size() - 1, 10, true);
            if (lKey >= 0 && rKey >= 0) {
                if (Math.abs(lKey-entity1) < Math.abs(rKey-entity2)) {
                    key = lKey;
                } else {
                    key = rKey;
                }
            } else if (lKey >= 0) {
                key = lKey;
            } else if (rKey >= 0) {
                key = rKey;
            }
        }
        
                
        return key;
    }
    
    private int findKeyTerm(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2, int start, int end, int dist, boolean leftToRight) {
        int key = -1;
        if (leftToRight) {
            for (int i = start; i <= end; i++) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    List<Integer> reachableIndices = depGraph.getReachableIndices(i, true, dist);
                    if (reachableIndices.contains(entity1) && reachableIndices.contains(entity2)) {
                        key = i;
                        break;
                    }
                }
            }
        } else {
            for (int i = end; i >= start; i--) {
                if (s.get(i).getTag("DOMAIN") != null) {
                    List<Integer> reachableIndices = depGraph.getReachableIndices(i, true, dist);
                    if (reachableIndices.contains(entity1) && reachableIndices.contains(entity2)) {
                        key = i;
                        break;
                    }
                }
            }
        }
        
        if (key == -1) {
            if (leftToRight) {
                for (int i = start; i <= end; i++) {
                    List<Integer> reachableIndices = depGraph.getReachableIndices(i, true, dist);
                    if (reachableIndices != null && reachableIndices.contains(entity1) && reachableIndices.contains(entity2)) {
                        for (Integer index : reachableIndices) {
                            if (s.get(index).getTag("DOMAIN") != null) {
                                key = i;
                                break;
                            }
                        }
                        if (key != -1) {
                            break;
                        }
                    }
                }
            } else {
                for (int i = end; i >= start; i--) {
                    List<Integer> reachableIndices = depGraph.getReachableIndices(i, true, dist);
                    if (reachableIndices != null && reachableIndices.contains(entity1) && reachableIndices.contains(entity2)) {
                        for (Integer index : reachableIndices) {
                            if (s.get(index).getTag("DOMAIN") != null) {
                                key = i;
                                break;
                            }
                        }
                        if (key != -1) {
                            break;
                        }
                    }
                }
            }
        }
        return key;
    }
}

class RelationInstance {
    public Object obj;
    public Sentence s;
    public int i;
    public int j;
    public boolean interaction;

    public RelationInstance(Object obj, Sentence s, int i, int j, boolean interaction) {
        this.obj = obj;
        this.s = s;
        this.i = i;
        this.j = j;
        this.interaction = interaction;
    }
    
    
}