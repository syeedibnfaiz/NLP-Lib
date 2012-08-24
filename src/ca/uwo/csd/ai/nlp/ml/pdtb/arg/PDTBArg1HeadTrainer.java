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
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBArg1HeadTrainer {
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
    
    public PDTBArg1HeadTrainer() {
        this("./pdtb_v2/piped_data_2", "./pdtb_v2/ptb", "./pdtb_v2/dep2");
        //this("./pdtb_v2/piped_data", "./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep");
    }

    public PDTBArg1HeadTrainer(String pdtbRoot, String ptbRoot, String depRoot) {
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
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);                
        /*NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, trainingInstanceList, 10); */
        InstanceList[] instanceLists =
                trainingInstanceList.splitInOrder(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(instanceLists[0], instanceLists[1]);
        showAccuracy(classifier, instanceLists[1]);
    }
    
    public void train(String[] trainSections, String[] testSections) {
        pipe = defaultPipe();
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        InstanceList testingInstanceList = prepareInstanceList(testSections);
        
        Classifier classifier = trainer.train(trainingInstanceList, testingInstanceList);
        
        System.out.println("training size: " + trainingInstanceList.size());
        System.out.println("testing size: " + testingInstanceList.size());
        
        showAccuracy(classifier, testingInstanceList);
    }
    /**
     * Shows accuracy according to Ben Wellner's definition of accuracy
     * @param classifier
     * @param instanceList 
     */
    private void showAccuracy(Classifier classifier, InstanceList instanceList) {
        MaxEnt maxEnt = (MaxEnt) classifier;
        SimpleDepGraph prevDepGraph = null;
        double bestMax = -1000000.0;
        boolean trueHead = false;
        int total = -1;
        int correct = 0;
        Arg1ConnInstance prevInstance = null;
        int wrongHeadPos = 0;
        for (Instance instance : instanceList) {
            Arg1ConnInstance arg1ConnInstance = (Arg1ConnInstance) instance.getSource();
            SimpleDepGraph curDepGraph = arg1ConnInstance.getDepGraph();
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
            if (prevInstance != null && prevInstance.reln != arg1ConnInstance.reln) {
                total++;
                if (trueHead) {
                    correct++;
                } else {
                    try {
                        //System.out.println(arg2ConnInstance.getTree());
                        Tree t = treeAnalyzer.getPennTree(prevInstance.getTree());
                        String gornAddress = prevInstance.reln.getArg1GornAddress();
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
                        System.out.println("Arg1: " + prevInstance.reln.getArg1RawText());
                        System.out.println("Arg2: " + prevInstance.reln.getArg2RawText());
                        System.out.println("True Arg1Head: " + t.getLeaves().get(headPos));
                        System.out.println("Wrong Arg1Head: " + t.getLeaves().get(wrongHeadPos));
                        //System.out.println(curDepGraph);
                        for (SimpleDependency sd : prevDepGraph) {
                            int g = sd.gov(); int d = sd.dep();
                            System.out.print(sd.reln()+"("+t.getLeaves().get(g).value() + "-"+g + ", " +t.getLeaves().get(d).value() + "-"+d+"), ");
                        }
                        System.out.println(prevDepGraph.getPath(headPos,prevInstance.getConnStart()));
                        System.out.println("---------------");
                    } catch (Exception ex) {
                        Logger.getLogger(PDTBArg1HeadTrainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }                
                bestMax = max;
                String tmpLabel = arg1ConnInstance.getLabel().toString();
                trueHead = tmpLabel.equals("true");
                wrongHeadPos = arg1ConnInstance.getArg1HeadPos();
            } else {
                if (max > bestMax) {
                    bestMax = max;
                    String tmpLabel = arg1ConnInstance.getLabel().toString();
                    trueHead = tmpLabel.equals("true");
                    wrongHeadPos = arg1ConnInstance.getArg1HeadPos();
                }
            }
            prevDepGraph = curDepGraph;
            prevInstance = arg1ConnInstance;
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
                        String connGornAddress = relation.getConnectiveGornAddress();
                        if (connGornAddress.equals("")) continue;
                        GornAddressList connGAList = new GornAddressList(connGornAddress);
                        
                        String gornAddress2 = relation.getArg1GornAddress();
                        if (gornAddress2.equals("") || gornAddress2.contains(";;")) {                            
                            continue;
                        }                        
                        GornAddressList gaList2 = new GornAddressList(gornAddress2);                        
                        
                        boolean sentential2 = true;
                        int lineNumber2 = connGAList.get(0).getLineNumber();
                        for (GornAddress gAddress : gaList2) {
                            if (gAddress.getLineNumber() != lineNumber2) {
                                sentential2 = false;
                                break;
                            }
                        }
                        if (sentential2 && sentential1 && (lineNumber1 == lineNumber2)) {
                            addInstancesThroughPipe(relation, ptbTrees.get(lineNumber2), depGraphs.get(lineNumber2), instanceList);
                        }
                    }
                }
            }
        }
        return instanceList;
    }
    
    private void addInstancesThroughPipe(PDTBRelation relation, Tree root, SimpleDepGraph depGraph, InstanceList instanceList) {        
        String connectiveGornAddress = relation.getConnectiveGornAddress();
        List<Tree> connHeadLeaves = connaAnalyzer.getConnHeadLeaves(root, connectiveGornAddress, relation.getConnHead());
        if (connHeadLeaves.isEmpty()) return;
        
        int connStart = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(0));
        int connEnd = treeAnalyzer.getLeafPosition(root, connHeadLeaves.get(connHeadLeaves.size() - 1));
        
        String arg1GornAddress = relation.getArg1GornAddress();
        List<Tree> gornNodes = treeAnalyzer.getGornNodes(root, arg1GornAddress);
        
        Tree syntacticHead = headAnalyzer.getSyntacticHead(root, gornNodes);
        int headPos = treeAnalyzer.getLeafPosition(root, syntacticHead);
        /*Tree semanticHead = headAnalyzer.getSemanticHead(root, gornNodes);
        int headPos = treeAnalyzer.getLeafPosition(root, semanticHead);*/
        
        String arg2GornAddress = relation.getArg2GornAddress();
        List<Tree> arg2GornNodes = treeAnalyzer.getGornNodes(root, arg2GornAddress);
        /*Tree arg2SemanticHead = headAnalyzer.getSemanticHead(root, arg2GornNodes);
        int arg2HeadPos = treeAnalyzer.getLeafPosition(root, arg2SemanticHead);*/
        Tree arg2SyntacticHead = headAnalyzer.getSyntacticHead(root, arg2GornNodes);
        int arg2HeadPos = treeAnalyzer.getLeafPosition(root, arg2SyntacticHead);
        if (arg2HeadPos == -1) {
            System.out.println("arg2Head == -1");
            return;
        }
        if (headPos == -1) {
            System.out.println("arg1Head == -1");
            return;
        }
        List<Tree> leaves = root.getLeaves();
        /*for (int i = 0; i < leaves.size(); i++) {
            System.out.print(i+"-"+leaves.get(i).value() + ", ");
        }*/
        /*System.out.println("");
        System.out.println("conn: " + relation.getConnRawText());
        System.out.println("arg1: " + relation.getArg1RawText());
        System.out.println("arg2: " + relation.getArg2RawText());
        System.out.println("start: " + connStart);
        System.out.println("end: " + connEnd);
        System.out.println("arg1Head: " + headPos);
        System.out.println("arg2Head: " + arg2HeadPos);
        System.out.println("-----------------------");*/
        String tree = treeAnalyzer.getPennOutput(root);
        String category = connaAnalyzer.getCategory(relation.getConnHead());
        int sz = leaves.size();
        
        int endPos = sz;
        /*if (category.matches("Coord.*")) {
            endPos = connStart;
        }*/
        Tree parentHead = leaves.get(headPos).parent(root);
        /*if (!parentHead.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
            System.out.println("skipping relations");
            return;
        }*/
        for (int i = 0; i < endPos; i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            if (parent.value().matches("VB.*|NN.*|JJ.*|AUX|MD")) {
                Arg1ConnInstance instance;
                if (i == headPos) {
                    instance = new Arg1ConnInstance(tree, depGraph, connStart, connEnd, i, arg2HeadPos, true, relation);
                } else {
                    instance = new Arg1ConnInstance(tree, depGraph, connStart, connEnd, i, arg2HeadPos, false, relation);
                }
                instanceList.addThruPipe(instance);
            }
        }
    }
    
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Arg1ConnInstance2FeatureVector());
        //pipes.add(new PrintFeatureVector());
        return new SerialPipes(pipes);
    }
    
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    
    public static void main(String args[]) {
        PDTBArg1HeadTrainer trainer = new PDTBArg1HeadTrainer();
        trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"biodrb"});
        //trainer.save("./resource/ml/models/pdtb/pdtb_arg1.model");
    }
}
