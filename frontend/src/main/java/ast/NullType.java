package ast;

import visitor.Visitor;

/**
 * Created by carlosballadares on 2017-04-23.
 */
public class NullType extends Type {
    @Override
    public boolean equals(Object other) {
            return this.getClass() == other.getClass();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }
}
