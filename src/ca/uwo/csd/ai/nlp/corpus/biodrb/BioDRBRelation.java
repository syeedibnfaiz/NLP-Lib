package ca.uwo.csd.ai.nlp.corpus.biodrb;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.SpanList;
import java.io.Serializable;

/**
 *
 * @author mibnfaiz
 */
public class BioDRBRelation implements Serializable {
    private String columns[];

    public BioDRBRelation(String pipedLine) {
        columns = pipedLine.split("\\|");
    }
    
    public String getType() {
        return columns[0];
    }
    
    public SpanList getConnSpanList() {
        return new SpanList(columns[1]);
    }
    
    public String getConnRawText() {
        return columns[7];
    }
    
    public void setConnRawText(String conn) {
        columns[7] = conn;
    }
    public void setConnectiveGornAddress(String gornAddress) {
        columns[2] = gornAddress;
    }
    public String getConnectiveGornAddress() {
        return columns[2];
    }
    /*public String getConnHead() {
        return columns[8];
    }*/
    
    public String getSense() {
        return columns[8];
    }
    public void setArg1RawText(String rawText) {
        columns[13] = rawText;
    }
    public String getArg1RawText() {
        return columns[13];
    }
    
    public SpanList getArg1SpanList() {
        return new SpanList(columns[14]);
    }
    
    public void setArg1GornAddress(String gornAddress) {
        columns[12] = gornAddress;
    }
    public String getArg1GornAddress() {
        return columns[12];
    }
    public void setArg2RawText(String rawText) {
        columns[19] = rawText;
    }
    public String getArg2RawText() {
        return columns[19];
    }
    
    public SpanList getArg2SpanList() {
        return new SpanList(columns[20]);
    }
    
    public void setArg2GornAddress(String gornAddress) {
        columns[18] = gornAddress;
    }
    public String getArg2GornAddress() {
        return columns[18];
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
    
    public PDTBRelation toPDTBRelation() {
        String[] columns = new String[48];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new String();
        }
        columns[0] = getType();
        columns[3] = this.columns[1]; //connSpanList
        columns[5] = getConnRawText(); 
        columns[8] = getConnRawText().toLowerCase();  //connHead
        columns[4] = getConnectiveGornAddress(); 
        
        columns[11] = getSense();
        columns[12] = this.columns[9]; //sense2
        
        columns[24] = getArg1RawText();
        columns[22] = this.columns[14];  //arg1SpanList
        columns[23] = getArg1GornAddress();
        
        columns[34] = getArg2RawText();
        columns[32] = this.columns[20]; //arg2SpanList
        columns[33] = getArg2GornAddress();
        
        return new PDTBRelation(columns);        
    }
}
