package typechecker.implementation;


import java.util.ArrayList;
import java.util.List;

import ast.*;

import typechecker.ErrorReport;
import util.ImpTable;
import visitor.Visitor;

/**
 * This class implements Phase 2 of the Type Checker. This phase
 * assumes that we have already constructed the program's symbol table in
 * Phase1.
 * <p>
 * Phase 2 checks for the use of undefined identifiers and type errors.
 * <p>
 * Visitors may return a Type as a result. Generally, only visiting
 * an expression or a type actually returns a type.
 * <p>
 * Visiting other nodes just returns null.
 *
 * @author kdvolder
 */
public class TypeCheckVisitor implements Visitor<Type> {

    /**
     * The place to send error messages to.
     */
    private ErrorReport errors;

    /**
     * The symbol table from Phase 1.
     */
    private ImpTable<Type> variables;


    public TypeCheckVisitor(ImpTable<Type> variables, ErrorReport errors) {
        this.variables = variables;
        this.errors = errors;
    }

    //// Helpers /////////////////////

    /**
     * Check whether the type of a particular expression is as expected.
     */
    private void check(Expression exp, Type expected) {
        Type actual = exp.accept(this);
        if (!assignableFrom(expected, actual))
            errors.typeError(exp, expected, actual);
    }

    /**
     * Check whether two types in an expression are the same
     */
    private void check(Expression exp, Type t1, Type t2) {
        if (!t1.equals(t2))
            errors.typeError(exp, t1, t2);
    }

    private boolean assignableFrom(Type varType, Type valueType) {
        return varType.equals(valueType);
    }

    ///////// Visitor implementation //////////////////////////////////////


// TODO PHASE2: Debug and additional testing

    @Override
    public Type visit(FunctionDeclaration n) {
        //The fnVisitor checks the list of Assign() against the expected types defined at local scope
        TypeCheckVisitor fnVisitor = new TypeCheckVisitor(n.vars, errors);
        n.assn.accept(fnVisitor);
        Type expectedType = n.type;
//<<<<<<< HEAD
//
//
//
//        Type actualType=       n.value.accept(this);
//
//        if(!expectedType.equals(actualType))
//=======
        //Value is defined locally so we visit this with the fnVisitor
        Type actualType = n.value.accept(fnVisitor);
        if(!expectedType.equals(actualType)) {
            errors.typeError(n.value, expectedType, actualType);
        }
        return null;
    }

    @Override
    public Type visit(PrimaryExpression n) {
//        Type type = variables.lookup(n.fi);
//        if (type == null)
//            type = new UnknownType();
//        return type;
        // Checks each expression in the list against the variable type
//<<<<<<< HEAD
        Type actualType = variables.lookup(n.fi);
        for (int i = 0; i < n.el.size(); i++) {
            Expression checkExpression = n.el.elementAt(i);
            check(checkExpression, actualType);
        }

        return n.getType();
//=======
//        TypeCheckVisitor fnVisitor = new TypeCheckVisitor(n.vars, errors);
//        n.el.accept(fnVisitor);
//        Type expectedType = variables.lookup(n.fi);
//        Type actualType = fnVisitor.visit(n.el);
////        ArrayList<Type> typeContext = new ArrayList<Type>();
////        typeContext.add()
//        return null;
//>>>>>>> 42f11678be073b5d4a0005aee75e4af600d0f878
    }

    @Override
    public Type visit(FormalList ns) {
        for (int i = 0; i < ns.size(); i++){
            ns.elementAt(i).accept(this);
        }
        return null;
    }

    @Override
    public Type visit(ListElement n){
        //A ListElement (Type Identifier) is trivially defined
        Type type = n.t;

        return type;
    }

// prima

    @Override
    public Type visit(ExpressionList ns) {
        for (int i = 0; i < ns.size(); i++){
            ns.elementAt(i).accept(this);
        }
        return null;    }



    @Override
    public <T extends AST> Type visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++) {
            ns.elementAt(i).accept(this);
        }
        return null;
    }

    @Override
    public Type visit(Program n) {
        //		variables = applyInheritance(variables);
        n.statements.accept(this);
        n.print.accept(this);
        return null;
    }

    @Override
    public Type visit(BooleanType n) {
        return n;
    }

    @Override
    public Type visit(IntegerType n) {
        return n;
    }

    @Override
    public Type visit(UnknownType n) {
        return n;
    }

    /**
     * Can't use check, because print allows either Integer or Boolean types
     */
    @Override
    public Type visit(Print n) {
        Type actual = n.exp.accept(this);
        if (!assignableFrom(new IntegerType(), actual) && !assignableFrom(new BooleanType(), actual)) {
            List<Type> l = new ArrayList<Type>();
            l.add(new IntegerType());
            l.add(new BooleanType());
            errors.typeError(n.exp, l, actual);
        }
        return null;
    }

    @Override
    public Type visit(Assign n) {
        Type expressionType = n.value.accept(this);
        variables.set(n.name, expressionType);
        return null;
    }

    @Override
    public Type visit(Conditional n) {
        check(n.e1, new BooleanType());
        Type t2 = n.e2.accept(this);
        Type t3 = n.e3.accept(this);
        check(n.e3, t2, t3);
        return t2;
    }

    @Override
    public Type visit(LessThan n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(Plus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Minus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Times n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IntegerLiteral n) {
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IdentifierExp n) {
        Type type = variables.lookup(n.name);
        if (type == null)
            type = new UnknownType();
        return type;
    }

    @Override
    public Type visit(Not n) {
        check(n.e, new BooleanType());
        n.setType(new BooleanType());
        return n.getType();
    }


}