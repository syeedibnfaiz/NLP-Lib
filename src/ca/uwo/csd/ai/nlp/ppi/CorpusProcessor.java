package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.utils.DBUtil;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CorpusProcessor {

    static final SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    static final TreebankLanguagePack TLP = new PennTreebankLanguagePack();
    static final GrammaticalStructureFactory GSF = TLP.grammaticalStructureFactory();
    DBUtil dbUtil;

    public CorpusProcessor() {
        dbUtil = new DBUtil();
    }

    private void annotateSentence(String sentId, Sentence s, String p1, String p2, String interact, int pCount, int nCount) {
        int index1 = -1;
        int index2 = -1;
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (word.word().contains(p1)) { //TODO:error: PROTEIN19 contains PROTEIN1!!! FIXED.
                //index1 = i;
                if (word.word().equals(p1)) {
                    index1 = i;
                } else {
                    String token = word.word();
                    int startPos = token.indexOf(p1);
                    int next = startPos + p1.length();
                    if (next == token.length()) {
                        index1 = i;
                    } else {
                        char nextChar = token.charAt(next);
                        if (!Character.isDigit(nextChar)) {
                            index1 = i;
                        }
                    }
                }
            }
            if (word.word().contains(p2)) {
                //index2 = i;
                if (word.word().equals(p2)) {
                    index2 = i;
                } else {
                    String token = word.word();
                    int startPos = token.indexOf(p2);
                    int next = startPos + p2.length();
                    if (next == token.length()) {
                        index2 = i;
                    } else {
                        char nextChar = token.charAt(next);
                        if (!Character.isDigit(nextChar)) {
                            index2 = i;
                        }
                    }
                }
            }
        }
        if (index1 != -1 && index2 != -1) {
            String tag1 = (interact.equals("0") ? "N1" : "P1");
            String tag2 = (interact.equals("0") ? "N2" : "P2");
            int count = (interact.equals("0") ? nCount : pCount);
            if (index1 > index2) {
                int tmp = index1;
                index1 = index2;
                index2 = tmp;
            }
            if (s.get(index1).getTag(tag1) == null) {
                s.get(index1).setTag(tag1, String.valueOf(count));
            } else {
                s.get(index1).setTag(tag1, s.get(index1).getTag(tag1) + ", " + String.valueOf(count));
            }

            if (s.get(index2).getTag(tag2) == null) {
                s.get(index2).setTag(tag2, String.valueOf(count));
            } else {
                s.get(index2).setTag(tag2, s.get(index2).getTag(tag2) + ", " + String.valueOf(count));
            }
        } else {
            //debug
            /*if (index1 == -1 && index2 == -1) {
                System.out.println("Misses both");
            } else {
                System.out.println("index == -1");
                System.out.println("sid: " + sentId);
                System.out.println("sent: " + s);
                System.out.println("p1: " + p1);
                System.out.println("p2: " + p2);
            }*/
        }
    }

    private void writeIOB(String docId, File iobDir, Sentence s) {
        try {
            FileWriter writer = new FileWriter(new File(iobDir, docId + ".txt"), true);
            for (TokWord word : s) {
                writer.write(word.word());
                String tags[] = {"P1", "P2", "N1", "N2"};
                for (String tag : tags) {
                    String value = word.getTag(tag);
                    if (value == null) {
                        value = "O";
                    }
                    writer.write("\t" + value);
                }
                writer.write("\n");
            }
            writer.write("\n");
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeTrees(String docId, File treesDir, Sentence s) {
        try {
            FileWriter writer = new FileWriter(new File(treesDir, docId + ".mrg"), true);
            Tree root = s.getParseTree();
            writer.write(root.pennString());
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeDeps(String docId, File depsDir, Sentence s, boolean cc) {
        FileWriter writer = null;
        try {
            Tree root = s.getParseTree();
            writer = new FileWriter(new File(depsDir, docId + ".dep"), true);
            GrammaticalStructure gs = GSF.newGrammaticalStructure(root);
            Collection<TypedDependency> dependencies = null;
            if (!cc) {
                dependencies = gs.typedDependencies();
            } else {
                dependencies = gs.typedDependenciesCCprocessed();
            }
            boolean first = true;
            for (TypedDependency td : dependencies) {
                if (!first) {
                    writer.write("\t");
                } else {
                    first = false;
                }
                writer.write(td.toString());
            }
            writer.write("\n");
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void write(String sentId, Sentence s, File iobDir, File treesDir, File depsDir, File depsCCDir) {
        String docId = sentId;
        docId = docId.substring(0, docId.lastIndexOf('.')).replace(".", "_");
        writeIOB(docId, iobDir, s);
        //writeTrees(docId, treesDir, s);
        //writeDeps(docId, depsDir, s, false);
        //writeDeps(docId, depsCCDir, s, true);
    }

    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                recursiveDelete(childFile);
            }
        }
        System.out.println("Deleting: " + file.getName());
        file.delete();
    }

    public void prepare(String dbName, String corpusPrefix, String outputDirPath) {

        if (dbUtil.connect(dbName)) {
            File outputDir = new File(outputDirPath);
            if (outputDir.exists()) {
                recursiveDelete(outputDir);
            }
            outputDir = new File(outputDirPath);
            outputDir.mkdir();

            File iobDir = new File(outputDir, "iob");
            iobDir.mkdir();
            File treesDir = new File(outputDir, "trees");
            treesDir.mkdir();
            File depsDir = new File(outputDir, "deps");
            depsDir.mkdir();
            File depsCCDir = new File(outputDir, "depsCC");
            depsCCDir.mkdir();
            File infoDir = new File(outputDir, "info");
            infoDir.mkdir();

            String sentQuery = "select id from sendata where id like \"" + corpusPrefix + "%\"";
            ResultSet rs = dbUtil.execQuery(sentQuery);
            List<String> sentIds = new ArrayList<String>();
            try {
                while (rs.next()) {
                    sentIds.add(rs.getString(1));
                }
            } catch (SQLException ex) {
                System.out.println(ex);
            }

            String treeQuery = "select tree from trees where id=\"";
            Statement stmt2 = dbUtil.createStatement();
            ResultSet rs2 = null;
            for (String sentId : sentIds) {
                rs = dbUtil.execQuery(treeQuery + sentId + "\"");
                Tree root = null;
                Sentence s = null;
                try {
                    while (rs.next()) {
                        String treeStr = rs.getString(1);
                        root = TREE_ANALYZER.getPennTree(treeStr);
                        s = new Sentence(root);
                        String interactionQuery = "select P1, P2, INTERACT, id from interaction where id like\"" + sentId + "%\"";
                        rs2 = dbUtil.execQuery(stmt2, interactionQuery);
                        int pCount = 0;
                        int nCount = 0;
                        List<String> infoList = new ArrayList<String>(); //stores information about each pair
                        try {
                            while (rs2.next()) {
                                String p1 = rs2.getString(1);
                                String p2 = rs2.getString(2);
                                String interact = rs2.getString(3);
                                String id = rs2.getString(4);
                                if (interact.equals("1")) {
                                    pCount++;
                                } else {
                                    nCount++;
                                }
                                annotateSentence(sentId, s, p1, p2, interact, pCount, nCount);
                                addPairInformation(p1, p2, interact, id, infoList);
                            }
                        } catch (SQLException ex) {
                            System.out.println(ex);
                        }
                        write(sentId, s, iobDir, treesDir, depsDir, depsCCDir);
                        writeInfo(infoDir, sentId, infoList);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void addPairInformation(String p1, String p2, String interaction, String id, List<String> infoList) {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append("\t");
        sb.append(p1);
        sb.append("\t");
        sb.append(p2);
        sb.append("\t");
        sb.append(interaction);
        infoList.add(sb.toString());
    }

    private void writeInfo(File infoDir, String sentId, List<String> infoList) {
        String docId = sentId;
        docId = docId.substring(0, docId.lastIndexOf('.')).replace(".", "_");
        try {
            FileWriter writer = new FileWriter(new File(infoDir, docId + ".txt"), true);
            writer.write(sentId + "\t" + infoList.size() + "\n");
            for (String info : infoList) {
                writer.write(info + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(CorpusProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void checkIntegrity(String rootPath) {
        File iobDir = new File(rootPath, "iob");
        File[] files = iobDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
        int totalP1 = 0;
        int totalN1 = 0;
        int sCount = 0;
        for (File file : files) {
            Text text = reader.read(file);
            for (Sentence s : text) {
                sCount++;
                int p1Count = 0;
                int p2Count = 0;
                int n1Count = 0;
                int n2Count = 0;
                for (TokWord word : s) {
                    String p1Val = word.getTag("P1");
                    if (!p1Val.equals("O")) {
                        p1Count += p1Val.split(", ").length;
                    }
                    String p2Val = word.getTag("P2");
                    if (!p2Val.equals("O")) {
                        p2Count += p2Val.split(", ").length;
                    }
                    String n1Val = word.getTag("N1");
                    if (!n1Val.equals("O")) {
                        n1Count += n1Val.split(", ").length;
                    }
                    String n2Val = word.getTag("N2");
                    if (!n2Val.equals("O")) {
                        n2Count += n2Val.split(", ").length;
                    }
                }
                totalP1 += p1Count;
                totalN1 += n1Count;
                if (p1Count != p2Count || n1Count != n2Count) {
                    System.out.println("count misMatch!");
                }
            }
        }
        System.out.println("total s: " + sCount);
        System.out.println("total p1: " + totalP1);
        System.out.println("total n1: " + totalN1);
    }

    public static void main(String args[]) {
        CorpusProcessor processor = new CorpusProcessor();
        System.out.println("--AImed--");
        processor.prepare("ppi", "AI", "./resource/relation/PPI5/AIMed");
        //processor.checkIntegrity("./resource/relation/PPI3/AIMed");

        System.out.println("--BioInfer--");
        processor.prepare("ppi", "Bio", "./resource/relation/PPI5/BioInfer");
        //processor.checkIntegrity("./resource/relation/PPI/BioInfer");

        System.out.println("--LLL--");
        processor.prepare("ppi", "L", "./resource/relation/PPI5/LLL");
        //processor.checkIntegrity("./resource/relation/PPI/LLL");

        System.out.println("--IEPA--");
        processor.prepare("ppi", "IE", "./resource/relation/PPI5/IEPA");
        //processor.checkIntegrity("./resource/relation/PPI/IEPA");

        System.out.println("--HPRD50--");
        processor.prepare("ppi", "HP", "./resource/relation/PPI5/HPRD50");
        //processor.checkIntegrity("./resource/relation/PPI/HPRD50");
    }
}
