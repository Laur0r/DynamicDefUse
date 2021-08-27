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

    public static void visitDef(Object value, int index, int linenumber, String method){
        //System.out.println("Def at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerDef(value, Integer.toString(index), linenumber, method);
    }

    public static void visitUse(Object value, int index, int linenumber, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerUse(Integer.toString(index), value, linenumber, method);
    }

    public static void visitStaticFieldUse(Object value, String index, int linenumber, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerFieldUse(index, value, linenumber, method, null);
    }

    public static void visitStaticFieldDef(Object value, String index, int linenumber, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        registerFieldDef(value, index, linenumber, method, null);
    }

    public static void visitFieldDef(Object instance, Object value, String name, int linenumber, String method){
        //System.out.println("Field Def at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        chains.removeAload(instance, linenumber, method);
        registerFieldDef(value, name, linenumber, method, instance);
    }

    public static void visitFieldUse(Object instance, Object value, String name, int linenumber, String method){
        //System.out.println("Field Use at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        chains.removeAload(instance, linenumber, method);
        registerFieldUse(name, value, linenumber, method, instance);
    }

    public static void visitArrayUse(Object array, int index, Object value, int linenumber, String method){
        //System.out.println("Array Use at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        chains.removeAload(array, linenumber, method);
        registerFieldUse(Integer.toString(index), value, linenumber, method, array);
    }

    public static void visitArrayDef(Object array, int index, Object value, int linenumber, String method){
        //System.out.println("Array Def at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        chains.removeAload(array, linenumber, method);
        registerFieldDef(value, Integer.toString(index), linenumber, method, array);
    }

    public static void visitParameter(Object value, int index, String method){
        //System.out.println("Parameter of method " + method +": var"+index+", value "+value);
        registerParameter(value, Integer.toString(index), method);
    }

    protected static void registerUse(String index, Object value, int linenumber, String method){
        DefUseVariable use = new DefUseVariable(linenumber, index, value, method);
        DefUseVariable def = defs.getLastDefinition(index, method, value);
        if(def == null && interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.newIndex.equals(index)){
                    if(alloc.isField) {
                        def = defs.getLastDefinition(alloc.currentIndex, alloc.value, null);
                    } else {
                        def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value);
                    }
                    break;
                }
            }
        }
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            if(!chains.contains(chain)){
                chains.addChain(chain);
            }
        }
    }

    protected static void registerFieldUse(String index, Object value, int linenumber, String method, Object fieldInstance){
        DefUseField use = new DefUseField(linenumber, index, value, method, fieldInstance);
        DefUseVariable def = defs.getLastDefinition(index, value, fieldInstance);
        if(def == null && interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.newIndex.equals(index)){
                    if(alloc.isField) {
                        def = defs.getLastDefinition(alloc.currentIndex, alloc.value, null);
                    } else {
                        def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value);
                    }
                    break;
                }
            }
        }
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            if(!chains.contains(chain)){
                chains.addChain(chain);
            }
        }
    }

    protected static void registerDef(Object value, String index, int linenumber, String method){
        DefUseVariable def = defs.contains(value, index, linenumber, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(linenumber, index, value, method);
        }
        if(def.getAlias() == null) {
            DefUseVariable alias = defs.hasAlias(def);
            if(alias != null){
                System.out.println("Is Alias!!!");
                def.setAlias(alias);
                alias.setAlias(def);
            }
        }
        defs.addDef(def);
    }

    protected static void registerFieldDef(Object value, String index, int linenumber, String method, Object instance){
        DefUseVariable def = defs.contains(value, index, linenumber, instance);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, index, value, method, instance);
        }
        if(def.getAlias() == null) {
            DefUseVariable alias = defs.hasAlias(def);
            if(alias != null){
                System.out.println("Is Alias!!!");
                def.setAlias(alias);
                alias.setAlias(def);
            }
        }
        defs.addDef(def);
    }

    protected static void registerParameter(Object value, String index, String method){
        if(interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.value.equals(value)){
                    alloc.newIndex=index;
                    String[] result = chains.findUse(alloc.currentMethod, alloc.linenumber, value);
                    if(result != null){
                        alloc.currentIndex = result[0];
                        alloc.isField = Boolean.parseBoolean(result[1]);
                        return;
                    }
                }
            }
        }
        registerDef(value, index, -1, method);
    }

    public static void registerInterMethod(Object value, int linenumber, String currentMethod, String newMethod){
        //System.out.println("interMethod");
        InterMethodAlloc m = new InterMethodAlloc(value, linenumber, currentMethod, newMethod);
        interMethods.add(m);
    }

    public static void registerInterMethod(Object[] values, int linenumber, String currentMethod, String newMethod){
        //System.out.println("interMethod");
        for(Object obj: values){
            InterMethodAlloc m = new InterMethodAlloc(obj, linenumber, currentMethod, newMethod);
            interMethods.add(m);
        }
    }

    public static void visitMethodEnd(String method){
        System.out.println("Ende von method "+method);
        for(DefUseChain chain : chains.getDefUseChains()){
            if(chain.getUse().getMethod().equals(method)){
                System.out.println(chain.toString());
            }
        }
    }
}
