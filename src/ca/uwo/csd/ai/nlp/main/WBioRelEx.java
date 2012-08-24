
package ca.uwo.csd.ai.nlp.main;

import abner.Tagger;
import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
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
public class WBioRelEx {
    private static int entityCount;
    private static Classifier classifier;
    
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
        Option abnerModel   = OptionBuilder.withArgName( "model" )
                                .hasArg()
                                .withDescription(  "use model trained on either NLPBA or BioCreative. NLPBA is used by default" )
                                .create( "abner" );
        Option stanfordFile   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "read Stanford parser from specified path. default path is ./lib/EnglishPCFG.ser.gz" )
                                .create( "stanford" );
        Option openNLPModel   = OptionBuilder.withArgName( "model" )
                                .hasArg()
                                .withDescription(  "use specified OpenNLP sentence boundary model. default is ./resource/ml/models/OpenNLP/en-sent.bin" )
                                .create( "opennlp" );
        Option outputMode   = OptionBuilder.withArgName( "mode" )
                                .hasArg()
                                .withDescription(  "use specified output mode. SENT to extract relation bearing sentences. REL to extract relations." )
                                .create( "show" );
        Option debug = new Option("debug", "show debugging information");
        Option exMethod   = OptionBuilder.withArgName( "method" )
                                .hasArg()
                                .withDescription(  "use specified extraction method. RULE for rule-based extraction (Extension of RelEx), ML for machine learning based extraction" )
                                .create( "ex" );
        Option neLexicon   = OptionBuilder.withArgName( "file" )
                                .hasArg()
                                .withDescription(  "use the named entity lexicon provided in specified file" )
                                .create( "lexicon" );
        Options options = new Options();
        options.addOption(help);
        options.addOption(inFile);
        options.addOption(outFile);
        options.addOption(domainFile);
        options.addOption(abnerModel);
        options.addOption(stanfordFile);
        options.addOption(openNLPModel);
        options.addOption(outputMode);
        options.addOption(debug);
        options.addOption(exMethod);
        options.addOption(neLexicon);
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

        Tagger abner = null;
        if (commandLine.hasOption("abner")) {
            String abnerModel = commandLine.getOptionValue("abner");
            if (abnerModel.equals("BioCreative")) {
                abner = new Tagger(Tagger.BIOCREATIVE);
            } else {
                abner = new Tagger();
            }
        } else {
            abner = new Tagger();
        }                
                
        
        ParserAnnotator parser;
        if (commandLine.hasOption("stanford")) {
            parser = new ParserAnnotator(commandLine.getOptionValue("stanford"));
        } else {
            parser = new ParserAnnotator();
        }
        
        OSentenceBoundaryDetector boundaryDetector;
        if (commandLine.hasOption("opennlp")) {
            boundaryDetector = new OSentenceBoundaryDetector(commandLine.getOptionValue("opennlp"));
        } else {
            boundaryDetector = new OSentenceBoundaryDetector();
        }
        
        String mode = "SENT";
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
        
        //whether to use named-entity lexicon or abner
        EntityLexicon lexicon = null;
        if (commandLine.hasOption("lexicon")) {
            String lexiconPath = commandLine.getOptionValue("lexicon");
            lexicon = new EntityLexicon(lexiconPath);
        }
        
        if (!inputFile.isDirectory()) {
            BufferedWriter outputWriter = null;
            if (commandLine.hasOption("output")) {
                File outputFile = new File(commandLine.getOptionValue("output"));
                outputWriter = new BufferedWriter(new FileWriter(outputFile));
            } else {
                outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            }
            
            String inputContent = Util.readContent(inputFile, true);
            String[] sentences = boundaryDetector.getSentences(inputContent);
            process(sentences, outputWriter, bioDomainAnnotator, relex, parser, abner, mode, debug, rule, lexicon);
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
                String inputContent = Util.readContent(file, true);                
                String[] sentences = boundaryDetector.getSentences(inputContent);
                inputContent = null;
                process(sentences, writer, bioDomainAnnotator, relex, parser, abner, mode, debug, rule, lexicon);                
                writer.close();                
                System.gc();
            }
        }
        
    }
    private static Sentence prepare(String tagIOB, HashMap<String, String> map) {
        String[] words = tagIOB.split("\n");
        Sentence s = new Sentence();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];            
            String[] tokens = word.split("\t");
            if (tokens.length < 2) { //skip blank lines
                continue;
            }
            if (!tokens[1].startsWith("B-")) {
                s.add(new TokWord(tokens[0]));                
            } else {
                entityCount++;
                s.add(new TokWord("PROTEIN" + count));
                sb = new StringBuilder();
                sb.append(tokens[0]);
                
                int j = i + 1;
                while (j < words.length) {
                    word = words[j];
                    tokens = word.split("\t");
                    if (!tokens[1].startsWith("I-")) {
                        break;
                    }
                    sb.append(" ").append(tokens[0]);
                    j++;
                }
                i = j - 1;
                map.put("PROTEIN" + count, sb.toString());
                count++;
            }
        }
        return s;
    }
    
    private static void process(String[] sentences, BufferedWriter outputWriter, BioDomainAnnotator bioDomainAnnotator, RunRelex relex, ParserAnnotator parser, Tagger abner, String mode, boolean debug, boolean rule, EntityLexicon lexicon) throws IOException {
        for (String sentence : sentences) {            
            String origSentence = sentence;
            origSentence = origSentence.replace("\n", " ");
            
            HashMap<String, String> map = new HashMap<String, String>();
            
            sentence = sentence.replaceAll("\\[[\\s\\d,;-]+\\]", "");            
            sentence = sentence.replace("\n", " ");
            
            String tagIOB = null;
            if (lexicon == null) { //use ABNER
                tagIOB = abner.tagIOB(sentence);
            } else {    //use custom tagging
                tagIOB = lexicon.tagIOB(sentence, abner);
            }
            
            //System.out.println("sent: " + sentence);
            //System.out.println(tagIOB);
            
            entityCount = 0;
            Sentence s = prepare(tagIOB, map);            
            Set<Pair<Integer, Integer>> predictedInteractions;
            
            if (entityCount > 1) {
                s = simplify(s);
                s = parser.annotate(s);
                relex.assignPOS(s);
                s = bioDomainAnnotator.annotate(s);
                SimpleDepGraph ccDepGraph = new SimpleDepGraph(s.getParseTree(), "CCProcessed");
                if (debug) {
                    System.out.println("-----------------");
                    System.out.println("Original sentence: " + origSentence);
                    System.out.println("After removing references: " + sentence);
                    System.out.print("ABNER's output: \n" + tagIOB);
                    System.out.println("Simplified sentence: " + s);
                    System.out.println("Syntax tree: ");
                    s.getParseTree().pennPrint();
                    System.out.println("");
                    System.out.println("Dependency structure: \n" + ccDepGraph.toString(s.getParseTree()));
                    System.out.println("-----------------");
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
                                    
            if (mode.equals("SENT")) {
                if (predictedInteractions.isEmpty()) {                    
                    outputWriter.write("No\t" + origSentence + "\n");
                } else {
                    outputWriter.write("Yes\t" + origSentence + "\n");                    
                }
            } else {
                outputWriter.write(origSentence + "\n");
                outputWriter.write(predictedInteractions.size() + "\n");
                for (Pair<Integer, Integer> pair : predictedInteractions) {
                    int agent = pair.first();
                    int patient = pair.second();
                    outputWriter.write(map.get(s.get(agent).word()) + "\t");
                    outputWriter.write(map.get(s.get(patient).word()) + "\n");
                }
            }
            outputWriter.flush();
        }
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
            //args = new String[]{"-input", "tmp.txt", "-ex", "RULE", "-lexicon", "lexicon.txt", "-show", "REL"};
            CommandLineParser parser = new PosixParser();
            Options options = getOptions();
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("input")) {
                process(line);            
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("WBioRelEx", options);
            }
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println("IO error occured: " + ex.getMessage());
            ex.printStackTrace();
        }  catch (Exception ex) {
            System.out.println(ex.getMessage()); 
            //ex.printStackTrace();
        }
    }
}

