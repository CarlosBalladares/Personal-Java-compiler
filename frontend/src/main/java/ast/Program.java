package ast;

import visitor.Visitor;

public class Program extends AST {

    public final NodeList<Statement> statements;
    public final Print print;

    public Program(NodeList<Statement> statements, Print print) {
        this.statements = statements;
        this.print = print;
    }

    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

}
