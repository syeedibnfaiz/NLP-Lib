/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Util {
    
    public static List<String> readLines(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            return lines;
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static List<String> readLines(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            return lines;
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static String readContent(File file, boolean withNewLine) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));            
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                if (withNewLine) sb.append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static List<String> listAllFiles(String path, String extension) {
        File baseDir = new File(path);
        if (!baseDir.isDirectory()) return null;
        List<String> files = new ArrayList<String>();
        File[] listFiles = baseDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isFile()) {
                    if (file.getName().endsWith(extension)) {
                        files.add(file.getPath());
                    }
                } else if (file.isDirectory()) {
                    List<String> tmpFiles = listAllFiles(file.getAbsolutePath(), extension);
                    files.addAll(tmpFiles);
                }
            }
        }
        return files;
    }
    
    public static class Pair<T1, T2> {
        T1 first;
        T2 second;

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }
        public T1 first() {
            return first;
        }
        public T2 second() {
            return second;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Pair) {
                Pair otherPair = (Pair) o;
                return first.equals(otherPair.first) && second.equals(otherPair.second);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = (this.first != null ? this.first.hashCode() : 0)*5;
            hash += (this.second != null ? this.second.hashCode() : 0)*6;            
            return hash;
        }
    }
    
    public static Document readXML(String filePath) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        Document document = null;
        try {
            document = builder.parse(filePath);
        } catch (SAXException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return document;
    }
    
    public static void main(String args[]) {
        List<String> files = Util.listAllFiles("./pdtb_v2/ptb/00", ".mrg");
        System.out.println(files);
    }
}
