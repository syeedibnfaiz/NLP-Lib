/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

/**
 *
 * @author mibnfaiz
 */
public class RawSentence {
    int startOffset;
    String line;

    public RawSentence(int offset, String line) {
        this.startOffset = offset;
        this.line = line;
    }

    public String getLine() {
        return line;
    }

    public int getStartOffset() {
        return startOffset;
    }
    
    public int getEndOffset() {
        return startOffset + line.length();
    }
    
    /**
     * Returns prefix ending at lastOffset (inclusive)
     * @param lastOffset
     * @return 
     */
    public String getPrefix(int lastOffset) {
        StringBuilder sb = new StringBuilder();
        int end = lastOffset - this.startOffset;
        for (int i = 0; i <= end; i++) {
            /*if (!Character.isSpaceChar(line.charAt(i))) {
                sb.append(line.charAt(i));
            }*/
            if (i > 2 && i < end && line.charAt(i)== '\'') {
                 //&& line.charAt(i)== '\'' && line.charAt(i-3)== 'c' && line.charAt(i-2)== 'a' && line.charAt(i-1)== 'n' && line.charAt(i+1)== 't'
                String tmp = line.substring(i - 3, i + 2);
                if (tmp.matches("(c|C)an't")) {
                    sb.append('n');
                } else if (tmp.equals("won't")) {
                    sb.replace(sb.length()-2, sb.length(), "illn");
                } else if (tmp.equals("ain't")) {
                    sb.replace(sb.length()-3, sb.length(), "ISn");
                }            
            }
            //|| line.charAt(i) == ',' is added for new piped version, coz , were being included in the gorn address
            if (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == ',') {
                sb.append(line.charAt(i));
            } /*else if (!Character.isSpaceChar(line.charAt(i))) {
                sb.append("-");
            }*/
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return line;
    }
}
