package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddress;
import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddressList;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.RankMaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBArg2HeadRankerTrainer {
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
    public PDTBArg2HeadRankerTrainer() {
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

    public PDTBArg2HeadRankerTrainer(String pdtbRoot, String ptbRoot, String depRoot) {
        this.pdtbRoot = new File(pdtbRoot);
        this.ptbRoot = new File(ptbRoot);
        this.depRoot = new File(depRoot);
                        
        trainer = new MyClassifierTrainer(new RankMaxEntTrainer());
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
        
        /*NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, trainingInstanceList, 10);*/
        
        /*InstanceList[] instanceLists =
                trainingInstanceList.splitInOrder(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(instanceLists[0]);
        showAccuracy(classifier, instanceLists[1]);*/
        
        //showNFoldAccuracy(trainingInstanceList, 10, count);
        trainer.train(trainingInstanceList);
        
        //showNFoldTypeSpecificAccuracy(trainingInstanceList, 10);
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
    
    void showNFoldTypeSpecificAccuracy(InstanceList instanceList, int n) {
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(n);
        double accuracies[] = new double[n];
        double accuracy = 0;
        int run = 0;
        while (cvIt.hasNext()) {
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            double[] result = getTypeSpecificAccuracy(trainingInstances, testingInstances, true);
            accuracies[run++] = result[2];
            accuracy += result[2];
        }
        System.out.println(n+"-Fold cross-validation:");
        System.out.println("Accuracy: " + accuracy/n);
    }
    
    double[] getTypeSpecificAccuracy(InstanceList trainingInstanceList, InstanceList testingInstanceList, boolean show) {        
        InstanceList[] trainingInstanceLists = new InstanceList[3];
        InstanceList[] testingInstanceLists = new InstanceList[3];
        
        for (int i = 0; i < 3; i++) {
            trainingInstanceLists[i] = new InstanceList(trainingInstanceList.getPipe());
            testingInstanceLists[i] = new InstanceList(testingInstanceList.getPipe());
        }
        
        for (Instance instance : trainingInstanceList) {
            Arg2RankConnInstance rankInstance = (Arg2RankConnInstance) instance;
            String conn = rankInstance.getConn().toLowerCase();
            String category = connAnalyzer.getCategory(conn);
            if (category == null) category = "Conj-adverbial";
            
            if (category.startsWith("Coord")) {
                trainingInstanceLists[0].add(instance);
            } else if (category.startsWith("Sub")) {
                trainingInstanceLists[1].add(instance);
            } else {
                trainingInstanceLists[2].add(instance);
            }
        }
        for (Instance instance : testingInstanceList) {
            Arg2RankConnInstance rankInstance = (Arg2RankConnInstance) instance;
            String conn = rankInstance.getConn().toLowerCase();
            String category = connAnalyzer.getCategory(conn);
            if (category == null) category = "Conj-adverbial";
            
            if (category.startsWith("Coord")) {
                testingInstanceLists[0].add(instance);
            } else if (category.startsWith("Sub")) {
                testingInstanceLists[1].add(instance);
            } else {
                testingInstanceLists[2].add(instance);
            }
        }
        
        MyClassifierTrainer trainers[] = new MyClassifierTrainer[3];
        Classifier classifiers[] = new Classifier[3];
        
        double total = 0;
        double correct = 0;
        for (int i = 0; i < 3; i++) {
            trainers[i] = new MyClassifierTrainer(new RankMaxEntTrainer());
            classifiers[i] = trainers[i].train(trainingInstanceLists[i]);
            total += testingInstanceLists[i].size();
            correct += getAccuracy(classifiers[i], testingInstanceLists[i]) * testingInstanceLists[i].size(); //accuracy * total
        }
        if (show) {
            System.out.println("Using type specific models:");
            System.out.println("Total: " + total);
            System.out.println("Correct: " + correct);
            System.out.println("Accuracy: " + correct / total);
        }
        return new double[] {total, correct, 1.0*correct/total};
    }
    
    private void showAccuracy(Classifier classifier, InstanceList instanceList) {
        int total = instanceList.size();
        int correct = 0;        
        for (Instance instance : instanceList) {
            Arg2RankConnInstance arg2RankInstance = (Arg2RankConnInstance) instance.getSource();            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) correct++;
            else {
                int trueHead = arg2RankInstance.trueHeadPos;
                int predictedHead = Integer.parseInt(classification.getLabeling().getBestLabel().toString());
                try {
                    Tree root = treeAnalyzer.getPennTree(arg2RankInstance.tree);
                    Sentence s = new Sentence(root);
                    if (s.get(trueHead).getTag("POS").matches("AUX|MD|IN|TO")) {
                        System.out.println(s);
                        System.out.println("Truehead: " + s.get(trueHead).word());
                        System.out.println("Predicted head: "+s.get(predictedHead).word());
                    }
                } catch (IOException ex) {
                    Logger.getLogger(PDTBArg2HeadRankerTrainer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        System.out.println("Total: " + total);
        System.out.println("Correct: " + correct);
        System.out.println("Accuracy: " + (1.0*correct)/total);
    }
    
    private void showNFoldAccuracy(InstanceList instanceList, int n, int count) {
        InstanceList.CrossValidationIterator cvIt = instanceList.crossValidationIterator(n);
        double accuracies[] = new double[n];
        double accuracy = 0;
        int run = 0;
        double totalTP = 0;
        while (cvIt.hasNext()) {
            InstanceList[] nextSplit = cvIt.nextSplit();
            InstanceList trainingInstances = nextSplit[0];
            InstanceList testingInstances = nextSplit[1];
            
            trainer = new MyClassifierTrainer(new RankMaxEntTrainer());
            Classifier classifier = trainer.train(trainingInstances);
            accuracies[run] = getAccuracy(classifier, testingInstances);
            accuracy += accuracies[run];
            totalTP += accuracies[run]*testingInstances.size();
            run++;
        }
        System.out.println(n + "-Fold accuracy(avg): " + accuracy/n);
        System.out.println("Total tp: " + totalTP);
        System.out.println("Total count: " + count);
        System.out.println(n + "-Fold accuracy: " + totalTP/count);
    }
    
    double getAccuracy(Classifier classifier, InstanceList instanceList) {
        int total = instanceList.size();
        int correct = 0;        
        for (Instance instance : instanceList) {            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) correct++;
        }        
        return (1.0*correct)/total;
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
        
        /*int synHeadPos = treeAnalyzer.getLeafPosition(root, syntacticHead);
        int headPos = getHeadPosFromDepGraph(root, gornNodes, depGraph);
        if (synHeadPos != headPos) {
            Sentence s = new Sentence(root);
            System.out.println(s);
            System.out.println("Arg2: " + relation.getArg2RawText());
            System.out.println("Wellner head: " + s.get(synHeadPos));
            System.out.println("Dep Head: " + s.get(headPos));
            System.out.println("-------------");
            headDisagreement++;
        }*/
        //headPos = getBetterHeadPos(root, depGraph, headPos);
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
        String category = connAnalyzer.getCategory(relation.getConnHead());
        if (category == null) category = "Conj-adverb";
        int initialPos = 0;
        if (category.matches("Coord.*")) {
            initialPos = connEnd + 1;
        }
        int sz = leaves.size();
        List<Integer> headPositions = new ArrayList<Integer>();
        int trueHeadIndex = -1;
        for (int i = initialPos; i < sz; i++) {
            if (i >= connStart && i <= connEnd) continue;
            Tree leaf = leaves.get(i);
            Tree parent = leaf.parent(root);
            //added 10-3-2012, next 2 lines
            //int depParent = depGraph.getParent(i);
            //if (parent.value().matches("VB.*|NNS?|JJ.*|MD") && (depParent == -1 || Math.abs(depParent - i) >= 4)) {
                        
            if (parent.value().matches("VB.*|NN.*|JJ.*|MD.*|AUX.*")) {
            //if (parent.value().matches("VB.*|NNS?|JJ.*")) {
                //Arg2ConnInstance instance;
                if (i == headPos) {
                    //instance = new Arg2ConnInstance(tree, depGraph, connStart, connEnd, i, true, relation);
                    trueHeadIndex = headPositions.size();
                    headPositions.add(i);
                } else {
                    //instance = new Arg2ConnInstance(tree, depGraph, connStart, connEnd, i, false, relation);
                    headPositions.add(i);
                }
                //instanceList.addThruPipe(instance);                
            }
            /*if (i == headPos) {
                System.out.println("POS: " + parent.value());
            }*/
        }
        /*if (trueHeadIndex == -1) {
            trueHeadIndex = headPositions.size();
            headPositions.add(headPos);
        }*/
        
        //added 10-3-2012
        /*int previousHead = headPositions.get(trueHeadIndex);
        System.out.println("#old candidates: " + headPositions.size());
        Sentence s = new Sentence(root);
        for (int i = 0; i < headPositions.size(); i++) {
            if (trueHeadIndex == i) System.out.print("*");
            System.out.print(s.get(headPositions.get(i))+", ");
        }
        System.out.println("");*/
        
        //trueHeadIndex = filterCandidates(root, headPositions, trueHeadIndex, depGraph, connStart, connEnd);
        
        if (trueHeadIndex == -1) {
            System.out.println("new true head == -1!!!");
        } else {
            /*int newHead = headPositions.get(trueHeadIndex);
            System.out.println("#new candidates: " + headPositions.size());
            for (int i = 0; i < headPositions.size(); i++) {
                if (trueHeadIndex == i) {
                    System.out.print("*");
                }
                System.out.print(s.get(headPositions.get(i)) + ", ");
            }
            System.out.println("");*/

            Arg2RankConnInstance instance = new Arg2RankConnInstance(tree, depGraph, connStart, connEnd, headPositions, relation, trueHeadIndex);
            instanceList.addThruPipe(instance);
        }
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
    int filterCandidates(Tree root, List<Integer> headPositions, int trueHeadIndex, SimpleDepGraph depGraph, int connStart, int connEnd) {
        String conn = getConnString(root, connStart, connEnd).toLowerCase();
        String cat = connAnalyzer.getCategory(conn.toLowerCase());        
        if (cat == null || cat.matches("Coord.*")) {
            //System.out.println("skipping: " + conn);
            return trueHeadIndex;
        }
        int trueHead = headPositions.get(trueHeadIndex);
        List<Integer> candidates = new ArrayList<Integer>();
        
        int newTrueHeadIndex = -1;        
        for (int i = 0; i < headPositions.size(); i++) {
            int dep = headPositions.get(i);
            /*int parent = depGraph.getParent(dep);
            if (parent == -1 || connStart <= parent && parent <= connEnd) {
            candidates.add(dep);
            } else {
            List<Integer> reachableIndices = depGraph.getReachableIndices(dep, true, 100);
            boolean flg = false;
            if (reachableIndices != null) {
            for (Integer r : reachableIndices) {
            if (connStart <= r && r <= connEnd) {
            flg = true;
            break;
            }
            }
            if (flg) {
            candidates.add(dep);
            }
            }
            }*/
            List<String> pathList = depGraph.getPathAsList(connEnd, dep, false);
            if (pathList == null || pathList.size() < 7) {
                candidates.add(dep);
            }
        }
        headPositions.clear();
        headPositions.addAll(candidates);
        for (int i = 0; i < headPositions.size(); i++) {
            if (headPositions.get(i) == trueHead) {
                newTrueHeadIndex = i;
            }
        }
        
        return newTrueHeadIndex;
    }
    /**
     * return parent for copular or auxilary verbs and noun modifiers, didn't work well
     * @param root
     * @param depGraph
     * @param oldPos
     * @return 
     */
    int getBetterHeadPos(Tree root, SimpleDepGraph depGraph, int oldPos) {
        int newPos = oldPos;
        //if (root.getLeaves().get(oldPos).parent(root).value().matches("MD")) return oldPos;
        List<SimpleDependency> depDependencies = depGraph.getDepDependencies(oldPos);
        if (!depDependencies.isEmpty() && depDependencies.get(0).reln().matches("det|nn|cop|aux.*|prep.*")) {
            newPos = depDependencies.get(0).gov();
        }
        return newPos;
    }
    
    int getHeadPosFromDepGraph(Tree root, List<Tree> gornNodes, SimpleDepGraph depGraph) {
        Set<Integer> span = new HashSet<Integer>();
        for (Tree t : gornNodes) {
            List<Tree> leaves = t.getLeaves();
            for (Tree leaf : leaves) {
                span.add(treeAnalyzer.getLeafPosition(root, leaf));
            }
        }
        List<Integer> candidates = new ArrayList<Integer>();
        for (Integer dep : span) {
            List<SimpleDependency> deps = depGraph.getDepDependencies(dep); //get parent(s)
            boolean isCandidate = true;
            for (SimpleDependency sDep : deps) {
                if (span.contains(sDep.gov())) {
                    isCandidate = false;
                    break;
                }
            }
            if (isCandidate) {
                candidates.add(dep);
            }
        }
        String rules[] =  new String[]{"V.*", "J.*", "NNS?", "MD"};
        List<Tree> leaves = root.getLeaves();
        for (int i = 0; i < rules.length; i++) {
            for (Integer candidate : candidates) {
                Tree parent = leaves.get(candidate).parent(root);
                if (parent.value().matches(rules[i])) {
                    return candidate;
                }
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        } else {
            return candidates.get(0);
        }
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
        pipes.add(new Arg2RankConnInstance2FeatureVectorSeq());
        //biodrb
        //pipes.add(new Arg2RankConnInstance2FeatureVectorSeq("./resource/ml/data/biodrb/connective_types"));        
        //pipes.add(new PrintFeatureVectorSequence());
        return new SerialPipes(pipes);
    }
    
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    
    public static void main(String args[]) {
        PDTBArg2HeadRankerTrainer trainer = new PDTBArg2HeadRankerTrainer();
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"biodrb"});
        
        //biodrb
        //trainer.train(new String[]{"00"});
        
        trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"});
        //trainer.save("./resource/ml/models/pdtb/pdtb_arg2.model");
        //trainer.save("./resource/ml/models/biodrb/biodrb_arg2.model");
    }
}
