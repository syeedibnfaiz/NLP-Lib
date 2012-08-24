/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.GeniaTaggerWrapper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class GeniaTagger implements Annotator, Serializable {

    private static GeniaTaggerWrapper geniaWrapper = new GeniaTaggerWrapper();

    @Override
    public Sentence annotate(Sentence s) {
        ArrayList<String> tokens = s.getWords();
        String tagMatrix[][] = geniaWrapper.doTagging(tokens);
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            word.setTag("BASE", tagMatrix[i][1]);
            word.setTag("POS", tagMatrix[i][2]);
            word.setTag("CHUNK", tagMatrix[i][3]);
            word.setTag("NE", tagMatrix[i][4]);
        }
        s.markAnnotation(getFieldNames());
        return s;
    }

    public static void main(String args[]) {
        GeniaTagger tagger = new GeniaTagger();
        Scanner in = new Scanner(System.in);
        String line;

        while ((line = in.nextLine()) != null) {
            String tokens[] = line.split("\\s+");
            ArrayList<TokWord> list = new ArrayList<TokWord>();            
            for (String token: tokens) list.add(new TokWord(token));

            Sentence s = new Sentence(list);
            System.out.println(s);
            s = tagger.annotate(s);
            System.out.println(s.toString("POS"));
        }
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"BASE", "POS", "CHUNK", "NE"};
    }

    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
    }
}
