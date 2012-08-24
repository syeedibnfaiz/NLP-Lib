/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import com.aliasi.io.FileLineReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class TokenSequenceConnLexiconTag extends Pipe implements Serializable {

    private String path2Lexicon;
    HashSet<String> lexicon;
    public TokenSequenceConnLexiconTag() {
        this(".\\resource\\ml\\data\\discourse\\lexicon.txt");
    }

    public TokenSequenceConnLexiconTag(String path2Lexicon) {
        this.path2Lexicon = path2Lexicon;
        lexicon = new HashSet<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path2Lexicon));
            String line;
            while ((line = reader.readLine()) != null) {
                lexicon.add(line);
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Could not find lexicon at: " + path2Lexicon);
        } catch (IOException ex) {
            System.err.println("Could not read from lexicon at: " + path2Lexicon);
        }
    }

    @Override
    public Instance pipe(Instance carrier) {
        TokenSequence ts = (TokenSequence) carrier.getData();        
        for (int i = 0; i < ts.size(); i++) {
            Token token = ts.get(i);
            if (lexicon.contains(token.getText())) {
                token.setFeatureValue("Lexicon", 1.0);
                if (i > 0) ts.get(i-1).setFeatureValue("Lexicon@+1", 1.0);
                if (i < (ts.size()-1)) ts.get(i+1).setFeatureValue("Lexicon@-1", 1.0);
            }
        }
        return carrier;
    }

    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeObject(path2Lexicon);
        out.writeObject(lexicon);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        path2Lexicon = (String) in.readObject();
        lexicon = (HashSet<String>) in.readObject();
    }
}
