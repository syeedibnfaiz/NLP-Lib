/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import edu.stanford.nlp.parser.charniak.CharniakParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class CharniakServer {
    String executablePath;
    String modelPath;    
    Process process;
    BufferedReader reader;
    PrintWriter writer;

    public CharniakServer() {
        //this("/home/mibnfaiz/UWO/thesis/reranking-parser/first-stage/PARSE/parseIt","/home/mibnfaiz/UWO/thesis/reranking-parser/biomodel/parser/");
        //windows
        //this("G:/UWO/thesis/Software/bllip/first-stage/PARSE/parseIt","G:/UWO/thesis/Software/bllip/first-stage/DATA/EN/");
        this("G:/UWO/thesis/Software/bllip/first-stage/PARSE/parseIt","G:/UWO/thesis/Software/bllip/biomodel/parser/");
    }
    
    
    public CharniakServer(String executablePath, String modelPath) {
        this.executablePath = executablePath;
        this.modelPath = modelPath;            
        runServer();
    }
    
    private boolean runServer() {
        
        try {
            process = Runtime.getRuntime().exec(new String[]{executablePath,modelPath, "-K", "-l399"}, null, null);
            System.out.println("Charniak is now loading...");
            Thread.sleep(7000);
            System.out.println("Loading done.");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
            return true;
        } catch (InterruptedException ex) {
            Logger.getLogger(CharniakServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CharniakServer.class.getName()).log(Level.SEVERE, null, ex);            
        }
        process = null;
        return false;
    }
    public String parse(String text) {
        
        if (process == null || writer.checkError()) {
            System.err.println("Restarting charniak..");
            if (!runServer()) {
                System.err.println("Could not run charniak parser..");
                return null;
            }
        }
        text = "<s> " + text + " </s>\n";
        try {
            writer.write(text);
            writer.flush();
            String output = reader.readLine();
            reader.readLine();
            return output;
        } catch (IOException ex) {
            Logger.getLogger(CharniakServer.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Charniak crashed!");
            return null;
        }
        
    }
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        CharniakServer server = new CharniakServer();
        String line;
        while ((line = in.nextLine()) != null) {
            System.out.println(server.parse(line));
        }
    }
}
