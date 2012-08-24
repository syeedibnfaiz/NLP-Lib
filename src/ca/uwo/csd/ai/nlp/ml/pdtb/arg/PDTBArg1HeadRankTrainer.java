/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ml.pdtb.arg;

import ca.uwo.csd.ai.nlp.corpus.pdtb.GornAddressList;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBPipedFileReader;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PDTBRelation;
import ca.uwo.csd.ai.nlp.corpus.pdtb.PTBFileReader;
import ca.uwo.csd.ai.nlp.integration.Arg2Extractor;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepFileReader;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ml.crf.MyClassifierTrainer;
import ca.uwo.csd.ai.nlp.ling.analyzers.ConnectiveAnalyzer;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.ling.analyzers.HeadAnalyzer;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ml.PrintFeatureVector;
import ca.uwo.csd.ai.nlp.ml.PrintFeatureVectorSequence;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.RankMaxEnt;
import cc.mallet.classify.RankMaxEntTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.stanford.nlp.trees.Tree;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class PDTBArg1HeadRankTrainer {
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
    
    final static Arg2Extractor ARG2_EXTRACTOR = new Arg2Extractor(true);
    
    public PDTBArg1HeadRankTrainer() {
        //this("./pdtb_v2/piped_data_2", "./pdtb_v2/ptb", "./pdtb_v2/dep2");
        //gs
        //this("./pdtb_v2/piped_data", "./package/treebank_3/parsed/mrg/wsj", "./package/treebank_3/parsed/mrg/dep");
        //auto
        this("./package/treebank_3/parsed/mrg/pspdtb", "./package/treebank_3/parsed/mrg/psptb", "./package/treebank_3/parsed/mrg/psdep");
        //biodrb
        //this("./resource/ml/data/biodrb/root/ann", "./resource/ml/data/biodrb/root/parses", "./resource/ml/data/biodrb/root/dep");
    }

    public PDTBArg1HeadRankTrainer(String pdtbRoot, String ptbRoot, String depRoot) {
        this.pdtbRoot = new File(pdtbRoot);
        this.ptbRoot = new File(ptbRoot);
        this.depRoot = new File(depRoot);
        
        trainer = new MyClassifierTrainer(new RankMaxEntTrainer());
        pipedFileReader = new PDTBPipedFileReader();
        ptbFileReader = new PTBFileReader();
        depFileReader = new SimpleDepFileReader();
        
        treeAnalyzer = new SyntaxTreeAnalyzer();
        headAnalyzer = new HeadAnalyzer();
        
        //connAnalyzer = new ConnectiveAnalyzer();
        //biodrb
        connAnalyzer = new ConnectiveAnalyzer("./resource/ml/data/biodrb/connective_types");
    }
    
    public void train(String[] trainSections) throws IOException {
        pipe = defaultPipe();       
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);                
        /*NFoldEvaluator evaluator = new NFoldEvaluator();
        evaluator.evaluate(trainer, trainingInstanceList, 10); */
        /*InstanceList[] instanceLists =
                trainingInstanceList.splitInOrder(new double[]{0.9, 0.1});
        Classifier classifier = trainer.train(instanceLists[0]);
        showAccuracy(classifier, instanceLists[1]);*/
        
        //showNFoldAccuracy(trainingInstanceList, 10, 2633);
        trainer.train(trainingInstanceList);
        
        //showNFoldTypeSpecificAccuracy(trainingInstanceList, 10);
    }
    
    public void train(String[] trainSections, String[] testSections) throws IOException {
        pipe = defaultPipe();
        InstanceList trainingInstanceList = prepareInstanceList(trainSections);
        InstanceList testingInstanceList = prepareInstanceList(testSections);
        
        //Classifier classifier = trainer.train(trainingInstanceList, testingInstanceList);
        
        Classifier classifier = trainer.train(trainingInstanceList);
        
        System.out.println("training size: " + trainingInstanceList.size());
        System.out.println("testing size: " + testingInstanceList.size());
        
        //showAccuracy(classifier, testingInstanceList);
        
        //getTypeSpecificAccuracy(trainingInstanceList, testingInstanceList, true);
        //showInterpolatedTCAccuracy(trainingInstanceList, testingInstanceList);
    }
    /**
     * Shows accuracy according to Ben Wellner's definition of accuracy
     * @param classifier
     * @param instanceList 
     */
    private void showAccuracy(Classifier classifier, InstanceList instanceList) throws IOException {
        int total = instanceList.size();
        int correct = 0;        
        HashMap<String, Integer> errorMap = new HashMap<String, Integer>();
        FileWriter errorWriter = new FileWriter("arg1Error.log");
                
        for (Instance instance : instanceList) {            
            Classification classification = classifier.classify(instance);
            if (classification.bestLabelIsCorrect()) {
                correct++;
            } else {
                Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
                Document doc = rankInstance.getDocument();
                Sentence s = doc.getSentence(rankInstance.getArg2Line());
                String conn = s.toString(rankInstance.getConnStart(), rankInstance.getConnEnd()).toLowerCase();
                //String category = connAnalyzer.getCategory(conn);
                if (errorMap.containsKey(conn)) {
                    errorMap.put(conn, errorMap.get(conn) + 1);
                } else {
                    errorMap.put(conn, 1);
                }
                int arg2Line = rankInstance.getArg2Line();
                int arg1Line = rankInstance.getCandidates().get(rankInstance.getTrueArg1Candidate()).first();
                int arg1HeadPos = rankInstance.getCandidates().get(rankInstance.getTrueArg1Candidate()).second();
                int predictedCandidateIndex = Integer.parseInt(classification.getLabeling().getBestLabel().toString());
                
                if (arg1Line == arg2Line) {
                    errorWriter.write("FileName: "+doc.getFileName() + "\n");
                    errorWriter.write("Sentential\n");
                    errorWriter.write("Conn: "+conn + "\n");
                    errorWriter.write("Arg1Head: " + s.get(arg1HeadPos).word() + "\n");                    
                    errorWriter.write(s.toString() + "\n\n");
                } else {
                    errorWriter.write("FileName: "+doc.getFileName() + "\n");
                    errorWriter.write("Inter-Sentential\n");
                    errorWriter.write("Arg1 in : " + arg1Line + "\n");
                    errorWriter.write("Arg2 in : " + arg2Line + "\n");
                    errorWriter.write("Conn: "+conn + "\n");
                    errorWriter.write(s.toString() + "\n");
                    Sentence s1 = doc.getSentence(arg1Line);
                    errorWriter.write("Arg1Head: " + s1.get(arg1HeadPos) + "\n");
                    errorWriter.write(s1.toString() + "\n\n");                                        
                }
                int predictedArg1Line = rankInstance.getCandidates().get(predictedCandidateIndex).first();
                int predictedArg1HeadPos = rankInstance.getCandidates().get(predictedCandidateIndex).second();
                Sentence pSentence = doc.getSentence(predictedArg1Line);
                errorWriter.write("Predicted arg1 sentence: " + pSentence.toString() + " [Correct: "+ (predictedArg1Line == arg1Line)+"]\n");
                errorWriter.write("Predicted head: " + pSentence.get(predictedArg1HeadPos).word() + "\n\n");
            }
        }
        errorWriter.close();
        
        Set<Entry<String, Integer>> entrySet = errorMap.entrySet();
        List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(entrySet);
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                if (o1.getValue() > o2.getValue()) return -1;
                else if (o1.getValue() < o2.getValue()) return 1;
                return 0;
            }
        });
        
        for (Entry<String, Integer> item : list) {
            System.out.println(item.getKey()+"-"+item.getValue());
        }
        
        System.out.println("Total: " + total);
        System.out.println("Correct: " + correct);
        System.out.println("Accuracy: " + (1.0*correct)/total);
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
            Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
            Sentence sentence = rankInstance.document.getSentence(rankInstance.getArg2Line());
            String conn = sentence.toString(rankInstance.connStart, rankInstance.connEnd).toLowerCase();
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
            Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
            Sentence sentence = rankInstance.document.getSentence(rankInstance.getArg2Line());
            String conn = sentence.toString(rankInstance.connStart, rankInstance.connEnd).toLowerCase();
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
    
    void showInterpolatedTCAccuracy(InstanceList trainingInstanceList, InstanceList testingInstanceList) {        
        trainer = new MyClassifierTrainer(new RankMaxEntTrainer());
        RankMaxEnt generalClassifier = (RankMaxEnt) trainer.train(trainingInstanceList);
        
        InstanceList[] trainingInstanceLists = new InstanceList[3];
        InstanceList[] testingInstanceLists = new InstanceList[3];
        
        for (int i = 0; i < 3; i++) {
            trainingInstanceLists[i] = new InstanceList(trainingInstanceList.getPipe());
            testingInstanceLists[i] = new InstanceList(testingInstanceList.getPipe());
        }
        
        for (Instance instance : trainingInstanceList) {
            Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
            Sentence sentence = rankInstance.document.getSentence(rankInstance.getArg2Line());
            String conn = sentence.toString(rankInstance.connStart, rankInstance.connEnd).toLowerCase();
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
            Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
            Sentence sentence = rankInstance.document.getSentence(rankInstance.getArg2Line());
            String conn = sentence.toString(rankInstance.connStart, rankInstance.connEnd).toLowerCase();
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
        RankMaxEnt classifiers[] = new RankMaxEnt[3];
        
        double total = 0;
        double correct = 0;
        for (int i = 0; i < 3; i++) {
            trainers[i] = new MyClassifierTrainer(new RankMaxEntTrainer());
            classifiers[i] = (RankMaxEnt) trainers[i].train(trainingInstanceLists[i]);
            total += testingInstanceLists[i].size();
            //correct += getAccuracy(classifiers[i], testingInstanceLists[i]) * testingInstanceLists[i].size(); //accuracy * total
            for (Instance instance : testingInstanceLists[i]) {
                Arg1RankInstance rankInstance = (Arg1RankInstance) instance;
                int trueIndex = rankInstance.trueArg1Candidate;
                double genScores[] = new double[((FeatureVectorSequence)instance.getData()).size()];
                generalClassifier.getClassificationScores(instance, genScores);
                double tcScores[] = new double[((FeatureVectorSequence)instance.getData()).size()];
                classifiers[i].getClassificationScores(instance, tcScores);
                double max = 0;
                int maxIndex = -1;
                
                for (int j = 0; j < genScores.length; j++) {
                    double score = genScores[j] * 0.4 + tcScores[j] * 0.6;
                    if (score > max) {
                        max = score;
                        maxIndex = j;
                    }
                }
                if (maxIndex == trueIndex) {
                    correct++;
                }
            }
        }
        
        System.out.println("Using interpolated model:");
        System.out.println("Total: " + total);
        System.out.println("Correct: " + correct);
        System.out.println("Accuracy: " + correct/total);
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
        System.out.println("Total tp:" + totalTP);
        System.out.println("Total count:" + count);
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
                Document document = new Document(ptbFile, depFile);
                
                for (PDTBRelation relation : relations) {
                    if (relation.getType().equals("Explicit")) {
                        String gornAddress1 = relation.getArg1GornAddress();
                        if (gornAddress1.equals("") || gornAddress1.contains(";;")) {                            
                            continue;
                        }                        
                        GornAddressList gaList1 = new GornAddressList(gornAddress1);                        
                                                
                        int lineNumber1 = gaList1.get(0).getLineNumber();
                        
                        String connGornAddress = relation.getConnectiveGornAddress();
                        if (connGornAddress.equals("")) continue;
                        GornAddressList connGAList = new GornAddressList(connGornAddress);
                        
                        String gornAddress2 = relation.getArg2GornAddress();
                        if (gornAddress2.equals("") || gornAddress2.contains(";;")) {                            
                            continue;
                        }                                                
                        int lineNumber2 = connGAList.get(0).getLineNumber();
                                                
                        addInstancesThroughPipe(relation, document, lineNumber1, lineNumber2, instanceList);
                    }
                }
            }
        }
        return instanceList;
    }
    
    private void addInstancesThroughPipe(PDTBRelation relation, Document document, int arg1Line, int arg2Line, InstanceList instanceList) {        
        //System.out.println("Relation: " + relation.toString());
        //System.out.println("arg1Line: " + arg1Line);
        //System.out.println("arg2Line: " + arg2Line);
        
        String connectiveGornAddress = relation.getConnectiveGornAddress();
        Tree arg2Tree = document.getTree(arg2Line);
        List<Tree> connHeadLeaves = connAnalyzer.getConnHeadLeaves(arg2Tree, connectiveGornAddress, relation.getConnHead());
        if (connHeadLeaves.isEmpty()) return;
        
        int connStart = treeAnalyzer.getLeafPosition(arg2Tree, connHeadLeaves.get(0));
        int connEnd = treeAnalyzer.getLeafPosition(arg2Tree, connHeadLeaves.get(connHeadLeaves.size() - 1));
        if ((connEnd - connStart) > 4) { //handle if..else, etc.
            connEnd = connStart;
        }
        
        //consider only the first sentence in case of multi-line argument1
        String arg1GornAddress = relation.getArg1GornAddress();
        Tree arg1Tree = document.getTree(arg1Line);        
        List<Tree> arg1GornNodes = getArgGornNodes(arg1Tree, arg1Line, arg1GornAddress);
        
        
        Tree syntacticHead = headAnalyzer.getSyntacticHead(arg1Tree, arg1GornNodes);
        int arg1HeadPos = treeAnalyzer.getLeafPosition(arg1Tree, syntacticHead);        
        
        String arg2GornAddress = relation.getArg2GornAddress();
        List<Tree> arg2GornNodes = getArgGornNodes(arg2Tree, arg2Line, arg2GornAddress);
                
        Tree arg2SyntacticHead = headAnalyzer.getSyntacticHead(arg2Tree, arg2GornNodes);
        int arg2HeadPos = treeAnalyzer.getLeafPosition(arg2Tree, arg2SyntacticHead);
        
        if (arg2HeadPos == -1) {
            System.out.println("arg2Head == -1");
            return;
        }
        if (arg1HeadPos == -1) {
            System.out.println("arg1Head == -1");
            return;
        }
        int trueCandidate = -1;
        List<Pair<Integer, Integer>> candidates = getCandidates(document, arg2Line, connStart, connEnd, arg1Line);
        for (int i = 0; i < candidates.size(); i++) {
            Pair<Integer, Integer> candidate = candidates.get(i);
            if (candidate.first() == arg1Line && candidate.second() == arg1HeadPos) {
                trueCandidate = i;
                break;
            }
        }
        if (trueCandidate == -1) {
            //trueCandidate = candidates.size();
            //candidates.add(new Pair<Integer, Integer>(arg1Line, arg1HeadPos));
            //System.out.println("Covered!");
            System.out.println("true candidate == -1!!!");
            System.out.println(syntacticHead.value());
        } else {
            int extractArg2 = ARG2_EXTRACTOR.extractArg2(document.getSentence(arg2Line), document.getTree(arg2Line), document.getDepGraph(arg2Line), connStart, connEnd);
            if (extractArg2 == -1) {
                extractArg2 = 0;
                System.out.println("Arg2 == -1!!!!!!!!!!!!!!!!!");
            }
            //Arg1RankInstance instance = new Arg1RankInstance(document, candidates, arg2Line, extractArg2, connStart, connEnd, trueCandidate);
            
            Arg1RankInstance instance = new Arg1RankInstance(document, candidates, arg2Line, arg2HeadPos, connStart, connEnd, trueCandidate);            
            instanceList.addThruPipe(instance);
        }
    }
    
    List<Tree> getArgGornNodes(Tree root, int lineNum, String gornAddress) {        
        String tokens[] = gornAddress.split(";");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            int line = Integer.parseInt(token.split(",")[0]);
            if (line != lineNum) continue;
            sb.append(token+";");
        }
        return treeAnalyzer.getGornNodes(root, sb.toString());
    }
    
    List<Util.Pair<Integer, Integer>> getCandidates(Document document, int arg2Line, int connStart, int connEnd, int arg1Line) {
        List<Util.Pair<Integer, Integer>> candidates = new ArrayList<Util.Pair<Integer, Integer>>();
        
        int distance = 10;   
        Sentence arg2Sentence = document.getSentence(arg2Line);
        //String conn = arg2Sentence.toString(connStart, connEnd).toLowerCase();
        //String category = connAnalyzer.getCategory(conn);
        int connHeadPos = connAnalyzer.getHeadWord(arg2Sentence.getParseTree(), connStart, connEnd);
        SimpleDepGraph arg2DepGraph = document.getDepGraph(arg2Line);
        List<Integer> reachable = arg2DepGraph.getReachableIndices(connHeadPos, false, distance);
        for (Integer i : reachable) {
            if (arg2Sentence.get(i).getTag("POS").matches("VB.*|NNS?|JJ.*|MD")) {
                candidates.add(new Util.Pair<Integer, Integer>(arg2Line, i));
            }
        }
        Tree mainHead = headAnalyzer.getCollinsHead(arg2Sentence.getParseTree().getChild(0));
        if (mainHead != null) {
            int mainHeadPos = treeAnalyzer.getLeafPosition(arg2Sentence.getParseTree(), mainHead);
            List<String> pathAsList = arg2DepGraph.getPathAsList(connHeadPos, mainHeadPos, false);
            if (pathAsList != null) {
                distance = distance - (1 + pathAsList.size());
            } else {
                //System.out.println("No path from connHead to mainHead!");
                distance--;
            }
        }
        //if (arg1Line == arg2Line) return candidates;
        
        for (int i = arg2Line - 1; i >= 0 && distance >= 0; i--) {
            Sentence sentence = document.getSentence(i);
            SimpleDepGraph depGraph = document.getDepGraph(i);
            mainHead = headAnalyzer.getCollinsHead(sentence.getParseTree().getChild(0));
            if (mainHead == null) {
                distance--;
                continue;
            }
            int mainHeadPos = treeAnalyzer.getLeafPosition(sentence.getParseTree(), mainHead);
            reachable = depGraph.getReachableIndices(mainHeadPos, false, distance);                        
            if (reachable == null) {
                distance--;
                continue;
            }
            for (Integer j : reachable) {
                if (sentence.get(j).getTag("POS").matches("VB.*|NNS?|JJ.*|MD")) {
                    candidates.add(new Util.Pair<Integer, Integer>(i, j));
                }
            }
            distance -= 2;
        }
        return candidates;
    }
    
    //added 11-03-2012
    int getMainHead(Sentence s, SimpleDepGraph depGraph) {
        for (int i = 0; i < s.size(); i++) {
            if (depGraph.getParent(i) == -1) return i;
        }
        return -1;
    }
    private Pipe defaultPipe() {
        ArrayList<Pipe> pipes = new ArrayList<Pipe>();
        pipes.add(new Arg1RankInstance2FeatureVectorSeq());
        //biodrb
        //pipes.add(new Arg1RankInstance2FeatureVectorSeq("./resource/ml/data/biodrb/connective_types"));        
        //pipes.add(new PrintFeatureVectorSequence());
        return new SerialPipes(pipes);
    }
    
    public void save(String fileName) {
        trainer.saveModel(fileName);
    }
    
    public static void main(String args[]) throws IOException {
        PDTBArg1HeadRankTrainer trainer = new PDTBArg1HeadRankTrainer();
        //trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"23", "24"});
        trainer.train(new String[]{"02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22"}, new String[]{"00", "01"});
        //trainer.train(new String[]{"02"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"22"}, new String[]{"23", "24"});
        //trainer.train(new String[]{"biodrb"});
        //new biodrb
        //trainer.train(new String[]{"00"});
        
        //trainer.save("./resource/ml/models/pdtb/pdtb_arg1.model");
        //trainer.save("./resource/ml/models/pdtb/biodrb_arg1.model");
    }
}
