/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.io.SimpleSentReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class OChunker implements Annotator {

    ChunkerME chunker;

    public OChunker(String modelPath) {
        try {
            InputStream modelIn = new FileInputStream(modelPath);
            chunker = new ChunkerME(new ChunkerModel(modelIn));
        } catch (IOException ex) {
            Logger.getLogger(OChunker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public OChunker() {
        this("resource/ml/models/OpenNLP/en-chunker.bin");
    }
    
    
    
    @Override
    public Sentence annotate(Sentence s) {
        String words[] = new String[s.size()];
        words = s.getWords().toArray(words);
        String posTags[] = s.getTags("POS");
        String chunkTags[] = chunker.chunk(words, posTags);
        for (int i = 0; i < chunkTags.length; i++) {
            s.get(i).setTag("CHUNK", chunkTags[i]);
        }
        s.markAnnotation(this.getFieldNames());
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"CHUNK"};
    }
    
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        String line;
        SimpleSentReader sentReader = new SimpleSentReader();
        OTagger tagger = new OTagger();
        OChunker chunker = new OChunker();
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            s = tagger.annotate(s);
            s = chunker.annotate(s);
            System.out.println(s.toString("CHUNK"));
        }
    }
}
