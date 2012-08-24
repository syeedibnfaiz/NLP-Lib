/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ml.PrintFeatureVector;
import ca.uwo.csd.ai.nlp.ml.PruneFeatureVectors;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import ca.uwo.csd.ai.nlp.relx.RelexPipeAIMed2;
import ca.uwo.csd.ai.nlp.relx.ppi.RunRelex;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import ca.uwo.csd.ai.nlp.utils.Weka;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.classify.SVMLightClassifierFactory;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class MLExtractor {    
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "P1", "P2", "N1", "N2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();    
    final BioDomainAnnotator domainAnnotator = new BioDomainAnnotator();
    static final RuleExtractor RULE_EXTRACTOR = new RuleExtractor();
    static final RunRelex TEST_RELEX = new RunRelex();
    
    private InstanceList getInstanceList(String corpusRoot, List<String> docIds, Pipe pipe) {
        InstanceList instances = new InstanceList(pipe);
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".mrg");
            File depFile = new File(corpusRoot + "/deps", docId + ".dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".dep");
            
            Text text = TEXT_READER.read(iobFile);
            List<Tree> trees = TREE__READER.read(treeFile);
            List<SimpleDepGraph> deps = DEP_READER.read(depFile);
            List<SimpleDepGraph> ccDeps = DEP_READER.read(depCCFile);
            
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                Tree root = trees.get(i);
                SimpleDepGraph depGraph = deps.get(i);
                SimpleDepGraph ccDepGraph = ccDeps.get(i);
                s.setParseTree(root);
                assignPOS(s);
                s = domainAnnotator.annotate(s);
                addInstances(docId, i, instances, s, depGraph, ccDepGraph);
            }
        }
        return instances;
    }
    
    public Classifier train(String corpusRoot, List<String> docIds) { 
        InstanceList instances = getInstanceList(corpusRoot, docIds, defaultPipe());
        return train(instances);
    }
    
    public Classifier train(InstanceList instances) {
        MyClassifierTrainer trainer = new MyClassifierTrainer();        
        return trainer.train(instances);
    }
    
    private static class TestResult {
        int tp, fp, fn, tn;

        public TestResult() {
        }
        
        public TestResult(int tp, int fp, int fn, int tn) {
            this.tp = tp;
            this.fp = fp;
            this.fn = fn;
            this.tn = tn;
        }
        
    }
    public TestResult test(Classifier classifier, String corpusRoot, List<String> docIds) {
        InstanceList instances = getInstanceList(corpusRoot, docIds, defaultPipe());
        return test(classifier, instances);
    }
    
    public TestResult test(Classifier classifier, InstanceList instances) {
        int tp = 0;
        int fp = 0;
        int fn = 0;
        int tn = 0;
        for (Instance instance : instances) {
            //PPIInstance ppiInstance = (PPIInstance) instance;
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) {
                //if (ppiInstance.interact) {
                if (String.valueOf(instance.getTarget()).equals("true")) {
                    tp++;
                    /*if (new Random().nextInt(50) == 1) {
                        System.out.println(ppiInstance.s);
                        System.out.println("e1=" + ppiInstance.s.get(ppiInstance.entity1));
                        System.out.println("e2=" + ppiInstance.s.get(ppiInstance.entity2));
                        System.out.println("depPath=" + ppiInstance.ccDepGraph.getPath(ppiInstance.entity1, ppiInstance.entity2));
                        System.out.println(ppiInstance.getData());
                    }*/
                } else {
                    tn++;
                }
            } else {
                //if (ppiInstance.interact) {
                if (String.valueOf(instance.getTarget()).equals("true")) {
                    fn++;
                    /*System.out.println("Miss");
                    System.out.println(ppiInstance.s);
                    System.out.println("e1=" + ppiInstance.s.get(ppiInstance.entity1));
                    System.out.println("e2=" + ppiInstance.s.get(ppiInstance.entity2));
                    System.out.println("depPath=" + ppiInstance.ccDepGraph.getPath(ppiInstance.entity1, ppiInstance.entity2));
                    System.out.println(ppiInstance.getData());*/
                } else {
                    fp++;
                }
            }
        }
        return new TestResult(tp, fp, fn, tn);
    }
    
    private void shuffle(List<String> docIds) {
        Random random = new Random();
        int size = docIds.size();
        for (int i = 0; i < size - 1; i++) {
            int r = random.nextInt(size - i) + i;            
            if (r != i) {
                String tmp = docIds.get(r);
                docIds.set(r, docIds.get(i));
                docIds.set(i, tmp);
            }
        }
    }
    public void NFoldDocLevelCV(String corpusRoot, int N) {
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        
        //shuffle
        //Collections.shuffle(docIds);
        
        InstanceList instanceList = getInstanceList(corpusRoot, docIds, defaultPipe());
        //instanceList = PruneFeatureVectors.pruneByCount(instanceList, 5);
        
        //uncomment the following line before generating model for use in WBioRelEx
        //otherwise you will get the error - ca.uwo.csd.ai.nlp.ling.Sentence cannot be cast to cc.mallet.types.FeatureVector
        //instanceList = PruneFeatureVectors.pruneByInfoGain(instanceList, 1500);
        
        MyClassifierTrainer trainer = new MyClassifierTrainer();
        trainer.train(instanceList);
        trainer.saveModel("./resource/ml/models/PPI/all_ppi.model");        
        
        
        Weka.convert2ARFF(instanceList, "aimed.arff", "AIMed");
        
        TestResult[] results = new TestResult[N];
        int size = instanceList.size()/N;
        int begin = 0;
        int end = size-1;
        for (int i = 1; i <= N; i++) { //N-fold            
            InstanceList trainingInstances = new InstanceList(instanceList.getPipe());
            InstanceList testingInstances = new InstanceList(instanceList.getPipe());
            for (int j = 0; j < instanceList.size(); j++) {
                if (j >= begin && j <= end) {                    
                    testingInstances.add(instanceList.get(j));                    
                } else {
                    trainingInstances.add(instanceList.get(j));                    
                }                                
            }
            
            Classifier classifier = train(trainingInstances);
            results[i-1] = test(classifier, testingInstances);
            
            //update
            begin = end + 1;
            end += size;
            if (i == (N - 1)) {
                end = instanceList.size();
            }
        }
        
        int tp = 0;
        int fp = 0;
        int fn = 0;        
        for (int i = 0; i < N; i++) {
            tp += results[i].tp;
            fp += results[i].fp;
            fn += results[i].fn;            
        }
        System.out.println("Feature size: " + instanceList.getDataAlphabet().size());
        System.out.println("tp+fn: " + (tp + fn));        
        System.out.println("tp: " + tp);
        System.out.println("fp: " + fp);
        double precision = 1.0 * tp/(tp + fp);
        double recall = 1.0 * tp/(tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);
        System.out.println("Result of " + N + "-fold CV:");
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-Score: " + fscore);
        
        
        //test(instanceList, N);
        
        //NFoldEvaluator evaluator = new NFoldEvaluator();
        //evaluator.evaluateInOrder(new MyClassifierTrainer(), instanceList, 10);
        //evaluator.evaluate(new MyClassifierTrainer(), instanceList, 10);
        
    }
    
    public void crossCorporaTest(String corpusRootTrain, String corpusRootTest) {
        Pipe pipe = defaultPipe();
        File iobDir = new File(corpusRootTrain, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }                
        InstanceList trainingInstanceList = getInstanceList(corpusRootTrain, docIds, pipe);                
        //trainingInstanceList = PruneFeatureVectors.pruneByCount(trainingInstanceList, 5);
        //trainingInstanceList = PruneFeatureVectors.pruneByInfoGain(trainingInstanceList, 1500);
        
        iobDir = new File(corpusRootTest, "iob");
        docIds = new ArrayList<String>();
        files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }                
        InstanceList testingInstanceList = getInstanceList(corpusRootTest, docIds, pipe);
        
        MyClassifierTrainer trainer = new MyClassifierTrainer();
        Classifier classifier = trainer.train(trainingInstanceList);
        
        TestResult result = test(classifier, testingInstanceList);
        double tp = result.tp;
        double fp = result.fp;
        double fn = result.fn;
        
        System.out.println("tp+fn: " + (tp + fn));        
        System.out.println("tp: " + tp);
        System.out.println("fp: " + fp);
        double precision = 1.0 * tp/(tp + fp);
        double recall = 1.0 * tp/(tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);
        System.out.println("-----Result----");
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-Score: " + fscore);
    }
    private void test(InstanceList instances, int n) {
        double proportions[] = new double[n];
        for (int i = 0; i < n; i++) {
            proportions[i] = 1.0/n;
        }
        InstanceList[] splits = instances.splitInOrder(proportions);        
        int tp = 0;
        int fp = 0;
        for (int k = 0; k < n; k++) {                        
            InstanceList trainingInstances = new InstanceList(splits[0].getPipe());
            InstanceList testingInstances = splits[k];
            for (int i = 0; i < n; i++) {
                if (i != k) {
                    trainingInstances.addAll(splits[i]);
                }
            }
            MyClassifierTrainer trainer = new MyClassifierTrainer();
            Classifier classifier = trainer.train(trainingInstances);
            TestResult result = test(classifier, testingInstances);
            System.out.println("Run: " + k);
            System.out.println("TP: " + result.tp);
            System.out.println("FP: " + result.fp);
            tp += result.tp;
            fp += result.fp;
        }
        System.out.println(n+"-" +"Fold CV:");
        System.out.println("TP: " + tp);
        System.out.println("FP: " + fp);
    }
    public void evaluate(String corpusRoot) {
        File iobDir = new File(corpusRoot, "iob");
        List<String> docIds = new ArrayList<String>();
        String[] files = iobDir.list();
        for (String file : files) {
            docIds.add(file.replace(".txt", "")); //get rid of extension
        }
        
        InstanceList instanceList = getInstanceList(corpusRoot, docIds, defaultPipe());
        NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(new MyClassifierTrainer(), instanceList, 10);
    }
    private static class PPIPair {
        int entity1, entity2;
        boolean interact;

        public PPIPair(int entity1, int entity2, boolean interact) {
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.interact = interact;
        }        
    }
    private List<PPIPair> getInteractionPairs(Sentence s) {
        List<PPIPair> pairs = new ArrayList<PPIPair>();
        Map<String, Integer> p1Map = new HashMap<String, Integer>();
        Map<String, Integer> n1Map = new HashMap<String, Integer>();
        
        //find P1 and N1
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token: tokens) {
                    p1Map.put(token, i);
                }
            }
            if (!word.getTag("N1").equals("O")) {
                String[] tokens = word.getTag("N1").split(", ");
                for (String token: tokens) {
                    n1Map.put(token, i);
                }
            }
        }
        
        //find P2 and N2 and match with P1 and N2
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P2").equals("O")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = p1Map.get(token);
                    pairs.add(new PPIPair(entity1, i, true));
                }
            }
            if (!word.getTag("N2").equals("O")) {
                String[] tokens = word.getTag("N2").split(", ");
                for (String token: tokens) {
                    Integer entity1 = n1Map.get(token);
                    pairs.add(new PPIPair(entity1, i, false));
                }
            }
        }
        return pairs;
    }
    
    private void addInstances(String docId, int lineNum, InstanceList instances, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        List<PPIPair> pairs = getInteractionPairs(s);
        for (PPIPair pair : pairs) {
            //if (RULE_EXTRACTOR.check(s, depGraph, ccDepGraph, pair.entity1, pair.entity2) != 2) continue; //filter
            PPIInstance instance = new PPIInstance(s, depGraph, ccDepGraph, docId, lineNum, pair.entity1, pair.entity2, pair.interact);
            instances.addThruPipe(instance);
        }
        /*Set<Pair<Integer, Integer>> goldInteractions = new HashSet<Pair<Integer, Integer>>();
        for (PPIPair pair : pairs) {
            if (pair.interact) {
                goldInteractions.add(new Pair<Integer, Integer>(pair.entity1, pair.entity2));
            }
        }
        Set<Pair<Integer, Integer>> predictedInteractions = TEST_RELEX.getPredictedInteractions(s, ccDepGraph);
        for (Pair<Integer, Integer> pair : predictedInteractions) {
            PPIInstance instance = null;
            if (goldInteractions.contains(pair)) {
                instance = new PPIInstance(s, depGraph, ccDepGraph, docId, lineNum, pair.first(), pair.second(), true);
            } else {
                instance = new PPIInstance(s, depGraph, ccDepGraph, docId, lineNum, pair.first(), pair.second(), false);
            }
            instances.addThruPipe(instance);
        }*/
    }
    
    Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new PPIPipe2());
        //pipes.add(new RelexPipeAIMed2());
        //pipes.add(new TokenSequence2FeatureSequence());
        //pipes.add(new FeatureSequence2FeatureVector());
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    
    private void assignPOS(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            s.get(i).setTag(parent.value());
        }
    }
    public static void test() {
        MLExtractor ex = new MLExtractor();
        String[] a = {"1", "2", "3", "4", "5", "6"};
        List<String> list = new ArrayList<String>(Arrays.asList(a));
        
        ex.shuffle(list);
        for (String s : list) {
            System.out.println(s);
        }
    }
    public static void main(String args[]) {
        MLExtractor extractor = new MLExtractor();
        //extractor.NFoldDocLevelCV("./resource/relation/PPI2/AIMed", 10);
        //extractor.NFoldDocLevelCV("./resource/relation/PPI2/BioInfer", 10);
        //extractor.NFoldDocLevelCV("./resource/relation/PPI2/LLL", 10);
        //extractor.NFoldDocLevelCV("./resource/relation/PPI2/IEPA", 10);
        //extractor.NFoldDocLevelCV("./resource/relation/PPI2/HPRD50", 10);
        extractor.NFoldDocLevelCV("./resource/relation/PPI2/All", 10);
        
        //extractor.evaluate("./resource/relation/PPI/IEPA");        
                  
    }
}
