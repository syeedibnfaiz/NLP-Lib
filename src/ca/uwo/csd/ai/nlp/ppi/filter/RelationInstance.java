package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import java.util.List;
import kernel.ds.SparseVector;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class RelationInstance {

    final static int LEFT = 1;
    final static int MIDDLE = 2;
    final static int RIGHT = 3;
    public Sentence s;
    public SimpleDepGraph depGraph;
    public List<String> path;
    public int lcs;
    public int entity1;
    public int entity2;
    public boolean interaction;
    public String docId;
    public String[] pairIds;
    public int key;
    public int type;
    public int lcsIndex;
    public int keyIndex;    
    public String filterVerdict;
    public String verdict;
    SparseVector leftVector;
    SparseVector rightVector;
    SparseVector fullVector;

    public RelationInstance(Sentence s, SimpleDepGraph depGraph, List<String> path, int lcs, int entity1, int entity2, boolean interaction, String docId, String[] pairIds) {
        this.s = s;
        this.depGraph = depGraph;
        this.path = path;
        this.lcs = lcs;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.interaction = interaction;
        this.docId = docId;
        this.pairIds = pairIds;        

        fixPath();
        
        this.key = findKeyTerm();
        this.type = getType();
        this.lcsIndex = getIndexInPath(lcs);
        this.keyIndex = getIndexInPath(key);
        
        computeVectors();
    }

    public void computeVectors() {
        /*this.leftVector = FeatureKernel.getLeftVector(this);
        this.rightVector = FeatureKernel.getRightVector(this);
        this.fullVector = FeatureKernel.getFullVector(this, leftVector, rightVector);*/

        //SImpleFeatureKernel
        this.leftVector = SimpleFeatureKernel.getLeftVector(this);
        this.rightVector = SimpleFeatureKernel.getRightVector(this);
        this.fullVector = SimpleFeatureKernel.getFullVector(this, leftVector, rightVector);
    }

    public void setRankedKeyTerm() {
        if (path == null) {
            key = lcs;
            keyIndex = lcsIndex;
            return;
        }
        int k = -1;
        double maxRank = -1;
        int pathIndex = -1;
        for (int i = 0; i < path.size(); i += 2) {
            int index = Integer.parseInt(path.get(i));
            if (s.get(index).getTag("DOMAIN") != null && s.get(index).getTag("POS").startsWith("V")) {
                String domainTerm = s.get(index).getTag("DOMAIN");
                double rank = DomainTermRanker.getRank(domainTerm);
                if (rank > maxRank) {
                    maxRank = rank;
                    k = index;
                    pathIndex = i;
                }
            }
        }
        if (k == -1) {
            for (int i = 0; i < path.size(); i += 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null) {
                    String domainTerm = s.get(index).getTag("DOMAIN");
                    double rank = DomainTermRanker.getRank(domainTerm);
                    if (rank > maxRank) {
                        maxRank = rank;
                        k = index;
                        pathIndex = i;
                    }
                }
            }
        }
        if (k == -1) {
            key = lcs;
            keyIndex = lcsIndex;
        } else {
            key = k;
            keyIndex = pathIndex;
            lcs = key;
            lcsIndex = keyIndex;
        }

    }

    private void fixPath() {
        if (path == null) return;
        if (path.get(1).equals("-abbrev")) {
            path.remove(0);
            path.remove(0);
        }
        int sz = path.size();
        if (path.get(sz-2).equals("abbrev")) {
            path.remove(sz-1);
            path.remove(sz-2);
        }
    }
            
    private int findKeyTerm() {
        if (path == null) {
            return lcs;
        }
        int key = -1;
        if (s.get(lcs).getTag("DOMAIN") != null) {
            key = lcs;
        } else {
            for (int i = lcsIndex; i < path.size(); i += 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null && s.get(index).getTag("POS").startsWith("V")) {
                    key = index;
                    break;
                }
            }
            for (int i = lcsIndex; i >= 0; i -= 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null && s.get(index).getTag("POS").startsWith("V")) {
                    key = index;
                    break;
                }
            }
            for (int i = lcsIndex; i < path.size(); i += 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null) {
                    key = index;
                    break;
                }
            }
            for (int i = lcsIndex; i >= 0; i -= 2) {
                int index = Integer.parseInt(path.get(i));
                if (s.get(index).getTag("DOMAIN") != null) {
                    key = index;
                    break;
                }
            }
        }
        if (key == -1) {
            key = lcs;
        }
        return key;
    }

    private int getType() {
        if (lcs < entity1) {
            return LEFT;
        } else if (lcs < entity2) {
            return MIDDLE;
        } else {
            return RIGHT;
        }
    }

    private int getIndexInPath(int index) {
        if (path == null) {
            return -1;
        }
        String indexStr = String.valueOf(index);
        int dist = 0;
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).equals(indexStr)) {
                dist = i;
                break;
            }
        }
        return dist;
    }

    @Override
    public int hashCode() {
        return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RelationInstance other = (RelationInstance) obj;
        if (this.s != other.s && (this.s == null || !this.s.equals(other.s))) {
            return false;
        }
        if (this.entity1 != other.entity1) {
            return false;
        }
        if (this.entity2 != other.entity2) {
            return false;
        }
        return true;
    }
}
