package dacite.core.defuse;

import java.util.HashMap;
import java.util.Map;

public class DefUseAnalyser {

    public static DefUseChains chains;
    protected static DefSet defs;
    protected static InterMethodAllocDequeue interMethods;
    protected static Map<Object,AliasAlloc> aliases;

    static{
        chains = new DefUseChains();
        defs = new DefSet();
        interMethods = new InterMethodAllocDequeue();
        aliases = new HashMap<Object, AliasAlloc>();
    }

    public static void visitDef(Object value, int index, int linenumber, int instruction, String method, String varname){
        //System.out.println("Def at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable def = defs.contains(value, index, linenumber, instruction, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(linenumber, instruction, index, value, method, varname);
        }
        if(value.getClass().isArray()){
            defs.setArrayName(value, varname);
        }
        registerDef(def);
    }

    public static void visitUse(Object value, int index, int linenumber, int instruction, String method, String varname){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable use = new DefUseVariable(linenumber, instruction, index, value, method, varname);
        DefUseVariable def = defs.getLastDefinition(index, method, value);
        if(def != null && def.isAlias()){
            AliasAlloc alloc = aliases.get(def.getValue());
            for(int i = 0; i<alloc.varNames.size(); i++){
                DefUseVariable alias = defs.getAliasDef(alloc.varIndexes.get(i), alloc.varNames.get(i), value);
                if(alias.getLinenumber() > def.getLinenumber()){
                    def = alias;
                }
            }
        }
        registerUse(def, use, index,varname, method);
    }

    public static void visitStaticFieldUse(Object value, String name, int linenumber, int instruction, String method, String classname){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        // TODO
        DefUseField use = new DefUseField(linenumber, instruction, -1, value, method, name, null, classname);
        DefUseVariable def = defs.getLastDefinitionFields(-1, name, value, null);
        registerUse(def, use, -1, name, method);
    }

    public static void visitStaticFieldDef(Object value, String name, int linenumber, int instruction, String method, String classname){
        //System.out.println("Use at line "+linenumber+": var"+index+", value "+value+", method :"+method);
        DefUseVariable def = defs.containsField(value, -1, name, linenumber, instruction,null);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, -1, value, method, name, null, classname);
        }
        registerDef(def);
    }

    public static void visitFieldDef(Object instance, Object value, String name, int linenumber, int instruction, String method){
        //System.out.println("Field Def at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        String instanceName = chains.removeAload(instance, linenumber, method);
        DefUseVariable def = defs.containsField(value, -1, name, linenumber, instruction, instance);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
        }
        registerDef(def);
    }

    public static void visitFieldUse(Object instance, Object value, String name, int linenumber, int instruction, String method){
        //System.out.println("Field Use at line "+linenumber+": var"+name+", instance "+instance+", value "+value+", method :"+method);
        String instanceName = chains.removeAload(instance, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
        DefUseVariable def = defs.getLastDefinitionFields(-1, name, value, instance);
        if(def != null && def.isAlias()){
            AliasAlloc alloc = aliases.get(def.getValue());
            for(int i = 0; i<alloc.varNames.size(); i++){
                DefUseVariable alias = defs.getAliasDef(-1, name, value);
                if(alias.getLinenumber() > def.getLinenumber()){
                    def = alias;
                }
            }
        }
        registerUse(def, use, -1, name, method);
    }

    public static void visitArrayUse(Object array, int index, Object value, int linenumber, int instruction, String method){
        //System.out.println("Array Use at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        String arrayName = chains.removeAload(array, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, index, value, method, arrayName+"[", array, arrayName);
        DefUseVariable def = defs.getLastDefinitionFields(index, "", value, array);
        registerUse(def, use, index, "", method);
    }

    public static void visitArrayDef(Object array, int index, Object value, int linenumber, int instruction, String method){
        //System.out.println("Array Def at line "+linenumber+": index"+index+", array "+array+", value "+value+", method :"+method);
        String arrayName = chains.removeAload(array, linenumber, method);
        if(arrayName.equals("this")){
            arrayName = null;
        }
        DefUseVariable def = defs.containsField(value, index, "", linenumber, instruction, array);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseField(linenumber, instruction, index, value, method, "", array, arrayName);
        }
        registerDef(def);
    }

    public static void visitParameter(Object value, int index, int linenumber, String method, String varname){
        //System.out.println("Parameter of method " + method +": var"+index+", value "+value);
        registerParameter(value, index, linenumber, method, varname);
    }

    protected static void registerUse(DefUseVariable def, DefUseVariable use, int index, String name, String method){
        if(def == null && interMethods.size() != 0){
            def = getAllocDef(method, index, name);
        }
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            if(!chains.containsSimilar(chain)){
                chains.addChain(chain);
            }
        }
    }

    protected static void registerDef(DefUseVariable def){
            AliasAlloc alloc = aliases.get(def.getValue());
            if(alloc == null) {
                DefUseVariable alias = defs.hasAlias(def);
                if(alias != null){
                    System.out.println("Is Alias!!!");
                    alloc = new AliasAlloc(def.getVariableName(), alias.getVariableName(), def.getVariableIndex(), alias.getVariableIndex());
                    aliases.put(def.getValue(), alloc);
                    def.setAlias(true);
                    alias.setAlias(true);
                }
            } else {
                alloc.addAlias(def.getVariableName(), def.getVariableIndex());
            }
        defs.addDef(def);
    }

    protected static void registerParameter(Object value, int index, int ln, String method, String varname){
        if(interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
                if(alloc.newMethod.equals(method)) {
                    if(alloc.value == null && value == null || alloc.value != null && alloc.value.equals(value)){
                        alloc.newIndex = index;
                        alloc.newName = varname;
                        DefUseVariable result = chains.findUse(alloc.currentMethod, alloc.linenumber, value, alloc.isRemoved);
                        if(result != null) {
                            if(!alloc.isRemoved){
                                alloc.isRemoved = true;
                            }
                            alloc.currentIndex = result.getVariableIndex();
                            alloc.currentName = result.getVariableName();
                            if(result instanceof DefUseField){
                                alloc.isField = true;
                            } else {
                                alloc.isField = false;
                            }
                            return;
                        }
                    }
                }
            }
        }
        DefUseVariable def = defs.contains(value, index, ln, -1, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(ln, -1, index, value, method, varname);
        }
        registerDef(def);
    }

    public static void registerInterMethod(Object value, int linenumber, String currentMethod, String newMethod){
        //System.out.println("interMethod");
        InterMethodAlloc m = new InterMethodAlloc(value, linenumber, currentMethod, newMethod);
        InterMethodAlloc a = interMethods.contains(m);
        if(a == null) {
            interMethods.addAlloc(m);
        } else {
            interMethods.moveToFirst(m);
        }
    }

    public static void registerInterMethod(Object[] values, int linenumber, String currentMethod, String newMethod){
        //System.out.println("interMethod");
        for(Object obj: values){
            registerInterMethod(obj, linenumber,currentMethod, newMethod);
        }
    }

    protected static DefUseVariable getAllocDef(String method, int index, String name){
        DefUseVariable def;
        for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
            if(alloc.newMethod.equals(method) && alloc.newName != null && alloc.newIndex == index && alloc.newName.equals(name)){
                if(alloc.isField) {
                    def = defs.getLastDefinitionFields(alloc.currentIndex, alloc.currentName, alloc.value, null);
                } else {
                    def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value);
                }
                if(def == null){
                    if(!alloc.currentMethod.equals(method)){
                        def = getAllocDef(alloc.currentMethod, alloc.currentIndex, alloc.currentName);
                        return def;
                    }
                } else {
                    return def;
                }
            }
        }
        return null;
    }

    public static void check(){
        System.out.println("Size: "+chains.getChainSize());
    }
}
