/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.gui;

import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.io.TextReader;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import com.aliasi.io.FileLineReader;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author tonatuni
 */
public class Experiment {
    static final TokenizerFactory TOKENIZER_FACTORY = new RegExTokenizerFactory("(\\||\\+|-|'|\\d|\\p{L})+|\\S");
    public static void main(String args[]) throws Exception {
        //mergeCONLLTask1AndTask2();
        //isolateMergedCONLLTask1AndTask2();
        prepareSententialArgumentDataset();
        //getConnArgStat();
        /*Scanner in = new Scanner(System.in);
        String line;
        Pattern pat = Pattern.compile("[().,!?;]");
        while ((line = in.nextLine()) != null) {
            String s[] = line.split(":");
            System.out.println(s.length);
        }*/
    }

    public static void isolateMergedCONLLTask1AndTask2() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(".\\resource\\ml\\data\\discourse\\biodrb_clause_ann_manual_edit.txt"));
        FileWriter writer1 = new FileWriter(".\\resource\\ml\\data\\discourse\\biodrb_clause_ann_manual_s.txt");
        FileWriter writer2 = new FileWriter(".\\resource\\ml\\data\\discourse\\biodrb_clause_ann_manual_e.txt");
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.equals("")) {
                String tokens[] = line.split("\\s+");
                for (int i = 0; i < 3; i++) {
                    writer1.write(tokens[i]);
                    writer2.write(tokens[i]);

                    writer1.write(" ");
                    writer2.write(" ");
                }
                writer1.write(tokens[3]);
                writer2.write(tokens[4]);
                if (!tokens[3].matches("X|S") || !tokens[4].matches("X|E")) {
                    System.out.println("Problem:" + line);
                }
            }
            writer1.write("\n");
            writer2.write("\n");
        }
        writer1.close();
        writer2.close();
        reader.close();
    }
    public static void mergeCONLLTask1AndTask2() throws IOException {
        BufferedReader reader1 = new BufferedReader(new FileReader(".\\resource\\ml\\data\\clause_data\\train1"));
        BufferedReader reader2 = new BufferedReader(new FileReader(".\\resource\\ml\\data\\clause_data\\train2"));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\clause_data\\merge_train12.txt");
        String line;
        while ((line = reader1.readLine()) != null) {
            writer.write(line);
            line = reader2.readLine();
            if (line.equals("")) {
                writer.write("\n");
                continue;
            }
            
            String tokens[] = line.split("\\s+");
            writer.write(" " + tokens[3] + "\n");
        }
        writer.close();
        reader1.close();
        reader2.close();
    }

    /**
     * I needed to manually remove a line occurring::activation from connArg.txt. otherwise there is a lot of mismatch.
     * @throws IOException
     */
    public static void prepareSententialArgumentDataset() throws IOException, Exception {
        BufferedReader connArgReader = new BufferedReader(new FileReader("C:\\Users\\tonatuni\\Documents\\NetBeansProjects\\BioDRB\\connArg.txt"));
        BufferedReader sentReader = new BufferedReader(new FileReader("C:\\Users\\tonatuni\\Documents\\NetBeansProjects\\BioDRB\\sentential.txt"));
        FileWriter writer = new FileWriter(".\\resource\\ml\\data\\discourse\\sentConnArg12.txt");
        
        ArrayList<String[]> arg1s = new ArrayList<String[]>();
        ArrayList<String[]> arg2s = new ArrayList<String[]>();
        String line;
        while ((line = connArgReader.readLine()) != null) {
            String tokens[] = line.split("::");
            Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(tokens[0].toCharArray(), 0, tokens[0].length());
            arg1s.add(tokenizer.tokenize());
            tokenizer = TOKENIZER_FACTORY.tokenizer(tokens[1].toCharArray(), 0, tokens[1].length());
            arg2s.add(tokenizer.tokenize());
        }
        connArgReader.close();

        int index = 0;
        int count = 0;
        while ((line = sentReader.readLine()) != null) {
            //System.out.println(line);
            String tokens[] = line.split("::");
            int sz = tokens.length;
            Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(tokens[sz-1].toCharArray(), 0, tokens[sz-1].length());
            String[] words = tokenizer.tokenize();
            int result[] = new int[2];
            
            for (int i = 0; i < sz - 1; i++) {
                String conn = tokens[i];
                int flg1S[] = new int[words.length];
                int flg1E[] = new int[words.length];
                int flg2S[] = new int[words.length];
                int flg2E[] = new int[words.length];
                boolean found1 = findMatch(words, arg1s.get(index), result);
                print(words);
                print(arg1s.get(index));
                System.out.println(found1);
                
                flg1S[result[0]] = 1;
                flg1E[result[1]] = 1;
                
                boolean found2 = findMatch(words, arg2s.get(index), result);
                print(words);
                print(arg2s.get(index));
                System.out.println(found2);
                flg2S[result[0]] = 1;
                flg2E[result[1]] = 1;
                
                index++;

                if (!found1 || !found2) continue;
                
                for (int j = 0; j < words.length; j++) {
                    
                    if (words[j].contains("|")) {
                        String word = words[j].replaceAll("\\|\\S+", "");
                        writer.write(word);
                        if (words[j].equals(conn)) {
                            writer.write("\t1");
                            writer.write("\t" + flg1S[j]);
                            writer.write("\t" + flg1E[j]);
                            writer.write("\t" + flg2S[j]);
                            writer.write("\t" + flg2E[j]);
                        } else {
                            writer.write("\t0");
                            writer.write("\t" + flg1S[j]);
                            writer.write("\t" + flg1E[j]);
                            writer.write("\t" + flg2S[j]);
                            writer.write("\t" + flg2E[j]);
                        }
                    } else {
                        writer.write(words[j]);
                        writer.write("\t0");
                        writer.write("\t"+flg1S[j]);
                        writer.write("\t"+flg1E[j]);
                        writer.write("\t"+flg2S[j]);
                        writer.write("\t"+flg2E[j]);
                    }
                    if (flg1S[j] == 1) count++;
                    writer.write("\n");
                }
                writer.write("\n");
                
            }            
        }
        System.out.println("count: " + count);
        writer.close();
    }

    public static boolean findMatch(String[] line, String[] arg, int result[]) {
        for (int i = 0; i <= line.length - arg.length; i++) {
            boolean flg = true;
            for (int j = 0; j < arg.length; j++) {
                String word = line[i + j].replaceAll("\\|\\S+", "");
                if (!word.equals(arg[j])) {
                    flg = false;
                    break;
                }
            }
            if (flg) {
                result[0] = i;
                result[1] = i + arg.length - 1;
                return true;
            }
        }
        return false;
    }
    public static void print(String t[]) {
        for (int i = 0; i < t.length; i++) {
            System.out.print(t[i] + " ");
        }
        System.out.println("");
    }

    
}
