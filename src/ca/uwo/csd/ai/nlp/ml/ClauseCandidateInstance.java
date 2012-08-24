/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.ling.Chunk;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import cc.mallet.types.Instance;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ClauseCandidateInstance extends Instance implements Serializable {
    int s;
    int e;
    boolean clause;
    public ClauseCandidateInstance(Sentence sentence, int s, int e, boolean clause) {
        super(sentence, null, null, sentence.toString());
        this.s = s;
        this.e = e;
        this.clause = clause;
    }

    public ClauseCandidateInstance(Sentence sentence, int s, int e) {
        this(sentence, s, e, false);
    }

    public int getE() {
        return e;
    }

    public int getS() {
        return s;
    }

    public boolean isClause() {
        return clause;
    }
    
    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeInt(s);
        out.writeInt(e);
        out.writeBoolean(clause);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        this.s = in.readInt();
        this.e = in.readInt();
        this.clause = in.readBoolean();
    }
}
