package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelationFilterer {
    final boolean DEBUG = true;
    final static SyntaxTreeAnalyzer TREE_ANALYZER = new SyntaxTreeAnalyzer();
    
    public static void main(String[] args) {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        String baseDirPath = "./resource/relation/PPI4/";
        CorpusReader corpusReader = new CorpusReader();
        
        RelationFilterer extractor = new RelationFilterer();
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PathFilter());
        filters.add(new DomainFilter());
        filters.add(new NegativeFilter());
        filters.add(new LeftPatternFilter());
        filters.add(new RightPatternFilter());
        filters.add(new MiddlePatternFilter());
        filters.add(new JuxtaposFilter());
        
        for (String corpus : corpora) {
            System.out.println("--"+corpus+"--");            
            List<RelationInstance> relationInstances = corpusReader.getRelationInstances(baseDirPath + corpus);
            extractor.showCount(relationInstances);
            List<RelationInstance> relationInstances2 = extractor.applyFilters(relationInstances, filters);    
            extractor.showResult(corpus, relationInstances2);
            System.out.println("");
            if (corpus.equals("LLL")) {
                //extractor.debug(relationInstances, relationInstances2);
            }
        }
    }
    
    public List<RelationInstance> applyFilters(List<RelationInstance> relationInstances, List<Filter> filters) {
        for (Filter filter : filters) {            
            relationInstances = applyFilter(relationInstances, filter);
        }
        return relationInstances;
    }
    
    public List<RelationInstance> applyFilter(List<RelationInstance> relationInstances, Filter filter) {        
        if (DEBUG) System.out.println("Applying.. " + filter.getName());
        relationInstances = filter.apply(relationInstances);
        if (DEBUG) showCount(relationInstances);
        return relationInstances;
    }
    
    private void showResult(String corpusName, List<RelationInstance> relationInstances) {
        int total = 0;
        if (corpusName.equals("LLL")) total = 164;
        else if (corpusName.equals("HPRD50")) total = 163;
        else if (corpusName.equals("IEPA")) total = 335;
        else if (corpusName.equals("AIMed")) total = 1000;
        else total = 2534;
        int positive = 0;
        int negative = 0;        
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction) {
                positive++;                
            } else {
                negative++;                
            }
        }
        int tp = positive;
        int fn = total - tp;
        int fp = negative;
        double precision = tp*1.0/(tp + fp);
        double recall = tp*1.0/(tp + fn);
        double fscore = 2*precision*recall/(precision + recall);
        System.out.println("Precision: " + String.format("%.2f", precision*100) + "%");
        System.out.println("Recall: " + String.format("%.2f", recall*100) + "%");
        System.out.println("Fscore: " + String.format("%.2f", fscore*100) + "%");
    }
    private void showCount(List<RelationInstance> relationInstances) {
        int positive = 0;
        int negative = 0;
        int p1 = 0;
        int p2 = 0;
        int p3 = 0;
        int n1 = 0;
        int n2 = 0;
        int n3 = 0;
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction) {
                positive++;
                if (instance.lcs < instance.entity1) p1++;
                else if (instance.lcs < instance.entity2) p2++;
                else p3++;
            } else {
                negative++;
                if (instance.lcs < instance.entity1) n1++;
                else if (instance.lcs < instance.entity2) n2++;
                else n3++;
            }
        }
        System.out.println("+ve: " + positive + "(" + p1 + ", " + p2 + ", " + p3 + ")");
        System.out.println("-ve: " + negative + "(" + n1 + ", " + n2 + ", " + n3 + ")");
    }
    
    private void debug(List<RelationInstance> relationInstances, List<RelationInstance> relationInstances2) {
        int before = 0;
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction) {
                System.out.println(instance.s);
                printBackbone(instance.s, instance.path);
                System.out.println(TREE_ANALYZER.getPennOutput(instance.s.getParseTree()));
                System.out.println("");
            }
        }
        int after = 0;
        for (RelationInstance instance : relationInstances2) {
            if (instance.interaction) {
                if (instance.entity1 == instance.entity2) {
                    after++;
                }
            }
        }
        System.out.println("before: " + before);
        System.out.println("after: " + after);
    }
    
    public static void printBackbone(Sentence s, List<String> backBonePath) {
        for (int i = 0; i < backBonePath.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.valueOf(backBonePath.get(i));
                System.out.print(s.get(index));
            } else {
                System.out.print(":"+backBonePath.get(i)+":");
            }            
        }
        System.out.println("");
    }
}
