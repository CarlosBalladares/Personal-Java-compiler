package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import util.Pair;
import visitor.DefaultVisitor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symboltable.
 *
 * @author norm
 */
public class BuildSymbolTableVisitor extends DefaultVisitor<ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>>> {

    private final ImpTable<Type> globals = new ImpTable<Type>();
    private final ImpTable<Type> functions = new ImpTable<Type>();

    // Variables for minijava

    //This ImpTable Collects all classes in one table
    // each of the classes consists of   Pair of imptable
    private final ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> classes = new ImpTable<>();

    //This is the pair of IMpTables each has the attributes of a class and the enviromnment for  each function
   // private final Pair<ImpTable<Type>, ImpTable<Type>> aClass= new Pair<ImpTable<Type>, ImpTable<Type>>(new ImpTable<>(), new ImpTable<>());

    private final ErrorReport errors;

    private  Pair<ImpTable<Type>, ImpTable<Type>> thisClass=null;

    private  ImpTable<Type> thisFunction= null;

    private String thisClassName="";

    private boolean inherits=false;

    private int wanted=0;

    public BuildSymbolTableVisitor(ErrorReport errors) {
        this.errors = errors;
    }

    // MiniJava impl
    // I removed all previous implementations because it easier to make it from scratch

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Program n) {
        if(n.mainClass.className.equals("TreeVisitor")){
            wanted=1;
            // for debugging
        }
        n.mainClass.accept(this);
        n.classes.accept(this);

        return classes;
    }

    //MainClass	::=
    // "class" Identifier"{"
    //      "public" "static" "void" "main" "(" "String" "[" "]" Identifier ")" "{"
    //          Statement
    //      "}"
    // "}"
    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(MainClass n) {

        thisClass=getPair();
        thisClassName="main";

        thisFunction=getTable();

        FunctionType ft = new FunctionType();
        NodeList<VarDecl> mainArgs= new NodeList<VarDecl>();
        mainArgs.add(new VarDecl(new ObjectType("String"), n.argName, 0));
        ft.locals=      thisFunction;
        ft.formals=     mainArgs;
        ft.returnType=  new UnknownType();
        mainArgs.accept(this);
        n.statement.accept(this);
        n.mainFunction=ft;

        def(thisClass.second, "main",ft);
        thisFunction=null;

        def(classes, n.className, thisClass );
        thisClass= null;


        return null;
    }
    //"class" Identifier ( "extends" Identifier )? "{"
    //      ( VarDeclaration )*
    //      ( MethodDeclaration )*
    // "}"
    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(ClassDecl n) {

        thisClass=getPair();
        thisClassName=n.name;

        //Inherit from superclass
        if(n.superName!=null){
            Pair<ImpTable<Type>, ImpTable<Type>> superClassEnv= lookupSuper(n.superName);

            // Inherit class attributes
            for(Map.Entry<String, Type> attribute : superClassEnv.first)
                def(thisClass.first, attribute.getKey(), attribute.getValue());

            //Inherit functions
            for(Map.Entry<String, Type> function : superClassEnv.second)
                def(thisClass.second, function.getKey(), function.getValue());

            //TODO instead of copying we may want to just pass a pointer, but
            // that requires us to define a class as
            // Pair<The Actual Class, The Super Classs>
            // The type becomes too verbose, we may want to abstract this to a datatype
            //Pair<Pair<ImpTable<Type>, ImpTable<Type>>, Pair<ImpTable<Type>, ImpTable<Type>>> s;
            inherits=true;
            def(thisClass.first, "super", new ObjectType(n.superName));

        }

        n.vars.accept(this);
        n.methods.accept(this);

        def(classes, n.name, thisClass );
        thisClassName="";
        thisClass= null;

        inherits=false;

        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(VarDecl n) {

        if(n.kind== VarDecl.Kind.FORMAL || n.kind== VarDecl.Kind.LOCAL)
            def(thisFunction, n.name, n.type);
        else
            def(thisClass.first, n.name, n.type);
        return null;
    }
    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(MethodDecl n) {
        thisFunction= getTable();

        FunctionType ft = new FunctionType();
        ft.locals = thisFunction;
        ft.formals = n.formals;
        ft.returnType = n.returnType;
        n.formals.accept(this);
        n.vars.accept(this);
        n.returnExp.accept(this);

        def(thisClass.second, n.name,ft);
        thisFunction=null;
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(BooleanLiteral n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(ArrayLookup n) {

        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(This n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Block n) {

        n.statements.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(ArrayLength n) {
        return null;
    }


/////////////////// Phase 1 ///////////////////////////////////////////////////////
    // In our implementation, Phase 1 builds up a symbol table containing all the
    // global identifiers defined in a Functions program, as well as symbol tables
    // for each of the function declarations encountered.
    //
    // We also check for duplicate identifier definitions in each symbol table


    @Override
    public <T extends AST> ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Assign n) {
        ImpTable<Type> t = thisFunction != null ? thisFunction : globals;
        def(t, n.name.name, new UnknownType());
        n.value.accept(this);
        return null;
    }


    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(IdentifierExp n) {
        lookup(n.name);
        //lookup for the identifier in this class globals
       // thisClass.first.lookup(n.name);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Conditional n) {
        n.e1.accept(this);
        n.e2.accept(this);
        n.e3.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(BooleanType n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(IntegerType n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Print n) {
        n.exp.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(LessThan n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Plus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Minus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Times n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(IntegerLiteral n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Not not) {
        not.e.accept(this);
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(UnknownType n) {
        return null;
    }



    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(FunctionType n) {
        return null;
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Call n) {
        return null;
    }
    /**
     * Extension 1
     * */

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(BooleanExpression n) {
        return null;
    }






    ///////////////////// Helpers ///////////////////////////////////////////////
    // Lookup a name in the two symbol tables that it might be in
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

    private Pair<ImpTable<Type>, ImpTable<Type>> lookupSuper(String name) {
        Pair<ImpTable<Type>, ImpTable<Type>> superclass;

        superclass = classes.lookup(name);
        if (superclass == null)
            errors.undefinedId(name);
        return superclass;
    }




    /**
     * Add an entry to a table, and check whether the name already existed.
     * If the name already existed before, the new definition is ignored and
     * an error is sent to the error report.
     */
    private <V> void def(ImpTable<V> tab, String name, V value) {
        try {
            tab.put(name, value);
        } catch (DuplicateException e) {
            if(inherits){
                //we override
                tab.set(name, value);
            }else {
                errors.duplicateDefinition(name);
            }

        }
    }

    private Pair<ImpTable<Type>, ImpTable<Type>> getPair(){
        return new Pair<ImpTable<Type>, ImpTable<Type>>(
                new ImpTable<Type>(), new ImpTable<Type>()
            );
    }

    private  ImpTable<Type> getTable(){
        return new ImpTable<Type>();
    }

    @Override
    public ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> visit(Null n) {
        return null;
    }
}