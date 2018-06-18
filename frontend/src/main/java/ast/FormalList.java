package ast;

import visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 2017-01-27.
 */
public class FormalList extends AST {

    // Directly taken from NodeList, edited specifically to handle ListElements
    // that is, a type and identifier

    //Could clean up some arg names..
    private List<ListElement> nodes;

    public FormalList() {
        this.nodes = new ArrayList<ListElement>();
    }

    public FormalList(List<ListElement> nodes) {
        this.nodes = nodes;
    }

    public void add(ListElement t) {
        this.nodes.add(t);
    }

    public int size() {
        return nodes.size();
    }

    public ListElement elementAt(int i) {
        return nodes.get(i);
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}

