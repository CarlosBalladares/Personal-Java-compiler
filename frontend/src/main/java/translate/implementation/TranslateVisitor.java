package translate.implementation;

import ast.*;
import ir.frame.Access;
import ir.frame.Frame;
import ir.temp.Label;
import ir.temp.Temp;
import ir.tree.BINOP.Op;
import ir.tree.*;
import ir.tree.CJUMP.RelOp;
import translate.DataFragment;
import translate.Fragments;
import translate.ProcFragment;
import util.*;
import visitor.Visitor;

import static ir.tree.IR.*;
import static translate.TranslatorLabels.*;


/**
 * This visitor builds up a collection of IRTree code fragments for the body
 * of methods in a Functions program.
 * <p>
 * Methods that visit statements and expression return a TRExp, other methods
 * just return null, but they may add Fragments to the collection by means
 * of a side effect.
 *
 * @author kdvolder
 */
public class TranslateVisitor implements Visitor<TRExp> {
    /**
    /**
     * We build up a list of Fragment (pieces of stuff to be converted into
     * assembly) here.
     */
    private Fragments frags;

    /**
     * We use this factory to create Frame's, without making our code dependent
     * on the target architecture.
     */
    private Frame frameFactory;

    /**
     * The symbol table may be needed to find information about classes being
     * instantiated, or methods being called etc.
     */
    private Frame frame;

    private FunTable<IRExp> currentEnv;



    //Symbols
    private Lookup<Pair<Lookup<Type>, Lookup<Type>>> miniJavaTable;
    private Pair<Lookup<Type>, Lookup<Type>> currentClass;
    private FunctionType currentFunction;

    // IR ENV
    private String thisClassName;
    private String thisFnName;



    private FunTable<IRExp> instanceFields;

    private ImpTable<ObjectTableFactory> tableFactory= new ImpTable<>();

    private boolean isIntType=false;

    public TranslateVisitor(Lookup<Pair<Lookup<Type>, Lookup<Type>>> table, Frame frameFactory) {
        this.frags = new Fragments(frameFactory);
        this.frameFactory = frameFactory;
        this.miniJavaTable=table;

    }



    /////// Helpers //////////////////////////////////////////////


    private boolean atGlobalScope() {
        return frame.getLabel().equals(L_MAIN);
    }

    // May run slow for large numbers of classes
    private void initTableFactory(NodeList<ClassDecl> classes){
        for (int i=0;i<classes.size();i++){
            String className=classes.elementAt(i).name;
            String superClass=classes.elementAt(i).superName;
            ObjectTableFactory tf;
            if(superClass!=null){
                for (int j=0;j<classes.size();j++){
                    ClassDecl someOtherclass= classes.elementAt(j);
                    if(someOtherclass.name.equals(superClass)){
                        tf=new ObjectTableFactory(classes.elementAt(i), someOtherclass);
                        try {
                            tableFactory.put(classes.elementAt(i).name, tf);
                        } catch (ImpTable.DuplicateException e) {
                            throw new Error("Couldn't  init table facotories");
                        }

                    }
                }
            }
            else {
                tf = new ObjectTableFactory(classes.elementAt(i), null);
                try {
                    tableFactory.put(className, tf);
                } catch (ImpTable.DuplicateException e) {
                    throw new Error("Couldn't  init table facotories");
                }
            }
        }
        return;
    }

    /**
     * Create a frame with a given number of formals. We assume that
     * no formals escape, which is the case in MiniJava.
     */
    private Frame newFrame(Label name, int nFormals) {
        return frameFactory.newFrame(name, nFormals);
    }

    /**
     * Creates a label for a function (used by calls to that method).
     * The label name is simply the function name.
     */
    private Label functionLabel(String functionName) {
        return Label.get("_" + functionName);
    }


    private void putEnv(String name, Access access) {
        currentEnv = currentEnv.insert(name, access.exp(frame.FP()));
    }

    private void putEnv(String name, IRExp exp) {
        currentEnv = currentEnv.insert(name, exp);
    }

    private void putSubEnv(){
        return ;
    }

    ////// Visitor ///////////////////////////////////////////////

