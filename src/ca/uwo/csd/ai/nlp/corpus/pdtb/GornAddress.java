/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class GornAddress {
    String address;
    int lineNumber;
    public GornAddress(String address) {
        this.address = address;
        if (address.split(",")[0].equals("")) {
            throw new IllegalArgumentException("Gorn address must start with a number");
        }
        lineNumber = Integer.parseInt(address.split(",")[0]);
    }

    public String getAddress() {
        return address;
    }

    public int getLineNumber() {
        return lineNumber;
    }
    
}
