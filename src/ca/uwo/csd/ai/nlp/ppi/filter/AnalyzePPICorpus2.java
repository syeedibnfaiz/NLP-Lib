/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import edu.stanford.nlp.trees.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

/**
 *
 * @author tonatuni
 */
public class AnalyzePPICorpus2 {
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static HashSet<String> set = new HashSet<String>();
    static {
        String[] names = new String[]{"BioInfer.d122.s0","BioInfer.d13.s0","BioInfer.d184.s0","BioInfer.d221.s0","BioInfer.d244.s0","BioInfer.d31.s0","BioInfer.d310.s1","BioInfer.d379.s2","BioInfer.d395.s0","BioInfer.d422.s0","BioInfer.d441.s0","BioInfer.d45.s2","BioInfer.d471.s3","BioInfer.d529.s0","BioInfer.d535.s0","BioInfer.d750.s0","BioInfer.d785."};
        set.addAll(Arrays.asList(names));
    }
    public static void main(String[] args) {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        String ppiBase = "./resource/relation/PPI4/";
        CorpusReader corpusReader = new CorpusReader();
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (String corpus : corpora) {
            if (!corpus.equals("BioInfer")) {
                continue;
            }
            System.out.println(corpus);
            String corpusRoot = ppiBase + corpus;
            List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusRoot);
            DomainTermRanker.rank(relationInstances);
            for (RelationInstance instance : relationInstances) {
                analyze(instance);
                //print(instance);
                //count(instance, map);
            }

        }
        showCount(map);
    }

    private static void print(RelationInstance instance) {
        if (instance.interaction) {
            Sentence s = instance.s;
            int lcs = instance.lcs;
            int key = instance.key;
            List<String> path = instance.path;
            if (path != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < path.size(); i += 2) {
                    int index = Integer.parseInt(path.get(i));
                    if (s.get(index).getTag("DOMAIN") != null) {
                        sb.append(s.get(index).word() + " ");
                    }
                }
                System.out.println(s);
                System.out.print("path: ");
                printBackbone(s, instance.path);
                System.out.println(convertBackbone(instance));
                System.out.println("key: " + s.get(key).word());
                System.out.println("lcs: " + s.get(lcs).word());
                System.out.println("domain terms on path: " + sb.toString());
                System.out.println("pair id: " + instance.pairIds[0]);
                System.out.println(instance.pairIds[1] + "-" + instance.pairIds[2]);
                System.out.println("");
            }
        }
    }

    private static void analyze(RelationInstance instance) {
        if (instance.interaction && instance.path != null) {
            Sentence s = instance.s;            
            LeftPatternFilter filter = new LeftPatternFilter();
            if (filter.apply(instance) == false) {
                System.out.println(s);
                printBackbone(s, instance.path);
                System.out.println("");
            }
        } else {
            Sentence s = instance.s;  
            if (instance.entity2 == (instance.entity1 + 1)) {
                //System.out.println("-");                
            }
        }
    }

    private static boolean needsTransformation(RelationInstance instance) {
        Sentence s = instance.s;
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        int colonIndex = -1;
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().equals(":")) {
                colonIndex = i;
                break;
            }
        }
        
        if (colonIndex != -1) {            
            Tree colonLeaf = leaves.get(colonIndex);
            Tree ccTree = null;
            if ((ccTree = findCC(colonLeaf, root)) != null) {
                System.out.println(s);
                System.out.println("count: " + countConjoins(ccTree, root));
                return true;
            }
        }
        return false;
    }
    private static int countConjoins(Tree ccTree, Tree root) {
        Tree parent = ccTree.parent(root);
        List<Tree> children = parent.getChildrenAsList();
        int count = 0;
        for (Tree child : children) {
            if (!child.value().matches(",|CC")) {
                count++;
            }
        }
        return count;
    }
    private static Tree findCC(Tree colonLeaf, Tree root) {
        Tree colonParent = colonLeaf.parent(root);
        Tree sibling = null;
        int dist = 1;
        Queue<Tree> q = new LinkedList<Tree>();
        while ((sibling = TREE_ANALYZER.getSibling(root, colonParent, dist)) != null) {
            q.add(sibling);
            dist++;
        }
        while (q.isEmpty() == false) {
            Tree t = q.poll();
            if (t.value().equals("CC")) {
                return t;
            } else if (t.isPhrasal()) {
                List<Tree> children = t.getChildrenAsList();
                for (Tree child : children) {
                    q.add(child);
                }
            }            
        }
        return null;
    }
    private static void count(RelationInstance instance, HashMap<String, Integer> map) {
        if (instance.interaction && instance.type == RelationInstance.MIDDLE && instance.path != null) {
            Sentence s = instance.s;
            int lcsIndex = instance.lcsIndex;
            int sz = instance.path.size();
            for (int i = lcsIndex + 1; i < sz; i += 2) {
                String reln = instance.path.get(i);
                if (map.containsKey(reln)) {
                    map.put(reln, map.get(reln) + 1);
                } else {
                    map.put(reln, 1);
                }
            }
        }
    }

    private static void showCount(HashMap<String, Integer> map) {
        List<Entry<String, Integer>> entrySet = new ArrayList<Entry<String, Integer>>(map.entrySet());
        Collections.sort(entrySet, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        for (Entry<String, Integer> entry : entrySet) {
            System.out.println(entry.getKey() + "-" + entry.getValue());
        }
    }

    private static void printBackbone(Sentence s, List<String> backBonePath) {
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

    private static String convertBackbone(RelationInstance instance) {
        List<String> path = instance.path;
        Sentence s = instance.s;
        int lcsIndex = instance.lcsIndex;
        int sz = path.size();
        String[] node = new String[sz];

        for (int i = 0; i < lcsIndex; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String word = s.get(index).word();
                if (s.get(index).getTag("DOMAIN") != null) {
                    word += "*";
                }
                if (index == instance.key) {
                    word += "^";
                }
                if (i == 0) {
                    node[i] = word;
                } else {
                    node[i] = "(" + word + " " + node[i - 1] + ")";
                }
            } else {
                String reln = path.get(i).substring(1);
                if (reln.contains("_")) {
                    reln = reln.substring(reln.indexOf('_') + 1);
                }
                node[i] = "(" + reln + " " + node[i - 1] + ")";
            }
        }
        for (int i = sz - 1; i > lcsIndex; i--) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String word = s.get(index).word();
                if (s.get(index).getTag("DOMAIN") != null) {
                    word += "*";
                }
                if (index == instance.key) {
                    word += "^";
                }
                if (i == (sz - 1)) {
                    node[i] = word;
                } else {
                    node[i] = "(" + word + " " + node[i + 1] + ")";
                }
            } else {
                String reln = path.get(i);
                if (reln.contains("_")) {
                    reln = reln.substring(reln.indexOf('_') + 1);
                }
                node[i] = "(" + reln + " " + node[i + 1] + ")";
            }
        }

        String lcsWord = s.get(instance.lcs).word();
        if (s.get(instance.lcs).getTag("DOMAIN") != null) {
            lcsWord += "*";
        }
        if (instance.lcs == instance.key) {
            lcsWord += "^";
        }
        node[lcsIndex] = "(" + lcsWord + " " + node[lcsIndex - 1] + " " + node[lcsIndex + 1] + ")";

        return node[lcsIndex];
    }
}
