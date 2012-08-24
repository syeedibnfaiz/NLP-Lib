/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class SimpleDependency {
    String reln;
    int gov;
    int dep;
    
    public SimpleDependency(String reln, int gov, int dep) {
        this.reln = reln;
        this.gov = gov;
        this.dep = dep;
    }
    /**
     * Constructs a SimpleDependency object by parsing the following format:
     * reln(w1-gov, w2-dep)
     * @param rawFormat 
     */
    public SimpleDependency(String rawStr) {
        int lb = rawStr.indexOf('(');
        boolean invalidFormat = true;
        String cp = rawStr;
        
        if (lb != -1 && lb < (rawStr.length() - 1)) {
            this.reln = rawStr.substring(0, lb);
            
            rawStr = rawStr.substring(lb + 1, rawStr.length() - 1);            
            
            String tokens[] = rawStr.split(", ");
            if (tokens.length == 2) {
                int hyp1 = tokens[0].lastIndexOf('-');
                int hyp2 = tokens[1].lastIndexOf('-');
                if (hyp1 != -1 && hyp2 != -1) {
                    
                    if (tokens[0].endsWith("'")) {
                        //System.out.println("token0: " + tokens[0]);
                        int end = tokens[0].length() - 1;
                        while (end > (hyp1 + 1) && tokens[0].charAt(end) == '\'') end--;
                        //System.out.println("end: " + end);
                        //System.out.println("sub: " + tokens[0].substring(hyp1 + 1, end + 1));
                        this.gov = Integer.parseInt(tokens[0].substring(hyp1 + 1, end + 1)) - 1;
                    } else {
                        this.gov = Integer.parseInt(tokens[0].substring(hyp1 + 1)) - 1;
                    }
                    if (tokens[1].endsWith("'")) {
                        int end = tokens[1].length() - 1;
                        while (end > (hyp2 + 1) && tokens[1].charAt(end) == '\'') end--;
                        this.dep = Integer.parseInt(tokens[1].substring(hyp2 + 1, end + 1)) - 1;
                    } else {
                        this.dep = Integer.parseInt(tokens[1].substring(hyp2 + 1)) - 1;
                    }
                    invalidFormat = false;
                }
            }
            //System.out.println(rawStr);                                  
        }
        if (invalidFormat) {
            throw new IllegalArgumentException(cp + " is not a valid dependency");
        }
    }
    
    @Override
    public String toString() {
        return reln+"("+gov+", "+dep+")";
    }
    
    public int gov() {
        return gov;
    }
    
    public int dep() {
        return dep;
    }
    
    public String reln() {
        return reln;
    }

    public void setDep(int dep) {
        this.dep = dep;
    }

    public void setGov(int gov) {
        this.gov = gov;
    }

    public void setReln(String reln) {
        this.reln = reln;
    }
    
    public static void main(String args[]) {
        SimpleDependency sd = new SimpleDependency("nn_conj(10.23-100, bib-23.3-02)");
        System.out.println(sd);
    }
}