    private Type lookupType(String name){
        Type objType=currentFunction.locals.lookup(name);
        if(objType==null){
            for (int i = 0; i < currentFunction.formals.size(); i++) {
                VarDecl a= currentFunction.formals.elementAt(i);
                if(a.name==name){
                    objType= a.type;
                }

            }
        }

        if(objType==null){
            objType=currentClass.first.lookup(name);
        }
        return objType;
    }

    @Override
    public <T extends AST> TRExp visit(NodeList<T> ns) {
        IRStm result = IR.NOP;
        for (int i = 0; i < ns.size(); i++) {
            AST nextStm = ns.elementAt(i);
            TRExp e = nextStm.accept(this);
            // e will be null if the statement was in fact a function declaration
            // just ignore these as they generate Fragments
            if (e != null)
                result = IR.SEQ(result, e.unNx());
        }
        return new Nx(result);
    }

    @Override
    public TRExp visit(Program n) {
        //frame = newFrame(Label.get("Program"), 0);
        currentEnv = FunTable.theEmpty();

        initTableFactory(n.classes);

        TRExp main = n.mainClass.accept(this);
        TRExp classes = n.classes.accept(this);

//        In every function to close
//        IRStm body = IR.SEQ(
//                main.unNx(),
//                classes.unNx());
//
//        body = frame.procEntryExit1(body);
//        frags.add(new ProcFragment(frame, body));
        return null;
    }
    @Override
    public TRExp visit(MainClass n) {

        thisClassName= n.className;
        currentClass=miniJavaTable.lookup(n.className);

        {
        currentFunction= (FunctionType) currentClass.second.lookup("main");
        thisFnName="main";
        //MainFunction
        //1- keep the old frame
        Frame oldframe = frame;

        //2- make a new frame for the function and save env
        frame = newFrame(L_MAIN, 1);
        FunTable<IRExp> saveEnv = currentEnv;

        //3- Get the access information for each regular formal and add it to the environment.
        putEnv(n.argName, frame.getFormal(0));


        //4- Visit Statements using existing method
        TRExp stats = visitStatement(n.statement);

        //5- Turn statements into IRSTM
        IRStm body = stats.unNx();

        body = frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));

