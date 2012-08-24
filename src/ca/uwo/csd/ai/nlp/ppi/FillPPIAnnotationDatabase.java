/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.uwo.csd.ai.nlp.ppi;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.analyzers.SyntaxTreeAnalyzer;
import ca.uwo.csd.ai.nlp.ppi.filter.CorpusReader;
import ca.uwo.csd.ai.nlp.ppi.filter.RelationInstance;
import ca.uwo.csd.ai.nlp.utils.DBUtil;
import ca.uwo.csd.ai.nlp.utils.SocketUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class FillPPIAnnotationDatabase {

    private final static SyntaxTreeAnalyzer treeAnalyzer = new SyntaxTreeAnalyzer();
    private DBUtil dbUtil;

    public FillPPIAnnotationDatabase() {
        dbUtil = new DBUtil("127.0.0.1", "3306", "root", "");        
        dbUtil.connect("ppiannotation");
    }

    public static void main(String[] args) throws IOException {
        FillPPIAnnotationDatabase filler = new FillPPIAnnotationDatabase();
        /*filler.firstInsert("./resource/relation/PPI4", "LLL");
        filler.firstInsert("./resource/relation/PPI4", "HPRD50");
        filler.firstInsert("./resource/relation/PPI4", "IEPA");
        filler.firstInsert("./resource/relation/PPI4", "AIMed");
        filler.firstInsert("./resource/relation/PPI4", "BioInfer");*/

        //filler.writeDepGraphs("./resource/relation/PPI4");
        filler.update("./resource/relation/PPI6");
    }

    public void writeDepGraphs(String ppibase) throws IOException {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};
        SocketUtil socketUtil = new SocketUtil("localhost", 8662);
        CorpusReader reader = new CorpusReader();
        for (String corpus : corpora) {
            HashSet<String> set = new HashSet<String>();
            List<RelationInstance> relationInstances = reader.getRelationInstances(new File(ppibase, corpus).getAbsolutePath());
            for (RelationInstance instance : relationInstances) {
                if (instance.interaction) {
                    String sentId = instance.pairIds[0];
                    sentId = sentId.substring(0, sentId.lastIndexOf('.'));
                    if (!set.contains(sentId)) {
                        String tree = treeAnalyzer.getPennOutput(instance.s.getParseTree());
                        socketUtil.sendLine(sentId + "\t" + tree + "\n");
                        set.add(sentId);
                    }
                }
            }
        }
    }

    public void update(String ppiBase) {
        String[] corpora = {"LLL", "HPRD50", "IEPA", "AIMed", "BioInfer"};        
        String update = "UPDATE %s SET entity1='%s',entity2='%s',syntax='%s',path='%s',type='%s',lcs='%s',lcs_pos='%s',keyterm='%s' where pairid='%s'";
        for (String corpus : corpora) {
            System.out.println("Updating.." + corpus);
            String tableName = corpus.toLowerCase() + "s";
            List<RelationInstance> relationInstances = new CorpusReader().getRelationInstances(new File(ppiBase, corpus).getAbsolutePath());
            for (RelationInstance instance : relationInstances) {
                if (instance.interaction /*&& isEligible(instance.s)*/ && instance.path != null) {
                    List<String> values = getValues(instance);
                    String entity1 = values.get(2);
                    String entity2 = values.get(3);
                    String syntax = values.get(6);
                    String path = values.get(4);
                    String type = values.get(5);
                    String lcs = values.get(7);
                    String lcs_pos = values.get(8);
                    String keyterm = values.get(9);
                    String id = instance.pairIds[0];
                    String updateQuery = String.format(update, tableName, entity1, entity2, syntax, path, type, lcs, lcs_pos, keyterm, id);
                    //System.out.println(updateQuery);
                    dbUtil.execUpdate(updateQuery);
                }
            }
        }
    }

    private boolean isEligible(Sentence s) {
        return s.toString().contains("PROTEIN10");
    }

    public void firstInsert(String ppiBase, String corpusName) {
        String query = "insert into " + corpusName.toLowerCase() + "s(pairid, sentence, entity1, entity2, path, type, syntax, lcs, lcs_pos, keyterm) values";
        List<RelationInstance> relationInstances = new CorpusReader().getRelationInstances(new File(ppiBase, corpusName).getAbsolutePath());
        for (RelationInstance instance : relationInstances) {
            if (instance.interaction && instance.path != null) {
                String q = query + getSQLValues(getValues(instance));
                dbUtil.execUpdate(q);
            }
        }
    }

    private List<String> getValues(RelationInstance instance) {
        List<String> values = new ArrayList<String>();
        values.add(instance.pairIds[0]); //pairid
        values.add(instance.s.toString()); //sentence

        String entity1 = instance.s.get(instance.entity1).word();
        String entity2 = null;
        //potential problem
        //PROT11 - PROT1: PROT11 contains PROT1
        if (entity1.contains(instance.pairIds[1])) {
            //entity1 = instance.pairIds[1];
            if (entity1.equals(instance.pairIds[1])) {
                entity1 = instance.pairIds[1];
                entity2 = instance.pairIds[2];
            } else {
                String token = entity1;
                String p1 = instance.pairIds[1];
                int startPos = token.indexOf(p1);
                int next = startPos + p1.length();
                if (next == token.length()) {
                    entity1 = instance.pairIds[1];
                    entity2 = instance.pairIds[2];
                } else {
                    char nextChar = token.charAt(next);
                    if (!Character.isDigit(nextChar)) {
                        entity1 = instance.pairIds[1];
                        entity2 = instance.pairIds[2];
                    } else {
                        entity1 = instance.pairIds[2];
                        entity2 = instance.pairIds[1];
                    }
                }
            }
        } else {
            entity1 = instance.pairIds[2];
            entity2 = instance.pairIds[1];
        }        

        values.add(entity1);
        values.add(entity2);

        values.add(convertBackbone(instance));
        values.add(getType(instance));
        values.add(treeAnalyzer.getPennOutput(instance.s.getParseTree()));

        values.add(instance.s.get(instance.lcs).word());
        values.add(instance.s.get(instance.lcs).getTag("POS"));
        values.add(instance.s.get(instance.key).word());

        //return getSQLValues(values);
        return values;
    }

    private String getSQLValues(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("'");
            sb.append(getSafeValue(value));
            sb.append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    private String getSafeValue(String value) {
        return value.replace("'", "\'");
    }

    private String getType(RelationInstance instance) {
        if (instance.lcs < instance.entity1) {
            return "LEFT";
        } else if (instance.lcs <= instance.entity2) {
            return "MIDDLE";
        } else {
            return "RIGHT";
        }
    }

    private String convertBackbone(RelationInstance instance) {
        List<String> path = instance.path;
        Sentence s = instance.s;
        int lcsIndex = instance.lcsIndex;
        int sz = path.size();
        String[] node = new String[sz];

        for (int i = 0; i < lcsIndex; i++) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String word = s.get(index).word();
                if (s.get(index).getTag("DOMAIN") != null) {
                    word += "*";
                }
                if (index == instance.key) {
                    word += "^";
                }
                if (i == 0) {
                    node[i] = word;
                } else {
                    node[i] = "(" + word + " " + node[i - 1] + ")";
                }
            } else {
                String reln = path.get(i).substring(1);
                if (reln.contains("_")) {
                    reln = reln.substring(reln.indexOf('_') + 1);
                }
                node[i] = "(" + reln + " " + node[i - 1] + ")";
            }
        }
        for (int i = sz - 1; i > lcsIndex; i--) {
            if (i % 2 == 0) {
                int index = Integer.parseInt(path.get(i));
                String word = s.get(index).word();
                if (s.get(index).getTag("DOMAIN") != null) {
                    word += "*";
                }
                if (index == instance.key) {
                    word += "^";
                }
                if (i == (sz - 1)) {
                    node[i] = word;
                } else {
                    node[i] = "(" + word + " " + node[i + 1] + ")";
                }
            } else {
                String reln = path.get(i);
                if (reln.contains("_")) {
                    reln = reln.substring(reln.indexOf('_') + 1);
                }
                node[i] = "(" + reln + " " + node[i + 1] + ")";
            }
        }

        String lcsWord = s.get(instance.lcs).word();
        if (s.get(instance.lcs).getTag("DOMAIN") != null) {
            lcsWord += "*";
        }
        if (instance.lcs == instance.key) {
            lcsWord += "^";
        }
        node[lcsIndex] = "(" + lcsWord + " " + node[lcsIndex - 1] + " " + node[lcsIndex + 1] + ")";

        return node[lcsIndex];
    }
}

