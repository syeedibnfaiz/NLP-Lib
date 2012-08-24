package ca.uwo.csd.ai.nlp.libsvm;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class AnalyzePPICorpus {

    final static private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final static private PTBFileReader TREE__READER = new PTBFileReader();
    final static private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    final static private SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();

    public static void main(String[] args) {
        analyze("./resource/relation/PPI2/AIMed");
    }

    public static void analyze(String corpusRoot) {        
        BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            //File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            //File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");

            Text text = TEXT_READER.read(iobFile);
            //List<Tree> trees = TREE__READER.read(treeFile);
            List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            //List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);

            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                s = domainAnnotator.annotate(s);
                //Tree root = trees.get(i);
                SimpleDepGraph depGraph = deps.get(i);
                //SimpleDepGraph ccDepGraph = ccDeps.get(i);
                //s.setParseTree(root);      
                //analyze(s, ccDepGraph);
                analyze(s, depGraph);
            }
        }        
    }
    
    public static void analyze(Sentence s, SimpleDepGraph depGraph) {
        List<RelationInstance> relationInstances = getRelationInstances(s, depGraph);
        for (RelationInstance rel : relationInstances) {
            if (rel.interaction) {
                int keyTerm = getKeyTerm(s, depGraph, rel.i, rel.j);
                if (keyTerm == -1) {
                    System.out.println("-1");
                    //System.out.println(s);
                    //System.out.println(s.get(rel.i)+" - "+s.get(rel.j));
                } else if (keyTerm >= rel.i && keyTerm <= rel.j) {
                    System.out.println("between");
                    System.out.println(s);
                    System.out.println(s.get(rel.i) + " - " + s.get(rel.j));
                } else if (keyTerm < rel.i) {
                    System.out.println("before");
                    System.out.println(s);
                    System.out.println(s.get(rel.i) + " - " + s.get(rel.j));
                } else {
                    System.out.println("after");
                    System.out.println(s);
                    System.out.println(s.get(rel.i) + " - " + s.get(rel.j));
                }
                /*int lcs = depGraph.getLCS(rel.i, rel.j);
                if (lcs == -1) {
                    System.out.println(s);
                    System.out.println(s.get(rel.i)+" - "+s.get(rel.j));
                }*/
            }
        }
    }
    
    private static int getKeyTerm(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {        
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
    
    private static int findKeyTerm(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2, int start, int end, int dist, boolean leftToRight) {
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
    private static List<RelationInstance> getRelationInstances(Sentence s, SimpleDepGraph depGraph) {
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
}
