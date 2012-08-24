package ca.uwo.csd.ai.nlp.ppi.filter;

import ca.uwo.csd.ai.nlp.ling.Sentence;
import ca.uwo.csd.ai.nlp.ling.SimpleDepGraph;
import ca.uwo.csd.ai.nlp.ling.SimpleDependency;
import ca.uwo.csd.ai.nlp.ppi.BioDomainAnnotator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Syeed Ibn Faiz
 */
public class DomainFilter implements Filter {

    final static BioDomainAnnotator DOMAIN_ANNOTATOR = new BioDomainAnnotator();

    @Override
    public String getName() {
        return "DOMAIN_FILTER";
    }

    @Override
    public List<RelationInstance> apply(List<RelationInstance> relationInstances) {
        List<RelationInstance> newInstances = new ArrayList<RelationInstance>();
        for (RelationInstance instance : relationInstances) {
            if (apply(instance)) {
                newInstances.add(instance);
            } else {
                instance.filterVerdict = getName();
            }
        }
        return newInstances;
    }

    public boolean apply(RelationInstance instance) {
        List<String> backBonePath = instance.path;
        if (backBonePath == null) {
            return false;
        }
        Sentence s = instance.s;
        SimpleDepGraph dep = instance.depGraph;
        int lcs = instance.lcs;
        boolean domain = false;
        for (int i = 0; i < backBonePath.size(); i += 2) {
            int index = Integer.parseInt(backBonePath.get(i));
            if (s.get(index).getTag("DOMAIN") != null) {
                domain = true;
                break;
            } else {
                if (index == lcs && lcs >= instance.entity2) {
                    for (SimpleDependency dependency : dep.getDepDependencies(index)) {
                        int gov = dependency.gov();
                        String reln = dependency.reln();
                        if (!reln.matches(".*cl|ccomp|csubj|rcmod|parataxis|dep")
                                && s.get(gov).getTag("DOMAIN") != null) {
                            domain = true;
                            break;
                        }
                    }
                }
                for (SimpleDependency dependency : dep.getGovDependencies(index)) {
                    int dependent = dependency.dep();
                    String reln = dependency.reln();
                    if (s.get(dependent).getTag("DOMAIN") != null) {
                        domain = true;
                        break;
                    }
                }
            }
        }
        return domain;
    }
}
