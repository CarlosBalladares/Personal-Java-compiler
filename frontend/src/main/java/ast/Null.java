package ast;

import visitor.Visitor;

/**
 * Created by carlosballadares on 2017-04-23.
 */
public class Null extends Expression {

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public String toString() {
        return "NullExpression";
    }
}
