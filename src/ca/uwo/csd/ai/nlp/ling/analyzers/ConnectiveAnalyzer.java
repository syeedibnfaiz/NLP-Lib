/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.analyzers;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.utils.Util;
import edu.stanford.nlp.trees.Tree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ConnectiveAnalyzer implements Serializable {

    private static final SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    HashMap<String, String> conn2CatMap;
    List<String> connectives;
    public ConnectiveAnalyzer() {
        this("./resource/ml/data/pdtb/explicit_conn_types_category");
    }

    /**
     * 
     * @param lexicon should contain the conn,category pairs
     */
    public ConnectiveAnalyzer(String lexiconPath) {
        List<String> lines = Util.readLines(lexiconPath);
        conn2CatMap = new HashMap<String, String>();
        connectives = new ArrayList<String>();
        for (String line : lines) {
            String tokens[] = line.split("\t");
            connectives.add(tokens[0]);
            conn2CatMap.put(tokens[0], tokens[1]);
            //System.out.println(tokens[0]);
        }
    }
    public String getCategory(String connectiveHead) {
        return conn2CatMap.get(connectiveHead);        
    }
    public boolean isBaseConnective(String conn) {
        return conn2CatMap.containsKey(conn);
    }
    public String getConnectiveHead(Tree root, String gornAddress) {
        List<Tree> connNodes = treeAnalyzer.getGornNodes(root, gornAddress);
        
        String fullConn = null;
        for (Tree connNode : connNodes) {
            if (fullConn != null) fullConn += " ";
            else fullConn = "";
            fullConn += treeAnalyzer.toString(connNode);
        }
        fullConn = fullConn.toLowerCase();
        for (String conn : connectives) {
            if (fullConn.contains(conn)) return conn;
        }
        root.pennPrint();
        System.out.println(fullConn);
        System.out.println(gornAddress);
        return null;
    }
    
    public List<Tree> getConnHeadLeaves(Tree root, String fullGornAddress, String connHead) {
        List<Tree> connNodes = treeAnalyzer.getGornNodes(root, fullGornAddress);
        List<Tree> leaves = new ArrayList<Tree>();
        for (Tree connNode : connNodes) {
            leaves.addAll(connNode.getLeaves());
        }
        String headTokens[] = connHead.split("\\s+");
        if (headTokens[0].matches("(A|a)fterwards?")) {
            headTokens[0] = "afterwards?";
        }
        List<Tree> headLeaves = new ArrayList<Tree>();
        for (int i = 0; i <= leaves.size() - headTokens.length; i++) {
            boolean matched = true;
            for (int j = 0; j < headTokens.length; j++) {
                //if (!leaves.get(i+j).value().toLowerCase().equals(headTokens[j])) {
                //changed on 8-01-2012 beacuse uppercase in connHead and afterward(s)
                if (!leaves.get(i+j).value().toLowerCase().matches(headTokens[j].toLowerCase())) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                for (int j = 0; j < headTokens.length; j++) {
                    headLeaves.add(leaves.get(i + j));
                }
                break;
            }
        }
        return headLeaves;
    }
    /**
     * Returns the leftmost preposition or the last word.
     * Follows Ben Wellner's definition of connective head word.
     * @param root
     * @param connStart
     * @param connEnd
     * @return 
     */
    public int getHeadWord(Tree root, int connStart, int connEnd) {
        if (connStart == connEnd) return connEnd;
        List<Tree> leaves = root.getLeaves();        
        for (int i = connStart; i <= connEnd; i++) {
            Tree parent = leaves.get(i).parent(root);
            if (parent.value().equals("IN")) return i;
        }
        return connEnd;
    }
    public static void main(String args[]) {
        /*List<String> pdtbFiles = Util.listAllFiles("./pdtb_v2/piped_data_2", ".pipe");
        List<String> ptbFiles = Util.listAllFiles("./pdtb_v2/ptb", ".mrg");
        
        
        for (int i = 0; i < pdtbFiles.size(); i++) {
            List<PDTBRelation> relations = pipedFileReader.read(new File(pdtbFiles.get(i)));
            List<Tree> trees = ptbFileReader.read(new File(ptbFiles.get(i)));
            System.out.println(pdtbFiles.get(i));
            System.out.println(ptbFiles.get(i));
            for (PDTBRelation relation : relations) {
                if (relation.getType().equals("Explicit")) {
                    String gornAddress = relation.getConnectiveGornAddress();
                    int lineNum = Integer.parseInt(gornAddress.split(",")[0]);
                    trees.get(lineNum).pennPrint();
                    System.out.println(relation.getConnRawText());
                    System.out.println(relation.getConnectiveGornAddress());
                    String conn = connAnalyzer.getConnective(trees.get(lineNum), gornAddress);
                    if (conn == null) {
                        System.out.println("Error for: " + relation.getConnRawText());
                        //treeAnalyzer.getGornNode(trees.get(lineNum), gornAddress).pennPrint();
                    }
                }
            }
        }*/
        PDTBPipedFileReader pipedFileReader = new PDTBPipedFileReader();
        PTBFileReader ptbFileReader = new PTBFileReader();
        ConnectiveAnalyzer connAnalyzer = new ConnectiveAnalyzer();
        File pdtbRoot = new File("./pdtb_v2/piped_data_2");
        File ptbRoot = new File ("./pdtb_v2/ptb");
        File[] pdtbSections = pdtbRoot.listFiles();
        int count = 0;
        for (File pdtbSection : pdtbSections) {
            File[] pdtbFiles = pdtbSection.listFiles();
            for (File pdtbFile : pdtbFiles) {
                File ptbFile = new File(ptbRoot, pdtbSection.getName() + "/" + pdtbFile.getName().replace(".pipe", ".mrg"));
                List<PDTBRelation> relations = pipedFileReader.read(pdtbFile);
                List<Tree> trees = ptbFileReader.read(ptbFile);
                
                for (PDTBRelation relation : relations) {
                    if (relation.getType().equals("Explicit")) {
                        String gornAddress = relation.getConnectiveGornAddress();
                        if (gornAddress.isEmpty()) continue;
                        int lineNum = Integer.parseInt(gornAddress.split(",")[0]);
                        /*trees.get(lineNum).pennPrint();
                        System.out.println(relation.getConnRawText());
                        System.out.println(relation.getConnectiveGornAddress());*/
                        String conn = connAnalyzer.getConnectiveHead(trees.get(lineNum), gornAddress);
                        if (conn == null) {
                            System.out.println("Error for: " + relation.getConnRawText());
                            //treeAnalyzer.getGornNode(trees.get(lineNum), gornAddress).pennPrint();
                        }
                        List<Tree> connHeadLeaves = connAnalyzer.getConnHeadLeaves(trees.get(lineNum), gornAddress, relation.getConnHead());
                        String head = null;
                        for (Tree connHeadLeaf : connHeadLeaves) {
                            if (head != null) head += " ";
                            else head = "";
                            head += connHeadLeaf.value().toLowerCase();
                        }
                        if (head != null && !head.equals(relation.getConnHead())) {
                            System.out.println("Head missmatch");
                            System.out.println("org: " + relation.getConnHead());
                            System.out.println(head);
                        } else if (head == null) {
                            System.out.println("head null: " + relation.getConnHead() + ", " + relation.getConnRawText());
                            //trees.get(lineNum).pennPrint();
                            //System.out.println(gornAddress);
                        } else {
                            System.out.println(++count);
                        }
                    }
                }
            }
        }
    }
}
