package ast;

import visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 2017-01-27.
 */
public class ExpressionList extends AST{

    // Directly taken from NodeList, changed to specifically handle expressions.

    private List<Expression> nodes;

    public ExpressionList() {
        this.nodes = new ArrayList<Expression>();
    }

    public ExpressionList(List<Expression> nodes) {
        this.nodes = nodes;
    }

    public void add(Expression t) {
        this.nodes.add(t);
    }

    public int size() {
        return nodes.size();
    }

    public Expression elementAt(int i) {
        return nodes.get(i);
    }
    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
