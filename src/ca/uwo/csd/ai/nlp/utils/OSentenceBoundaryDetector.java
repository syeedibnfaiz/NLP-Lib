/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

/**
 * Wrapper for OpenNLP sentence segmenter
 * @author Syeed Ibn Faiz
 */
public class OSentenceBoundaryDetector {
    SentenceModel model;
    SentenceDetectorME sentenceDetector;
    
    public OSentenceBoundaryDetector(String path) {        
        InputStream modelIn = null;
        try {
            modelIn = new FileInputStream(path);
            model = new SentenceModel(modelIn);
            sentenceDetector = new SentenceDetectorME(model);
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
    
    public OSentenceBoundaryDetector() {        
        this("./resource/ml/models/OpenNLP/en-sent.bin");        
    }
    
    public String[] getSentences(String text) {
        return sentenceDetector.sentDetect(text);
    }
    
    public Span[] getSpans(String text) {
        return sentenceDetector.sentPosDetect(text);
    }
}
