/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import java.io.Serializable;

/**
 *
 * File format:
 * 
Col 1: Relation type 
Col 2: Section number 
Col 3: File number 
Col 4: Connective/AltLex SpanList 
Col 5: Connective/AltLex GornAddressList 
Col 6: Connective/AltLex RawText 
Col 7: String position  
Col 8: Sentence number 
Col 9: ConnHead 
Col 10: Conn1 
Col 11: Conn2 
Co 12: 1st Semantic Class  corresponding to ConnHead, Conn1 or AltLex span 
Col 13: 2nd Semantic Class  corresponding to ConnHead, Conn1 or AltLex span 
Col 14: 1st Semantic Class corresponding to Conn2 
Col 15: 2nd Semantic Class corresponding to Conn2 
Col 16-22: relation level attribution
Col 23: Arg1 SpanList
Col 24: Arg1 GornAddress
Col 25: Arg1 RawText
Col 26: Arg1 attribution
Col 33: Arg2 SpanList
Col 34: Arg2 GornAddress
Col 35: Arg2 RawText
Col 36-48: Arg2 & Sup attribution

 * @author Syeed Ibn Faiz
 */
public class PDTBRelation implements Serializable {
    private String columns[];

    public PDTBRelation(String pipedLine) {
        columns = pipedLine.split("\\|");
    }

    public PDTBRelation(String[] columns) {
        this.columns = columns;
    }
    
    
    public String getType() {
        return columns[0];
    }
        
    
    public SpanList getConnSpanList() {
        return new SpanList(columns[3]);
    }
    
    public String getConnRawText() {
        return columns[5];
    }
    
    public void setConnectiveGornAddress(String gornAddress) {
        columns[4] = gornAddress;
    }
    public String getConnectiveGornAddress() {
        return columns[4];
    }
    public String getConnHead() {
        return columns[8];
    }
    
    public String getSense() {
        return columns[11];
    }
    public String getSense2() {
        return columns[12];
    }
    public String getArg1RawText() {
        return columns[24];
    }
    
    public SpanList getArg1SpanList() {
        return new SpanList(columns[22]);
    }
    
    public void setArg1GornAddress(String gornAddress) {
        columns[23] = gornAddress;
    }
    public String getArg1GornAddress() {
        return columns[23];
    }
    public String getArg2RawText() {
        return columns[34];
    }
    
    public SpanList getArg2SpanList() {
        return new SpanList(columns[32]);
    }
    
    public void setArg2GornAddress(String gornAddress) {
        columns[33] = gornAddress;
    }
    public String getArg2GornAddress() {
        return columns[33];
    }
    
    public String getSectionNumber() {
        return columns[1];
    }
    
    public String getFileNumber() {
        return columns[2];
    }
    public void setConnHead(String head) {
        columns[8] = head;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) sb.append("|");
            sb.append(columns[i]);
        }
        return sb.toString();
    }
}
