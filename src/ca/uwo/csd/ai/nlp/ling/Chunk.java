/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling;

import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.io.LPSentReader;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Chunk {
    private Sentence sentence;
    private int start;
    private int end;
    String chunkTag;

    /**
     * Create a new <code>Chunk</code> object.
     * @param sentence The sentence containing the chunk
     * @param start The starting position of the chunk (inclusive)
     * @param end The ending position of the chunk (inclusive)
     */
    public Chunk(Sentence sentence, int start, int end) {
        //if (!sentence.hasTaggedWords("CHUNK")) throw new IllegalArgumentException("Sentence is not chunked!");
        if (!sentence.isAnnotatedBy("CHUNK")) throw new IllegalArgumentException("Sentence is not chunked!");
        else if(sentence.isEmpty()) throw new IllegalArgumentException("Can not produce chunk from an empty sentence");
        else if (start < 0 || end < 0 || start > sentence.size() || end > sentence.size() || start > end) {
            throw new IllegalArgumentException("Invalid start end chunk position.");
        } else if (sentence.get(start).getTag("CHUNK") != null) {
            boolean valid = true;
            for (int i = start + 1; i <= end; i++) {
                if (sentence.get(i).getTag("CHUNK") == null) {
                    valid = false;
                    break;
                } else if (!sentence.get(i).getTag("CHUNK").substring(2).equals(sentence.get(i - 1).getTag("CHUNK").substring(2))) {
                    valid = false;
                    break;
                }
            }
            if (!valid) throw new IllegalArgumentException("Invalid chunk range provided.");
        }
        
        this.sentence = sentence;
        this.start = start;
        this.end = end;

        this.chunkTag = sentence.get(start).getTag("CHUNK").substring(2);
    }

    public Chunk(Sentence sentence, int middle) {
        //if (!sentence.hasTaggedWords("CHUNK")) throw new IllegalArgumentException("Sentence is not chunked!");
        if (!sentence.isAnnotatedBy("CHUNK")) throw new IllegalArgumentException("Sentence is not chunked!");
        else if(sentence.isEmpty() || middle < 0 || middle >= sentence.size() || sentence.get(middle).getTag("CHUNK").equals("O")) {
            throw new IllegalArgumentException("Imposiible to get a chunk, because the position is not part of any chank");
        }
        
        this.sentence = sentence;
        this.chunkTag = sentence.get(middle).getTag("CHUNK").substring(2);
        
        int startPos = middle;
        int endPos = middle + 1;

        //finding the start position of chunk
        while (startPos >= 0) {
            String tmpTag = sentence.get(startPos).getTag("CHUNK");
            if (tmpTag.startsWith("B-")) break;
            startPos--;
        }
        //finding the end position of chunk
        while (endPos < sentence.size()) {
            String tmpTag = sentence.get(endPos).getTag("CHUNK");
            if (tmpTag.startsWith("B-") || tmpTag.equals("O")) break;
            endPos++;
        }

        this.start = startPos;
        this.end = endPos - 1;
    }

    public String getChunkTag() {
        return chunkTag;
    }

    public int getEnd() {
        return end;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public int getStart() {
        return start;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean printChunkTag) {
        if (printChunkTag) return this.chunkTag + " : " + sentence.subList(this.start, this.end + 1).toString();
        return sentence.subList(this.start, this.end + 1).toString();
    }

    public static void main(String args[]) {
        LPSentReader sentReader = new LPSentReader();
        GeniaTagger tagger = new GeniaTagger();
        Scanner in = new Scanner(System.in);
        String line;
        
        while ((line = in.nextLine()) != null) {
            Sentence s = sentReader.read(line);
            s = tagger.annotate(s);
            ArrayList<Chunk> chunks = s.getChunks();
            System.out.println(chunks);
        }
    }
}
