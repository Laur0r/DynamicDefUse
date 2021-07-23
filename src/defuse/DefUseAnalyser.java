package defuse;

import java.util.ArrayList;
import java.util.Stack;

public class DefUseAnalyser {

    protected static DefUseChains chains;
    protected static DefSet defs;
    protected static ArrayList<InterMethodAlloc> interMethods;

    static{
        chains = new DefUseChains();
        defs = new DefSet();
        interMethods = new ArrayList<>();
    }

    public static void visitDef(int value, int index, int linenumber, String method){
        System.out.println("Def at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerDef(value, index, linenumber, method);
    }

    public static void visitDef(Object value, int index, int linenumber, String method){
        System.out.println("Def at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerDef(value, index, linenumber, method);
    }

    public static void visitUse(int value, int index, int linenumber, String method){
        System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerUse(index, value, linenumber, method);
    }

    public static void visitUse(Object value, int index, int linenumber, String method){
        System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerUse(index, value, linenumber, method);
    }

    public static void visitParameter(int value, int index, String method){
        System.out.println("Parameter of method " + method +": var"+index+", value "+value);
        registerParameter(value, index, method);
    }

    public static void visitParameter(Object value, int index, String method){
        System.out.println("Parameter of method " + method +": var"+index+", value "+value);
        registerParameter(value, index, method);
    }

    protected static void registerUse(int index, Object value, int linenumber, String method){
        DefUseVariable use = new DefUseVariable(linenumber, index, value, method);
        DefUseVariable def = defs.getLastDefinition(index, method);
        if(def == null && interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.newIndex == index){
                    def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod);
                    break;
                }
            }
        }
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            chains.addChain(chain);
            System.out.println(chain.toString());
        }
    }

    protected static void registerDef(Object value, int index, int linenumber, String method){
        DefUseVariable def = new DefUseVariable(linenumber, index, value, method);
        defs.addDef(def);
    }

    protected static void registerParameter(Object value, int index, String method){
        if(interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.value.equals(value)){
                    alloc.newIndex=index;
                    DefUseVariable use = chains.findUse(alloc.currentMethod, alloc.linenumber, value);
                    if(use != null){
                        alloc.currentIndex = use.getVariableIndex();
                        return;
                    }
                }
            }
        }
        registerDef(value, index, -1, method);
    }

    public static void registerInterMethod(Object value, int linenumber, String currentMethod, String newMethod){
        System.out.println("interMethod");
        InterMethodAlloc m = new InterMethodAlloc(value, linenumber, currentMethod, newMethod);
        interMethods.add(m);
    }

    public static void registerInterMethod(Object[] values, int linenumber, String currentMethod, String newMethod){
        System.out.println("interMethod");
        for(Object obj: values){
            InterMethodAlloc m = new InterMethodAlloc(obj, linenumber, currentMethod, newMethod);
            interMethods.add(m);
        }
    }

    /*public static void addLinenumber(int i){
        linenumber.pop();
        linenumber.push(i);
    }

    public static void newLinenumber(){ linenumber.push(-1);}

    public static void removeLinenumber() {linenumber.pop();}

    public static void addMethod(String m, int number) {
        DefUseMethod method = new DefUseMethod(m, number);
        int ln = -1;
        if(!linenumber.empty()) {
            ln = linenumber.peek();
        }
        for(int i=0; i<number+1; i++){
            System.out.println("Argument Def at line "+ln+": var"+i+", method :"+m);
            DefUseVariable def = new DefUseVariable(ln, i, null, m);
            defs.addDef(def);
        }
        methods.push(method);
    }

    public static void removeMethod(){methods.pop();}*/

    public static void visitLocalVariable(String name, int index, Object value){
        System.out.println(name);
    }
}
