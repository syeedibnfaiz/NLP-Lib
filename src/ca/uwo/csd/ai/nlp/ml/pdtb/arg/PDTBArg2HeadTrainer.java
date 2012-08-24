/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddress;
import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddressList;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ml.crf.NFoldEvaluator;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBArg2HeadTrainer {
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
    ConnectiveAnalyzer connaAnalyzer;
    int count;
    public PDTBArg2HeadTrainer() {
        //this("./pdtb_v2/piped_data_2", "./pdtb_v2/ptb", "./pdtb_v2/dep2");
        this("./pdtb_v2/piped_data", "./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep");
    }

    public PDTBArg2HeadTrainer(String pdtbRoot, String ptbRoot, String depRoot) {
        this.pdtbRoot = new File(pdtbRoot);
        this.ptbRoot = new File(ptbRoot);
        this.depRoot = new File(depRoot);
                        
        trainer = new MyClassifierTrainer();
        pipedFileReader = new PDTBPipedFileReader();
        ptbFileReader = new PTBFileReader();
        depFileReader = new SimpleDepFileReader();
        
        treeAnalyzer = new SyntaxTreeAnalyzer();
        headAnalyzer = new HeadAnalyzer();
        connaAnalyzer = new ConnectiveAnalyzer();
    }
    public void train(String[] trainSections) {
        pipe = defaultPipe();
        count = 0;
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        System.out.println("count: " + count);
        
        /*NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, trainingInstanceList, 10);*/
        
        InstanceList[] instanceLists =
                trainingInstanceList.splitInOrder(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(instanceLists[0], instanceLists[1]);
        showAccuracy(classifier, instanceLists[1]);
        
    }
    public void train(String[] trainSections, String[] testSections) {
        pipe = defaultPipe();
        count = 0;
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        System.out.println("count: " + count);
        count = 0;
        InstanceList testingInstanceList = prepareInstanceList(testSections);
        System.out.println("count: " + count);
        Classifier classifier = trainer.train(trainingInstanceList, testingInstanceList);
        
        System.out.println("training size: " + trainingInstanceList.size());
        System.out.println("testing size: " + testingInstanceList.size());
        
        showAccuracy(classifier, testingInstanceList);
    }
    
    private void showAccuracy(Classifier classifier, InstanceList instanceList) {
        MaxEnt maxEnt = (MaxEnt) classifier;
        SimpleDepGraph prevDepGraph = null;
        double bestMax = -1000000.0;
        boolean trueHead = false;
        int total = -1;
        int correct = 0;
        Arg2ConnInstance prevInstance = null;
        int wrongHeadPos = 0;
        for (Instance instance : instanceList) {
            Arg2ConnInstance arg2ConnInstance = (Arg2ConnInstance) instance.getSource();
            SimpleDepGraph curDepGraph = arg2ConnInstance.getDepGraph();
            Classification classification = classifier.classify(instance);
            Labeling labeling = classification.getLabeling();
            String label = labeling.labelAtLocation(labeling.getBestIndex()).toString();
            
            double score[] = new double[2];
            maxEnt.getClassificationScores(instance, score);
            double max;
            if (label.equals("true")) {
                max = Math.max(score[0], score[1]);
            } else {
                max = Math.min(score[0], score[1]) /*- 0.5*/;                                
            }
            //if (curDepGraph != prevDepGraph) {
            if (prevInstance != null && prevInstance.reln != arg2ConnInstance.reln) {
                total++;
                if (trueHead) {
                    correct++;
                } else {
                    try {
                        //System.out.println(arg2ConnInstance.getTree());
                        Tree t = treeAnalyzer.getPennTree(prevInstance.getTree());
                        String gornAddress = prevInstance.reln.getArg2GornAddress();
                        System.out.println(gornAddress);
                        System.out.println("sec: " + prevInstance.reln.getSectionNumber());
                        System.out.println("file: " + prevInstance.reln.getFileNumber());
                        List<Tree> gornNodes = treeAnalyzer.getGornNodes(t, gornAddress);
                        Tree syntacticHead = headAnalyzer.getSyntacticHead(t, gornNodes);
                        System.out.println(syntacticHead.value());
                        int headPos = treeAnalyzer.getLeafPosition(t, syntacticHead);                       
                        t.pennPrint();
                        List<Tree> paths = t.pathNodeToNode(t.getLeaves().get(prevInstance.getConnStart()), t.getLeaves().get(headPos));
                        for (Tree tmp : paths) {
                            System.out.print(tmp.value()+":");
                        }
                        System.out.println("");
                        System.out.println(treeAnalyzer.toString(t));
                        System.out.println("Arg1: " + prevInstance.reln.getArg1RawText());
                        System.out.println("Arg2: " + prevInstance.reln.getArg2RawText());
                        System.out.println("True Arg2Head: " + t.getLeaves().get(headPos));
                        System.out.println("Wrong Arg2Head: " + t.getLeaves().get(wrongHeadPos));
                        //System.out.println(curDepGraph);
                        for (SimpleDependency sd : prevDepGraph) {
                            int g = sd.gov(); int d = sd.dep();
                            System.out.print(sd.reln()+"("+t.getLeaves().get(g).value() + "-"+g + ", " +t.getLeaves().get(d).value() + "-"+d+"), ");
                        }
                        System.out.println(prevDepGraph.getPath(headPos,prevInstance.getConnStart()));
                        System.out.println("---------------");
                    } catch (Exception ex) {
                        Logger.getLogger(PDTBArg2HeadTrainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }                
                bestMax = max;
                trueHead = (Boolean) arg2ConnInstance.getLabel();
                wrongHeadPos = arg2ConnInstance.getHeadPos();
            } else {
                if (max > bestMax) {
                    bestMax = max;
                    trueHead = (Boolean) arg2ConnInstance.getLabel();
                    wrongHeadPos = arg2ConnInstance.getHeadPos();
                }
            }
            prevDepGraph = curDepGraph;
            prevInstance = arg2ConnInstance;
        }
        System.out.println("Total: " + total);
        System.out.println("Correct: " + correct);
        System.out.println("Accuracy: " + (1.0*correct)/total);
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
                        String gornAddress1 = relation.getArg2GornAddress();
                        if (gornAddress1.equals("") || gornAddress1.contains(";;")) {                            
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
                        String gornAddress2 = relation.getArg1GornAddress();
                        if (gornAddress2.equals("") || gornAddress2.contains(";;")) {                            
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
                        if (sentential1 /*&& sentential2 && (lineNumber1 == lineNumber2)*/) {
                            addInstancesThroughPipe(relation, ptbTrees.get(lineNumber1), depGraphs.get(lineNumber1), instanceList);
                            count++;
                        }
                    }
                }
            }
        }
        return instanceList;
    }
    
    private void addInstancesThroughPipe(PDTBRelation relation, Tree root, SimpleDepGraph depGraph, InstanceList instanceList) {        
        String connectiveGornAddress = relation.getConnectiveGornAddress();
        //System.out.println("connGornAdd: "+connectiveGornAddress);
        List<Tree> connHeadLeaves = connaAnalyzer.getConnHeadLeaves(root, connectiveGornAddress, relation.getConnHead());
        if (connHeadLeaves.isEmpty()) return;
        
        int connStart = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(0));
        int connEnd = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(connHeadLeaves.size() - 1));
        
        String arg2GornAddress = relation.getArg2GornAddress();
        List<Tree> gornNodes = treeAnalyzer.getGornNodes(root, arg2GornAddress);
        
        Tree syntacticHead = headAnalyzer.getSyntacticHead(root, gornNodes);
        int headPos = treeAnalyzer.getLeafPosition(root, syntacticHead);
        /*Tree semanticHead = headAnalyzer.getSemanticHead(root, gornNodes);
        int headPos = treeAnalyzer.getLeafPosition(root, semanticHead);*/
        
        List<Tree> leaves = root.getLeaves();
        /*for (int i = 0; i < leaves.size(); i++) {
            System.out.print(i+"-"+leaves.get(i).value() + ", ");
        }
        System.out.println("");
        System.out.println("conn: " + relation.getConnRawText());
        System.out.println("arg2: " + relation.getArg2RawText());
        System.out.println("start: " + connStart);
        System.out.println("end: " + connEnd);
        System.out.println("head: " + headPos);
        System.out.println("-----------------------");*/
        if (headPos == -1) {
            System.out.println("Headpos == -1");
            System.out.println("Arg2GA: " + arg2GornAddress);
            System.out.println("GornNodes==null: " + (gornNodes == null));
            //root.pennPrint();            
            return;
        }
        //if (1 < 2) return;
        /*Tree parentHead = leaves.get(headPos).parent(root);
        if (!parentHead.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
            System.out.println("skipping relations");
            return;
        }*/
        String tree = treeAnalyzer.getPennOutput(root);
        String category = connaAnalyzer.getCategory(relation.getConnHead());
        int initialPos = 0;
        if (category.matches("Sub.*|Coord.*")) {
            initialPos = connEnd + 1;
        }
        int sz = leaves.size();
        for (int i = initialPos; i < sz; i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            if (parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                Arg2ConnInstance instance;
                if (i == headPos) {
                    instance = new Arg2ConnInstance(tree, depGraph, connStart, connEnd, i, true, relation);
                } else {
                    instance = new Arg2ConnInstance(tree, depGraph, connStart, connEnd, i, false, relation);
                }
                instanceList.addThruPipe(instance);                
            }
        }
    }
    
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Arg2ConnInstance2FeatureVector());
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    
    public static void main(String args[]) {
        PDTBArg2HeadTrainer trainer = new PDTBArg2HeadTrainer();
        trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"biodrb"});
        //trainer.train(new String[]{"biodrb"});
        //trainer.save("./resource/ml/models/pdtb/pdtb_arg2.model");
    }
}
