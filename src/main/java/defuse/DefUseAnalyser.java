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

    public static void visitDef(Object value, int index, int linenumber, int instruction, String method){
        //System.out.println("Def at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable def = defs.contains(value, Integer.toString(index), linenumber, instruction, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(linenumber, instruction, Integer.toString(index), value, method);
        }
        registerDef(def);
    }

    public static void visitUse(Object value, int index, int linenumber, int instruction, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable use = new DefUseVariable(linenumber, instruction, Integer.toString(index), value, method);
        DefUseVariable def = defs.getLastDefinition(Integer.toString(index), method, value);
        registerUse(def, use, Integer.toString(index), method);
    }

    public static void visitStaticFieldUse(Object value, String index, int linenumber, int instruction, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseField use = new DefUseField(linenumber, instruction, index, value, method, null);
        DefUseVariable def = defs.getLastDefinitionFields(index, value, null);
        registerUse(def, use, index,method);
    }

    public static void visitStaticFieldDef(Object value, String index, int linenumber, int instruction, String method){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable def = defs.containsField(value, index, linenumber, instruction,null);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, index, value, method, null);
        }
        registerDef(def);
    }

    public static void visitFieldDef(Object instance, Object value, String name, int linenumber, int instruction, String method){
        //System.out.println("Field Def at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        chains.removeAload(instance, linenumber, method);
        DefUseVariable def = defs.containsField(value, name, linenumber, instruction, instance);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, name, value, method, instance);
        }
        registerDef(def);
    }

    public static void visitFieldUse(Object instance, Object value, String name, int linenumber, int instruction, String method){
        //System.out.println("Field Use at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        chains.removeAload(instance, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, name, value, method, instance);
        DefUseVariable def = defs.getLastDefinitionFields(name, value, instance);
        registerUse(def, use, name,method);
    }

    public static void visitArrayUse(Object array, int index, Object value, int linenumber, int instruction, String method){
        //System.out.println("Array Use at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        chains.removeAload(array, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, Integer.toString(index), value, method, array);
        DefUseVariable def = defs.getLastDefinitionFields(Integer.toString(index), value, array);
        registerUse(def, use, Integer.toString(index),method);
    }

    public static void visitArrayDef(Object array, int index, Object value, int linenumber, int instruction, String method){
        //System.out.println("Array Def at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        chains.removeAload(array, linenumber, method);
        DefUseVariable def = defs.containsField(value, Integer.toString(index), linenumber, instruction, array);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, Integer.toString(index), value, method, array);
        }
        registerDef(def);
    }

    public static void visitParameter(Object value, int index, String method){
        //System.out.println("Parameter of method " + method +": var"+index+", value "+value);
        registerParameter(value, Integer.toString(index), method);
    }

    protected static void registerUse(DefUseVariable def, DefUseVariable use, String index, String method){
        if(def == null && interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods){
                if(alloc.newMethod.equals(method) && alloc.newIndex != null && alloc.newIndex.equals(index)){
                    if(alloc.isField) {
                        def = defs.getLastDefinitionFields(alloc.currentIndex, alloc.value, null);
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

    protected static void registerDef(DefUseVariable def){
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
                if(alloc.newMethod.equals(method)) {
                    if(alloc.value == null && value == null || alloc.value != null && alloc.value.equals(value)){
                        alloc.newIndex=index;
                        String[] result = chains.findUse(alloc.currentMethod, alloc.linenumber, value);
                        if(result != null) {
                            alloc.currentIndex = result[0];
                            alloc.isField = Boolean.parseBoolean(result[1]);
                            return;
                        }
                    }
                }
            }
        }
        DefUseVariable def = defs.contains(value, index, -1, -1, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(-1, -1, index, value, method);
        }
        registerDef(def);
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
        DefUseChains output = new DefUseChains();
        for(DefUseChain chain : chains.getDefUseChains()){
            if(chain.getUse().getMethod().equals(method)){
                if(!output.containsSimilar(chain)){
                    output.addChain(chain);
                }
            }
        }
        for(DefUseChain chain : output.getDefUseChains()){
            System.out.println(chain.toString());
        }
    }
}
