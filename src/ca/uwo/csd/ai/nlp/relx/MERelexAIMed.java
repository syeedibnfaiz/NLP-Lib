/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.relx;

import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ling.ann.BLLIPParser;
import ca.uwo.csd.ai.nlp.ling.ann.GeniaTagger;
import ca.uwo.csd.ai.nlp.ml.PrintFeatureVector;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import ca.uwo.csd.ai.nlp.utils.CharniakClient;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
public class MERelexAIMed {
    final private GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
    final private PTBFileReader TREE__READER = new PTBFileReader();
    final private SimpleDepFileReader DEP_READER = new SimpleDepFileReader();
    final static GeniaTagger TAGGER = null;//new GeniaTagger();
    //final static ParserAnnotator PARSER_ANNOTATOR = new ParserAnnotator(new String[]{"-retainTmpSubcategories"});
    
    MyClassifierTrainer trainer;
    Pipe pipe;

    public MERelexAIMed() {
        trainer = new MyClassifierTrainer();        
    }
    
    public void train(String iobRootPath, String parseRootPath, String depRootPath, String depCCRootPath) {
        
        File rootDir = new File(iobRootPath);
        File parseRootDir = new File(parseRootPath);
        File depRootDir = new File(depRootPath);
        File depCCRootDir = new File(depCCRootPath);
        File[] files = rootDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE", "P1", "P2"});
        PTBFileReader ptbReader = new PTBFileReader();
        SimpleDepFileReader depReader = new SimpleDepFileReader();
        
        InstanceList instanceList = new InstanceList(defaultPipe());
        
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
            Text text = reader.read(file);
            File parseFile = new File(parseRootDir, file.getName() + ".mrg");
            File depFile = new File(depRootDir, file.getName() + ".dep");
            File depCCFile = new File(depCCRootDir, file.getName() + ".dep");
            List<Tree> trees = ptbReader.read(parseFile);
            List<SimpleDepGraph> depGraphs = depReader.read(depFile);
            List<SimpleDepGraph> depGraphsCC = depReader.read(depCCFile);
            for (int i = 0; i < text.size(); i++) {
                Sentence s = text.get(i);
                System.out.println(s);
                SimpleDepGraph depGraph = depGraphs.get(i);
                SimpleDepGraph depGraphCC = depGraphsCC.get(i);
                Tree root = trees.get(i);
                root.setValue("ROOT");
                
                try {
                    //s = TAGGER.annotate(s);                      
                    s.setParseTree(trees.get(i));                    
                    s = setPOSTags(s);
                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex.getMessage());
                    continue;
                }
                addThroughPipe(instanceList, s, depGraph, depGraphCC);
            }
        }   
        /*InstanceList[] instanceLists =
                    instanceList.splitInOrder(new double[]{0.6, 0.4});
            Classifier classifier = trainer.train(instanceLists[0], instanceLists[1]);*/                        
        
        NFoldEvaluator evaluator = new NFoldEvaluator();
        //evaluator.evaluate(trainer, instanceList, 10);
        evaluator.evaluateInOrder(trainer, instanceList, 10);
        
        //trainer.train(instanceList);
        
        //debug(instanceList);
        //Weka.convert2ARFF(instanceList, "bioinfer_weka.arff", "Bioinfer Relation Extraction");        
        
        int positiveCount = 0;
        for (Instance instance : instanceList) {
            boolean b = Boolean.parseBoolean(instance.getTarget().toString());
            if (b) positiveCount++;
        }
        System.out.println("Positive: " + positiveCount);
        System.out.println("Negative: " + (instanceList.size() - positiveCount));
    }
    
    void debug(InstanceList instanceList) {
        InstanceList[] lists = instanceList.split(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(lists[0]);
        Pipe pipe = defaultPipe();
        for (Instance instance : lists[1]) {
            Classification classification = classifier.classify(instance);
            if (!classification.bestLabelIsCorrect()) {
                RelexInstance relexInstance = (RelexInstance) instance;
                if (relexInstance.relation) {
                    System.out.println("======");
                    System.out.println(relexInstance.s.get(relexInstance.entity1));
                    System.out.println(relexInstance.s.get(relexInstance.entity2));
                    System.out.println(relexInstance.s);
                    System.out.println(relexInstance);
                    System.out.println(relexInstance.depGraph.getPathAsList(relexInstance.entity1, relexInstance.entity2, false));
                    
                    RelexInstance newInstance = new RelexInstance(null, relexInstance.s, relexInstance.depGraph, relexInstance.depGraphCC, relexInstance.entity1, relexInstance.entity2, true);
                    Instance carrier = pipe.instanceFrom(newInstance);
                    FeatureVector fv = (FeatureVector) carrier.getData();                    
                }
            }
        }
    }
    void addThroughPipe(InstanceList instanceList, Sentence s, SimpleDepGraph depGraph, SimpleDepGraph depGraphCC) {
        Set<String> relations = new HashSet<String>();
        Map<String, Integer> p1PairToIndex = new HashMap<String, Integer>();
        for (int i = 0; i < s.size(); i++) {
            TokWord word = s.get(i);
            if (!word.getTag("P1").equals("O") && word.getTag("LEXE").equals("B")) {
                String[] tokens = word.getTag("P1").split(", ");
                for (String token : tokens) {
                    p1PairToIndex.put(token, i);
                }
            }
            if (!word.getTag("P2").equals("O") && word.getTag("LEXE").equals("B")) {
                String[] tokens = word.getTag("P2").split(", ");
                for (String token : tokens) {
                    if (p1PairToIndex.containsKey(token)) {
                        int first = p1PairToIndex.get(token);
                        relations.add(first + "-" + i);
                    } else {
                        System.out.println("WTF!!");
                    }
                }
            }
        }
        
        //consider all pairs of known entity
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).getTag("LEXE").equals("B")) {
                for (int j = i + 1; j < s.size(); j++) {                    
                    if (s.get(j).getTag("LEXE").equals("B")) {
                        String signature = i + "-" + j;
                        if (relations.contains(signature)) {
                            System.out.println("True: " + getEntity(s, i) + " - " + getEntity(s, j));
                            //System.out.println(s.toString());
                            //System.out.println(depGraph.getPath(i, j));                            
                            instanceList.addThruPipe(new RelexInstance(null, s, depGraph, depGraphCC, i, j, true));
                        } else {
                            System.out.println("False: " + getEntity(s, i) + " - " + getEntity(s, j));
                            instanceList.addThruPipe(new RelexInstance(null, s, depGraph, depGraphCC, i, j, false));
                        }
                    }
                }
            }
        }
    }
    private InstanceList getInstanceList(String corpusRoot, List<String> docIds, Pipe pipe) {
        InstanceList instances = new InstanceList(pipe);
        for (String docId : docIds) {
            File iobFile = new File(corpusRoot + "/iob", docId + ".txt");
            File treeFile = new File(corpusRoot + "/trees", docId + ".txt.mrg");
            File depFile = new File(corpusRoot + "/deps", docId + ".txt.dep");
            File depCCFile = new File(corpusRoot + "/depsCC", docId + ".txt.dep");
            
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
                s.getParseTree().setValue("ROOT");
                s = setPOSTags(s);                
                addThroughPipe(instances, s, depGraph, ccDepGraph);
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
            RelexInstance relexInstance = (RelexInstance) instance;
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) {
                if (relexInstance.relation) {
                    tp++;
                } else {
                    tn++;
                }
            } else {
                if (relexInstance.relation) {
                    fn++;
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
        shuffle(docIds);
        
        TestResult[] results = new TestResult[N];
        int size = docIds.size()/N;
        int begin = 0;
        int end = size-1;
        for (int i = 1; i <= N; i++) { //N-fold
            List<String> trainDocIds = new ArrayList<String>();
            List<String> testDocIds = new ArrayList<String>();
            
            for (int j = 0; j < docIds.size(); j++) {
                if (j >= begin && j <= end) {
                    testDocIds.add(docIds.get(j));
                } else {
                    trainDocIds.add(docIds.get(j));
                }                                
            }
            
            Classifier classifier = train(corpusRoot, trainDocIds);
            results[i-1] = test(classifier, corpusRoot, testDocIds);
            
            //update
            begin = end + 1;
            end += size;
            if (i == (N - 1)) {
                end = docIds.size();
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
        System.out.println("tp+fn: " + (tp + fn));        
        
        double precision = 1.0 * tp/(tp + fp);
        double recall = 1.0 * tp/(tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);
        System.out.println("Result of " + N + "-fold CV:");
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-Score: " + fscore);
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
    
    Sentence setPOSTags(Sentence s) {
        Tree root = s.getParseTree();
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < s.size() && i < leaves.size(); i++) {
            Tree parent = leaves.get(i).parent(root);
            s.get(i).setTag("POS", parent.value());
        }
        return s;
    }
    
    private int getEntityStartPos(Sentence s, int pos) {        
        while (pos > 0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            System.out.println("Returning -1!");
            return -1;
        }
        return pos;
    }
    private String getEntity(Sentence s, int pos) {        
        while (pos >0 && s.get(pos).getTag("LEXE").equals("I")) {
            pos--;
        }
        if (!s.get(pos).getTag("LEXE").equals("B")) {
            return null;
        }
        int start = pos;
        int end = start;
        pos++;
        while (pos < s.size() && s.get(pos).getTag("LEXE").equals("I")) {
            end = pos;
            pos++;
        }
        return s.toString(start, end);
    }
    
    public Sentence correctTokenization(Sentence s) {
        Sentence newS = new Sentence();
        for (int i = 0; i < s.size(); i++) {
            String word = s.get(i).word();
            if (word.startsWith("(") && word.endsWith(")")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else if (word.startsWith("(") && !word.equals("(")) {
                newS.add(new TokWord("("));
                newS.add(new TokWord(word.substring(1)));
            } else if (word.endsWith(")") && !word.equals(")") && !word.contains("(")) {                
                newS.add(new TokWord(word.substring(0, word.length() - 1)));
                newS.add(new TokWord(")"));
            } else {
                newS.add(new TokWord(word));
            }
        }
        return newS;
    }
    
    Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new RelexPipeAIMed());
        //pipes.add(new RelexPipeAIMedMinipar());
        pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }    
    
    public void bioNLPtest(String inRoot, String outRoot) throws IOException {
        BLLIPParser parser = new BLLIPParser();
        File inDir = new File(inRoot);
        File outDir = new File(outRoot);
        outDir.mkdir();
        File[] files = inDir.listFiles();
        GenericTextReader reader = new GenericTextReader("\n\n", "\n", "\t", new String[]{"Word", "LEXE"});
        Classifier classifier = trainer.getClassifier();
        for (File file : files) {
            //File file = new File("C:/Users/tonatuni/Downloads/BioNLP2011/BioNLP2011-SharedTask- (1).txt.iob");
            FileWriter writer1 = new FileWriter(new File(outDir, file.getName() + ".rel"));
            FileWriter writer2 = new FileWriter(new File(outDir, file.getName() + ".norel"));            
            //CharniakClient charniakClient = new CharniakClient();
            //SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
            Text text = reader.read(file);            
            for (Sentence s : text) {
                int count = 0;
                for (TokWord word : s) {
                    if (word.getTag("LEXE").startsWith("B")) {
                        count++;
                    }
                    if (count > 2) {
                        try {
                            //String treeStr = charniakClient.parse(s.toString());
                            //Tree root = treeAnalyzer.getPennTree(treeStr);
                            //s.setParseTree(root);
                            s = parser.annotate(s);
                            s = setPOSTags(s);
                            SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), null);
                            SimpleDepGraph ccDepGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
                            boolean flg = false;
                            for (int i = 0; i < s.size() && !flg; i++) {
                                if (s.get(i).getTag("LEXE").startsWith("B")) {
                                    for (int j = i + 1; j < s.size(); j++) {
                                        if (s.get(j).getTag("LEXE").startsWith("B")) {
                                            RelexInstance instance = new RelexInstance(null, s, depGraph, ccDepGraph, i, j, true);
                                            Pipe pipe = classifier.getInstancePipe();
                                            instance = (RelexInstance) pipe.instanceFrom(instance);
                                            Classification classification = classifier.classify(instance);
                                            if (classification.bestLabelIsCorrect()) {
                                                flg = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (flg) {
                                writer2.write("\n\n");
                                writer1.write(s.toString() + "\n");
                            } else {
                                writer1.write("\n\n");
                                writer2.write(s.toString() + "\n");
                            }
                        } catch(Exception ex) {
                            writer1.write("\n\n");
                            writer2.write("\n\n");
                        }
                    } else {
                        writer1.write("\n\n");
                        writer2.write(s.toString() + "\n");
                    }
                }
            }
            writer1.close();
            writer2.close();
        }
    }
    public static void main(String args[]) throws IOException {
        MERelexAIMed mERelex = new MERelexAIMed();
        mERelex.train("./resource/relation/aimed/iob", "./resource/relation/aimed/trees", "./resource/relation/aimed/deps", "./resource/relation/aimed/depsCC");
        //mERelex.train("./resource/relation/aimed/iob", "./resource/relation/aimed/trees", "./resource/relation/aimed/minipar_dep", "./resource/relation/aimed/depsCC");
        //mERelex.train("./resource/relation/HPRD50/iob", "./resource/relation/HPRD50/trees", "./resource/relation/HPRD50/deps", "./resource/relation/HPRD50/depsCC");
        //mERelex.train("./resource/relation/bioinfer/iob", "./resource/relation/bioinfer/trees", "./resource/relation/bioinfer/deps", "./resource/relation/bioinfer/depsCC");

        //mERelex.NFoldDocLevelCV("./resource/relation/HPRD50", 10);
        //mERelex.evaluate("./resource/relation/HPRD50");
        //mERelex.bioNLPtest("bionlpin", "bionlpout");
    }
}
