/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Preprocess corpus data from a unified xml file
 * @author Syeed Ibn Faiz
 * @deprecated 
 */
public class PrepareCorpusXML {
    private boolean produceTrees;
    private boolean produceSentences;
    private boolean produceDependencyTrees;
    private boolean produceCCDependencyTrees;

    public PrepareCorpusXML(boolean produceTrees, boolean produceSentences, boolean produceDependencyTrees, boolean produceCCDependencyTrees) {
        this.produceTrees = produceTrees;
        this.produceSentences = produceSentences;
        this.produceDependencyTrees = produceDependencyTrees;
        this.produceCCDependencyTrees = produceCCDependencyTrees;
    }

    public PrepareCorpusXML() {
        this(true, true, true, true);
    }
    
    private static class BioDocument {
        String documentName;
        Text text;
    }
    private static class Span {
        int start;
        int end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }                
    }
    private Span[] getSpans(Element tokenizationElement) {
        NodeList tokenNodes = tokenizationElement.getElementsByTagName("token");
        Span[] spans = new Span[tokenNodes.getLength()];
        for (int i = 0; i < tokenNodes.getLength(); i++) {
            Element tokenElement = (Element) tokenNodes.item(i);
            String offsetStr = tokenElement.getAttribute("charOffset");            
            String[] parts = offsetStr.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            spans[i] = new Span(start, end);
        }
        return spans;
    }
    private Sentence getSentence(String rawText, Span[] spans) {
        Sentence s = new Sentence();
        for (Span span : spans) {
            s.add(new TokWord(rawText.substring(span.start, span.end + 1)));
        }
        return s;
    }
    private void markEntities(Sentence s, Element sentElement) {
        NodeList entityNodes = sentElement.getElementsByTagName("entity");
        for (int i = 0; i < entityNodes.getLength(); i++) {
            Element entityElement = (Element) entityNodes.item(i);
            String id = entityElement.getAttribute("id");
            String offsetStr = entityElement.getAttribute("charOffset");
            if (offsetStr.contains(",")) {
                offsetStr = offsetStr.substring(offsetStr.indexOf(',') + 1);
            }
            
        }
    }
    private void getSentence(Element sentElement) {
        NodeList tokenNodes = sentElement.getElementsByTagName("tokenization");
        Element tokenElement = null;
        for (int i = 0; i < tokenNodes.getLength(); i++) {
            Element elem = (Element) tokenNodes.item(i);
            String tokenizer = elem.getAttribute("tokenizer");
            if (tokenizer.equals("Charniak-Lease")) {
                tokenElement = elem;
                break;
            }
        }
        Span[] spans = getSpans(tokenElement);
        String rawText = sentElement.getAttribute("text");
        Sentence s = getSentence(rawText, spans);
    }
    
    private void getText(Element documentElement) {
        NodeList sentNodes = documentElement.getElementsByTagName("sentence");
        for (int i = 0; i < sentNodes.getLength(); i++) {
            Element sentElement = (Element) sentNodes.item(i);
            
        }
    }
    private BioDocument getDocument(Element documentElement) {
        BioDocument document = new BioDocument();
        //set document name from origId attribute
        String origId = documentElement.getAttribute("origId");
        document.documentName = origId;
        return document;
    }
    private List<BioDocument> getDocuments(Document xmlDocument) {
        List<BioDocument> bioDocumentList = new ArrayList<BioDocument>();
        NodeList documentNodes = xmlDocument.getElementsByTagName("document");
        for (int i = 0; i < documentNodes.getLength(); i++) {
            Element documentElement = (Element) documentNodes.item(i);
            bioDocumentList.add(getDocument(documentElement));
        }
        return bioDocumentList;
    }
    public void processCorpus(String corpusFilePath, String outputRootPath) {
        Document xmlDocument = Util.readXML(corpusFilePath);
        File outputRoot = new File(outputRootPath);
        if (!outputRoot.exists()) {
            throw new IllegalArgumentException(outputRootPath + " does not exist!");
        }
        List<BioDocument> bioDocuments = getDocuments(xmlDocument);
    }
}
