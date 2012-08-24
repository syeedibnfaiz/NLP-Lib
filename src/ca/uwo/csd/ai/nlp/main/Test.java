/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.main;

import abner.Scanner;
import java.io.StringReader;


/**
 *
 * @author tonatuni
 */
public class Test {
    public  static String tokenize(String s) {
	StringBuffer sb = new StringBuffer();
	try {
	    Scanner scanner = new Scanner(new StringReader(s));;
	    String t;
	    while ((t = scanner.nextToken()) != null) {
		sb.append(t+" ");
		if (t.toString().matches("[?!\\.]"))
		    sb.append("\n");
	    }
	    return sb.toString();
	} catch (Exception e) {
	    System.err.println(e);
	}
	return sb.toString();
    }
    
    public static void main(String[] args) {
        String s = "This is a test(mild)";
        System.out.println(tokenize(s));
    }
}