        // restore frame and env
        frame = oldframe;
        currentEnv = saveEnv;
        //MainFunction ends
        thisFnName="";
        currentFunction=null;
        }

        thisClassName="";
        currentClass=null;

        return new Ex(IR.CONST(0));
    }

    @Override
    public TRExp visit(ClassDecl n) {
        // TODO Auto-generated method stub
        currentClass= miniJavaTable.lookup(n.name);
        thisClassName=n.name;

        FunTable<IRExp> oldersaveEnv = currentEnv;

        IRStm body = IR.SEQ();

       // n.vars.accept(this);
        n.methods.accept(this);


        //body = frame.procEntryExit1(body);
        //frags.add(new ProcFragment(frame, body));

        currentEnv = oldersaveEnv;
        thisClassName="";
        currentClass=null;
        return null;
    }



    @Override
    public TRExp visit(BooleanType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(IntegerType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(UnknownType n) {
        throw new Error("Not implemented");
    }

    private TRExp visitStatements(NodeList<Statement> statements) {
        IRStm result = IR.NOP;
        for (int i = 0; i < statements.size(); i++) {
            Statement nextStm = statements.elementAt(i);
            result = IR.SEQ(result, nextStm.accept(this).unNx());
        }
        return new Nx(result);
    }

    private TRExp visitStatement(Statement s) {
        NodeList<Statement> stmt = new NodeList<>();
        stmt.add(s);
        return visitStatements(stmt);
    }

    @Override
    public TRExp visit(Conditional n) {
        Temp temp = new Temp();
        Label t = Label.gen();
        Label f = Label.gen();
        Label join = Label.gen();


        IRStm tst = n.e1.accept(this).unCx(t, f);

        IRStm thn = IR.SEQ(
                IR.LABEL(t),
                new Nx(IR.MOVE(temp, n.e2.accept(this).unEx())).unNx(),
                IR.JUMP(join));
        IRStm els = IR.SEQ(
                IR.LABEL(f),
                new Nx(IR.MOVE(temp, n.e3.accept(this).unEx())).unNx());

        return new Ex(IR.ESEQ(IR.SEQ(tst, thn, els, IR.LABEL(join)), IR.TEMP(temp)));
    }

    @Override
    public TRExp visit(Print n) {
        TRExp arg = n.exp.accept(this);
        return new Ex(IR.CALL(L_PRINT, arg.unEx()));
    }

    @Override
    public TRExp visit(Assign n) {
//        IRExp lhs;
//        if (atGlobalScope()||thisFnName=="") {
//            //remove since in minijava there's no implicit declaration
////            Label g = Label.get(n.name.name);
////            IRExp zero = IR.CONST(0);
////            IRData data = new IRData(g, List.list(zero));
////            DataFragment decl = new DataFragment(frame, data);
////            frags.add(decl);
//
//            lhs = currentEnv.lookup(n.name.name);
//            putEnv(n.name.name, lhs);
//        }else if(thisFnName!=""){
//            lhs= currentEnv.lookup(n.name.name);
//            putEnv(n.name.name, lhs);
//        } else {
//            Access var = frame.allocLocal(false);
//            putEnv(n.name.name, var);
//            lhs = var.exp(frame.FP());
//        }
//        TRExp val = n.value.accept(this);
//
//        return new Nx(IR.MOVE(lhs, val.unEx()));

        IRExp id=   n.name.accept(this).unEx();
        IRExp val = n.value.accept(this).unEx();

//        if(currentEnv.lookup(n.name.name)==null&&instanceFields.lookup(n.name.name)!=null){
//            //label frag
//            Label g = Label.get(n.name.name);
//            List l;
//            if(val instanceof  ESEQ) {
//               l = List.list(((ESEQ) val).exp);
//            }else{
//                l = List.list(val);
//            }
//
//            IRData data = new IRData(g, l);
//            DataFragment decl = new DataFragment(frame, data);
//
//            instanceFields=instanceFields.insert(n.name.name,MEM(IR.NAME(g)));
//
//            frags.add(decl);
//
//            return new Nx(IR.MOVE(MEM(IR.NAME(g)),val));
//        }
//
//
//        if(currentEnv.lookup(n.name.name)!=null){
//            return new Nx(IR.MOVE(id, val));
//        }else{
//        }
        return new Nx(IR.MOVE(id, val));


    }

    private TRExp relOp(final CJUMP.RelOp op, AST ltree, AST rtree) {
        final TRExp l = ltree.accept(this);
        final TRExp r = rtree.accept(this);
        return new TRExp() {
            @Override
            public IRStm unCx(Label t, Label f) {
                return IR.CJUMP(op, l.unEx(), r.unEx(), t, f);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return IR.CMOVE(op, l.unEx(), r.unEx(), dst, src);
            }

            @Override
            public IRExp unEx() {
                TEMP v = TEMP(new Temp());
                return ESEQ(SEQ(
                        MOVE(v, FALSE),
                        CMOVE(op, l.unEx(), r.unEx(), v, TRUE)),
                        v);
            }

            @Override
            public IRStm unNx() {
                Label end = Label.gen();
                return SEQ(unCx(end, end),
                        LABEL(end));
            }

        };

    }

    @Override
    public TRExp visit(LessThan n) {
        return relOp(RelOp.LT, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////

    private TRExp numericOp(Op op, Expression e1, Expression e2) {
        TRExp l = e1.accept(this);
        TRExp r = e2.accept(this);
        return new Ex(IR.BINOP(op, l.unEx(), r.unEx()));
    }

    @Override
    public TRExp visit(Plus n) {
        return numericOp(Op.PLUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Minus n) {
        return numericOp(Op.MINUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Times n) {
        return numericOp(Op.MUL, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////////

    @Override
    public TRExp visit(IntegerLiteral n) {
        return new Ex(IR.CONST(n.value));
    }

    @Override
    public TRExp visit(IdentifierExp n) {
        IRExp var;

//        if(thisFnName!=""){
//            var=currentEnv.lookup(n.name);
//        }else{
//            var=thisClassEnv.lookup(n.name);
//        }

        if(currentEnv.lookup(n.name)!=null){
            var=currentEnv.lookup(n.name);

        }else{
            var = instanceFields.lookup(n.name);

        }

//        if(lookupType(n.name) instanceof IntegerType){
//            return new Ex(IR.MEM(var));
//        }

        return new Ex(var);
    }

    @Override
    public TRExp visit(Not n) {
        final TRExp negated = n.e.accept(this);
        return new Cx() {
            @Override
            public IRStm unCx(Label ifTrue, Label ifFalse) {
                return negated.unCx(ifFalse, ifTrue);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unCx(dst, src);
            }

            @Override
            public IRExp unEx() {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unEx();
            }
        };
    }


    @Override
    public TRExp visit(If n) {

        IRStm   thn = visitStatement(n.thn).unNx();
        IRStm   els = visitStatement(n.els).unNx();

        Label t = Label.gen();
        Label f = Label.gen();
        Label join = Label.gen();


        IRStm body=IR.SEQ(
                            n.tst.accept(this).unCx(t, f),
                            IR.LABEL(t),
                            thn,
                            IR.JUMP(join),
                            IR.LABEL(f),
                            els,
                            IR.LABEL(join)

                            );

        return new Nx(body);

    }



    @Override
    public TRExp visit(While n) {
        // TODO Auto-generated method stub
        IRExp testExp = n.tst.accept(this).unEx();
        IRStm bodyStm = visitStatement(n.body).unNx();
        Label test = Label.gen();
        Label body = Label.gen();
        Label done = Label.gen();

        // Loop body
        IRStm loop = IR.SEQ(
                IR.LABEL(test),
                n.tst.accept(this).unCx(body, done),
                IR.LABEL(body),
                bodyStm,
                IR.JUMP(test),
                IR.LABEL(done));

        return new Nx(loop);


    }

    @Override
    public TRExp visit(FunctionDecl n) {

        //1- keep the old frame
        Frame oldframe = frame;

        //2- make a new frame for the function and save env
        frame = newFrame(functionLabel(n.name), n.formals.size());
        FunTable<IRExp> saveEnv = currentEnv;

        //3- Get the access information for each regular formal and add it to the environment.
        for (int i = 0; i < n.formals.size(); i++) {
            putEnv(n.formals.elementAt(i).name, frame.getFormal(i));
        }

        //4- Add statements to current env
        TRExp stats = visitStatements(n.statements);
        TRExp exp = n.returnExp.accept(this);

        //5- Turn statements into IRSTM
        IRStm body = IR.SEQ(
                stats.unNx(),
                IR.MOVE(frame.RV(), exp.unEx()));


        body = frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));

        // restore frame and env
        frame = oldframe;
        currentEnv = saveEnv;

        return null;

    }


    @Override
    public TRExp visit(VarDecl n) {
        if(n.kind== VarDecl.Kind.FIELD){
            Label g = Label.get(n.name);
            IRExp zero = IR.CONST(0);
            IRData data = new IRData(g, List.list(zero));
            DataFragment decl = new DataFragment(frame, data);
            frags.add(decl);
            putEnv(n.name,IR.MEM(IR.NAME(g)));
            return  null ;
        }
        Access var;
        if(n.kind==VarDecl.Kind.LOCAL){
             var = frame.allocLocal(false);


        }else{
             var = frame.getInArg(n.index);
        }
        //Access var = frame.getInArg(n.index);
        putEnv(n.name, var);
        return null;
    }

//
//    @Override
//    public TRExp visit(Call n) {
//        String functionName = "unknown";
//        if (n.name instanceof IdentifierExp) {
//            functionName = ((IdentifierExp) n.name).name;
//        }
//        List<IRExp> args = List.list();
//
//
//
//
//        for (int i = 0; i < n.rands.size(); i++) {
//            TRExp arg = n.rands.elementAt(i).accept(this);
//            args.add(arg.unEx());
//        }
//
//
//
//        if(n.receiver instanceof  IdentifierExp) {
//
//        }else if(n.receiver instanceof NewObject){
//            IRExp objPTR = n.receiver.accept(this).unEx();
//            String typeName=((NewObject) n.receiver).typeName;
//
//            instanceFields=tableFactory.lookup(typeName).getEnv(frags, frame, ((ESEQ)objPTR).exp, frame.wordSize());
//
//            TEMP t = TEMP(new Temp());
//
//
//
//            return new Ex(ESEQ(MOVE(t, objPTR), IR.CALL(functionLabel(functionName),args )));
//
//            //return new  IR.CALL(functionLabel(functionName), args)));
//
//
//        }else if(n.receiver instanceof  Call){
////            IRExp objPTR = n.receiver.accept(this).unEx();
////            Type objType=currentFunction.locals.lookup(((Call) n.receiver).typeName);
////            String typeName= ((ObjectType)objType).name;
////
////            instanceFields=tableFactory.lookup(typeName).getEnv(objPTR, frame.wordSize());
//        }
//        //tableFactory.lookup();
//        //Otherwise if receiver is new OBject we lookup
//
//        return new Ex(CALL(functionLabel(functionName), args));
//    }

    /**
     * visits Call AST node for minijava
     * translates the Call receiver
     * @param n
     * @return Translated Call
     */
    public TRExp visit(Call n) {
        String methodName = "unknown";
        if (n.name instanceof IdentifierExp) {
            methodName = ((IdentifierExp) n.name).name;
        }



        TRExp receiver      = n.receiver.accept(this);

        List<IRExp> args = List.list();
        //Add this pointer
        args.add(receiver.unEx());
        for (int i = 0; i < n.rands.size(); i++) {
            TRExp arg = n.rands.elementAt(i).accept(this);
            args.add(arg.unEx());
        }

        String objType= n.receiver.getType().toString();

        // If class has no method
        if(!tableFactory.lookup(objType).containsMethod(methodName)){

            String superClass= miniJavaTable.lookup(objType).first.lookup("super").toString();


            while(!tableFactory.lookup(superClass).containsMethod(methodName)){
                superClass=miniJavaTable.lookup(superClass).first.lookup("super").toString();
            }
            objType=superClass;

        }

        String fullname= objType+"."+methodName;

        return new Ex(IR.CALL(functionLabel(fullname), args));



    }


    private void setInstanceFields(Type t){

    }

    @Override
    public TRExp visit(FunctionType n) {
        throw new Error("Not implemented");
    }

    /**
     * After the visitor successfully traversed the program,
     * retrieve the built-up list of Fragments with this method.
     */
    public Fragments getResult() {
        return frags;
    }



    @Override
    public TRExp visit(MethodDecl n) {

        thisFnName=n.name;
        // currentFunction=currentClass.second.lookup(n.name);
        currentFunction=(FunctionType)currentClass.second.lookup(n.name);

        //1- keep the old frame
        Frame oldframe = frame;
        ///formals= FunTable.theEmpty();
        //ocals= FunTable.theEmpty();

        //2- make a new frame for the function and save env
        frame = newFrame(functionLabel(thisClassName+"."+n.name), n.formals.size()+1);
        FunTable<IRExp> saveEnv = currentEnv;

        //3- Get the access information for each regular formal and add it to the environment.
        for (int i = 0; i < n.formals.size(); i++) {
            putEnv(n.formals.elementAt(i).name, frame.getFormal(i+1));
        }


        TRExp varDecls= n.vars.accept(this);

            instanceFields = tableFactory.lookup(thisClassName).getEnv(frags, frame, frame.getFormal(0).exp(frame.FP()), frame.wordSize());


        //4- Add statements to current env
        TRExp stats = visitStatements(n.statements);
        TRExp exp = n.returnExp.accept(this);

        //5- Turn statements into IRSTM
        IRStm body = IR.SEQ(
                stats.unNx(),
                varDecls.unNx(),
                IR.MOVE(frame.RV(), exp.unEx()));


        body = frame.procEntryExit1(body);
        frags.add(new ProcFragment(frame, body));

        // restore frame and env
        frame = oldframe;
        currentEnv = saveEnv;

        thisFnName="";

        //formals=null;
        //locals=null;

        currentFunction=null;

        return null;
    }

    @Override
    public TRExp visit(IntArrayType n) {
        // TODO Auto-generated method stub
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(ObjectType n) {
        // TODO Auto-generated method stub
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(Block n) {
        // TODO Auto-generated method stub
        TRExp stats = visitStatements(n.statements);
        return stats;
    }


    @Override
    public TRExp visit(ArrayAssign n) {
        Label arrayName = Label.get(n.name);
        IRExp index = n.index.accept(this).unEx();
        IRExp value = n.value.accept(this).unEx();

        TRExp receiver=new IdentifierExp(n.name).accept(this);

        IRStm assignValue =
                IR.MOVE(
                        IR.MEM(IR.PLUS (
                                receiver.unEx(),
                                IR.MUL(index, frame.wordSize())
                        )),
                        value);


        IRExp arrayMax=IR.MEM(IR.MINUS(receiver.unEx(), frame.wordSize()));
        IRExp error=IR.CALL(L_ERROR, IR.CONST(1));



        Label t=Label.gen();
        Label f=Label.gen();
        Label join=Label.gen();


        TEMP result= TEMP(new Temp());

        IRStm callError=IR.MOVE(result, error);



        IRStm body=IR.SEQ(
                IR.CJUMP(RelOp.LT,index, arrayMax, t, f),
                IR.LABEL(t),
                assignValue,
                IR.JUMP(join),
                IR.LABEL(f),
                callError,
                IR.LABEL(join)

        );




        return new Nx(body);
    }





    @Override
    public TRExp visit(ArrayLookup n) {
        TRExp array= n.array.accept(this);
        TRExp index= n.index.accept(this);

        IRExp arrAddress= array.unEx();
        IRExp offset = IR.MUL(index.unEx(), frame.wordSize());

        IRExp destinationAddress= IR.PLUS(arrAddress, offset);
        IRExp lookup= IR.MEM(destinationAddress);



        IRExp arrayMax=IR.MEM(IR.MINUS(arrAddress, frame.wordSize()));
        IRExp error=IR.CALL(L_ERROR, IR.CONST(1));



        Label t=Label.gen();
        Label f=Label.gen();
        Label join=Label.gen();


        TEMP result= TEMP(new Temp());
        IRStm lookupValue=IR.MOVE(result, lookup);

        IRStm callError=IR.MOVE(result, error);



        IRStm compare=IR.SEQ(
                IR.CJUMP(RelOp.LT,index.unEx(), arrayMax, t, f),
                IR.LABEL(t),
                lookupValue,
                IR.JUMP(join),
                IR.LABEL(f),
                callError,
                IR.LABEL(join)

        );

        IRExp body = IR.ESEQ(compare, result);


        return new Ex(body);
    }

    @Override
    public TRExp visit(ArrayLength n) {

        TRExp array= n.array.accept(this);


        IRExp arrAddress= array.unEx();

        IRExp getArrLen= IR.MEM(
                                IR.MINUS(
                                        arrAddress,
                                        frame.wordSize()
                                        )
        );

        return new Ex(getArrLen);
    }

    @Override
    public TRExp visit(BooleanLiteral n) {
        return new Ex((n.value)? IR.TRUE: IR.FALSE);
    }

    @Override
    public TRExp visit(This n) {
        //TODO
        IRExp that = instanceFields.lookup("this");
        TEMP t =  IR.TEMP(new Temp());

        IRExp theThis = ESEQ(IR.MOVE(t, that), t);

        return new Ex(theThis);
    }

    @Override
    public TRExp visit(NewArray n) {
        // TODO Auto-generated method stub
        TRExp   arrlen =n.size.accept(this);
        TEMP    address = IR.TEMP(new Temp());
        IRStm   makeObject = IR.MOVE(
                    address,
                    IR.CALL(
                            L_NEW_ARRAY,
                            arrlen.unEx()));


        IRExp body= IR.ESEQ(makeObject, address);




        return new Ex(body);
    }

    @Override
    public TRExp visit(NewObject n) {
        // TODO Auto-generated method stub
        // It's important to notice that I added a super variable to every instance
        // to represent each class' superclass name
        int attrs =miniJavaTable.lookup(n.typeName).first.size();
        TEMP address = IR.TEMP(new Temp());
        IRStm makeObject = IR.MOVE(
                                address,
                                IR.CALL(
                                        L_NEW_OBJECT,
                                        IR.CONST((attrs+1)*frame.wordSize())));

        IRExp body= IR.ESEQ(makeObject, address);
        return new Ex(body);
    }


//    private FunTable<IRExp> getObjectTable(ObjectType objectType, IRExp addr){
//        FunTable<IR> objectTable= FunTable.theEmpty();
//        String typeName =  objectType.name;
//        Pair<Lookup<Type>, Lookup<Type>> theClass=miniJavaTable.lookup(typeName);
//        Lookup<Type> instanceVars= theClass.first;
//        for (int i = 0; i < instanceVars.size(); i++) {
//            //objectTable.insert(instanceVars.)
//        }
//
//
//        return null;
//    }



/**
 * Extension 1
 * */


@Override
public TRExp visit(And n) {
    // TODO Auto-generated method stub !!!!!!!!
    IRExp exp1 = n.e1.accept(this).unEx();
    IRExp exp2 = n.e2.accept(this).unEx();
    Label t = Label.gen();
    Label f = Label.gen();
    Label test = Label.gen();
    Label done = Label.gen();
    TEMP temp =  IR.TEMP(new Temp());
    IRExp body =
            IR.ESEQ(
                    IR.SEQ(
                            IR.CJUMP(RelOp.NE, exp1, IR.FALSE, test, f),
                            IR.LABEL(test),
                            IR.CJUMP(RelOp.NE, exp2, IR.FALSE, t, f),
                            IR.LABEL(t),
                            IR.MOVE(temp, IR.TRUE),
                            IR.JUMP(done),
                            IR.LABEL(f),
                            IR.MOVE(temp, IR.FALSE),
                            IR.LABEL(done)
                    ),
                    temp
            );
    return new Ex(body);

}
    @Override
    public TRExp visit(BooleanExpression n) {
        IRExp exp1 = n.e1.accept(this).unEx();
        IRExp exp2 = n.e2.accept(this).unEx();
        Label t = Label.gen();
        Label f = Label.gen();
        Label test = Label.gen();
        Label done = Label.gen();
        TEMP temp =  IR.TEMP(new Temp());

        IRExp body =null;
        switch(n.op) {
            case AND:
                body =
                    IR.ESEQ(
                            IR.SEQ(
                                    IR.CJUMP(RelOp.NE, exp1, IR.FALSE, test, f),
                                    IR.LABEL(test),
                                    IR.CJUMP(RelOp.NE, exp2, IR.FALSE, t, f),
                                    IR.LABEL(t),
                                    IR.MOVE(temp, IR.TRUE),
                                    IR.JUMP(done),
                                    IR.LABEL(f),
                                    IR.MOVE(temp, IR.FALSE),
                                    IR.LABEL(done)
                            ),
                            temp
                    );
                break;
            case OR:
                body =
                        IR.ESEQ(
                                IR.SEQ(
                                        IR.CJUMP(RelOp.EQ, exp1, IR.TRUE, t, test),
                                        IR.LABEL(test),
                                        IR.CJUMP(RelOp.EQ, exp2, IR.TRUE, t, f),
                                        IR.LABEL(t),
                                        IR.MOVE(temp, IR.TRUE),
                                        IR.JUMP(done),
                                        IR.LABEL(f),
                                        IR.MOVE(temp, IR.FALSE),
                                        IR.LABEL(done)
                                ),
                                temp
                        );
                break;

            case EQ:
                body =
                        IR.ESEQ(
                                IR.SEQ(
                                        IR.CJUMP(RelOp.EQ, exp1, exp2, t, f),
                                        IR.LABEL(t),
                                        IR.MOVE(temp, IR.TRUE),
                                        IR.JUMP(done),
                                        IR.LABEL(f),
                                        IR.MOVE(temp, IR.FALSE),
                                        IR.LABEL(done)
                                ),
                                temp
                        );
                break;

            case NEQ:
                body =
                        IR.ESEQ(
                                IR.SEQ(
                                        IR.CJUMP(RelOp.NE, exp1, exp2, t, f),
                                        IR.LABEL(t),
                                        IR.MOVE(temp, IR.TRUE),
                                        IR.JUMP(done),
                                        IR.LABEL(f),
                                        IR.MOVE(temp, IR.FALSE),
                                        IR.LABEL(done)
                                ),
                                temp
                        );
                break;
            case LT:
                return relOp(RelOp.LT, n.e1, n.e2);
            case GT:
                return relOp(RelOp.GT, n.e1, n.e2);
            case GEQ:
                return relOp(RelOp.GE, n.e1, n.e2);
            case LEQ:
                return relOp(RelOp.LE, n.e1, n.e2);


            default:
                throw new Error("Unhandled boolean expression case: "+n.op);

        }
        return new Ex(body);
    }

    @Override
    public TRExp visit(Divide n) {
        return numericOp(Op.DIV, n.e1, n.e2);

    }

    @Override
    public TRExp visit(Null n) {
        return new Ex(IR.NULL);

    }

    @Override
    public TRExp visit(NullType n) {
        throw new Error("Not implemented yet");

    }

}
