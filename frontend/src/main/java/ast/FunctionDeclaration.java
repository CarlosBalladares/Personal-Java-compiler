package ast;

import util.ImpTable;
import visitor.Visitor;


/**
 * Created by daniel on 2017-01-27.
 */
public class FunctionDeclaration extends Statement {
    public final Type type;
    public final String name;
    public final NodeList<Statement> assn;
    public final FormalList fl;
    public final Expression value;

    public FunctionDeclaration(Type type, String name, FormalList fl, NodeList<Statement> assn, Expression value) {
        super();
        this.type = type;
        this.name = name;
        this.assn = assn;
        this.fl = fl;
        this.value = value;



    }


    //Added for typechecking purposes..
    public ImpTable<Type> vars;



    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
