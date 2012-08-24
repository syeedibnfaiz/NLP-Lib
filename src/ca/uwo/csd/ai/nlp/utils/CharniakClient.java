/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import edu.stanford.nlp.parser.charniak.CharniakScoredParsesReaderWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class CharniakClient {
    public static CharniakServer charniakServer = new CharniakServer();
        
    public CharniakClient() {        
    }        
    
    public String parse(String text) {
        
        String output = charniakServer.parse(text);
        if (output == null) {   //try again
            output = charniakServer.parse(text);
        }
        return output;
    }
    
    public static void main(String args[]) {
        CharniakClient charniakClient = new CharniakClient();
        Scanner in  =new Scanner(System.in);
        String line;
        CharniakScoredParsesReaderWriter rw = new CharniakScoredParsesReaderWriter();
        while ((line = in.nextLine()) != null) {
            System.out.println(charniakClient.parse(line));            
        }
    }
}
