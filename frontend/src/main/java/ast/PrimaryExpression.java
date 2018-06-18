package ast;

import util.ImpTable;
import visitor.Visitor;

/**
 * Created by daniel on 2017-01-27.
 */
public class PrimaryExpression extends Expression {
    public final ExpressionList el;
    public final String fi;

    public PrimaryExpression(String fi, ExpressionList el){
        super();
        this.el = el;
        this.fi = fi;
    }

    public ImpTable<Type> vars;



    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
