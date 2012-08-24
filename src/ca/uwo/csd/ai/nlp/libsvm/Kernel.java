/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.libsvm;

/**
 *
 * @author Syeed Ibn Faiz
 */
public interface Kernel {
    double evaluate(Object k1, Object k2);
}
