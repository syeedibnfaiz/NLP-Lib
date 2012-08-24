/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.DBUtil;
import ca.uwo.csd.ai.nlp.utils.SocketUtil;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author tonatuni
 */
public class ApplyTransformedApposPatch {
    private final static SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    HashMap<String, String> sentID2SentenceMap;

    public static void main(String args[]) throws IOException {
        ApplyTransformedApposPatch patch = new ApplyTransformedApposPatch();
        //patch.apply("./resource/relation/PPI6/BioInfer", "./resource/relation/bioinfer_colon_appos_patch.txt");
        //patch.updateDB("./resource/relation/PPI6/BioInfer", "./resource/relation/bioinfer_colon_appos_patch.txt");
    }

    public ApplyTransformedApposPatch() {
        sentID2SentenceMap = new HashMap<String, String>();
    }

    public void updateDB(String corpusRoot, String patchFile) throws IOException {
        fillMap(patchFile);
        CorpusReader reader = new CorpusReader();
        List<RelationInstance> relationInstances = reader.getRelationInstances(corpusRoot);
        DBUtil dbUtil = new DBUtil();
        dbUtil.connect("ppiannotation");
        String update = "UPDATE bioinfers SET sentence='%s', type='%s', path='%s', syntax='%s', lcs='%s', lcs_pos='%s', keyTerm='%s' where pairid='%s'";
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction && instance.path != null) {
                String sentId = instance.pairIds[0].substring(0, instance.pairIds[0].lastIndexOf('.'));
                if (sentID2SentenceMap.containsKey(sentId)) {
                    System.out.println("Updateing: " + instance.pairIds[0]);
                    printBackbone(instance.s, instance.path);
                    String sentence = instance.s.toString();
                    String type = getType(instance);
                    String path = convertBackbone(instance);
                    String syntax = treeAnalyzer.getPennOutput(instance.s.getParseTree());
                    String lcs = instance.s.get(instance.lcs).word();
                    String lcsPos = instance.s.get(instance.lcs).getTag("POS");
                    String key = instance.s.get(instance.key).word();
                    String pairid = instance.pairIds[0];
                    String query = String.format(update, sentence, type, path, syntax, lcs, lcsPos, key, pairid);
                    dbUtil.execUpdate(query);
                }
            }
        }
    }

    public static void printBackbone(Sentence s, List<String> backBonePath) {
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
    private String getSafeValue(String value) {
        return value.replace("'", "''");
    }

    private String getType(RelationInstance instance) {
        if (instance.lcs < instance.entity1) {
            return "LEFT";
        } else if (instance.lcs <= instance.entity2) {
            return "MIDDLE";
        } else {
            return "RIGHT";
        }
    }

    private String convertBackbone(RelationInstance instance) {
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

    public void apply(String corpusRoot, String patchFile) throws IOException {
        fillMap(patchFile);
        SocketUtil parserSocket = new SocketUtil("78.129.181.9", 4449);
        SocketUtil depSocket = new SocketUtil("localhost", 8662);
        for (String sentId : sentID2SentenceMap.keySet()) {
            String newLine = sentID2SentenceMap.get(sentId);
            parserSocket.sendLine(newLine + "\n");
            String parse = parserSocket.readline();
            depSocket.sendLine(parse + "\n");
            String dep = depSocket.readline();

            Document doc = new Document(corpusRoot, sentId);
            Sentence sentence = doc.getSentence(sentId);
            //System.out.println("old: " + sentence.toString("P1"));
            Sentence newS = getNewSentence(sentence, newLine);
            doc.setSentence(sentId, newS);
            doc.setTree(sentId, parse);
            doc.setDep(sentId, dep);

            doc.write();
        }
        parserSocket.closeConnection();
        depSocket.closeConnection();
    }

    private Sentence getNewSentence(Sentence oldS, String newText) {
        Sentence newS = new Sentence();
        HashMap<String, TokWord> map = new HashMap<String, TokWord>();
        for (TokWord word : oldS) {
            map.put(word.word(), word);
        }
        String[] tokens = newText.split("\\s+");
        TokWord comma = new TokWord(",");
        comma.setTag("P1", "O");
        comma.setTag("P2", "O");
        comma.setTag("N1", "O");
        comma.setTag("N2", "O");
        comma.setTag("BASE", ",");
        for (String token : tokens) {
            if (!map.containsKey(token)) {
                if (token.equals(",")) {
                    newS.add(comma);
                } else {
                    System.out.println(oldS);
                    System.out.println("Not found: " + token);
                }
            } else {
                newS.add(map.get(token));
            }
        }
        return newS;
    }

    private void fillMap(String patchFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(patchFile));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String sentID = line;
            String sentence = reader.readLine();
            sentID2SentenceMap.put(sentID, sentence);
            line = reader.readLine(); //empty line
        }
        reader.close();
    }
}

