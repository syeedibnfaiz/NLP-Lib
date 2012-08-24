/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.uwo.csd.ai.nlp.ling.ann;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A <code>SerialAnnotator</code> object is a pipeline of annotators.
 * @author Syeed Ibn Faiz
 */
public class SerialAnnotator implements Annotator, Serializable {
    private ArrayList<Annotator> annotators;

    public SerialAnnotator(ArrayList<Annotator> annotators) {
        this.annotators = annotators;
    }

    public SerialAnnotator() {
        annotators = new ArrayList<Annotator>();
    }

    public Sentence annotate(Sentence s) {
        for (Annotator annotator : annotators) {
            s = annotator.annotate(s);
        }
        return s;
    }

    public String[] getFieldNames() {
        //Set<String> fieldSet = new HashSet<String>();
        ArrayList<String> fieldList = new ArrayList<String>();
        for (Annotator annotator : annotators) {
            String[] fieldNames = annotator.getFieldNames();
            fieldList.addAll(Arrays.asList(fieldNames));
        }
        String fieldNames[] = new String[fieldList.size()];
        fieldList.toArray(fieldNames);        
        return fieldNames;
    }

    public void add(Annotator annotator) {
        annotators.add(annotator);
    }

    // Serialization
    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_SERIAL_VERSION);
        out.writeObject(this.annotators);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        this.annotators = (ArrayList<Annotator>) in.readObject();
    }
}
