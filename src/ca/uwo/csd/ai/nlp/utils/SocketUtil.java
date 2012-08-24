/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SocketUtil {
    Socket socket;
    String hostName;
    int port;
    BufferedReader reader;
    BufferedWriter writer;
    
    public SocketUtil(String hostName, int port) throws IOException {        
        this.hostName = hostName;
        this.port = port;
        socket = new Socket(hostName, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }    
    
    public String readline() throws IOException {
        return reader.readLine();
    }
    
    public void sendLine(String msg) throws IOException {
        writer.write(msg);
        writer.flush();
    }
    
    public void closeConnection() throws IOException {
        writer.close();
        reader.close();
        socket.close();
    }
    
    public static void main(String[] args) throws IOException {
        SocketUtil socket = new SocketUtil("78.129.218.202", 4449);
        socket.sendLine("This is a test .\n");
        System.out.println(socket.readline());
        socket.closeConnection();
    }
}
