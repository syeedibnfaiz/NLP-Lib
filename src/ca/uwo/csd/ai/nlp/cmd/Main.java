/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.cmd;

import ca.uwo.csd.ai.nlp.io.LPSentReader;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.ClauseBoundaryAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.DiscourseMarkerAnnotator;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.ann.SerialAnnotator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Main {

    public static void main(String args[]) throws IOException {
        SerialAnnotator annotator = new SerialAnnotator();
        annotator.add(new GeniaTagger());
        annotator.add(new ClauseBoundaryAnnotator(false));
        annotator.add(new ClauseAnnotator());
        annotator.add(new DiscourseMarkerAnnotator(false));
        LPSentReader sentReader = new LPSentReader();
        ServerSocket serverSocket = new ServerSocket(5727);

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Local: " + socket.getLocalSocketAddress());
                System.out.println("Remote: " + socket.getRemoteSocketAddress());

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                String query = "";
                query = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    if (line.equals("")) {
                        break;
                    }
                }
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                System.out.println(query);
                if (query != null) {
                    String[] tokens = query.split("\\s+");
                    if (tokens.length > 1) {
                        query = tokens[1];
                    }
                }
                if (query != null && query.startsWith("/?q=")) {
                    query = query.substring(4);
                    query = URLDecoder.decode(query, "UTF-8");
                    Sentence s = sentReader.read(query);
                    if (s.length() < 1024) {
                        s = annotator.annotate(s);
                    }
                    System.out.println(s.toString(annotator.getFieldNames()));
                    writer.write("HTTP/1.0 200 OK\nContent-Type: text/html\nConnection: Close\n\n");
                    writer.write("<html><body><br/><br/><center><h2>Annotations</h2><br/><br/>");
                    writer.write(s.toHTML(annotator.getFieldNames()));
                    writer.write("<br/><a href=\"javascript: history.go(-1)\">Back</a></center></body></html>");
                } else {
                    writer.write("HTTP/1.0 200 OK\nContent-Type: text/html\nConnection: Close\n\n");
                    writer.write("<html><body><br/><br/><center><h2>Incorrect Input</h2><br/><br/>");
                    writer.write("<br/><a href=\"javascript: history.go(-1)\">Back</a></center></body></html>");
                }

                System.out.println(query);
                writer.flush();
                writer.close();
                reader.close();
            } catch (Exception ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
        }
    }
}
