
package ca.uwo.csd.ai.nlp.main;

import abner.Tagger;
import ca.uwo.csd.ai.nlp.io.GenericTextReader;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ling.Text;
import ca.uwo.csd.ai.nlp.ling.TokWord;
import ca.uwo.csd.ai.nlp.ling.ann.ParserAnnotator;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import ca.uwo.csd.ai.nlp.ppi.PPIInstance;
import ca.uwo.csd.ai.nlp.relx.ppi.RunRelex;
import ca.uwo.csd.ai.nlp.utils.FileExtensionFilter;
import ca.uwo.csd.ai.nlp.utils.OSentenceBoundaryDetector;
import ca.uwo.csd.ai.nlp.utils.Util;
import ca.uwo.csd.ai.nlp.utils.Util.Pair;
import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class WGPRelEx {
    private static int entityCount;
    private static Classifier classifier;
    private final static GenericTextReader TEXT_READER = new GenericTextReader("\n\n", "\n", "\t", new String[]{"WORD", "TAG"});
    
    private static Options getOptions() {
        Option help = new Option("help", "print this message");
        Option inFile   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "* read text from file. For batch processing the argument can be a directory. In that case an output directory"
                + " must be provided via -output. Only txt files are considered as input files" )
                                .create( "input" );
        Option outFile   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "write output to file" )
                                .create( "output" );
        Option domainFile   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "read relation terms from file" )
                                .create( "relterms" );
        
        Option stanfordFile   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "read Stanford parser from specified path. default path is ./lib/EnglishPCFG.ser.gz" )
                                .create( "stanford" );
        
        Option outputMode   = OptionBuilder.withArgName( "mode" )
                                .hasArg()
                                .withDescription(  "use specified output mode. REL to extract relations (default). SENT to extract relation bearing sentences." )
                                .create( "show" );
        Option debug = new Option("debug", "show debugging information");
        Option exMethod   = OptionBuilder.withArgName( "method" )
                                .hasArg()
                                .withDescription(  "use specified extraction method. RULE for rule-based extraction (Extension of RelEx) (default), ML for machine learning based extraction." )
                                .create( "ex" );
        Options options = new Options();
        options.addOption(help);
        options.addOption(inFile);
        options.addOption(outFile);
        options.addOption(domainFile);        
        options.addOption(stanfordFile);        
        options.addOption(outputMode);
        options.addOption(debug);
        options.addOption(exMethod);
        return options;
    } 
    
    private static Sentence simplify(Sentence s) {
        Sentence newS = new Sentence();
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().equals("(")) {
                boolean found = false;                
                int end = s.size() - 1;
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().equals(")")) {
                        end = j;
                        break;
                    }
                    if (s.get(j).word().contains("PROTEIN")) {
                        found = true;
                    }
                }
                if (!found) {
                    i = end;
                } else {
                    newS.add(s.get(i));
                }
            } else {
                newS.add(s.get(i));
            }
        }
        return newS;
    }
    public static void process(CommandLine commandLine) throws IOException, Exception {
        File inputFile = new File(commandLine.getOptionValue("input"));
        if (!inputFile.exists()) {
            throw new Exception(inputFile.getAbsolutePath() + " does not exist.");
        } else if (inputFile.isDirectory()) { //batch mode
            if (!commandLine.hasOption("output")) {
                throw new Exception("For batch processing you must provide the output directory through the -output parameter.");
            } else {
                File outputFile = new File(commandLine.getOptionValue("output"));
                if (!outputFile.exists()) {
                    if (commandLine.hasOption("debug")) {
                        System.out.println(outputFile.getAbsoluteFile() + " does not exist. Creating it..");                        
                    }
                    outputFile.mkdir();
                } else if (!outputFile.isDirectory()) {
                    throw new Exception("For batch processing the argument to -output should be a directory.");
                }
            }
        }
        BioDomainAnnotator bioDomainAnnotator;
        RunRelex relex;        
        
        if (commandLine.hasOption("relterms")) {
            bioDomainAnnotator = new BioDomainAnnotator(commandLine.getOptionValue("relterms"));
            relex = new RunRelex(commandLine.getOptionValue("relterms"));
        } else {
            bioDomainAnnotator = new BioDomainAnnotator();
            relex = new RunRelex();
        }        
                        
        ParserAnnotator parser;
        if (commandLine.hasOption("stanford")) {
            parser = new ParserAnnotator(commandLine.getOptionValue("stanford"));
        } else {
            parser = new ParserAnnotator();
        }
                
        
        String mode = "REL";
        if (commandLine.hasOption("show")) {
            mode = commandLine.getOptionValue("show");
        }
        
        boolean debug = false;
        if (commandLine.hasOption("debug")) {
            debug = true;
        }
        
        boolean rule = true;
        classifier = null;
        if (commandLine.hasOption("ex")) {
            String method = commandLine.getOptionValue("ex");
            if (method.equals("ML")) {
                rule = false;
                classifier = loadClassifier("./resource/ml/models/PPI/all_ppi.model");
                if (classifier == null) {
                    throw new Exception("Can not load model from ./resource/ml/models/PPI/all_ppi.model");
                }
            }
        } 
        if (!inputFile.isDirectory()) {
            BufferedWriter outputWriter = null;
            if (commandLine.hasOption("output")) {
                File outputFile = new File(commandLine.getOptionValue("output"));
                outputWriter = new BufferedWriter(new FileWriter(outputFile));
            } else {
                outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            }
            
            Text inputText = readMarkedUpFile(inputFile);            
            process(inputText, outputWriter, bioDomainAnnotator, relex, parser, mode, debug, rule);
            outputWriter.close();
            
        } else { //batch processing
            File[] inputFiles = inputFile.listFiles(new FileExtensionFilter("txt"));
            File outputDir = new File(commandLine.getOptionValue("output"));
            for (File file : inputFiles) {
                if (debug) {
                    System.out.println("Processing: " + file.getAbsolutePath());
                }
                File outputFile = new File(outputDir, file.getName());
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                
                Text inputText = readMarkedUpFile(inputFile);                
                process(inputText, writer, bioDomainAnnotator, relex, parser, mode, debug, rule);                
                writer.close();                
                System.gc();
            }
        }
        
    }
    /**
     * The markup format follows the following rules:
     * 1. Each word/token is on a separate line
     * 2. Each token is assigned a tag (BG, IG, BP, IP, O), where,
     * B-G - First token of a genotype
     * I-G - other token of a genotype for multi-word genotype
     * B-P - First token of a phenotype
     * I-P - other token of a phenotype for multi-word phenotype
     * O - any other word/token
     * 3. Every line should start with a word followed by a tab and a tag
     * 4. consecutive sentences should be separated by a blank line.
     * 
     * Example:
     * Suppose "Gx A" is a genotype and "Py B" is a phenotype. Then the sentence "We found that Gx A may cause Py B ." should be represented as:
     * We O
     * found O
     * that O
     * Gx B-G
     * A   I-G
     * may  O
     * cause O
     * Py B-P
     * B I-P
     * . O
     * @param filePath 
     */
    private static Text readMarkedUpFile(File file) {
        return TEXT_READER.read(file);
        
    }
    private static Sentence prepare(Sentence oldS, HashMap<String, String> map) {        
        Sentence s = new Sentence();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < oldS.size(); i++) {            
            String word = oldS.get(i).word();
            String tag = oldS.get(i).getTag("TAG");
            
            if (!tag.startsWith("B")) {
                s.add(new TokWord(word));                
            } else {
                entityCount++;
                String prefix = "GENOTYPE";
                if (tag.equals("BP")) {
                    prefix = "PHENOTYPE";
                }
                s.add(new TokWord("PROTEIN" + count));
                sb = new StringBuilder();
                sb.append(word);
                
                int j = i + 1;
                while (j < oldS.size()) {
                    word = oldS.get(j).word();
                    tag = oldS.get(j).getTag("TAG");                    
                    if (!tag.startsWith("I")) {
                        break;
                    }
                    sb.append(" ").append(word);
                    j++;
                }
                i = j - 1;
                map.put("PROTEIN" + count, prefix + "-"+ sb.toString());
                count++;
            }
        }
        return s;
    }
    
    private static void process(Text text, BufferedWriter outputWriter, BioDomainAnnotator bioDomainAnnotator, RunRelex relex, ParserAnnotator parser, String mode, boolean debug, boolean rule) throws IOException {
        for (Sentence s : text) {            
            String origSentence = s.toString();
            String origSentenceWithTag = s.toString("TAG");
            HashMap<String, String> map = new HashMap<String, String>();
            entityCount = 0;
            s = prepare(s, map);
            Set<Pair<Integer, Integer>> predictedInteractions;
            
            SimpleDepGraph ccDepGraph = null;
            if (entityCount > 1) {
                s = simplify(s);
                s = parser.annotate(s);
                relex.assignPOS(s);
                s = bioDomainAnnotator.annotate(s);
                ccDepGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
                if (debug) {
                    System.out.println("-----------------");
                    System.out.println("Original sentence: " + origSentence);
                    System.out.println("Original sentence with tags: " + origSentenceWithTag);                    
                    System.out.println("Simplified sentence: " + s);
                    System.out.println("Syntax tree: ");
                    s.getParseTree().pennPrint();
                    System.out.println("");
                    System.out.println("Dependency structure: \n" + ccDepGraph.toString(s.getParseTree()));                    
                }
                if (rule) {
                    predictedInteractions = relex.getPredictedInteractions(s, ccDepGraph);
                } else {                    
                    SimpleDepGraph depGraph = new SimpleDepGraph(s.getParseTree(), null);
                    predictedInteractions = getMLPredictions(s, depGraph, ccDepGraph);
                }                     
            } else {
                if (debug) {
                    System.out.println("Original sentence: " + origSentence);
                    System.out.println("Trivial: No relation in the sentence.\n");
                }
                predictedInteractions = new HashSet<Pair<Integer, Integer>>();
            }
                              
            //remove all other than the genotype-phenotype interactions
            predictedInteractions = filterRelations(s, map, predictedInteractions);
            
            if (mode.equals("SENT")) {
                if (predictedInteractions.isEmpty()) {                    
                    outputWriter.write("No\t" + origSentence + "\n");
                } else {
                    outputWriter.write("Yes\t" + origSentence + "\n");                    
                }
            } else {
                outputWriter.write(origSentence + "\n");
                outputWriter.write(predictedInteractions.size() + "\n");
                if (debug && predictedInteractions.size() > 0) {
                    System.out.println("Dependency paths");
                }
                for (Pair<Integer, Integer> pair : predictedInteractions) {                    
                    if (debug) {
                        //System.out.println(ccDepGraph.getPathAsList(pair.first(), pair.second(), false));
                        printDependencyPath(s, ccDepGraph, pair.first(), pair.second());
                    }
                    int agent = pair.first();
                    int patient = pair.second();
                    outputWriter.write("["+map.get(s.get(agent).word())+"]" + "\t");
                    outputWriter.write("["+map.get(s.get(patient).word())+"]" + "\n");
                }                
            }
            if (debug) {
                System.out.println("-----------------\n");
            }
            outputWriter.flush();
        }
    }
    
    private static void printDependencyPath(Sentence s, SimpleDepGraph depGraph, int entity1, int entity2) {
        List<SimpleDependency> pathAsRelnList = depGraph.getPathAsRelnList(entity1, entity2, false);
        for (SimpleDependency reln : pathAsRelnList) {
            System.out.print("(" + s.get(reln.gov()) + "-" + reln.reln() + "-" + s.get(reln.dep()) + ") ");
        }
        System.out.println("");
    }
    private static HashSet<Pair<Integer, Integer>> filterRelations(Sentence s, HashMap<String, String> map, Set<Pair<Integer, Integer>> relations) {
        HashSet<Pair<Integer, Integer>> filteredRelations = new HashSet<Pair<Integer, Integer>>();
        for (Pair<Integer, Integer> pair : relations) {
            String entity1 = s.get(pair.first()).word();
            String entity2 = s.get(pair.second()).word();
            String type1 = map.get(entity1);
            String type2 = map.get(entity2);
            
            if (type1.startsWith("GENOTYPE") && type2.startsWith("PHENOTYPE")) {
                filteredRelations.add(pair);
            } else if (type2.startsWith("GENOTYPE") && type1.startsWith("PHENOTYPE")) {
                filteredRelations.add(pair);
            }
        }
        return filteredRelations;
    }
    private static HashSet<Pair<Integer, Integer>> getMLPredictions(Sentence s, SimpleDepGraph depGraph, SimpleDepGraph ccDepGraph) {
        HashSet<Pair<Integer, Integer>> predictions = new HashSet<Pair<Integer, Integer>>();
        for (int i = 0; i < s.size(); i++) {
            if (s.get(i).word().contains("PROTEIN")) {
                for (int j = i + 1; j < s.size(); j++) {
                    if (s.get(j).word().contains("PROTEIN")) {
                        PPIInstance ppiInstance = new PPIInstance(s, depGraph, ccDepGraph, null, -1, i, j, true);                                                
                        Instance instance = classifier.getInstancePipe().instanceFrom(ppiInstance);
                        Classification classification = classifier.classify(instance);
                        if (classification.bestLabelIsCorrect()) {
                            predictions.add(new Pair<Integer, Integer>(i, j));
                        }
                    }
                }
            }
        }
        return predictions;
    }
    
    private static Classifier loadClassifier(String modelPath) {
        Classifier classifier = null;
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(modelPath));
            classifier =  (Classifier) ois.readObject();                        
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return classifier;
    }
            
    
    public static void main(String args[]) {   
        try {
            //args = new String[]{"-input", "example.txt", "-ex", "ML", "-debug"};
            CommandLineParser parser = new PosixParser();
            Options options = getOptions();
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("input")) {
                process(line);            
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("WGPRelEx", options);
            }
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println("IO error occured: " + ex.getMessage());
            ex.printStackTrace();
        }  catch (Exception ex) {
            System.out.println(ex.getMessage()); 
            ex.printStackTrace();
        }
    }
    
    
}
