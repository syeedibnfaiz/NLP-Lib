/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class OTagger implements Annotator {

    POSTaggerME tagger;

    public OTagger(String modelPath) {
        try {
            InputStream modelIn = new FileInputStream(modelPath);
            tagger = new POSTaggerME(new POSModel(modelIn));
        } catch (IOException ex) {
            Logger.getLogger(OChunker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public OTagger() {
        this("resource/ml/models/OpenNLP/en-pos-maxent.bin");
    }
    
    @Override
    public Sentence annotate(Sentence s) {
        String words[] = new String[s.size()];
        words = s.getWords().toArray(words);        
        String tags[] = tagger.tag(words);
        for (int i = 0; i < tags.length; i++) {
            s.get(i).setTag("POS", tags[i]);
        }
        s.markAnnotation(this.getFieldNames());
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"POS"};
    }
    
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        String line;
        SimpleSentReader sentReader = new SimpleSentReader();
        OTagger tagger = new OTagger();
        
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            s = tagger.annotate(s);
            System.out.println(s.toString("POS"));
        }
    }
}
