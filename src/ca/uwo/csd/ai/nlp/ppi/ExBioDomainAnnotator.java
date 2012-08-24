package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class ExBioDomainAnnotator implements Annotator {

    private HashMap<String, String> domainMap;

    public ExBioDomainAnnotator() {
        domainMap = new HashMap<String, String>();
        List<String> wordTagList = Util.readLines("./resource/relation/biomedical_terms_tag.txt");
        for (String wordTag : wordTagList) {
            int index = wordTag.lastIndexOf('|');
            String word = wordTag.substring(0, index);
            String tag = wordTag.substring(index + 1);
            domainMap.put(word, tag);
        }
    }
    
    private boolean isDomainTerm(TokWord tokWord) {
        String word = tokWord.word().toLowerCase();
        if (word.contains("-")) {
            word = word.substring(word.lastIndexOf('-') + 1);
        }
        String pos = domainMap.get(word);
        
        if (pos == null) {
            return false;
        } else if (pos.equals("*")) {
            return true;
        } else if (tokWord.getTag("POS") != null) {
            if (tokWord.getTag("POS").charAt(0) == pos.charAt(0)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public Sentence annotate(Sentence s) {
        for (TokWord word : s) {
            if (isDomainTerm(word)) {
                word.setTag("DOMAIN", word.word());
            }
        }
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"DOMAIN"};
    }
    
}
