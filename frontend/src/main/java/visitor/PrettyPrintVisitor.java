package visitor;

import ast.*;
import util.IndentingWriter;

import java.io.PrintWriter;


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
//TODO: check program
    @Override
    public Void visit(Program n) {
        n.mainClass.accept(this);
        n.classes.accept(this);
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
        out.print("System.out.println(");
        n.exp.accept(this);
        out.print(");\n");
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
        out.print(" (");
        out.print("!");
        n.e.accept(this);
        out.print(" )");

        return null;
    }

    @Override
    public Void visit(FunctionDecl n) {
        n.returnType.accept(this);
        out.print(" " + n.name);
        out.print(" (");
        for (int i = 0; i < n.formals.size(); i++) {
            n.formals.elementAt(i).accept(this);
            if (i + 1 < n.formals.size()) {
                out.print(", ");
            }
        }
        out.println(") { ");
        out.indent();
        for (int i = 0; i < n.statements.size(); i++) {
            n.statements.elementAt(i).accept(this);
        }
        out.print("return ");
        n.returnExp.accept(this);
        out.println(";");
        out.outdent();
        out.println("}");
        return null;
    }

    @Override
    public Void visit(VarDecl n) {
        n.type.accept(this);
        out.print(" " + n.name);
        out.print(";");
        return null;
    }

    @Override
    public Void visit(FunctionType n) {
        out.print("function (");
        for (int i = 0; i < n.formals.size(); ++i) {
            VarDecl v = n.formals.elementAt(i);
            out.print(v.type/* + " " + v.name*/);
            if (i < n.formals.size() - 1) out.print(", ");
        }
        out.print(") -> ");
        n.returnType.accept(this);
        out.print("\n  locals ");
        out.print(n.locals);
        return null;
    }

    @Override
    public Void visit(Call n) {
        n.receiver.accept(this);
        out.print(".");
        n.name.accept(this);
        out.print("(");
        for (int i = 0; i < n.rands.size(); i++) {
            n.rands.elementAt(i).accept(this);
            if (i + 1 < n.rands.size()) {
                out.print(", ");
            }
        }
        out.println(")");
        return null;
    }

    @Override
    public <T extends AST> Void visit(NodeList<T> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.elementAt(i).accept(this);
            out.print("\n");
        }
        return null;
    }

    @Override
    public Void visit(MainClass n) {
        // TODO Auto-generated method stub
        out.print("class " + n.className + " {\n");
        out.indent();
        out.print("public static void main (String[] " + n.argName + ") {\n");
        out.indent();
        n.statement.accept(this);
        out.outdent();
        out.print( "}\n");
        out.outdent();
        out.print("}\n");
        return null;
    }

    @Override
    public Void visit(ClassDecl n) {
        // TODO Auto-generated method stub
        out.print("class " + n.name);
        if(n.superName != null){
            out.print(" extends " + n.superName);
        }
        out.print(" {\n");
        out.indent();
        n.vars.accept(this);
        n.methods.accept(this);
        out.outdent();
        out.print("}\n");
        return null;
    }

    @Override
    public Void visit(MethodDecl n) {
        // TODO Auto-generated method stub
        out.print("public ");
        n.returnType.accept(this);
        out.print(" " + n.name + "(");
        for (int i = 0; i < n.formals.size(); i++) {

            out.print(n.formals.elementAt(i).type);
            out.print(" ");
            out.print(n.formals.elementAt(i).name);

            if (i + 1 < n.formals.size()) {
                out.print(", ");
            }
        }
        out.print(" ) {\n");
        out.indent();
//        for (int i = 0; i < n.vars.size(); i++) {
//            n.vars.elementAt(i).accept(this);
//            out.print(";\n");
//        }
        n.vars.accept(this);
        n.statements.accept(this);
        out.print("return ");
        n.returnExp.accept(this);
        out.print(";\n");

        out.outdent();
        out.print("}\n");
        return null;
    }

    @Override
    public Void visit(IntArrayType n) {
        // TODO Auto-generated method stub
        out.print("int[]");
        return null;
    }

    @Override
    public Void visit(ObjectType n) {
        // TODO Auto-generated method stub
        out.print(n.name);
        return null;
    }

    @Override
    public Void visit(Block n) {
        // TODO Auto-generated method stub
        out.print("{ ");
        n.statements.accept(this);
        out.print(" }");
        return null;
    }

    @Override
    public Void visit(If n) {
        // TODO Auto-generated method stub
        out.print("if (");
        n.tst.accept(this);
        out.print(")");
        n.thn.accept(this);
        out.print(" else ");
        n.els.accept(this);
        out.print("\n");

        return null;
    }

    @Override
    public Void visit(While n) {
        // TODO Auto-generated method stub
        out.print("while (");
        n.tst.accept(this);
        out.print(")\n");
        out.indent();
        n.body.accept(this);
        out.print("\n");
        out.outdent();
        return null;
    }

    @Override
    public Void visit(ArrayAssign n) {
        // TODO Auto-generated method stub
        out.print(n.name + "[");
        n.index.accept(this);
        out.print("] = ");
        n.value.accept(this);
        out.print(";");
        return null;
    }

    @Override
    public Void visit(And n) {
        // TODO Auto-generated method stub
        out.print("( ");
        n.e1.accept(this);
        out.print(" && ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(ArrayLookup n) {
        // TODO Auto-generated method stub
        n.array.accept(this);
        out.print("[");
        n.index.accept(this);
        out.print("]");
        return null;
    }

    @Override
    public Void visit(ArrayLength n) {
        // TODO Auto-generated method stub
        n.array.accept(this);
        out.print(".length");
        return null;
    }

    @Override
    public Void visit(BooleanLiteral n) {
        // TODO Auto-generated method stub
        out.print(n.value);
        return null;
    }

    @Override
    public Void visit(This n) {
        // TODO Auto-generated method stub
        out.print("this");
        return null;
    }

    @Override
    public Void visit(NewArray n) {
        // TODO Auto-generated method stub
        out.print("new int [");
        n.size.accept(this);
        out.print("]");
        return null;
    }

    @Override
    public Void visit(NewObject n) {
        // TODO Auto-generated method stub
        out.print("new " + n.typeName + "()");
        return null;
    }

    /**
     * Extension 1
     * */

//    @Override
//    public Void visit(And n) {
//        // TODO Auto-generated method stub
//        out.print("( ");
//        n.e1.accept(this);
//        out.print(" && ");
//        n.e2.accept(this);
//        out.print(")");
//        return null;
//    }
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
        // TODO Auto-generated method stub

        String op = null;
        switch(n.op){
            case AND:
                op="&&";
                break;
            case EQ:
                op="==";
                break;
            case OR:
                op="||";
                break;
            case LT:
                op="<";
                break;
            case GT:
                op=">";
                break;
            case LEQ:
                op="<=";
                break;
            case GEQ:
                op=">=";
                break;
            case NEQ:
                op="!=";
                break;
            default:
                throw new Error("Unhandled Case");
        }



        out.print("( ");
        n.e1.accept(this);
        out.print(" "+op+" ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Divide n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" / ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Null n) {
        out.print("null");
        return null;
    }

    @Override
    public Void visit(NullType n) {
        out.print("NullType");
        return null;
    }


}
