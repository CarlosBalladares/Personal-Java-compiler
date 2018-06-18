package visitor;

import java.io.PrintWriter;

import ast.*;

import util.IndentingWriter;


/**
 * This is an adaptation of the PrettyPrintVisitor from the textbook
 * online material, but updated to work with the "modernized"
 * Visitor and our own versions of the AST classes.
 * <p>
 * This version is also cleaned up to actually produce *properly* indented
 * output.
 *
 * @author kdvolder
 */
public class PrettyPrintVisitor implements Visitor<Void> {

    /**
     * Where to send out.print output.
     */
    private IndentingWriter out;

    public PrettyPrintVisitor(PrintWriter out) {
        this.out = new IndentingWriter(out);
    }

    ///////////// Visitor methods /////////////////////////////////////////

    @Override
    public Void visit(Program n) {
        n.statements.accept(this);
        n.print.accept(this);
        return null;
    }

    @Override
    public Void visit(BooleanType n) {
        out.print("boolean");
        return null;
    }

    @Override
    public Void visit(UnknownType n) {
        out.print("unknown");
        return null;
    }

    @Override
    public Void visit(IntegerType n) {
        out.print("int");
        return null;
    }

    @Override
    public Void visit(Conditional n) {
        out.print("( ");
        n.e1.accept(this);
        out.print(" ? ");
        n.e2.accept(this);
        out.print(" : ");
        n.e3.accept(this);
        out.print(" )");
        return null;
    }

    @Override
    public Void visit(Print n) {
        out.print("print ");
        n.exp.accept(this);
        out.println();
        return null;
    }

    @Override
    public Void visit(Assign n) {
        out.print(n.name + " = ");
        n.value.accept(this);
        out.println(";");
        return null;
    }

    @Override
    public Void visit(LessThan n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" < ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Plus n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" + ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Minus n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" - ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Times n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" * ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(IntegerLiteral n) {
        out.print("" + n.value);
        return null;
    }

    @Override
    public Void visit(IdentifierExp n) {
        out.print(n.name);
        return null;
    }

    @Override
    public Void visit(Not n) {
        out.print("!");
        n.e.accept(this);
        return null;
    }

    @Override
    public <T extends AST> Void visit(NodeList<T> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.elementAt(i).accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FormalList n) {
        for (int i = 0; i < n.size(); i++) {

            ListElement le =  n.elementAt(i);
            le.t.accept(this);
            out.print(" " + le.id);
            if(i <= (n.size()-1) && (i != n.size()-1))
                out.print(", ");
        }
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration n) {
        n.type.accept(this);
        out.print(" " + n.name);
        out.print("(");
        n.fl.accept(this);
        out.print(") { ");
        n.assn.accept(this);
        out.print("return ");
        n.value.accept(this);
        out.print("; }\n");

        return null;
    }

    @Override
    public Void visit(ExpressionList n) {

        for (int i = 0; i < n.size(); i++) {
            n.elementAt(i).accept(this);
            if(i <= (n.size()-1) && (i != n.size()-1))
                out.print(", ");
        }
        return null;
    }

    @Override
    public Void visit(PrimaryExpression n) {
        out.print(n.fi);
        out.print("(");
        n.el.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(ListElement n){
        n.t.accept(this);
        out.print(" " + n.id);
        return null;
    }

//    @Override
//    public Void visit(TypeContext n) {
//        return null;
//    }
}
