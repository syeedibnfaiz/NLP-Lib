/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;


import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class PTBFileReader {
    
    public List<Tree> read(File file) {
        List<Tree> list = new ArrayList<Tree>();
        TreeFactory tf = new LabeledScoredTreeFactory();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) continue;  //skip blank lines
                line = line.trim();
                StringBuilder sb = new StringBuilder(line);
                int lCount = 0;
                int rCount = 0;
                for (int i = 0; i < sb.length(); i++) {
                    if (sb.charAt(i) == '(') lCount++;
                    else if (sb.charAt(i) == ')') rCount++;
                }
                if (lCount != rCount) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(" ").append(line.trim());
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) == '(') {
                                lCount++;
                            } else if (line.charAt(i) == ')') {
                                rCount++;
                            }
                        }
                        if (lCount == rCount) break;
                    }
                }
                if (lCount != rCount) {
                    System.err.println("lcount != rcount");
                }
                TreeReader tr = new PennTreeReader(new StringReader(sb.toString()), tf);
                //System.out.println(sb.toString());
                Tree t = tr.readTree();
                list.add(t);
            }
        } catch (IOException ex) {
            Logger.getLogger(PTBFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list;
    }
    
    public List<String> readStrings(File file) {
        List<String> trees = new ArrayList<String>();        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) continue;
                line = line.trim();
                StringBuilder sb = new StringBuilder(line);
                int lCount = 0;
                int rCount = 0;
                for (int i = 0; i < sb.length(); i++) {
                    if (sb.charAt(i) == '(') lCount++;
                    else if (sb.charAt(i) == ')') rCount++;
                }
                if (lCount != rCount) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(" ").append(line.trim());
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) == '(') {
                                lCount++;
                            } else if (line.charAt(i) == ')') {
                                rCount++;
                            }
                        }
                        if (lCount == rCount) break;
                    }
                }
                if (lCount != rCount) {
                    System.err.println("lcount != rcount");
                }                
                trees.add(sb.toString());
            }
        } catch (IOException ex) {
            Logger.getLogger(PTBFileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return trees;
    }
    public static void main(String args[]) {
        File file = new File("./pdtb_v2/ptb/00/wsj_0009.mrg");
        PTBFileReader reader = new PTBFileReader();
        List<String> list = reader.readStrings(file);
        for (String s : list) {
            System.out.println(s);
        }
    }
}
