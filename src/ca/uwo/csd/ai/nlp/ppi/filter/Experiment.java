package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.utils.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kernel.KernelManager;
import libsvm.Instance;
import libsvm.SVMPredictor;
import libsvm.SVMTrainer;
import libsvm.svm_model;
import libsvm.svm_parameter;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class Experiment {

    final static int LEFT = 1;
    final static int MIDDLE = 2;
    final static int RIGHT = 3;
    final String[] types = {"", "Left", "Middle", "Right"};
       
    
    public static void main(String[] args) throws IOException {
        Experiment exp = new Experiment();
        //exp.doCVTest("./resource/relation/PPI4/LLL");
        //exp.doCVTest("./resource/relation/PPI4/HPRD50");
        //exp.doCVTest("./resource/relation/PPI4/IEPA");
        //exp.doCVTest("./resource/relation/PPI4/AIMed");
        //exp.doCVTest("./resource/relation/PPI4/BioInfer");

        //exp.doCLTest("./resource/relation/PPI4/", "LLL");        
        //exp.doCLTest("./resource/relation/PPI4/", "HPRD50");
        //exp.doCLTest("./resource/relation/PPI4/", "IEPA");        
        //exp.doCLTest("./resource/relation/PPI4/", "AIMed");
        //exp.doCLTest("./resource/relation/PPI4/", "BioInfer");

        //exp.doCCTest("./resource/relation/PPI4/AIMed", "./resource/relation/PPI4/BioInfer");

        exp.doAllCV("./resource/relation/PPI6");
        //exp.doAllCL("./resource/relation/PPI6");
        //exp.doAllCC("./resource/relation/PPI4");
        
        //exp.updateVerdict("./resource/relation/PPI6", "CL");
                
    }

    public void doAllCV(String ppiBase) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        String[] results = new String[5];
        File root = new File(ppiBase);
        for (int i = 0; i < corpora.length; i++) {
            File corpusDir = new File(root, corpora[i]);
            results[i] = doCVTest(corpusDir.getAbsolutePath());
        }
        for (int i = 0; i < corpora.length; i++) {
            System.out.println("--" + corpora[i] + "--");
            System.out.println(results[i]);
        }
    }

    public void doAllCL(String ppiBase) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        String[] results = new String[5];
        for (int i = 0; i < corpora.length; i++) {
            results[i] = doCLTest(ppiBase, corpora[i]);
        }
        for (int i = 0; i < corpora.length; i++) {
            System.out.println("--" + corpora[i] + "--");
            System.out.println(results[i]);
        }
    }

    public void doAllCC(String ppiBase) {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        File root = new File(ppiBase);
        String[][] results = new String[5][5];
        for (int i = 0; i < corpora.length; i++) {
            File trainCorpusDir = new File(root, corpora[i]);
            for (int j = 0; j < corpora.length; j++) {
                if (i != j) {
                    File testCorpusDir = new File(root, corpora[j]);
                    results[i][j] = doCCTest(trainCorpusDir.getAbsolutePath(), testCorpusDir.getAbsolutePath());
                }
            }

        }
        for (int i = 0; i < corpora.length; i++) {
            System.out.println("--" + corpora[i] + "--");
            for (int j = 0; j < corpora.length; j++) {
                if (i != j) {
                    String result = results[i][j];
                    result = result.replace("\n", "\n\t");
                    System.out.println("\t--" + corpora[j] + "--");
                    System.out.println("\t" + result);
                }
            }
        }
    }

    public String doCVTest(String corpusRoot) throws IOException {
        CorpusReader corpusReader = new CorpusReader();
        List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusRoot);
        //do type specific experiments
        //relationInstances = new RelationFilterer().applyFilter(relationInstances, new RelationTypeFilter(RelationTypeFilter.RIGHT));

        List<List<RelationInstance>> splits = getSplits(corpusRoot, relationInstances);
        //relationInstances = null; //clear

        //return doCrossValidation(splits);

        //some positive relations were filtered
        List<Integer> totalPositives = countPositives(splits);

        //apply filtering        
        splits = doFilterings(splits);

        List<List<Instance>> svmSplits = getSVMSplits(splits);
        splits = null;
        
        String result = doCrossValidation(svmSplits, totalPositives);
        writeOutput(corpusRoot, relationInstances);
        return result;
    }

    private void writeOutput(String corpusRoot, List<RelationInstance> relationInstances) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(corpusRoot, "output.txt")));
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction && instance.path != null) {
                writer.write(instance.pairIds[0] + " ");
                if (instance.filterVerdict != null) {
                    writer.write("FN(" + instance.filterVerdict + ")\n");
                } else {
                    writer.write(instance.verdict + "\n");
                }
            }
        }
        writer.close();
    }
    private String doCrossValidation(List<List<Instance>> svmSplits, List<Integer> totalPositives) {
        //KernelManager.setCustomKernel(new SimilarityKernel());
        //KernelManager.setCustomKernel(new SimpleKernel());
        //KernelManager.setCustomKernel(new SimpleKernel2());
        //KernelManager.setCustomKernel(new DepWalkKernel());
        //KernelManager.setCustomKernel(new EditDistanceKernel());
        //KernelManager.setCustomKernel(new FeatureKernel());
        KernelManager.setCustomKernel(new SimpleFeatureKernel());
        //KernelManager.setCustomKernel(new PathStructureKernel());
        svm_parameter param = new svm_parameter();
        param.cache_size = 2500;

        int N = svmSplits.size();
        double precision = 0;
        double recall = 0;
        double fscore = 0;
        int TP = 0;
        int FP = 0;
        int FN = 0;
        int[] typeTPs = new int[4];
        int[] typeFPs = new int[4];
        double avgTrainTime = 0;
        double avgTestTime = 0;
        for (int i = 0; i < N; i++) {
            int tp = 0;
            int fp = 0;
            int fn = 0;
            System.out.println("Iteration: " + i);
            List<Instance> testingInstances = svmSplits.get(i);
            List<Instance> trainingInstances = new ArrayList<Instance>();
            for (int j = 0; j < N; j++) {
                if (j != i) {
                    trainingInstances.addAll(svmSplits.get(j));
                }
            }

            double start = System.currentTimeMillis();
            svm_model model = SVMTrainer.train(trainingInstances, param);
            double end = System.currentTimeMillis();
            avgTrainTime += (end - start);

            start = System.currentTimeMillis();
            double[] predictions = SVMPredictor.predict(testingInstances, model, false);
            end = System.currentTimeMillis();
            avgTestTime += (end - start);;

            for (int k = 0; k < predictions.length; k++) {
                if (testingInstances.get(k).getLabel() == predictions[k]) {
                    if (testingInstances.get(k).getLabel() > 0) {
                        tp++;
                        RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                        instance.verdict = "TP";
                        typeTPs[getType(instance)]++;                              
                    }
                } else if (testingInstances.get(k).getLabel() > 0) {
                    //fn++;
                    RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    instance.verdict = "FN";
                    /*System.out.println("FN");
                    System.out.println(instance.s);                    
                    printBackbone(instance.s, instance.path);
                    System.out.println("lcs="+instance.s.get(instance.lcs).word());
                    System.out.println("key="+instance.s.get(instance.key).word());
                    System.out.println(SimpleFeatureKernel.featureSet2Vector.toString(instance.leftVector));
                    System.out.println(SimpleFeatureKernel.featureSet2Vector.toString(instance.rightVector));
                    System.out.println(types[getType(instance)]);
                    System.out.println("");*/
                } else {
                    fp++;
                    RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    typeFPs[getType(instance)]++;                    
                    /*System.out.println("FP");
                    System.out.println(instance.s);                    
                    printBackbone(instance.s, instance.path);
                    System.out.println(instance.s.get(instance.lcs).word());
                    System.out.println(types[getType(instance)]);
                    System.out.println("");*/
                }
            }

            fn = totalPositives.get(i) - tp;
            double _precision = tp * 1.0 / (tp + fp);
            double _recall = tp * 1.0 / (tp + fn);
            double _fscore = 2 * _precision * _recall / (_precision + _recall);

            precision += _precision;
            recall += _recall;
            fscore += _fscore;

            TP += tp;
            FP += fp;
            FN += fn;

            System.out.println("TP: " + tp + ", FP: " + fp + ", FN: " + fn);
            System.out.println("Precision: " + String.format("%.2f", _precision * 100) + "%");
            System.out.println("Recall: " + String.format("%.2f", _recall * 100) + "%");
            System.out.println("Fscore: " + String.format("%.2f", _fscore * 100) + "%");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Avg. Training time: " + avgTrainTime / (N * 1000.0) + "\n");
        sb.append("Avg. Testing time: " + avgTestTime / (N * 1000.0) + "\n");
        sb.append("TP: " + TP + "(" + typeTPs[LEFT] + ", " + typeTPs[MIDDLE] + ", " + typeTPs[RIGHT] + ")");
        sb.append(", FP: " + FP + "(" + typeFPs[LEFT] + ", " + typeFPs[MIDDLE] + ", " + typeFPs[RIGHT] + ")");
        sb.append(", FN: " + FN + "\n");
        sb.append("Precision: " + String.format("%.2f", precision * 100 / N) + "%\n");
        sb.append("Recall: " + String.format("%.2f", recall * 100 / N) + "%\n");
        sb.append("Fscore: " + String.format("%.2f", fscore * 100 / N) + "%\n");
        System.out.println(sb.toString());

        return sb.toString();
    }

    private String doCLTest(String ppiCorporaBase, String corpusName) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        List<RelationInstance> trainingRelationInstances = new ArrayList<RelationInstance>();
        List<RelationInstance> testingRelationInstances = new ArrayList<RelationInstance>();

        CorpusReader corpusReader = new CorpusReader();
        for (int i = 0; i < corpora.length; i++) {
            File corpusDir = new File(ppiCorporaBase, corpora[i]);
            List<RelationInstance> relationInstances = corpusReader.getRelationInstances(corpusDir.getAbsolutePath());
            if (corpora[i].equals(corpusName)) {
                testingRelationInstances.addAll(relationInstances);
            } else {
                trainingRelationInstances.addAll(relationInstances);
            }
        }

        List<RelationInstance> origTestingRelationInstances = testingRelationInstances;
        //do type specific experiments
        //trainingRelationInstances = new RelationFilterer().applyFilter(trainingRelationInstances, new RelationTypeFilter(RelationTypeFilter.LEFT));
        //testingRelationInstances = new RelationFilterer().applyFilter(testingRelationInstances, new RelationTypeFilter(RelationTypeFilter.LEFT));

        int totalPositive = countPositive(testingRelationInstances);

        System.out.println("Before: " + trainingRelationInstances.size());
        trainingRelationInstances = doFiltering(trainingRelationInstances);
        System.out.println("After: " + trainingRelationInstances.size());
        testingRelationInstances = doFiltering(testingRelationInstances);

        List<Instance> trainingInstances = getSVMInstances(trainingRelationInstances);
        List<Instance> testingInstances = getSVMInstances(testingRelationInstances);
        trainingRelationInstances = null;
        testingRelationInstances = null;

        //KernelManager.setCustomKernel(new SimpleKernel());        
        //KernelManager.setCustomKernel(new FeatureKernel());
        KernelManager.setCustomKernel(new SimpleFeatureKernel());
        svm_parameter param = new svm_parameter();
        System.out.println("Running parameter selection...");
        param.C = 1;//new GridSearch().search(trainingInstances);
        param.cache_size = 2500;
        System.out.println("Done parameter selection.");

        svm_model model = SVMTrainer.train(trainingInstances, param);
        double[] predictions = SVMPredictor.predict(testingInstances, model);

        int tp = 0;
        int fp = 0;
        int fn = 0;
        int[] typeTPs = new int[4];
        int[] typeFPs = new int[4];

        for (int k = 0; k < predictions.length; k++) {
            if (testingInstances.get(k).getLabel() == predictions[k]) {
                if (testingInstances.get(k).getLabel() > 0) {
                    tp++;
                    RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    instance.verdict = "TP";
                    typeTPs[getType(instance)]++;
                }
            } else if (testingInstances.get(k).getLabel() > 0) {
                //fn++;
                RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                instance.verdict = "FN";
                /*System.out.println("FN");
                System.out.println(instance.s);
                printBackbone(instance.s, instance.path);
                System.out.println("lcs=" + instance.s.get(instance.lcs).word());
                System.out.println("key=" + instance.s.get(instance.key).word());
                System.out.println(FeatureKernel.featureSet2Vector.toString(instance.leftVector));
                System.out.println(FeatureKernel.featureSet2Vector.toString(instance.rightVector));
                System.out.println(types[getType(instance)]);
                System.out.println("");*/
            } else {
                fp++;
                RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                typeFPs[getType(instance)]++;
            }
        }

        fn = totalPositive - tp;
        double precision = tp * 1.0 / (tp + fp);
        double recall = tp * 1.0 / (tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);

        StringBuilder sb = new StringBuilder();
        sb.append("TP: " + tp + "(" + typeTPs[LEFT] + ", " + typeTPs[MIDDLE] + ", " + typeTPs[RIGHT] + ")");
        sb.append(", FP: " + fp + "(" + typeFPs[LEFT] + ", " + typeFPs[MIDDLE] + ", " + typeFPs[RIGHT] + ")");
        sb.append(", FN: " + fn + "\n");
        sb.append("Precision: " + String.format("%.2f", precision * 100) + "%\n");
        sb.append("Recall: " + String.format("%.2f", recall * 100) + "%\n");
        sb.append("Fscore: " + String.format("%.2f", fscore * 100) + "%\n");
        System.out.println(sb.toString());

        SimpleFeatureKernel kernel = (SimpleFeatureKernel) KernelManager.getCustomKernel();        
        System.out.println("s1=" + kernel.s1/kernel.count);
        System.out.println("s2=" + kernel.s2/kernel.count);
        System.out.println("s3=" + kernel.s3/kernel.count);
        
        writeOutput(new File(ppiCorporaBase, corpusName).getAbsolutePath(), origTestingRelationInstances);
        return sb.toString();
    }

    private String doCCTest(String trainCorpus, String testCorpus) {

        CorpusReader corpusReader = new CorpusReader();
        List<RelationInstance> trainingRelationInstances = corpusReader.getRelationInstances(trainCorpus);
        List<RelationInstance> testingRelationInstances = corpusReader.getRelationInstances(testCorpus);

        int totalPositive = countPositive(testingRelationInstances);

        System.out.println("Before: " + trainingRelationInstances.size());
        trainingRelationInstances = doFiltering(trainingRelationInstances);
        System.out.println("After: " + trainingRelationInstances.size());
        testingRelationInstances = doFiltering(testingRelationInstances);

        List<Instance> trainingInstances = getSVMInstances(trainingRelationInstances);
        List<Instance> testingInstances = getSVMInstances(testingRelationInstances);
        trainingRelationInstances = null;
        testingRelationInstances = null;

        //KernelManager.setCustomKernel(new SimpleKernel());
        KernelManager.setCustomKernel(new FeatureKernel());
        svm_parameter param = new svm_parameter();
        param.cache_size = 1500;

        svm_model model = SVMTrainer.train(trainingInstances, param);
        double[] predictions = SVMPredictor.predict(testingInstances, model);

        int tp = 0;
        int fp = 0;
        int fn = 0;
        int[] typeTPs = new int[4];
        int[] typeFPs = new int[4];

        for (int k = 0; k < predictions.length; k++) {
            if (testingInstances.get(k).getLabel() == predictions[k]) {
                if (testingInstances.get(k).getLabel() > 0) {
                    tp++;
                    RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    typeTPs[getType(instance)]++;
                }
            } else if (testingInstances.get(k).getLabel() > 0) {
                //fn++;
            } else {
                fp++;
                RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                typeFPs[getType(instance)]++;
            }
        }

        fn = totalPositive - tp;
        double precision = tp * 1.0 / (tp + fp);
        double recall = tp * 1.0 / (tp + fn);
        double fscore = 2 * precision * recall / (precision + recall);

        StringBuilder sb = new StringBuilder();
        sb.append("TP: " + tp + "(" + typeTPs[LEFT] + ", " + typeTPs[MIDDLE] + ", " + typeTPs[RIGHT] + ")");
        sb.append(", FP: " + fp + "(" + typeFPs[LEFT] + ", " + typeFPs[MIDDLE] + ", " + typeFPs[RIGHT] + ")");
        sb.append(", FN: " + fn + "\n");
        sb.append("Precision: " + String.format("%.2f", precision * 100) + "%\n");
        sb.append("Recall: " + String.format("%.2f", recall * 100) + "%\n");
        sb.append("Fscore: " + String.format("%.2f", fscore * 100) + "%\n");
        System.out.println(sb.toString());

        return sb.toString();
    }

    private List<Instance> getSVMInstances(List<RelationInstance> relationInstances) {
        List<Instance> instances = new ArrayList<Instance>();
        for (RelationInstance relInstance : relationInstances) {
            if (relInstance.interaction) {
                instances.add(new Instance(+1, relInstance));
            } else {
                instances.add(new Instance(-1, relInstance));
            }
        }
        return instances;
    }

    private List<Integer> countPositives(List<List<RelationInstance>> splits) {
        List<Integer> list = new ArrayList<Integer>();
        for (List<RelationInstance> split : splits) {
            list.add(countPositive(split));
        }
        return list;
    }

    private int countPositive(List<RelationInstance> relationInstances) {
        int count = 0;
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction) {
                count++;
            }
        }
        return count;
    }

    private List<List<RelationInstance>> doFilterings(List<List<RelationInstance>> relationInstances) {
        List<List<RelationInstance>> newLists = new ArrayList<List<RelationInstance>>();
        for (List<RelationInstance> list : relationInstances) {
            newLists.add(doFiltering(list));
        }
        return newLists;
    }

    private List<RelationInstance> doFiltering(List<RelationInstance> relationInstances) {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PathFilter());
        filters.add(new DomainFilter());
        filters.add(new NegativeFilter());
        filters.add(new LeftPatternFilter());
        filters.add(new RightPatternFilter());
        filters.add(new MiddlePatternFilter());
        filters.add(new JuxtaposFilter());

        RelationFilterer filterer = new RelationFilterer();
        return filterer.applyFilters(relationInstances, filters);
    }

    private List<List<Instance>> getSVMSplits(List<List<RelationInstance>> relationInstances) {
        List<List<Instance>> instanceLists = new ArrayList<List<Instance>>();
        for (List<RelationInstance> instances : relationInstances) {
            List<Instance> instanceList = getSVMInstanceList(instances);
            instanceLists.add(instanceList);
        }
        return instanceLists;
    }

    private List<Instance> getSVMInstanceList(List<RelationInstance> relationInstances) {
        List<Instance> instanceList = new ArrayList<Instance>();
        for (RelationInstance relInstance : relationInstances) {
            if (relInstance.interaction) {
                instanceList.add(new Instance(+1, relInstance));
            } else {
                instanceList.add(new Instance(-1, relInstance));
            }
        }
        return instanceList;
    }

    private List<List<RelationInstance>> getSplits(String corpusRoot, List<RelationInstance> relationInstances) {
        List<List<RelationInstance>> foldSplits = new ArrayList<List<RelationInstance>>();
        RelationFilterer filterer = new RelationFilterer();
        List<List<String>> foldDocIds = loadFoldDocIds(corpusRoot);
        for (List<String> docIds : foldDocIds) {
            DocIdFilter filter = new DocIdFilter(docIds);
            foldSplits.add(filterer.applyFilter(relationInstances, filter));
        }
        return foldSplits;
    }

    private List<List<String>> loadFoldDocIds(String corpusRootPath) {
        File splitDir = new File(corpusRootPath, "splits");
        List<List<String>> docIds = new ArrayList<List<String>>();
        for (int i = 0; i < 10; i++) {
            List<String> lines = Util.readLines(new File(splitDir, i + ".txt"));
            System.out.println(lines);
            docIds.add(lines);
        }
        return docIds;
    }

    private int getType(RelationInstance instance) {
        if (instance.lcs < instance.entity1) {
            return LEFT;
        } else if (instance.lcs < instance.entity2) {
            return MIDDLE;
        } else {
            return RIGHT;
        }
    }

    public static void printBackbone(Sentence s, List<String> backBonePath) {
        for (int i = 0; i < backBonePath.size(); i++) {
            if (i % 2 == 0) {
                int index = Integer.valueOf(backBonePath.get(i));
                System.out.print(s.get(index));
            } else {
                System.out.print(":" + backBonePath.get(i) + ":");
            }
        }
        System.out.println("");
    }

    private void setRankedKey(List<RelationInstance> trainInstances, List<RelationInstance> testInstances) {
        DomainTermRanker.rank(trainInstances);
        for (RelationInstance instance : trainInstances) {
            instance.setRankedKeyTerm();
        }
        for (RelationInstance instance : testInstances) {
            instance.setRankedKeyTerm();
        }
    }

    /**
     * For testing CV without filtering test set
     * @param splits
     * @return 
     */
    private String doCrossValidation(List<List<RelationInstance>> splits) {
        List<Integer> totalPositives = countPositives(splits);
        //KernelManager.setCustomKernel(new FeatureKernel());
        KernelManager.setCustomKernel(new SimpleFeatureKernel());

        svm_parameter param = new svm_parameter();
        param.cache_size = 2500;

        int N = splits.size();
        double precision = 0;
        double recall = 0;
        double fscore = 0;
        int TP = 0;
        int FP = 0;
        int FN = 0;
        int[] typeTPs = new int[4];
        int[] typeFPs = new int[4];
        double avgTrainTime = 0;
        double avgTestTime = 0;
        for (int i = 0; i < N; i++) {
            int tp = 0;
            int fp = 0;
            int fn = 0;
            System.out.println("Iteration: " + i);
            List<RelationInstance> testingRelationInstances = new ArrayList<RelationInstance>(splits.get(i));
            List<RelationInstance> trainingRelationInstances = new ArrayList<RelationInstance>();
            for (int j = 0; j < N; j++) {
                if (j != i) {
                    trainingRelationInstances.addAll(splits.get(j));
                }
            }

            trainingRelationInstances = doFiltering(trainingRelationInstances);
            testingRelationInstances = doFiltering(testingRelationInstances);
            //testingRelationInstances = new PathFilter().apply(testingRelationInstances);
            setRankedKey(trainingRelationInstances, testingRelationInstances);

            List<Instance> trainingInstances = getSVMInstanceList(trainingRelationInstances);
            List<Instance> testingInstances = getSVMInstanceList(testingRelationInstances);

            double start = System.currentTimeMillis();
            svm_model model = SVMTrainer.train(trainingInstances, param);
            double end = System.currentTimeMillis();
            avgTrainTime += (end - start);

            start = System.currentTimeMillis();
            double[] predictions = SVMPredictor.predict(testingInstances, model, false);
            end = System.currentTimeMillis();
            avgTestTime += (end - start);;

            for (int k = 0; k < predictions.length; k++) {
                if (testingInstances.get(k).getLabel() == predictions[k]) {
                    if (testingInstances.get(k).getLabel() > 0) {
                        tp++;
                        RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                        typeTPs[getType(instance)]++;
                    }
                } else if (testingInstances.get(k).getLabel() > 0) {
                    //fn++;
                    /*RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    System.out.println("FN");
                    System.out.println(instance.s);                    
                    printBackbone(instance.s, instance.path);
                    System.out.println("lcs="+instance.s.get(instance.lcs).word());
                    System.out.println("key="+instance.s.get(instance.key).word());
                    System.out.println(FeatureKernel.featureSet2Vector.toString(instance.leftVector));
                    System.out.println(FeatureKernel.featureSet2Vector.toString(instance.rightVector));
                    System.out.println(types[getType(instance)]);
                    System.out.println("");*/
                } else {
                    fp++;
                    RelationInstance instance = (RelationInstance) testingInstances.get(k).getData();
                    typeFPs[getType(instance)]++;
                    /*System.out.println("FP");
                    System.out.println(instance.s);                    
                    printBackbone(instance.s, instance.path);
                    System.out.println(instance.s.get(instance.lcs).word());
                    System.out.println(types[getType(instance)]);
                    System.out.println("");*/
                }
            }

            fn = totalPositives.get(i) - tp;
            double _precision = tp * 1.0 / (tp + fp);
            double _recall = tp * 1.0 / (tp + fn);
            double _fscore = 2 * _precision * _recall / (_precision + _recall);

            precision += _precision;
            recall += _recall;
            fscore += _fscore;

            TP += tp;
            FP += fp;
            FN += fn;

            System.out.println("TP: " + tp + ", FP: " + fp + ", FN: " + fn);
            System.out.println("Precision: " + String.format("%.2f", _precision * 100) + "%");
            System.out.println("Recall: " + String.format("%.2f", _recall * 100) + "%");
            System.out.println("Fscore: " + String.format("%.2f", _fscore * 100) + "%");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Avg. Training time: " + avgTrainTime / (N * 1000.0) + "\n");
        sb.append("Avg. Testing time: " + avgTestTime / (N * 1000.0) + "\n");
        sb.append("TP: " + TP + "(" + typeTPs[LEFT] + ", " + typeTPs[MIDDLE] + ", " + typeTPs[RIGHT] + ")");
        sb.append(", FP: " + FP + "(" + typeFPs[LEFT] + ", " + typeFPs[MIDDLE] + ", " + typeFPs[RIGHT] + ")");
        sb.append(", FN: " + FN + "\n");
        sb.append("Precision: " + String.format("%.2f", precision * 100 / N) + "%\n");
        sb.append("Recall: " + String.format("%.2f", recall * 100 / N) + "%\n");
        sb.append("Fscore: " + String.format("%.2f", fscore * 100 / N) + "%\n");
        System.out.println(sb.toString());

        return sb.toString();
    }
    
    public void updateVerdict(String ppiBase, String expType) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(ppiBase, "update_output.sql")));
        String update = "UPDATE %s SET %s='%s' where pairid='%s';";
        for (String corpus : corpora) {
            File corpusDir = new File(ppiBase, corpus);
            String tableName = corpus.toLowerCase() + "s";
            List<String> lines = Util.readLines(new File(corpusDir, "output.txt"));
            for (String line : lines) {
                String[] tokens = line.split("\\s+");                
                String query = String.format(update, tableName, expType, tokens[1], tokens[0]);
                writer.write(query + "\n");
            }
        }
        writer.close();
    }
}
