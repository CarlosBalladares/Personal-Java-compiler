package ast;

import visitor.Visitor;

/**
 * Created by daniel on 2017-01-29.
 */
public class ListElement extends AST {
    //Consists of type + id
    public final Type t;
    public final String id;

    public ListElement(Type t, String id){
        this.t = t;
        this.id = id;
    }
    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
