package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.Annotator;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class BioDomainAnnotator implements Annotator {

    final static boolean INCLUDE_STEMS = true;
    private Set<String> domainTerms;
    private boolean includeStems;
    private SnowballStemmer stemmer;

    public BioDomainAnnotator() {
        this(INCLUDE_STEMS);
    }

    public BioDomainAnnotator(boolean includeStems) {
        this(Util.readLines("./resource/relation/biomedical_terms.txt"), includeStems);
    }

    public BioDomainAnnotator(String relationTermsFilePath) {
        this(relationTermsFilePath, INCLUDE_STEMS);
    }

    public BioDomainAnnotator(String relationTermsFilePath, boolean includeStems) {
        this(Util.readLines(relationTermsFilePath), includeStems);
    }

    public BioDomainAnnotator(List<String> terms, boolean includeStems) {
        this.includeStems = includeStems;
        domainTerms = new TreeSet<String>();
        for (String term : terms) {
            domainTerms.add(term);
        }
        //add stems
        if (this.includeStems) {
            stemmer = new englishStemmer();
            for (String term : terms) {
                domainTerms.add(getStem(term));
            }
        }
    }

    private String getStem(String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    @Override
    public Sentence annotate(Sentence s) {
        for (TokWord word : s) {
            if (this.includeStems) {
                String w = word.word().toLowerCase();
                if (domainTerms.contains(w)) {
                    word.setTag("DOMAIN", w);
                } else if (domainTerms.contains(getStem(w))) {
                    word.setTag("DOMAIN", getStem(w));
                } else if (w.contains("-")) {
                    String[] tokens = w.split("-");
                    for (String token : tokens) {
                        if (domainTerms.contains(token)) {
                            word.setTag("DOMAIN", token);
                        } else if (domainTerms.contains(getStem(token))) {
                            word.setTag("DOMAIN", getStem(token));
                        }
                    }
                }
            } else {
                for (String term : domainTerms) {
                    if (word.word().toLowerCase().startsWith(term)) {
                        word.setTag("DOMAIN", term);
                        break;
                    } else if (word.word().contains("-") && word.word().toLowerCase().endsWith(term)) {
                        word.setTag("DOMAIN", term);
                        break;
                    }
                }
            }
        }
        s.markAnnotation(getFieldNames());
        return s;
    }

    @Override
    public String[] getFieldNames() {
        return new String[]{"DOMAIN"};
    }

    public boolean isDomainTerm(String word) {
        if (this.domainTerms.contains(word)) {
            return true;
        } else if (includeStems && this.domainTerms.contains(getStem(word))) {
            return true;
        }
        return false;
    }

    public static void main(String args[]) {
        BioDomainAnnotator annotator = new BioDomainAnnotator();
        SnowballStemmer stemmer = new englishStemmer();
        /*HashSet<String> newSet = new HashSet<String>();
        for (String term : annotator.domainTerms) {
            newSet.add(term);
            stemmer.setCurrent(term);
            stemmer.stem();
            String stem = stemmer.getCurrent();
            newSet.add(stem);
        }
        for (String newTerm : newSet) {
            if (!annotator.domainTerms.contains(newTerm)) {
                System.out.println("New: " + newTerm);
            }
        }*/
        stemmer.setCurrent("interactable");
        stemmer.stem();
        System.out.println(stemmer.getCurrent());
    }
}
