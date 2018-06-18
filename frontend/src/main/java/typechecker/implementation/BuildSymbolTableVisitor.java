package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.DefaultVisitor;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symboltable.
 *
 * @author norm
 */
public class BuildSymbolTableVisitor extends DefaultVisitor<ImpTable<Type>> {

    private final ImpTable<Type> variables = new ImpTable<Type>();
    private final ErrorReport errors;

    public BuildSymbolTableVisitor(ErrorReport errors) {
        this.errors = errors;
    }


    //Todo: PHASE2 - Implement FunctionDeclaration and PrimaryExpression

    @Override
    public ImpTable<Type> visit(FunctionDeclaration n) {
        ErrorReport fnErrors = new ErrorReport();
        BuildSymbolTableVisitor fnVisitor = new BuildSymbolTableVisitor(fnErrors);
        // Add definitions to the function declaration's symboltable
        def(variables,n.name,n.type);
        n.fl.accept(fnVisitor);
        n.assn.accept(fnVisitor);
        n.value.accept(fnVisitor);
        //Pass the local symboltable to the fndeclaration AST so TypeCheckVisitor can access.
        ImpTable<Type> vars = fnVisitor.variables;

//        vars.forEach(new Consumer<Map.Entry<String, Type>>() {
//            @Override
//            public void accept(Map.Entry<String, Type> stringTypeEntry) {
//                def(variables, n.name +"."+stringTypeEntry.getKey(), stringTypeEntry.getValue());
//            }
//        });
        n.vars = vars;

        return null;
    }

    @Override
    public ImpTable<Type> visit(PrimaryExpression n) {
        ErrorReport fnErrors = new ErrorReport();
        BuildSymbolTableVisitor fnVisitor = new BuildSymbolTableVisitor(fnErrors);
        def(variables,n.fi, new UnknownType());
        n.el.accept(fnVisitor);
        ImpTable<Type> vars = fnVisitor.variables;
        return null;
    }

    @Override
    public ImpTable<Type> visit(ListElement n){
        def(variables,n.id,n.t);
        return null;
    }


    @Override
    public ImpTable<Type> visit(ExpressionList ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }
    @Override
    public ImpTable<Type> visit(FormalList ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }


    /////////////////// Phase 1 ///////////////////////////////////////////////////////
    // In our implementation, Phase 1 builds up a single symbol table containing all the
    // identifiers defined in an Expression program.
    //
    // We also check for duplicate identifier definitions

    @Override
    public ImpTable<Type> visit(Program n) {
        n.statements.accept(this);
        n.print.accept(this); // process all the "normal" classes.
        return variables;
    }

    @Override
    public <T extends AST> ImpTable<Type> visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(Assign n) {
        n.value.accept(this);
        def(variables, n.name, new UnknownType());
        return null;
    }


    @Override
    public ImpTable<Type> visit(IdentifierExp n) {
        if (variables.lookup(n.name) == null)
            errors.undefinedId(n.name);
        return null;
    }

    @Override
    public ImpTable<Type> visit(BooleanType n) {
        return null;
    }

    @Override
    public ImpTable<Type> visit(IntegerType n) {
        return null;
    }

    @Override
    public ImpTable<Type> visit(Print n) {
        n.exp.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(LessThan n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(Conditional n) {
        n.e1.accept(this);
        n.e2.accept(this);
        n.e3.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(Plus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(Minus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(Times n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(IntegerLiteral n) {
        return null;
    }

    @Override
    public ImpTable<Type> visit(Not not) {
        not.e.accept(this);
        return null;
    }

    @Override
    public ImpTable<Type> visit(UnknownType n) {
        return null;
    }






    ///////////////////// Helpers ///////////////////////////////////////////////

    /**
     * Add an entry to a table, and check whether the name already existed.
     * If the name already existed before, the new definition is ignored and
     * an error is sent to the error report.
     */
    private <V> void def(ImpTable<V> tab, String name, V value) {
        try {
            tab.put(name, value);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }

}