class EntityLexicon 
{

    String lexiconPath;
    Set<String> tokenSet;

    public EntityLexicon(String lexiconPath) {
        this.lexiconPath = lexiconPath;        
        init();
    }
    
    private void init() {
        List<String> entityNames = Util.readLines(lexiconPath);                
        tokenSet = new HashSet<String>();
        for (String entity : entityNames) {
            String[] tokens = entity.split("\\s+");
            
            String word = tokens[0].toLowerCase();
            tokenSet.add(word);
            for (int i = 1; i < tokens.length; i++) {
                word = word + " " + tokens[i].toLowerCase();
                tokenSet.add(word);
            }
        }
    }
    
    public String tagIOB(String text, Tagger abner) {
        StringBuilder sb = new StringBuilder();
        String sentence = abner.tokenize(text.trim());
        String[] tokens = sentence.split("\\s+");
        
        for (int i = 0; i < tokens.length; i++) {            
            String word = tokens[i].toLowerCase();
            if (tokenSet.contains(word)) {
                sb.append(tokens[i] + "\t");
                sb.append("B-PROTEIN" + "\n");
                int j = i + 1;
                for (; j < tokens.length; j++) {
                    word = word + " " + tokens[j].toLowerCase();
                    if (tokenSet.contains(word)) {
                        sb.append(tokens[j] + "\t");
                        sb.append("I-PROTEIN" + "\n");
                    } else {
                        sb.append(tokens[j] + "\t");
                        sb.append("O" + "\n");
                        break;
                    }
                }
                i = j;
            } else {
                sb.append(tokens[i] + "\t");
                sb.append("O" + "\n");
            }            
        }
        sb.append("\n");
        return sb.toString();
    }
    
    
    
}