/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.postmed;

import ca.uwo.csd.ai.nlp.utils.Util;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PostMedAnalyzer {
    public static void showComments() {
        File comment = new File("resource/postmed/comment");
        File[] files = comment.listFiles();
        for (File file : files) {
            String content = Util.readContent(file, true);
            System.out.println(file.getName()+"\n"+content+"-----");
        }
    }
    public static void compareWithOpenNLP() throws IOException {
        File diff = new File("resource/postmed/diff");
        File comment = new File("resource/postmed/comment");
        File[] diffFiles = diff.listFiles();
        Scanner in = new Scanner(System.in);
        for (File diffFile : diffFiles) {            
            File commentFile = new File(comment, diffFile.getName());
            if (!commentFile.exists()) {
                List<String> lines = Util.readLines(diffFile);
                int count = 0;
                for (String line : lines) {
                    if (line.equals("---")) {
                        count++;
                    }
                }
                FileWriter writer = new FileWriter(commentFile);
                writer.write(count+" mismatches\n");
                writer.close();
                
                Desktop.getDesktop().open(diffFile);
                Desktop.getDesktop().open(commentFile);
                
                System.out.println("Press enter:");
                in.nextLine();
            }
        }        
    }
    public static void main(String args[]) throws IOException {
        //compareWithOpenNLP();
        showComments();
    }
}
