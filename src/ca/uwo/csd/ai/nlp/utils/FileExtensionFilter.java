/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author tonatuni
 */
public class FileExtensionFilter implements FilenameFilter {

    String extension;

    public FileExtensionFilter(String extension) {
        this.extension = extension;
    }
    
    @Override
    public boolean accept(File dir, String name) {
        if (name.endsWith(name)) return true;
        return false;
    }
    
}
