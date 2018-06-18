package translate.implementation;

import ast.Program;
import ast.Type;
import ir.frame.Frame;
import translate.Fragments;
import typechecker.TypeChecked;
import typechecker.implementation.TypeCheckerImplementation;
import util.ImpTable;
import util.Pair;
import util.Lookup;

import java.util.Map;
import java.util.function.Consumer;

public class TranslateImplementation {

    private Frame frameFactory;
    private Program program;
    private ImpTable<Pair<ImpTable<Type>, ImpTable<Type>>> table;

    public TranslateImplementation(Frame frameFactory, TypeChecked _typechecked) {
        this.frameFactory = frameFactory;
        TypeCheckerImplementation typechecked = (TypeCheckerImplementation) _typechecked;
        this.program = typechecked.getProgram();
        this.table = typechecked.getTable();
    }

    public Fragments translate() {
        ImpTable<Pair<Lookup<Type>, Lookup<Type>>> newTable= new ImpTable<>();

        table.forEach(new Consumer<Map.Entry<String, Pair<ImpTable<Type>, ImpTable<Type>>>>() {
            @Override
            public void accept(Map.Entry<String, Pair<ImpTable<Type>, ImpTable<Type>>> stringPairEntry) {
                Pair<Lookup<Type>, Lookup<Type>> newPair= new Pair(stringPairEntry.getValue().first, stringPairEntry.getValue().second);
                try{
                    newTable.put(stringPairEntry.getKey(), newPair);
                }catch(Exception e){
                    throw new Error("Error initializing translation");
                }
            }
        });

        TranslateVisitor vis = new TranslateVisitor(newTable, frameFactory);
        program.accept(vis);
        return vis.getResult();
    }

}
