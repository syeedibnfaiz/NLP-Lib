package ca.uwo.csd.ai.nlp.ling;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Syeed Ibn Faiz
 * @param <T> 
 */
public class SimpleTree<T> {
   T data;
   SimpleTree parent;
   List<SimpleTree<T>> children;

    public SimpleTree(T data, SimpleTree parent, List<SimpleTree<T>> children) {
        this.data = data;
        this.parent = parent;
        this.children = children;
    }
   
    public void addChild(SimpleTree<T> tree) {
        if (children == null) {
            children = new ArrayList<SimpleTree<T>>();
        }
        children.add(tree);
    }
    
    public int getNumChild() {
        return (children == null)? 0: children.size();
    }
    
    public SimpleTree<T> getChild(int i) {
        return (i >= getNumChild())? null: children.get(i);
    }
    
    public boolean isLeaf() {
        return (getNumChild() == 0);
    }
    
    public boolean isPreTerminal() {
        return (getNumChild() == 1) && (children.get(0).isLeaf());
    }
    
    public void deleteChild(int i) {
        if (children != null) {
            children.remove(i);
        }
    }

    public List<SimpleTree<T>> getChildren() {
        return children;
    }

    public void setChildren(List<SimpleTree<T>> children) {
        this.children = children;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public SimpleTree getParent() {
        return parent;
    }

    public void setParent(SimpleTree parent) {
        this.parent = parent;
    }
    
    
}
