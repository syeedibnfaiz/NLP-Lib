/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.ann.CharniakParser;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import java.awt.Desktop;
import java.io.File;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author tonatuni
 */
public class DependencyViewer {
    ParserAnnotator parser = new ParserAnnotator();
    //CharniakParser parser = new CharniakParser();
    
    public void showDependencyTree(Sentence s, String type) {
        Tree t = s.getParseTree();
        EnglishGrammaticalStructure egs = new EnglishGrammaticalStructure(t);
        Collection<TypedDependency> tld = null;
        
        if (type == null) type = "";
                
        if (type.equalsIgnoreCase("CCProcessed")) {
            tld = egs.typedDependenciesCCprocessed();
        } else if (type.equalsIgnoreCase("collapsed")) {
            tld = egs.typedDependenciesCollapsed();
        } else if (type.equalsIgnoreCase("collapsedTree")) {
            tld = egs.typedDependenciesCollapsedTree();
        } else {
            tld = egs.typedDependencies();
        }
        
        try {
            //String path = System.getProperty("java.io.tmpdir") +"tmp.png";
            String path = "tmp.png";
            com.chaoticity.dependensee.Main.writeImage(t, tld, path, 2);            
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }
        
    public void showDependencyTree(String sent, String type) {
        Sentence s = new Sentence(sent.split("\\s+"));
        s = parser.annotate(s);
        showDependencyTree(s, type);
    }
    public static void main(String args[]) {
        String line;
        Scanner in = new Scanner(System.in);
        DependencyViewer viewer = new DependencyViewer();
        System.out.print("Enter a line: ");
        while ((line = in.nextLine()) != null) {
            viewer.showDependencyTree(line, "CCProcessed");
            System.out.print("Enter another line: ");
        }
    }
}
