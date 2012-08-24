/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class BLLIPClient {
    /*public static BLLIPServer server = new BLLIPServer("/home/mibnfaiz/UWO/thesis/bllip",
                "/home/mibnfaiz/UWO/thesis/bllip/first-stage/DATA/EN/", 
                "/home/mibnfaiz/UWO/thesis/bllip/second-stage/models/ec50spfinal",
                true, 50, 399, 4);*/
    /*public static BLLIPServer server = new BLLIPServer("/home/mibnfaiz/UWO/thesis/bllip",
                "/home/mibnfaiz/UWO/thesis/bllip/biomodel/parser/", 
                "/home/mibnfaiz/UWO/thesis/bllip/biomodel/reranker",
                false, 50, 399, 4);*/
    public static BLLIPServer server = new BLLIPServer("G:/UWO/thesis/Software/bllip",
                "G:/UWO/thesis/Software/bllip/biomodel/parser/", 
                "G:/UWO/thesis/Software/bllip/biomodel/reranker",
                false, 50, 399, 4);

    public BLLIPClient() {
    }
    
    public String parse(String text) {
        return server.parse(text);
    }
    
    public static void main(String args[]) {
        BLLIPClient client = new BLLIPClient();
        
        Scanner in = new Scanner(System.in);
        String line;
        TreeFactory tf = new LabeledScoredTreeFactory();
        while ((line = in.nextLine()) != null) {
            line = client.parse(line);
            System.out.println("output:" + line);
            TreeReader tr = new PennTreeReader(new StringReader(line), tf);
            try {
                Tree t = tr.readTree();
                t.setValue("");
                System.out.print(t.pennString());
            } catch (IOException ex) {
                Logger.getLogger(BLLIPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }    
}
