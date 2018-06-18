
package visitor;

import ast.*;
import util.IndentingWriter;

import java.io.PrintWriter;


/**
 * This prints the structure of an AST, showing its hierarchical relationships.
 * <p>
 * This version is also cleaned up to actually produce *properly* indented
 * output.
 *
 * @author norm
 */
public class StructurePrintVisitor implements Visitor<Void> {

    /**
     * Where to send out.print output.
     */
    private IndentingWriter out;

    public StructurePrintVisitor(PrintWriter out) {
        this.out = new IndentingWriter(out);
    }

    ///////////// Visitor methods /////////////////////////////////////////

    @Override
    public Void visit(Program n) {
        out.println("Goal");
        out.indent();
        n.mainClass.accept(this);
        n.classes.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(BooleanType n) {
        out.println("BooleanType");
        return null;
    }

    @Override
    public Void visit(UnknownType n) {
        out.println("UnknownType");
        return null;
    }

    @Override
    public Void visit(IntegerType n) {
        out.println("IntegerType");
        return null;
    }

    @Override
    public Void visit(Conditional n) {
        out.println("Conditional");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        n.e3.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Print n) {
        out.println("Print");
        out.indent();
        n.exp.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Assign n) {
        out.println("Assign");
        out.indent();
        n.name.accept(this);
        n.value.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(LessThan n) {
        out.println("LessThan");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Plus n) {
        out.println("Plus");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Minus n) {
        out.println("Minus");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Times n) {
        out.println("Times");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(IntegerLiteral n) {
        out.println("IntegerLiteral " + n.value);
        return null;
    }

    @Override
    public Void visit(IdentifierExp n) {
        out.println("IdentifierExp " + n.name);
        return null;
    }

    @Override
    public Void visit(Not n) {
        out.println("Not");
        out.indent();
        n.e.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(FunctionDecl n) {
        out.println("FunctionDecl");
        out.indent();
        n.returnType.accept(this);
        new IdentifierExp(n.name).accept(this);
        n.formals.accept(this);
        n.statements.accept(this);
        n.returnExp.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(VarDecl n) {
        out.println("VarDecl");
        out.indent();
        n.type.accept(this);
        new IdentifierExp(n.name).accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(FunctionType n) {
        out.println("FunctionType");
        out.indent();
        n.formals.accept(this);
        n.returnType.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Call n) {
        out.println("Call");
        out.indent();
        n.receiver.accept(this);
        n.name.accept(this);
        n.rands.accept(this);
        out.outdent();
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
    public Void visit(MainClass n){
        out.println("MainClass "+n.className);
        out.indent();
        out.println(n.argName);
        n.statement.accept(this);
        out.outdent();
        return null;


    }

    @Override
    public Void visit(ClassDecl n) {
        out.println("ClassDecl " + n.name);
        out.indent();
        if (n.superName != null) {
            out.println("ObjectType " + n.superName);
        }
        n.vars.accept(this);
        n.methods.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(MethodDecl n) {
        out.println("MethodDecl");
        out.indent();
        n.returnType.accept(this);
        out.println(n.name);
        n.formals.accept(this);
        n.vars.accept(this);
        n.statements.accept(this);
        n.returnExp.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(IntArrayType n) {
        out.println("IntArrayType");
        return null;
    }

    @Override
    public Void visit(ObjectType n) {
        out.println("ObjectType");
        out.indent();
        out.print(n.name);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Block n) {
        out.println("Block");
        out.indent();
        n.statements.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(If n) {
        out.println("If");
        out.indent();
        n.tst.accept(this);
        n.thn.accept(this);
        n.els.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(While n) {
        out.println("While");
        out.indent();
        n.tst.accept(this);
        n.body.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(ArrayAssign n) {
        out.println("ArrayAssign");
        out.indent();
        out.print(n.name);
        n.index.accept(this);
        n.value.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(And n) {
        out.println("And");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(ArrayLookup n) {
        out.println("ArrayLookup");
        out.indent();
        n.array.accept(this);
        n.index.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(ArrayLength n) {
        out.println("ArrayLength");
        out.indent();
        n.array.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(BooleanLiteral n) {
        out.println("BooleanLiteral ");
        return null;
    }

    @Override
    public Void visit(This n) {
        out.println("This");
        return null;
    }

    @Override
    public Void visit(NewArray n) {
        out.println("NewArray");
        out.indent();
        n.size.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(NewObject n) {
        out.println("NewObject");
        out.indent();
        out.println(n.typeName);
        out.outdent();
        return null;
    }


    /**
     * Extension 1
     * */


//
//    @Override
//    public Void visit(Equals n) {
//        return null;
//    }
//
//    @Override
//    public Void visit(Or n) {
//        return null;
//    }

    @Override
    public Void visit(BooleanExpression n) {


        String op=null;

        switch(n.op){
            case AND:
                op="And";
                break;
            case EQ:
                op="Equals";
                break;
            case OR:
                op="Or";
                break;
            case LT:
                op="LessThan";
                break;
            case GT:
                op="GreaterThan";
                break;
            case LEQ:
                op="SmallerOrEqual";
                break;
            case GEQ:
                op="GreaterOrEqual";
                break;
            case NEQ:
                op="NotEquals";
                break;
            default:
                throw new Error("Unhandled Case "+n.getClass().getName());
        }



        out.println(op);
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Divide n) {
        out.println("Divide");
        out.indent();
        n.e1.accept(this);
        n.e2.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(Null n) {
        out.println("Null");
        return null;
    }

    @Override
    public Void visit(NullType n) {
        out.println("NullType");
        return null;

    }

}