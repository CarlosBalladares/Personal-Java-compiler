package visitor;

import ast.*;

/**
 * A modernized version of the Visitor interface, adapted from the textbook's
 * version.
 * <p>
 * Changes: this visitor allows you to return something as a result.
 * The "something" can be of any particular type, so the Visitor
 * uses Java generics to express this.
 *
 * @author kdvolder
 */
public interface Visitor<R> {

    //Lists
    public <T extends AST> R visit(NodeList<T> ns);

    //Declarations
    public R visit(Program n);

    //Types

    //Statements
    public R visit(Print n);

    public R visit(Assign n);

    //Expressions
    public R visit(LessThan n);

    public R visit(Conditional n);

    public R visit(Plus n);

    public R visit(Minus n);

    public R visit(Times n);

    public R visit(IntegerLiteral n);

    public R visit(IdentifierExp n);

    public R visit(Not not);

    public R visit(IntegerType n);

    public R visit(BooleanType n);

    public R visit(UnknownType n);

    //Functions phase 1

    public R visit(FormalList n);

    public R visit (FunctionDeclaration n);

    public R visit(ExpressionList n);

    public R visit(PrimaryExpression n);

    public R visit(ListElement n);

    //public R visit(TypeContext n);



}
