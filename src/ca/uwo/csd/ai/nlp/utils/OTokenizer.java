/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/**
 * Wrapper for OpenNLP tokenizer
 * @author Syeed Ibn Faiz
 */
public class OTokenizer {
    private TokenizerModel model;
    Tokenizer tokenizer;

    public OTokenizer(String modelPath) {        
        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream(modelPath);
            model = new TokenizerModel(modelIn);
            tokenizer = new TokenizerME(model);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public OTokenizer() {
        this("./resource/ml/models/OpenNLP/en-token.bin");
    }
    
    public String[] getTokens(String sentence) {        
        return tokenizer.tokenize(sentence);
    }
    
    public Span[] getSpans(String sentence) {
        return tokenizer.tokenizePos(sentence);
    }
    
    public static void main(String args[]) {
        OTokenizer tokenizer = new OTokenizer();
        String[] tokens = tokenizer.getTokens("This is a test (beta-test)");
        for (String token: tokens) {
            System.out.println(token);
        }
        Span[] spans = tokenizer.getSpans("Another test.");
        for (Span span : spans) {
            System.out.println(span);
        }
    }
}
