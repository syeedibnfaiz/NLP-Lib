/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.corpus.pdtb;

import java.util.ArrayList;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class GornAddressList extends ArrayList<GornAddress> {

    public GornAddressList(String addressList) {
        super();
        String[] addresses = addressList.split(";");
        for (String address : addresses) {
            add(new GornAddress(address));
        }
    }
    
}
