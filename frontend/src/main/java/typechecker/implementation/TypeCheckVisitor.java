package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.Pair;
import visitor.Visitor;
import ast.BooleanExpression.Op;

import java.util.ArrayList;
import java.util.List;


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
    private ImpTable<Type> globals;
    private ImpTable<Type> functions;

    private ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> classes;
    private ImpTable<Type> thisFunction;
    private Pair<ImpTable<Type>, ImpTable<Type>> thisClass;
    private String thisClassName;
    private ImpTable<String> inheritanceTable= new ImpTable<String>();

    private Type lookup(String name) {
        Type t;
        if (thisFunction != null) {
            t = thisFunction.lookup(name);
            if (t != null)
                return t;
        }
        t = thisClass.first.lookup(name);
        if (t == null)
            errors.undefinedId(name);
        return t;
    }

    public TypeCheckVisitor(ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> variables, ErrorReport errors) {
        this.classes = variables;
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
    /**
     * checks whether two types are comparable
     * */
    private void checkCompare(Expression e1,  Expression e2) {
        Type t2= e2.accept(this);


        if(t2 instanceof IntegerType){
            check(e1, new IntegerType());
        }else if(t2 instanceof NullType || t2 instanceof ObjectType){
            Type expType = e1.accept(this);
            if(!(expType instanceof  NullType || expType instanceof ObjectType)){
                errors.typeError(e1, t2, expType);

            }
        }


    }

    private boolean assignableFrom(Type varType, Type valueType) {

        return varType.equals(valueType)|| inherits(varType, valueType) || assignableFromNull(varType, valueType);
    }

    private boolean inherits(Type a, Type b){
        // check if they inherit from one another

        if(a instanceof ObjectType && b instanceof ObjectType){
            ObjectType superType=(ObjectType) classes.lookup(((ObjectType) b).name).first.lookup("super");
            while(superType!=null){
                if(a.equals(superType))
                    return true;
                superType=(ObjectType)classes.lookup(((ObjectType) superType).name).first.lookup("super");
            }
        }

        return false;
    }

    private boolean assignableFromNull(Type a, Type b){
        // Chack if Type a can be assignmed nullType
        return a instanceof ObjectType && b instanceof NullType;
    }

    ///////// Visitor implementation //////////////////////////////////////

    @Override
    public Type visit(Program n) {
        n.mainClass.accept(this);
        n.classes.accept(this);
        return null;
    }

    @Override
    public Type visit(MainClass n) {
        thisClass=classes.lookup(n.className);
        thisClassName=n.className;
        thisFunction=((FunctionType)thisClass.second.lookup("main")).locals;
        n.statement.accept(this);

        thisFunction=null;
        thisClassName=null;
        thisClass=null;
        return null;
    }
    @Override
    public Type visit(ClassDecl n) {
        thisClass=classes.lookup(n.name);
        thisClassName=n.name;
        n.methods.accept(this);


        thisClass=null;
        thisClassName=null;
        return null;
    }

    @Override
    public Type visit(MethodDecl n) {
        thisFunction=((FunctionType)thisClass.second.lookup(n.name)).locals;
        n.statements.accept(this);

        check(n.returnExp, n.returnType);

        thisFunction=null;
        return null;
    }

    @Override
    public Type visit(Call n) {
//        Expression e = n.name;
//        FunctionType ft = null;
//        String functionName = "unknown";
//        if (e instanceof IdentifierExp)
//            functionName = ((IdentifierExp) e).name;
//        Type t = functions.lookup(functionName);
//        if (t == null) {
//            errors.undefinedId(functionName);
//            n.setType(new UnknownType());
//        } else if (!(t instanceof FunctionType)) {
//            errors.typeError(n, new FunctionType(), t);
//            n.setType(new UnknownType());
//        } else {
//            ft = (FunctionType) t;
//            n.setType(ft.returnType);
//        }
        Expression receiver = n.receiver;
        Expression name = n.name;
        FunctionType ft=null;

        if(n.name instanceof IdentifierExp) {
            //we check for the definition of name in the receiver
            //and we match the args

            Type receiverType = receiver.accept(this);

            if(receiver instanceof This){
                // Is this error message ok?
                //errors.undefinedId("Undefined receiver type");
                ft=((FunctionType) thisClass.second.lookup(((IdentifierExp) n.name).name));
            }
            else {
                Pair<ImpTable<Type>, ImpTable<Type>> recvClass=classes.lookup(((ObjectType)receiverType).name);
                ft=(FunctionType) recvClass.second.lookup(((IdentifierExp) n.name).name);


            }

        }else if (n.name instanceof ArrayLength){
            // We check that receiver is an array

            check(n.receiver, new IntArrayType());


        }
        else{
            throw new Error("Unhandled case");
        }

        if (ft != null) {
            if (n.rands.size() != ft.formals.size()) {
                errors.wrongNumberOfArguments(ft.formals.size(), n.rands.size());
            }
            for (int i = 0; i < n.rands.size(); ++i) {
                if (i < ft.formals.size()) {
                    Expression actual = n.rands.elementAt(i);
                    Type formal = ft.formals.elementAt(i).type;
                    check(actual, formal);
                }
            }
            n.setType(ft.returnType);

        }

//        if(receiver.getType()==null){
//            System.out.println("Hello");
//            throw new Error();
//        }


        // Check number and types of arguments

//        if (ft != null) {
//            if (n.rands.size() != ft.formals.size()) {
//                errors.wrongNumberOfArguments(ft.formals.size(), n.rands.size());
//            }
//            for (int i = 0; i < n.rands.size(); ++i) {
//                if (i < ft.formals.size()) {
//                    Expression actual = n.rands.elementAt(i);
//                    Type formal = ft.formals.elementAt(i).type;
//                    check(actual, formal);
//                }
//            }
//        }
        return n.getType();
    }
    @Override
    public <T extends AST> Type visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++) {
            ns.elementAt(i).accept(this);
        }
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
        boolean isLocal = thisFunction != null && thisFunction.lookup(n.name.name) != null;
        Type assignedType = n.value.accept(this);
        Type expectedType=null;
        if (isLocal) {
            //thisFunction.set(n.name.name, expressionType);
            expectedType = thisFunction.lookup(n.name.name);

        } else {
            //thisClass.first.set(n.name.name, expressionType);
            expectedType = thisClass.first.lookup(n.name.name);

        }
        if(!assignableFrom(expectedType, assignedType)){

            errors.typeError(n.name,expectedType, assignedType);
        }

        return null;
    }

    @Override
    public Type visit(LessThan n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(Conditional n) {
        check(n.e1, new BooleanType());
        Type t2 = n.e2.accept(this);
        Type t3 = n.e3.accept(this);
        check(n, t2, t3);
        return t2;
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
        List<Type> l = new ArrayList<Type>();
        Type type = lookup(n.name);
        if (type == null)
            type = new UnknownType();
        if (type instanceof FunctionType) {
            l.add(new IntegerType());
            l.add(new BooleanType());
            errors.typeError(n, l, type);
        }
        n.setType(type);
        return type;
    }

    @Override
    public Type visit(Not n) {
        check(n.e, new BooleanType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(FunctionDecl n) {
        thisFunction = n.type.locals;
        n.statements.accept(this);
        check(n.returnExp, n.returnType);
        thisFunction = null;
        return null;
    }

    @Override
    public Type visit(VarDecl n) {
        return null;
    }



    @Override
    public Type visit(FunctionType n) {

        return n;
    }







    @Override
    public Type visit(IntArrayType n) {
        return n;
    }

    @Override
    public Type visit(ObjectType n) {

        return n;
    }

    @Override
    public Type visit(Block n) {
        n.statements.accept(this);
        return null;
    }

    @Override
    public Type visit(If n) {
        check(n.tst,new BooleanType());
        n.thn.accept(this);
        n.els.accept(this);
        return null;
    }

    @Override
    public Type visit(While n) {
        check(n.tst, new BooleanType());
        n.body.accept(this);
        return null;
    }

    @Override
    public Type visit(ArrayAssign n) {
        Type recordedType=lookup(n.name);
        check(new IdentifierExp(n.name), recordedType, new IntArrayType());
        check(n.index, new IntegerType());
        check(n.value, new IntegerType());
        return null;
    }

    @Override
    public Type visit(And n) {
        check(n.e1, new BooleanType());
        check(n.e2, new BooleanType());
        //n.setType(new BooleanType());

        return new BooleanType();
    }

    @Override
    public Type visit(ArrayLookup n) {
        check(n.array, new IntArrayType());
        check(n.index, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(ArrayLength n) {
        check(n.array, new IntArrayType());
        return new IntegerType();
    }

    @Override
    public Type visit(BooleanLiteral n) {
        return new BooleanType();
    }

    @Override
    public Type visit(This n) {
        n.setType(new ObjectType(thisClassName));
        return n.getType();
    }

    @Override
    public Type visit(NewArray n) {
        check(n.size, new IntegerType());
        return new IntArrayType();
    }

    @Override
    public Type visit(NewObject n) {
        String className= n.typeName;
        if(classes.lookup(className)==null){
            return null;
        }
        n.setType(new ObjectType(className));
        return n.getType();
    }


    /**
     * Extension 1 and 2
     * */

    @Override
    public Type visit(BooleanExpression n) {

            switch (n.op){
                case AND:
                case OR:
                    check(n.e1, new BooleanType());
                    check(n.e2, new BooleanType());
                    break;

                case NEQ:
                case EQ:


                    checkCompare(n.e1, n.e2);

                    break;

                case LT:
                case GT:
                case GEQ:
                case LEQ:
                    check(n.e1, new IntegerType());
                    check(n.e2, new IntegerType());
                    break;
                default:
                    throw new Error("Unhandled Boolean expression case");
            }
            n.setType(new BooleanType());

            return n.getType();

    }

    @Override
    public Type visit(Divide n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Null n) {
        n.setType(new NullType());
        return n.getType();
    }
    @Override
    public Type visit(NullType n) {
        return n;
    }

}
