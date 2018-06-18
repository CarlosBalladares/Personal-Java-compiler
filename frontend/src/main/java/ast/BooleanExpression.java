package ast;

import visitor.Visitor;

/**
 * Created by carlosballadares on 2017-04-22.
 */
public class BooleanExpression extends Expression{
    /**
     * Extension 1
     * Implemented Add additional binary arithmetic and relational operators for integers.
     * This class represents  an Binomial expression that evaluates to a boolean value
     * This is used for:
     * -AND, EQ, OR, LT so far
     * */

    public final Expression e1;
    public final Expression e2;
    public final Op op;
    public enum Op{AND, EQ, OR, LT, GT, GEQ, LEQ , NEQ}


    public BooleanExpression(Expression e1, Expression e2, Op op) {
        super();
        this.e1 = e1;
        this.e2 = e2;
        this.op=op;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