class Document {

    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2", "BASE"});
    String docId;
    String corpusRoot;
    Text text;
    List<String> trees;
    List<String> deps;
    List<String> sentids;

    public Document(String corpusRoot, String sentId) throws IOException {
        this.corpusRoot = corpusRoot;
        this.docId = sentId.substring(0, sentId.lastIndexOf('.')).replace('.', '_');
        read();
    }

    private void read() throws IOException {
        File corpusDir = new File(corpusRoot);
        File iobDir = new File(corpusDir, "iob");
        text = TEXT_READER.read(new File(iobDir, docId + ".txt"));

        File treeDir = new File(corpusDir, "trees");
        trees = Util.readLines(new File(treeDir, docId + ".mrg"));

        File depsDir = new File(corpusDir, "depsCC");
        deps = Util.readLines(new File(depsDir, docId + ".dep"));

        File infoDir = new File(corpusDir, "info");
        sentids = readInfos(infoDir);
    }

    private List<String> readInfos(File infoDir) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(infoDir, docId + ".txt")));
        List<String> ids = new ArrayList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            ids.add(tokens[0]);
            int n = Integer.parseInt(tokens[1]);
            for (int i = 0; i < n; i++) {
                line = reader.readLine();
            }
        }
        reader.close();
        return ids;
    }

    private int getIndex(String sentId) {
        if (!sentids.contains(sentId)) {
            return -1;
        } else {
            int index = sentids.indexOf(sentId);
            return index;
        }
    }

    public Sentence getSentence(String sentId) {
        int index = getIndex(sentId);
        if (index != -1) {
            return text.get(index);
        }
        return null;
    }

    public String getTree(String sentId) {
        int index = getIndex(sentId);
        if (index != -1) {
            return trees.get(index);
        }
        return null;
    }

    public String getDep(String sentId) {
        int index = getIndex(sentId);
        if (index != -1) {
            return deps.get(index);
        }
        return null;
    }

    public void setSentence(String sentId, Sentence s) {
        int index = getIndex(sentId);
        if (index != -1) {
            text.set(index, s);
        }
    }

    public void setTree(String sentId, String tree) {
        int index = getIndex(sentId);
        if (index != -1) {
            trees.set(index, tree);
        }
    }

    public void setDep(String sentId, String dep) {
        int index = getIndex(sentId);
        if (index != -1) {
            deps.set(index, dep);
        }
    }

    public void write() throws IOException {
        File corpusDir = new File(corpusRoot);
        File iobDir = new File(corpusDir, "iob");
        writeIOBFile(new File(iobDir, docId + ".txt"));

        File treeDir = new File(corpusDir, "trees");
        writeListToFile(trees, new File(treeDir, docId + ".mrg"));

        File depsDir = new File(corpusDir, "depsCC");
        writeListToFile(deps, new File(depsDir, docId + ".dep"));
    }

    private void writeIOBFile(File iobFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(iobFile));
        for (Sentence s : text) {
            for (TokWord word : s) {
                writer.write(word.word() + "\t");
                writer.write(word.getTag("P1") + "\t");
                writer.write(word.getTag("P2") + "\t");
                writer.write(word.getTag("N1") + "\t");
                writer.write(word.getTag("N2") + "\t");
                writer.write(word.getTag("BASE") + "\n");
            }
            writer.write("\n");
        }
        writer.close();
    }

    private void writeListToFile(List<String> list, File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String line : list) {
            writer.write(line + "\n");
        }
        writer.close();
    }
}