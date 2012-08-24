/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.io;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * A <code>GenericTextReader</code> object can be used to read from any
 * source given the sentence, token and optionally the tag delimiters.
 * It can be used to read raw text, or CONLL formatted text, etc.
 * @author Syeed Ibn Faiz
 */
public class GenericTextReader implements TextReader{
    private String lineSpearator;       //sentence delimiter
    private String tokenDelimiter;      //word/token delimiter
    private String tagDelimiter;        //optional
    private String[] fieldNames;        //names for the tags, optional

    public GenericTextReader(String lineSpearator, String tokenDelimiter, String tagDelimiter, String[] fieldNames) {
        this.lineSpearator = lineSpearator;
        this.tokenDelimiter = tokenDelimiter;
        this.tagDelimiter = tagDelimiter;
        this.fieldNames = fieldNames;
    }
    
    public Text read(String content) {
        Text text = new Text();
        String lines[] = content.split(lineSpearator);
        //System.out.println(lines.length);
        for (String line : lines) {
            String tokens[] = line.split(tokenDelimiter);
            Sentence s = new Sentence();
            for (String token : tokens) {                
                TokWord word = new TokWord();
                if (tagDelimiter != null) {
                    String tags[] = token.split(tagDelimiter);
                    word.setWord(tags[0]);
                    for (int i = 1; i < tags.length; i++) {
                        if (fieldNames != null && i < fieldNames.length) {
                            word.setTag(fieldNames[i], tags[i]);
                        } else {
                            word.setTag("field-" + i, tags[i]);
                        }
                    }
                } else {
                    word.setWord(token);
                }
                s.add(word);
            }
            if (fieldNames != null) {
                for (int i = 1; i < fieldNames.length; i++) s.markAnnotation(fieldNames[i]);
            }
            text.add(s);            
        }
        return text;        
    }

    public Text read(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line + "\n");
            }
            reader.close();

            return this.read(content.toString());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.err.println("Cant not read from: " + file.getAbsolutePath());
        }

        return null;
    }

    public static void main(String args[]) {
        GenericTextReader textReader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "Arg"});
        Text text = textReader.read(new File(".\\resource\\ml\\data\\discourse\\sentConnArg12.txt"));
        System.out.println(text);
    }
}
