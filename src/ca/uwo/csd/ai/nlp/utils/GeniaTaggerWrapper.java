/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.utils;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A wrapper class for Genia Tagger 3.0.1
 * @author Syeed Ibn Faiz
 */
public class GeniaTaggerWrapper {

    private String path2GeniaTagger;
    boolean tokenize;
    private Process p;
    boolean initialized;
    BufferedReader reader;
    BufferedWriter writer;
    boolean ascii = true;
    public GeniaTaggerWrapper() {
        //this(".\\resource\\genia\\geniatagger_win.exe", false); //very slow
        this("./resource/genia/geniatagger.exe", false);
        //this("/csd/thesis/mibnfaiz/public_html/nlp"+File.separator+"resource"+File.separator+"genia"+File.separator+"geniatagger", false);
        //this("."+File.separator+"resource"+File.separator+"genia"+File.separator+"geniatagger", false);
        //this("/home/mibnfaiz/UWO/thesis/Netbeans Project/NLP Lib/resource/genia/geniatagger", false);
    }

    /**
     * Create a new <code>GeniaTaggerWrapper</code> object.
     * @param path2GeniaTagger
     * @param tokenize  Enable/Disable -nt option of genia tagger
     */
    public GeniaTaggerWrapper(String path2GeniaTagger, boolean tokenize) {
        this.path2GeniaTagger = path2GeniaTagger;
        this.tokenize = tokenize;
        initialized = init();
    }

    /**
     * Run genia tagger and open input & output stream.
     * @return <code>true</code> if successful
     */
    private boolean init() {        
        String workingDir = "./";
        if (path2GeniaTagger.contains("/")) {
            int where = path2GeniaTagger.lastIndexOf("/");
            workingDir = path2GeniaTagger.substring(0, where);
        }
        try {
            File file = new File(workingDir);
            if (!file.exists()) System.out.println(workingDir + " does not exist.");
            file = new File(path2GeniaTagger);
            if (!file.exists()) System.out.println(path2GeniaTagger + " does not exist.");
            
            if (tokenize) p = Runtime.getRuntime().exec(path2GeniaTagger, null, new File(workingDir));
            else p = Runtime.getRuntime().exec(new String[]{path2GeniaTagger, "-nt"}, null, new File(workingDir));
            
            reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "US-ASCII"));
            writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "US-ASCII"));
            
            System.out.println("Genia tagger is now loading...");
            Thread.sleep(15000);
            System.out.println("Loading done.");
            
        } catch (Exception ex) {
            System.err.println(ex);
            return false;
        }
        return true;
    }

    public String[][] doTagging(String tokens[]) {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(tokens));
        return doTagging(list);
    }

    /**
     * Retrieves tags from genia tagger
     * @param tokens The list of tokens to be tagged
     * @return <code>tokens.size()</code> x 5 matrix
     */
    public String[][] doTagging(ArrayList<String> tokens) {
        if (!initialized) {
            System.err.println("Not initialized");
            return null;
        }
        try {
            String input = "";
            for (int i = 0; i < tokens.size(); i++) {
                if (i != 0) input += " ";
                input += tokens.get(i);
            }
            input += "\n";

            if (input.length() >= 1024) throw new IllegalArgumentException("Input is too large for genia tagger.");
            if (ascii) input = convert2ASCII(input);
            
            writer.write(input);
            writer.flush();
            
            String output[][] = new String[tokens.size()][];
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) break;
                output[i++] = line.split("\\s+");
            }

            return output;
        } catch (IOException ex) {
            System.out.println(ex);
            return null;
        }
    }

    /**
     * Replace non-ASCII characters with -
     * @param s The string to be converted to ASCII
     * @return Converted string
     */
    private String convert2ASCII(String s) {
        String tmp = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 128) tmp += '-';
            else tmp += s.charAt(i);
        }
        return tmp;
    }

    public static void main(String args[]) {
        GeniaTaggerWrapper server = new GeniaTaggerWrapper();
        SimpleSentReader sentReader = new SimpleSentReader();
        Scanner in = new Scanner(System.in);
        String line;

        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);            
            String tokens[] = new String[s.size()];// = line.split("\\s+");
            tokens = s.getWords().toArray(tokens);
            String output[][] = server.doTagging(tokens);
            for (String row[] : output) {
                for (String column : row) {
                    System.out.print(column + " ");
                }
                System.out.println("");
            }
        }
    }
}
