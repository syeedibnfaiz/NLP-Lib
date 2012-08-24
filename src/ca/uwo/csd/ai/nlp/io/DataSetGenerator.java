/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.CharniakClient;

import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import com.aliasi.io.FileLineReader;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DataSetGenerator {


    public static void main(String args[]) throws IOException {
        //generateClauseCandidate();
        //getConnArgStat();
        //generateBioDRBConnCONLLFormat();
        //testClauseWithSyntaxTree();
        //parseBioDRB();
        //checkBioDRBParsedText();
        //writeBioDRBSentRel();
        testClauseWithSyntaxTree2();
    }

    /**
     * Generate clause candidate dataset from CONLL2001 train3
     */
    public static void generateClauseCandidate() throws IOException {

        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", " ", new String[]{"Word", "POS", "CHUNK", "CLS_ANN"});
        Text text = textReader.read(new File(".\\resource\\ml\\data\\clause_data\\testb3"));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\clause_data\\testb3_cls_cnddt");
        for (Sentence s : text) {
            int startCount = 1;
            int endCount = 1;
            Stack<Integer> stack = new Stack<Integer>();
            for (TokWord word : s) {
                writer.write(word.word() + " " + word.getTag("POS") + " " + word.getTag("CHUNK") + " ");
                //System.out.println(word.word() + " " + word.getTag("POS") + " " + word.getTag("CHUNK") + " ");
                String clsAnn = word.getTag("CLS_ANN");
                if (clsAnn.equals("*")) {
                    writer.write("0 0");
                }
                else {                    
                    String sTag = "";
                    String eTag = "";
                    for (char ch : clsAnn.toCharArray()) {
                        if (ch == '(') {                            
                            sTag += startCount + ":";
                            stack.push(startCount);
                            startCount++;                  
                        }
                        else if(ch == ')') {
                            if (!stack.empty())eTag += stack.pop() + ":";
                        }
                    }
                    if (!sTag.equals("")) {
                        word.setTag("CLS_S#", sTag);
                    }
                    else {                        
                        word.setTag("CLS_S#", "0");
                    }
                    if (!eTag.equals("")) {
                        word.setTag("CLS_E#", eTag);
                    }
                    else {                        
                        word.setTag("CLS_E#", "0");
                    }
                    writer.write(word.getTag("CLS_S#") + " " + word.getTag("CLS_E#"));
                }
                writer.write("\n");
            }//end for tokWord
            //s.markAnnotation(new String[]{"CLS_BN_S", "CLS_BN_E", "CLS_S#", "CLS_E#"});
            writer.write("\n");
        }//end for sentence
        writer.close();
    }

    private static boolean isClause(Sentence s, int i, int j) {
        String sNum = s.get(i).getTag("CLS_S#");
        String eNum = s.get(j).getTag("CLS_E#");
        System.out.println(sNum+" : " + eNum + " " + i + " :" + " : " + j + " : " + s.toString(new String[]{"CLS_ANN","CLS_S#", "CLS_E#"}));
        String sPos[] = sNum.split(":");
        String ePos[] = eNum.split(":");
        for (int m = 0; m < sPos.length; m++) {
            for (int n = 0; n < ePos.length; n++) {
                if (sPos[m].equals(ePos[n])) return true;
            }
        }
        return false;
    }
    
    public static void getConnArgStat() throws IOException {
        SerialAnnotator annotator = new SerialAnnotator();
        annotator.add(new GeniaTagger());
        annotator.add(new DiscourseMarkerAnnotator(false));
        annotator.add(new ClauseBoundaryAnnotator(false));
        annotator.add(new ClauseAnnotator());

        TextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "CONN","ARG1S","ARG1E","ARG2S","ARG2E"});
        Text text = reader.read(new File(".\\resource\\ml\\data\\discourse\\sentConnArg12.txt"));
        System.out.println(text.size());
        
        FileWriter writer = new FileWriter("sentconnarg_stat.txt");
        int countArg1Start = 0;
        int countArg1StartAtClauseS = 0;
        int countArg2End = 0;
        int countArg2EndAtClauseE = 0;
        int arg1Clause = 0;
        int arg2Clause = 0;
        int count = 0;
        int countArg1StartAtChunkStart = 0;
        int countArg1EndAtChunkEnd = 0;
        int countArg2StartAtChunkStart = 0;
        int countArg2EndAtChunkEnd = 0;
        int countArg1AfterArg2 = 0;
        int countArg1AtClauseStartAfterArg2 = 0;
        int arg2SDistance = 0;
        
        for (Sentence s : text) {
            System.out.println("Line: " + ++count);
            if (s.length() >= 1024) continue;
            try {
                s = annotator.annotate(s);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println(s);
                continue;
            }
            //System.out.println(s.toString(new String[]{"ARG1S"}));
            int arg1S = -1;
            int arg2S = -1;
            int arg1E = -1;
            int arg2E = -1;
            int connPos = -1;
            for (int i = 0; i < s.size(); i++) {
                TokWord word = s.get(i);

                boolean print = false;
                if (word.getTag("ARG1S").equals("1")) {
                    countArg1Start++;
                    arg1S = i;
                    if (word.getTag("CHUNK").startsWith("B-")) {
                        countArg1StartAtChunkStart++;
                    }
                    if (word.getTag("CLS_BN_S").equals("S")) {
                        countArg1StartAtClauseS++;
                    }
                    if (arg2S != -1) {
                        countArg1AfterArg2++;
                        if (word.getTag("CLS_BN_S").equals("S")) {
                            countArg1AtClauseStartAfterArg2++;
                        }
                    }
                    print = true;
                }
                if (word.getTag("ARG1E").equals("1")) {
                    arg1E = i;
                    if (i == (s.size() - 1) || (!s.getTags("CHUNK").equals("O") && s.get(i + 1).getTag("CHUNK").matches("O|B.*"))) {
                        countArg1EndAtChunkEnd++;
                    }
                    print = true;
                }
                if (word.getTag("ARG2S").equals("1")) {
                    arg2S = i;
                    if (word.getTag("CHUNK").startsWith("B-")) {
                        countArg2StartAtChunkStart++;
                    }
                    print = true;
                    if (connPos != -1) {
                        arg2SDistance += (i - connPos);
                    }
                }
                if (word.getTag("ARG2E").equals("1")) {
                    countArg2End++;
                    arg2E = i;
                    if (i == (s.size() - 1) || (!s.getTags("CHUNK").equals("O") && s.get(i + 1).getTag("CHUNK").matches("O|B.*"))) {
                        countArg2EndAtChunkEnd++;
                    }
                    if (word.getTag("CLS_BN_E").equals("E")) {
                        countArg2EndAtClauseE++;
                    }
                    print = true;
                }
                if (word.getTag("CONN").equals("1")) {
                    print = true;
                    connPos = i;
                }
                
                if (print) {
                    writer.write(word.toString(new String[]{"CONN","ARG1S","ARG1E","ARG2S","ARG2E", "POS", "CHUNK", "CLS_BN_S", "CLS_BN_E", "CLS_ANN", "DIS_CON"}, "\t"));
                    writer.write("\n");
                }
            }

            if (isClause(s, arg1S, arg1E)) {
                arg1Clause++;
            }
            if (isClause(s, arg2S, arg2E)) {
                arg2Clause++;
            }
        }
        writer.close();
        System.out.println("# arg1Start: " + countArg1Start);
        System.out.println("# arg1Start@clauseS: " + countArg1StartAtClauseS);
        System.out.println("ratio: " + 1.0*countArg1StartAtClauseS/countArg1Start);

        System.out.println("# arg2End: " + countArg2End);
        System.out.println("# arg2End@clauseE: " + countArg2EndAtClauseE);
        System.out.println("ratio: " + 1.0*countArg2EndAtClauseE/countArg2End);

        System.out.println("arg1Clause: " + arg1Clause);
        System.out.println("Ratio: " + 1.0*arg1Clause/text.size());
        System.out.println("arg2Clause: " + arg2Clause);
        System.out.println("Ratio: " + 1.0*arg2Clause/text.size());

        System.out.println("Arg1StartAtChunkStart: " + countArg1StartAtChunkStart);
        System.out.println("Ratio: " + 1.0*countArg1StartAtChunkStart/text.size());
        System.out.println("Arg1EndAtChunkEnd: " + countArg1EndAtChunkEnd);
        System.out.println("Ratio: " + 1.0*countArg1EndAtChunkEnd/text.size());

        System.out.println("Arg2StartAtChunkStart: " + countArg2StartAtChunkStart);
        System.out.println("Ratio: " + 1.0*countArg2StartAtChunkStart/text.size());
        System.out.println("Arg2EndAtChunkEnd: " + countArg2EndAtChunkEnd);
        System.out.println("Ratio: " + 1.0*countArg2EndAtChunkEnd/text.size());

        System.out.println("Arg1 After Arg2: " + countArg1AfterArg2);
        System.out.println("Arg1 at clause start after Arg2: " + countArg1AtClauseStartAfterArg2);
        System.out.println("Ratio: " + 1.0*countArg1AtClauseStartAfterArg2/countArg1AfterArg2);

        System.out.println("arg2 start distance from connective: " + arg2SDistance*1.0/text.size());
    }

    public static void generateBioDRBConnCONLLFormat() throws IOException {
        //LPTextReader textReader = new LPTextReader("(\\||\\+|-|'|\\d|\\p{L})+|\\S");
        GenericTextReader textReader = new GenericTextReader("\n", "\\s+", null, null);
        Text text = textReader.read(new File(".\\resource\\ml\\data\\discourse\\biodrb_conn_edited.txt"));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\discourse\\biodrb_conn_2.txt");
        GeniaTagger tagger = new GeniaTagger();
        for (Sentence sentence : text) {
            for (TokWord tokWord : sentence) {                
                String s = tokWord.word();
                int where = s.lastIndexOf('|');
                String word;
                String tag;
                if (where == -1 || where == 0 || where == (s.length() - 1)) {
                    word = s;
                    tag = "O";
                } else {
                    word = s.substring(0, where);
                    tag = s.substring(where + 1);
                }

                tokWord.setWord(word);
                tokWord.setTag("CONN", tag);
            }
            if (sentence.length() >= 1024) continue;
            sentence = tagger.annotate(sentence);
            for (TokWord word : sentence) {
                writer.write(word.word() + "\t");
                writer.write(word.getTag("POS") + "\t");
                writer.write(word.getTag("CHUNK") + "\t");
                writer.write(word.getTag("CONN"));
                writer.write("\n");
            }
            writer.write("\n");
        }
        writer.close();
    }

    public static void testClauseWithSyntaxTree() throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[] {"Word","POS","CHUNK","CLS_ANN_ORG"});
        Text text = textReader.read(new File(".\\resource\\ml\\data\\clause_data\\testb3"));
        ParserAnnotator annotator = new ParserAnnotator();
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\clause_data\\testb3_syntax");
        for (Sentence s : text) {
            s = annotator.annotate(s);
            Tree tree = s.getParseTree();
            explore(tree, tree, s, 0, s.size() - 1);
            for (TokWord word : s) {
                if (word.getTag("CLS_ANN") == null) word.setTag("CLS_ANN", "*");
                writer.write(word.getTag("CLS_ANN_ORG") + " " + word.getTag("CLS_ANN") + "\n");

            }
            writer.write("\n");
            writer.flush();
        }
        writer.close();
    }
    public static void explore(Tree root, Tree t, Sentence s, int begin, int end) {
        if (t.isLeaf()) return;
        SyntaxTreeAnalyzer analyzer = new SyntaxTreeAnalyzer();
        List<Tree> children = t.getChildrenAsList();
        int newBegin = begin;
        int newEnd = -1;
        for (Tree child : children) {
            newEnd = newBegin + child.getLeaves().size() - 1;
            explore(root, child, s, newBegin, newEnd);
            newBegin = newEnd + 1;
        }
        if (t.value().startsWith("S") && !t.value().equals("S1")) {            
            boolean skip = false;
            if (s.get(begin).getTag("POS").matches("VBG|TO")) {
                Tree parent = t.parent(root);
                if (parent != null && parent.value().matches("VP")) {                    
                    //Tree ancestor = parent.parent(root);
                    
                    /*for (int i = -1; ; i--) {
                        Tree leftSibling = analyzer.getSibling(root, t, i);
                        if (leftSibling == null) {
                            break;
                        }
                        System.out.println("Sibling at: " + i + ", " + leftSibling.value());
                        if (leftSibling.value().startsWith("V")) {
                            System.out.println("We are skipping");
                            skip = true;
                        }
                    }*/
                    Tree leftSibling = analyzer.getSibling(root, t, -1);
                    if (leftSibling.value().matches("VB(Z|P|D)")) {
                            System.out.println("We are skipping");
                            skip = true;
                        }
                                        
                    //System.out.println("skip");
                }
            }
            if (!skip) {
                String tag = s.get(begin).getTag("CLS_ANN");
                if (tag == null) {
                    s.get(begin).setTag("CLS_ANN", "(S*");
                } else {
                    s.get(begin).setTag("CLS_ANN", "(S" + tag);
                }

                tag = s.get(end).getTag("CLS_ANN");
                if (tag == null) {
                    s.get(end).setTag("CLS_ANN", "*S)");
                } else {
                    s.get(end).setTag("CLS_ANN", tag + "S)");
                }
            } else {
                if (!s.get(begin).getTag("CLS_ANN_ORG").equals("*")) {
                    //System.out.println("*******");
                    System.out.println("begin: " + s.get(begin).word() + ", " + s);
                    System.out.println(analyzer.getPennOutput(root));
                    System.out.println("");
                }
            }
        }
    }
    public static void parseBioDRB() throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","POS","CHUNK","CONN"});
        Text text = textReader.read(new File("./resource/ml/data/discourse/biodrb_conn_2.txt"));
        FileWriter writer = new FileWriter("./resource/ml/data/discourse/biodrb_conn_2_parsed.txt", true);
        CharniakClient charniakClient = new CharniakClient();
        int count = 0;
        for (Sentence s : text) {
            ++count;
            if (count <= 2110) continue;
            writer.write(s.toString() + "\n");
            if (s.size() < 400 && s.length() < 800) {
                String parseTree = charniakClient.parse(s.toString());
                writer.write(parseTree + "\n");
            } else {
                writer.write("\n");
            }
            writer.flush();
        }
        writer.close();
    }
    public static void checkBioDRBParsedText() throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word","POS","CHUNK","CONN"});
        Text text = textReader.read(new File("./resource/ml/data/discourse/biodrb_conn_2.txt"));
        BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/discourse/biodrb_conn_2_parsed.txt"));
        String line;
        int i = 0;
        Pattern pat = Pattern.compile("[^\\)]\\)");
        while ((line = reader.readLine()) != null) {
            
            if (!line.equals(text.get(i).toString())) {
                System.out.println("Missmatch at : " + i);                
            }
            
            line = reader.readLine();
            int nTokens = text.get(i).size();
            int mTokens = 0;
            Matcher mat = pat.matcher(line);
            while (mat.find()) {
                mTokens++;
            }
            if (nTokens != mTokens) {
                System.out.println("Problem at: " + i + " - " + nTokens + ", " + mTokens + " : " + line);
            }
            i++;
        }
        reader.close();
    }
    public static void writeBioDRBSentRel() throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[]{"Word","CONN","ARG1S","ARG1E","ARG2S","ARG2E"});
        Text text = textReader.read(new File("./resource/ml/data/discourse/sentConnArg12.txt"));
        FileWriter writer = new FileWriter("./resource/ml/data/discourse/SentConnArg12_modified.txt");
        FileWriter writer2 = new FileWriter("./resource/ml/data/discourse/SentConnArg12_modified_plain.txt");
        for (Sentence s : text) {
            if (s.size() >= 400 || s.length() >= 800) continue;
            int arg2S = -1;
            ArrayList<Integer> connList = new ArrayList<Integer>();
            for (int i = 0; i < s.size(); i++) {
                TokWord word = s.get(i);
                if (!word.getTag("ARG2S").equals("0")) {
                    arg2S = i;
                }
                if (!word.getTag("CONN").equals("0")) {
                    connList.add(i);
                }
            }
            
            TokWord bestConn = null;
            int minDist = 1000;
            for (Integer i : connList) {
                if (Math.abs(arg2S - i) < minDist) {
                    minDist = Math.abs(arg2S - i);
                    bestConn = s.get(i);
                }
                s.get(i).setTag("CONN", "0");
            }
            bestConn.setTag("CONN", "1");
            
            for (TokWord word : s) {
                writer.write(word.word() +"\t"+word.getTag("CONN")+"\t"+word.getTag("ARG1S")+"\t"+word.getTag("ARG1E")+"\t"+word.getTag("ARG2S")+"\t"+word.getTag("ARG2E")+"\n");                
            }
            writer.write("\n");
            writer2.write("<s> "+s.toString()+ " </s>" + "\n");
        }
        writer.close();
        writer2.close();
    }
    
    public static void testClauseWithSyntaxTree2() throws IOException {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\\s+", new String[] {"Word","POS","CHUNK","CLS_ANN_ORG"});
        Text text = textReader.read(new File("./resource/ml/data/clause_data/testb3"));
        BufferedReader reader = new BufferedReader(new FileReader("./resource/ml/data/clause_data/testb3_parsed_bllip"));        
        FileWriter writer = new FileWriter("./resource/ml/data/clause_data/testb3_syntax_bllip");
        TreeFactory tf = new LabeledScoredTreeFactory();
        for (Sentence s : text) {
            String line = reader.readLine();
            //System.out.println(line);
            TreeReader tr = new PennTreeReader(new StringReader(line), tf);
            Tree tree = tr.readTree();
            s.setParseTree(tree);
            explore(tree, tree, s, 0, s.size() - 1);
            for (TokWord word : s) {
                if (word.getTag("CLS_ANN") == null) word.setTag("CLS_ANN", "*");
                writer.write(word.getTag("CLS_ANN_ORG") + " " + word.getTag("CLS_ANN") + "\n");
            }
            writer.write("\n");
            writer.flush();
        }
        writer.close();
        reader.close();
    }
}
