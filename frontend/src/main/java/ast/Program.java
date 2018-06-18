package ast;

import visitor.Visitor;

import java.util.List;


public class Program extends AST {

    public final MainClass mainClass;
    public final NodeList<ClassDecl> classes;


    public Program(MainClass mainClass, NodeList<ClassDecl> otherClasses) {
        this.mainClass = mainClass;
        this.classes = otherClasses;

    }

    public Program(NodeList<AST> statements, Print print) {
        this.mainClass = null;
        this.classes = null;

    }

    public Program(List<AST> statements, Print print) {
        this(new NodeList<AST>(statements), print);
    }

    public Program(MainClass m, List<ClassDecl> cs) {
        this(m, new NodeList<ClassDecl>(cs));
    }

    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

}
