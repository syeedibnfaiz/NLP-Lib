package ca.uwo.csd.ai.nlp.ml;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddress;
import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddressList;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldTTest;
import ca.uwo.csd.ai.nlp.utils.Weka;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBNewConnectiveSenseTrainer {
    MyClassifierTrainer trainer;
    File pdtbRoot;
    File ptbRoot;
    File depRoot;
    
    PDTBPipedFileReader pipedFileReader;
    PTBFileReader ptbFileReader;
    SimpleDepFileReader depFileReader;
    Pipe pipe;
    SyntaxTreeAnalyzer treeAnalyzer;
    HeadAnalyzer headAnalyzer;
    ConnectiveAnalyzer connAnalyzer;
    int count;
    
    int headDisagreement;
    public PDTBNewConnectiveSenseTrainer() {
        //this("./pdtb_v2/piped_data_2", "./pdtb_v2/ptb", "./pdtb_v2/dep2");
        //gs
        //this("./pdtb_v2/piped_data", "./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep");
        //auto
        this("./package/treebank_3/parsed/mrg/pspdtb", "./package/treebank_3/parsed/mrg/psptb", "./package/treebank_3/parsed/mrg/psdep");
        //biodrb
        //this("./resource/ml/data/biodrb/root/ann", "./resource/ml/data/biodrb/root/parses", "./resource/ml/data/biodrb/root/dep");
        //modified base connectives
        //this("./resource/ml/data/biodrb/root/ann2", "./resource/ml/data/biodrb/root/parses", "./resource/ml/data/biodrb/root/dep");
    }

    public PDTBNewConnectiveSenseTrainer(String pdtbRoot, String ptbRoot, String depRoot) {
        this.pdtbRoot = new File(pdtbRoot);
        this.ptbRoot = new File(ptbRoot);
        this.depRoot = new File(depRoot);
                        
        trainer = new MyClassifierTrainer();
        pipedFileReader = new PDTBPipedFileReader();
        ptbFileReader = new PTBFileReader();
        depFileReader = new SimpleDepFileReader();
        
        treeAnalyzer = new SyntaxTreeAnalyzer();
        headAnalyzer = new HeadAnalyzer();
        
        connAnalyzer = new ConnectiveAnalyzer();
        //biodrb
        //connAnalyzer = new ConnectiveAnalyzer("./resource/ml/data/biodrb/connective_types");
    }
    
    public void train(String[] trainSections) {
        pipe = defaultPipe();
        count = 0;
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        System.out.println("count: " + count);
        
        //trainingInstanceList = PruneFeatureVectors.pruneByInfoGain(trainingInstanceList, 7000);
        trainingInstanceList = PruneFeatureVectors.pruneByCount(trainingInstanceList, 2);
        /*NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, trainingInstanceList, 10);*/
        
        /*InstanceList[] instanceLists =
                trainingInstanceList.splitInOrder(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(instanceLists[0]);
        showAccuracy(classifier, instanceLists[1]);*/
        
        //showNFoldMacroAccuracy(trainingInstanceList, 10);
        
        
        //trainer.train(trainingInstanceList);
        System.out.println("Feature size: " + trainingInstanceList.getDataAlphabet().size());
        //Weka.convert2ARFF(trainingInstanceList, "biodrb_sense.arff", "BioDRB Sense Classification");
        //showNFoldTypeSpecificAccuracy(trainingInstanceList, 10);
        
        NFoldTTest tTest = new NFoldTTest();
        tTest.evaluate(trainingInstanceList, 10);
    }
    public void train(String[] trainSections, String[] testSections) {
        pipe = defaultPipe();
        count = 0;
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        System.out.println("count: " + count);
        count = 0;
        InstanceList testingInstanceList = prepareInstanceList(testSections);
        System.out.println("count: " + count);
        //Classifier classifier = trainer.train(trainingInstanceList, testingInstanceList);
        
        Classifier classifier = trainer.train(trainingInstanceList);
        
        System.out.println("training size: " + trainingInstanceList.size());
        System.out.println("testing size: " + testingInstanceList.size());
        System.out.println("Head disagreement: " + headDisagreement);
        
        showAccuracy(classifier, testingInstanceList);
        
        //getTypeSpecificAccuracy(trainingInstanceList, testingInstanceList, true);
    }        
            
    private void showAccuracy(Classifier classifier, InstanceList instanceList) {
        int total = instanceList.size();
        int correct = 0;        
        for (Instance instance : instanceList) {            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) correct++;            
        }
        System.out.println("Total: " + total);
        System.out.println("Correct: " + correct);
        System.out.println("Accuracy: " + (1.0*correct)/total);
    }
    
    
    private void showNFoldMacroAccuracy(InstanceList instanceList, int n) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(n);
        double accuracies[] = new double[n];
        double accuracy = 0;
        int run = 0;
        while (cvIt.hasNext()) {
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            
            trainer = new MyClassifierTrainer(new MaxEntTrainer());
            Classifier classifier = trainer.train(trainingInstances);
            accuracies[run] = getAccuracy(classifier, testingInstances, map);
            accuracy += accuracies[run];
            run++;
        }
        System.out.println(n + "-Fold accuracy(avg): " + accuracy/n);
        showMap(map);
    }
    
    double getAccuracy(Classifier classifier, InstanceList instanceList, Map<String, Integer> map) {
        int total = instanceList.size();
        int correct = 0;        
        for (Instance instance : instanceList) {            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) {
                correct++;                                
                /*String bestLabel = classification.getLabeling().getBestLabel().toString();
                String key = bestLabel + "-" + bestLabel;
                if (map.containsKey(key)) {
                    map.put(key, map.get(key) + 1);
                } else {
                    map.put(key, 1);
                }*/
            }
            else {
                //PDTBConnectiveSenseInstance senseInstance = (PDTBConnectiveSenseInstance) instance;
                //PDTBRelation relation = senseInstance.getRelation();
                String bestLabel = classification.getLabeling().getBestLabel().toString();
                //String secondSense = relation.getSense2();
                String secondSense = instance.getSource().toString();
                if (secondSense.contains(".")) {
                    secondSense = secondSense.substring(0, secondSense.indexOf('.'));
                }
                //secondSense = convertBioDRBSense2PDTBSense(secondSense);
                if (bestLabel.equals(secondSense)) {
                    correct++;
                    //System.out.println(relation.getSense()+", "+relation.getSense2());
                    //System.out.println(senseInstance.sentence);
                    /*String key = bestLabel + "-" + bestLabel;                    
                    if (map.containsKey(key)) {
                        map.put(key, map.get(key) + 1);
                    } else {
                        map.put(key, 1);                        
                    }*/
                } else {
                    //String key = senseInstance.label.toString() + "-" + bestLabel + "-" + relation.getConnHead();
                    //String key = relation.getConnHead() + "-" + senseInstance.label.toString();
                    //if (map.containsKey(key)) {
                    //    map.put(key, map.get(key) + 1);
                    //} else {
                    //    map.put(key, 1);
                        /*System.out.println(key);
                        System.out.println("Sentence:"+senseInstance.sentence);
                        System.out.println("Connective: " + senseInstance.sentence.toString(senseInstance.s, senseInstance.e));
                        System.out.println("Arg1: " + relation.getArg1RawText());
                        System.out.println("Arg2: " + relation.getArg2RawText());*/
                    //}
                }
            }
        }        
        return (1.0*correct)/total;
    }
    private void showMap(Map<String, Integer> map) {
        Set<Entry<String, Integer>> entrySet = map.entrySet();
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(entrySet);
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return -1;
                } else if (o1.getValue() < o2.getValue()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        for (Entry<String, Integer> e : list) {
            System.out.println(e.getKey() + "-" + e.getValue());
        }
    }
    private InstanceList prepareInstanceList(String[] sections) {
        InstanceList instanceList = new InstanceList(pipe);
        //prepare training data
        for (String section : sections) {
            File pdtbSection = new File(pdtbRoot, section);
            File[] pdtbFiles = pdtbSection.listFiles(new FileExtensionFilter(".pipe"));
            for (File pdtbFile : pdtbFiles) {
                File ptbFile = new File(ptbRoot, section + "/" + pdtbFile.getName().replace(".pipe", ".mrg"));
                File depFile = new File(depRoot, section + "/" + pdtbFile.getName().replace(".pipe", ".dep"));
                List<PDTBRelation> relations = pipedFileReader.read(pdtbFile);
                List<Tree> ptbTrees = ptbFileReader.read(ptbFile);
                List<SimpleDepGraph> depGraphs = depFileReader.read(depFile);                
                
                for (PDTBRelation relation : relations) {
                    if (relation.getType().equals("Explicit")) {
                        String gornAddress1 = relation.getArg1GornAddress();
                        
                        if (gornAddress1.equals("") || gornAddress1.contains(";;")) {
                            System.out.println("Empty gornAddress1");
                            System.out.println(relation);
                            continue;
                        }                        
                        GornAddressList gaList1 = new GornAddressList(gornAddress1);                        
                        
                        boolean sentential1 = true;
                        int lineNumber1 = gaList1.get(0).getLineNumber();
                        for (GornAddress gAddress : gaList1) {
                            if (gAddress.getLineNumber() != lineNumber1) {
                                sentential1 = false;
                                break;
                            }
                        }
                        String gornAddress2 = relation.getArg2GornAddress();
                        
                        if (gornAddress2.equals("") || gornAddress2.contains(";;")) {                            
                            System.out.println("Empty gornAddress2");
                            System.out.println(relation);
                            continue;
                        }                        
                        GornAddressList gaList2 = new GornAddressList(gornAddress2);                        
                        
                        boolean sentential2 = true;
                        int lineNumber2 = gaList2.get(0).getLineNumber();
                        for (GornAddress gAddress : gaList2) {
                            if (gAddress.getLineNumber() != lineNumber2) {
                                sentential2 = false;
                                break;
                            }
                        }
                        if (true /*&& sentential2 && (lineNumber1 == lineNumber2)*/) {
                            addInstancesThroughPipe(relation, ptbTrees.get(lineNumber2), depGraphs.get(lineNumber2), instanceList);
                            count++;
                        }
                    }
                }
            }
        }
        return instanceList;
    }
    private String convertBioDRBSense2PDTBSense(String bioDRBSense) {
        if (bioDRBSense.matches("Concession|Contrast")) {
            return "Comparison";
        } else if (bioDRBSense.matches("Cause|Condition|Purpose")) {
            return "Contingency";
        } else if (bioDRBSense.matches("Temporal")) {
            return "Temporal";
        } else {
            return "Expansion";
        }
    }
    private void addInstancesThroughPipe(PDTBRelation relation, Tree root, SimpleDepGraph depGraph, InstanceList instanceList) {        
        String connectiveGornAddress = relation.getConnectiveGornAddress();
        //System.out.println("connGornAdd: "+connectiveGornAddress);
        List<Tree> connHeadLeaves = connAnalyzer.getConnHeadLeaves(root, connectiveGornAddress, relation.getConnHead());
        if (connHeadLeaves.isEmpty()) {
            System.out.println("Connective head leaves empty!");
            return;
        }
        
        int connStart = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(0));
        int connEnd = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(connHeadLeaves.size() - 1));
        if ((connEnd - connStart) > 4) { //handle if..else, etc.
            connEnd = connStart;
        }
        
        String arg2GornAddress = relation.getArg2GornAddress();
        //List<Tree> gornNodes = treeAnalyzer.getGornNodes(root, arg2GornAddress);
        List<Tree> gornNodes = getArgGornNodes(root, arg2GornAddress);
        
        Tree syntacticHead = headAnalyzer.getSyntacticHead(root, gornNodes);
        if (syntacticHead == null) {
            System.out.println("Caught null head!");
            return;
        }
        int headPos = treeAnalyzer.getLeafPosition(root, syntacticHead);                                
        if (headPos == -1) {
            System.out.println("Headpos == -1");
            System.out.println("Arg2GA: " + arg2GornAddress);
            System.out.println("GornNodes==null: " + (gornNodes == null));
            //root.pennPrint();            
            return;
        }        
        
        String sense = relation.getSense();
        if (sense.contains(".")) {
            sense = sense.substring(0, sense.indexOf('.'));
        }
        //sense = convertBioDRBSense2PDTBSense(sense);
        
        Sentence s = new Sentence(root);        
        PDTBConnectiveSenseInstance instance = new PDTBConnectiveSenseInstance(s, connStart, connEnd, headPos, sense, depGraph, relation);
        instanceList.addThruPipe(instance);        
    }
    
    private String getConnString(Tree root, int connStart, int connEnd) {
        List<Tree> leaves = root.getLeaves();
        StringBuilder sb = new StringBuilder();
        for (int i = connStart; i <= connEnd; i++) {
            Tree leaf = leaves.get(i);
            if (sb.length() != 0) sb.append(" ");
            sb.append(leaf.value());
        }
        return sb.toString();
    }
    
    List<Tree> getArgGornNodes(Tree root, String gornAddress) {        
        String tokens[] = gornAddress.split(";");
        int lineNum = -1;
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            int line = Integer.parseInt(token.split(",")[0]);
            if (lineNum == -1) lineNum = line;
            else if (line != lineNum) continue;
            sb.append(token+";");
        }
        return treeAnalyzer.getGornNodes(root, sb.toString());
    }
    
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new PDTBConnectiveSense2FeatureVector());
        //biodrb
        //pipes.add(new PDTBConnectiveSense2FeatureVector("./resource/ml/data/biodrb/connective_types"));        
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    
    public static void main(String args[]) {
        PDTBNewConnectiveSenseTrainer trainer = new PDTBNewConnectiveSenseTrainer();
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"biodrb"});
        
        trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"});
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"00", "01"});
        //biodrb
        //trainer.train(new String[]{"00"});
        //trainer.save("./resource/ml/models/pdtb/pdtb_sense.model");
        //trainer.save("./resource/ml/models/biodrb/biodrb_sense.model");
    }
}
