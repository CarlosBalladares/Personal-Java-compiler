package translate.implementation;

import ast.*;
import ir.frame.Frame;
import ir.temp.Label;
import ir.tree.*;
import translate.DataFragment;
import translate.Fragments;
import util.FunTable;
import util.List;


import javax.xml.soap.Node;

import static ir.tree.IR.MEM;

/**
 * Created by carlosballadares on 2017-04-04.
 */

/**
 * This is a useful class for getting the offset to a certain field in each class.
 *
 * After that we also added a reference to the super class to allow static dispatch.
 * */
public class ObjectTableFactory {
    private ClassDecl theClass;
    private ClassDecl superClass;

    public ObjectTableFactory(ClassDecl theClass, ClassDecl superClass) {
        this.theClass = theClass;
        this.superClass=superClass;
    }

    public boolean containsMethod(String name){

        for(int i=0;i<theClass.methods.size();i++){
            if(theClass.methods.elementAt(i).name.equals(name))
                return true;
        }

        return false;
    }



    public FunTable<IRExp> getEnv(Fragments frags, Frame f , IRExp addr, int wordSize){

        FunTable<IRExp> empty= FunTable.theEmpty();

        //Label g = Label.get("this");

        List l = List.list(addr);




        //IRData data = new IRData(g, l);
        //DataFragment decl = new DataFragment(f, data);




       // empty=empty.insert("this",IR.MEM(addr));
        empty = empty.insert("this", (addr));

        //frags.add(decl)
        if(superClass!=null){

            for(int i=0; i<superClass.vars.size();i++){
                empty = empty.insert(superClass.vars.elementAt(i).name, IR.MEM(IR.PLUS(addr,(i+1)*wordSize)));

            }

            int off=empty.size()-1;


            for(int i=0+off; i<(theClass.vars.size())+superClass.vars.size();i++){
                empty = empty.insert(theClass.vars.elementAt(i-off).name, IR.MEM(IR.PLUS(addr,(i+1)*wordSize)));

            }
            return empty;


        }

        else {
            for (int i = 0; i < theClass.vars.size(); i++) {
                empty = empty.insert(theClass.vars.elementAt(i).name, IR.MEM(IR.PLUS(addr, (i+1) * wordSize)));

            }
        }

        return empty;
    }


}
