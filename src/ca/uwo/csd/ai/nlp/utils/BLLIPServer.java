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
import edu.stanford.nlp.util.ArrayUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mibnfaiz
 */
public class BLLIPServer {
    private String bllipHome;
    private Process parser;
    private Process reranker;
    String firstStageModelPath;
    String secondStageModelPath;
    BufferedReader parserReader;
    BufferedReader rerankerReader;
    PrintWriter parserWriter;
    PrintWriter rerankerWriter;
    boolean running;
    private BufferedReader parserErrorReader;
    private List<String> parserArgumentList;
    private boolean tokenize;
    private final int maxAttempt = 2;
    
    public BLLIPServer(String bllipHome, String firstStageModelPath, String secondStageModelPath, boolean tokenize, int N, int L, int nThread) {
        this.bllipHome = bllipHome;
        this.firstStageModelPath = firstStageModelPath;
        this.secondStageModelPath = secondStageModelPath;
        this.tokenize = tokenize;
        
        //setup argument list for first-stage parser
        this.parserArgumentList = new ArrayList<String>();        
        parserArgumentList.add(bllipHome+"/first-stage/PARSE/parseIt");
        parserArgumentList.add(firstStageModelPath);
        if (!tokenize) parserArgumentList.add("-K");
        parserArgumentList.add("-N"+N);
        parserArgumentList.add("-l"+L);
        parserArgumentList.add("-t"+nThread);
        
        //run server        
        running = runServer();        
    }

    public BLLIPServer(String bllipHome, String firstStageModelPath, String secondStageModelPath) {        
        this(bllipHome, firstStageModelPath, secondStageModelPath, false, 50, 399, 4);
    }
    
    
    
    private boolean runServer() {
        //String parserArgument[] = new String[]{bllipHome+"/first-stage/PARSE/parseIt",firstStageModelPath, "-K", "-N50", "-l399", "-t4"};
        String parserArgument[] = new String[parserArgumentList.size()];
        System.out.println(parserArgumentList);
        parserArgument = parserArgumentList.toArray(parserArgument);
        
        String rerankerArgument[] = new String[]{bllipHome+"/second-stage/programs/features/best-parses", "-l", secondStageModelPath+"/features.gz", secondStageModelPath+"/weights.gz"};
        try {
            parser = Runtime.getRuntime().exec(parserArgument, null, null);
            parserReader = new BufferedReader(new InputStreamReader(parser.getInputStream()));
            parserWriter = new PrintWriter(new OutputStreamWriter(parser.getOutputStream()));
            
            reranker = Runtime.getRuntime().exec(rerankerArgument, null, null);
            rerankerReader = new BufferedReader(new InputStreamReader(reranker.getInputStream()));
            rerankerWriter = new PrintWriter(new OutputStreamWriter(reranker.getOutputStream()));            
            parserErrorReader = new BufferedReader(new InputStreamReader(parser.getErrorStream()));
            
            System.out.println("BLLIP parser is now loading...");
            Thread.sleep(20000);            
            System.out.println("Loading done.");
            return true;
        } catch (InterruptedException ex) {
            Logger.getLogger(BLLIPServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BLLIPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    private void closeServer() {
        parser.destroy();
        reranker.destroy();
    }
    public String parse(String sentence) {
        return parse(sentence, 1);
    }
    public String parse(String sentence, int attempt) {
        if (!running) {
            return null;
        }
        //try {     
            
            String text = "<s> " + sentence + " </s>";
            //probably there is a bug in bllip parser if -K is not given
            //I found this workaround of adding .\n
            if (tokenize) text += "\n.";
            parserWriter.write(text + "\n");
            parserWriter.flush();
            
            //StringBuilder sb = new StringBuilder();
            //String line = "";
            /*while ((line = parserErrorReader.readLine()) != null) {
                System.out.println(line);
            }*/
            //while (!parserReader.ready());
            /*while ((line = parserReader.readLine()) != null) {       
                //System.out.println(line);
                sb.append(line).append("\n");                
                if (line.equals("")) break;
            }*/
            
            
            
            //System.out.println(":"+sb.toString());
            
            /*rerankerWriter.write(sb.toString());
            rerankerWriter.flush();
            
            line = rerankerReader.readLine();             
             */
            //System.out.println(line);
            //rerankerReader.readLine();
            
            String output = "";
            Worker worker = new Worker(parserReader, true, "");
            worker.start();
            try {
                worker.join(60000);
                output = worker.getOutput();
                if (output == null) {
                    //timeout
                    if (attempt >= maxAttempt) {
                        throw new RuntimeException("Could not parse with " + maxAttempt + " attempt: " + sentence);
                    }
                    System.err.println("Timeout for: " + sentence);
                    if (worker.isAlive()) worker.interrupt();
                    closeServer();
                    running = runServer();
                    return parse(sentence, attempt + 1);
                }
            } catch (InterruptedException ex) {                
                worker.interrupt();
                Thread.currentThread().interrupt(); 
                return null;
            }
            
            rerankerWriter.write(output);
            rerankerWriter.flush();
            
            worker = new Worker(rerankerReader);
            worker.start();
            try {
                worker.join(60000);
                output = worker.getOutput();
                if (output == null) {
                    //timeout
                    if (worker.isAlive()) worker.interrupt();
                    closeServer();
                    running = runServer();
                    return parse(sentence, attempt + 1);
                }
            } catch (InterruptedException ex) {                
                worker.interrupt();
                Thread.currentThread().interrupt(); 
                return null;
            }
            
            return output;
        /*} catch (IOException ex) {
            Logger.getLogger(BLLIPServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }*/
    }
    
    public static void main(String args[]) {
        BLLIPServer server = new BLLIPServer("/home/mibnfaiz/UWO/thesis/bllip",
                "/home/mibnfaiz/UWO/thesis/bllip/first-stage/DATA/EN/", 
                "/home/mibnfaiz/UWO/thesis/bllip/second-stage/models/ec50spfinal");
        Scanner in = new Scanner(System.in);
        String line;
        TreeFactory tf = new LabeledScoredTreeFactory();
        while ((line = in.nextLine()) != null) {
            line = server.parse(line);
            System.out.println("output:"+line);
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

/**
 * An <code>Worker</code> object reads lines from a <code>BufferedReader</code>.
 * The purpose of this class is to impose a timeout on the readline method.
 * @author mibnfaiz
 */
class Worker extends Thread {
    BufferedReader reader;
    boolean multiLine;
    String terminator;
    String output;
    
    public Worker(BufferedReader reader) {
        this(reader, false, null);
    }

    public Worker(BufferedReader reader, boolean multiLine, String terminator) {
        this.reader = reader;
        this.multiLine = multiLine;
        this.terminator = terminator;
    }
    
    
    @Override
    public void run () {        
        try {
            if (!multiLine) {
                output = reader.readLine();
            } else {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (line.equals(terminator)) break;
                }
                output = sb.toString();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getOutput() {
        return output;
    }
    
}