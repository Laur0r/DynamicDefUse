package dacite.core.defuse;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class responsible for analyzing the data flow
 */
public class DefUseAnalyser {

    static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // all identified DUC that were passed so far
    public static DefUseChains chains;
    // all variable definitions that were passed so far
    protected static DefSet defs;

    // all symbolic variable definitions that were passed so far
    protected static DefSet symbolicDefs;

    // all identified DUC that were passed so far
    public static List<DefUseVariable> symbolicUsages;

    // allocation of variables over the boundary of methods
    protected static InterMethodAllocDequeue interMethods;
    // allocation of aliases
    protected static Map<Object,AliasAlloc> aliases;

    static{
        chains = new DefUseChains();
        defs = new DefSet();
        symbolicDefs = new DefSet();
        symbolicUsages = new ArrayList<>();
        interMethods = new InterMethodAllocDequeue();
        aliases = new HashMap<Object, AliasAlloc>();
    }

    /**
     * Method which is called from the instrumented source code whenever a local variable is defined.
     * @param value value of the defined variable
     * @param index variable index with which this is stored in the variable table
     * @param linenumber line number where the variable is defined in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the variable is defined
     * @param varname name of the defined variable
     */
    public static void visitDef(Object value, int index, int linenumber, int instruction, String method, String varname){
        if(value instanceof SubstitutedVar){
            DefUseVariable symbolicDef = symbolicDefs.symbolicContains(value, index, linenumber, instruction, method);
            if(symbolicDef != null){
                defs.removeDef(symbolicDef);
            } else {
                symbolicDef = new DefUseVariable(linenumber, instruction, index, value, method, varname);
            }
            registerDef(symbolicDef);
        } else {
            DefUseVariable def = defs.contains(value, index, linenumber, instruction, method);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                defs.removeDef(def);
            } else {
                def = new DefUseVariable(linenumber, instruction, index, value, method, varname);
            }
            // if an array is defined, the array name is added to all element definitions
            if(value.getClass().isArray()){
                defs.setArrayName(value, varname);
            }
            // add definition to defs
            registerDef(def);
        }
    }

    /**
     * Method which is called from the instrumented source code whenever a local variable is used.
     * @param value value of the used variable
     * @param index variable index with which this is stored in the variable table
     * @param linenumber line number where the variable is used in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the variable is used
     * @param varname name of the used variable
     */
    public static void visitUse(Object value, int index, int linenumber, int instruction, String method, String varname){
        DefUseVariable use = new DefUseVariable(linenumber, instruction, index, value, method, varname);
        // get most recent variable definition for this usage
        if(value instanceof SubstitutedVar){
            // can be compared similar to concrete values
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                DefUseVariable symbolicDef = symbolicDefs.getLastSymbolicDefinition(index, method, value, varname);
                if(symbolicDef != null && symbolicDef.isAlias()){
                    AliasAlloc alloc = aliases.get(symbolicDef.getValue());
                    for(int i = 0; i<alloc.varNames.size(); i++){
                        DefUseVariable alias = defs.getSymbolicAliasDef(alloc.varIndexes.get(i), alloc.varNames.get(i), value);
                        if(alias.getLinenumber() > symbolicDef.getLinenumber()){
                            symbolicDef = alias;
                        }
                    }
                }
                registerUse(symbolicDef, use, index,varname, method);
            } else {
                // Evaluation has to be made later when use.value has a concrete value
                symbolicUsages.add(use);
            }

        } else {
            DefUseVariable def = defs.getLastDefinition(index, method, value, varname);
            // if there exists a definition and this is an alias, check whether alias definition was more recent
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
    }

    /**
     * Method which is called from the instrumented source code whenever a static field is used.
     * @param value value of the used field
     * @param name name of the used field
     * @param linenumber line number where the field is used in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the field is used
     * @param classname name of the static class
     */
    public static void visitStaticFieldUse(Object value, String name, int linenumber, int instruction, String method, String classname){
        DefUseField use = new DefUseField(linenumber, instruction, -1, value, method, name, null, classname);
        DefUseVariable def = null;
        // get most recent field definition for this usage
        if(value instanceof SubstitutedVar){
            // can be compared similar to concrete values
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = symbolicDefs.getLastSymbolicDefinitionFields(-1, name, value, null);
            } else {
                symbolicUsages.add(use);
                return;
            }
        } else {
            def = defs.getLastDefinitionFields(-1, name, value, null);
        }
        registerUse(def, use, -1, name, method);
    }

    /**
     * Method which is called from the instrumented source code whenever a static field is defined.
     * @param value value of the defined field
     * @param name name of the defined field
     * @param linenumber line number where the field is defined in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the field is defined
     * @param classname name of the static class
     */
    public static void visitStaticFieldDef(Object value, String name, int linenumber, int instruction, String method, String classname){
        DefUseVariable def = null;
        // TODO value oder instance SubstitutedVar? Oder geht immer nur beides oder nichts?
        if(value instanceof SubstitutedVar){
            def = symbolicDefs.containsSymbolicField(value, -1, name, linenumber, instruction,null);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                symbolicDefs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, -1, value, method, name, null, classname);
            }
        } else {
            def = defs.containsField(value, -1, name, linenumber, instruction,null);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                defs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, -1, value, method, name, null, classname);
            }
        }
        registerDef(def);
    }

    /**
     * Method which is called from the instrumented source code whenever a field is defined.
     * @param instance class instance of the referenced field
     * @param value value of the defined field
     * @param name name of the defined field
     * @param linenumber line number where the field is defined in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the field is defined
     */
    public static void visitFieldDef(Object instance, Object value, String name, int linenumber, int instruction, String method){
        /*
         To access a field, first the class instance is loaded in bytecode. However, this is not a usage as this is only used
         for the field access. Thus, the identified chain using the class instance is removed.
         */
        String instanceName = chains.removeAload(instance, linenumber, method);
        DefUseVariable def = null;
        // TODO value oder instance SubstitutedVar? Oder geht immer nur beides oder nichts?
        if(value instanceof SubstitutedVar){
            def = symbolicDefs.containsSymbolicField(value, -1, name, linenumber, instruction, instance);
            if(def != null){
                symbolicDefs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
            }
        } else {
            def = defs.containsField(value, -1, name, linenumber, instruction, instance);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                defs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
            }
        }
        registerDef(def);
    }

    /**
     * Method which is called from the instrumented source code whenever a field is used.
     * @param instance class instance of the referenced field
     * @param value value of the used field
     * @param name name of the used field
     * @param linenumber line number where the field is used in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the field is used
     */
    public static void visitFieldUse(Object instance, Object value, String name, int linenumber, int instruction, String method){
        /*
         To access a field, first the class instance is loaded in bytecode. However, this is not a usage as this is only used
         for the field access. Thus, the identified chain using the class instance is removed.
         */
        String instanceName = chains.removeAload(instance, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
        DefUseVariable def = null;
        if(value instanceof SubstitutedVar){
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = symbolicDefs.getLastSymbolicDefinitionFields(-1, name, value, instance);
                if(def != null && def.isAlias()){
                    AliasAlloc alloc = aliases.get(def.getValue());
                    for(int i = 0; i<alloc.varNames.size(); i++){
                        DefUseVariable alias = symbolicDefs.getSymbolicAliasDef(-1, name, value);
                        if(alias.getLinenumber() > def.getLinenumber()){
                            def = alias;
                        }
                    }
                }
            } else {
                symbolicUsages.add(use);
                return;
            }
        } else {
            // get most recent field definition for this usage
            def = defs.getLastDefinitionFields(-1, name, value, instance);
            // if there exists a definition and this is an alias, check whether alias definition was more recent
            if(def != null && def.isAlias()){
                AliasAlloc alloc = aliases.get(def.getValue());
                for(int i = 0; i<alloc.varNames.size(); i++){
                    DefUseVariable alias = defs.getAliasDef(-1, name, value);
                    if(alias.getLinenumber() > def.getLinenumber()){
                        def = alias;
                    }
                }
            }
        }
        registerUse(def, use, -1, name, method);
    }

    /**
     * Method which is called from the instrumented source code whenever an array element is used.
     * @param array array instance of the referenced element
     * @param index index of the array element which is used
     * @param value value of the used array element
     * @param linenumber line number where the array element is used in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the array element is used
     */
    public static void visitArrayUse(Object array, int index, Object value, int linenumber, int instruction, String method){
        /*
         To access an array element, first the class instance is loaded in bytecode. However, this is not a usage as this
         is only used for the array element access. Thus, the identified chain using the array instance is removed.
         */
        String arrayName = chains.removeAload(array, linenumber, method);
        DefUseField use = new DefUseField(linenumber, instruction, index, value, method, arrayName+"[", array, arrayName);
        DefUseVariable def = null;
        // get most recent field definition for this usage
        if(value instanceof SubstitutedVar){
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = symbolicDefs.getLastSymbolicDefinitionFields(index, "", value, array);
            } else {
                symbolicUsages.add(use);
            }
        } else {
            def = defs.getLastDefinitionFields(index, "", value, array);
        }

        if(def.variableName.equals("")){
            def.variableName = arrayName+"[";
        }
        registerUse(def, use, index, "", method);
    }

    /**
     * Method which is called from the instrumented source code whenever an array element is defined.
     * @param array array instance of the referenced element
     * @param index index of the array element which is defined
     * @param value value of the defined array element
     * @param linenumber line number where the array element is defined in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the array element is defined
     */
    public static void visitArrayDef(Object array, int index, Object value, int linenumber, int instruction, String method){
        /*
         To access an array element, first the class instance is loaded in bytecode. However, this is not a usage as this
         is only used for the array element access. Thus, the identified chain using the array instance is removed.
         */
        String arrayName = chains.removeAload(array, linenumber, method);
        if(arrayName.equals("this")){
            arrayName = null;
        }
        DefUseVariable def = null;
        if(value instanceof SubstitutedVar){
            def = symbolicDefs.containsSymbolicField(value, index, "", linenumber, instruction, array);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                symbolicDefs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, index, value, method, "", array, arrayName);
            }
        } else {
            // get most recent field definition for this usage
            def = defs.containsField(value, index, "", linenumber, instruction, array);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if (def != null) {
                defs.removeDef(def);
            } else {
                def = new DefUseField(linenumber, instruction, index, value, method, "", array, arrayName);
            }
        }
        registerDef(def);
    }

    /**
     * Method which is called from the instrumented source code whenever an array element is used with a potentially symbolic
     * index.
     * @param array array instance of the referenced element
     * @param index index of the array element which is used, potentially symbolic
     * @param value value of the used array element
     * @param linenumber line number where the array element is used in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the array element is used
     */
    public static void visitArrayUse(Object array, Sint index, Object value, int linenumber, int instruction, String method){
        if (index instanceof Sint.ConcSint) {
            visitArrayUse(array, ((Sint.ConcSint) index).intVal(), value, linenumber, instruction, method);
            return;
        }
        //// TODO
        throw new NotYetImplementedException();
    }

    /**
     * Method which is called from the instrumented source code whenever an array element is defined with a potentially symbolic
     * index.
     * @param array array instance of the referenced element
     * @param index index of the array element which is defined, potentially symbolic
     * @param value value of the defined array element
     * @param linenumber line number where the array element is defined in the source code
     * @param instruction integer helping to differentiating instructions within a line
     * @param method name of the source code method where the array element is defined
     */
    public static void visitArrayDef(Object array, Sint index, Object value, int linenumber, int instruction, String method){
        if (index instanceof Sint.ConcSint) {
            visitArrayDef(array, ((Sint.ConcSint) index).intVal(), value, linenumber, instruction, method);
            return;
        }
        //// TODO
        throw new NotYetImplementedException();
    }

    /**
     * Method which is called from the instrumented source code whenever a new method with a parameter is entered. This
     * defines the parameter variable.
     * @param value parameter value
     * @param index index with which this is stored in the variable table
     * @param linenumber line number where this parameter is defined the source code
     * @param method name of the source code method where the parameter is defined
     * @param varname name of the parameter
     * @param parameter integer indicating which number parameter this is
     */
    public static void visitParameter(Object value, int index, int linenumber, String method, String varname, int parameter){
        if(value instanceof SubstitutedVar){
            registerSymbolicParameter(value, index, linenumber, method, varname, parameter);
        } else {
            registerParameter(value, index, linenumber, method, varname, parameter);
        }
    }

    /**
     * Given a usage and its most recent definition, these form a DUC and are added to chains.
     * @param def most recent definition
     * @param use usage
     * @param index index based on type of definition/usage
     * @param name name of variable/field/element
     * @param method method where this usage occured
     */
    protected static void registerUse(DefUseVariable def, DefUseVariable use, int index, String name, String method){
        // if no definition was found for this method, find definition of allocations in other methods
        if(def == null && interMethods.size() != 0){
            def = getAllocDef(method, index, name);
        }
        // if a definition was found and DUC does not exist, add DUC
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            if(!chains.containsSimilar(chain)){
                chains.addChain(chain);
            }
        }
    }

    /**
     * Given a definition, this is added to defs. If there exist an alias, this is registered.
     * @param def definition
     */
    protected static void registerDef(DefUseVariable def){
        // check if there already exists a registered alias for this variable value
        // TODO remove alias if there is a new definition
        AliasAlloc alloc = aliases.get(def.getValue());
        if(alloc == null) {
            DefUseVariable alias = null;
            if(def.getValue() instanceof SubstitutedVar){
                alias = symbolicDefs.hasSymbolicAlias(def);
            } else {
                // check if there exists an alias
                alias = defs.hasAlias(def);
            }
            if(alias != null){
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

    /**
     * Method parameter is registered at the entry of the called method. If this parameter was defined elsewhere and is
     * passed on to this method, this is not considered as a definition but registered as an allocation over the boundary
     * of methods. Otherwise, a definition is registered.
     * @param value value of parameter
     * @param index index with which this is stored in variable table
     * @param ln line number where the parameter is defined
     * @param method method name of this parameter
     * @param varname name of the parameter
     * @param parameter integer indicating which number parameter this is
     */
    protected static void registerParameter(Object value, int index, int ln, String method, String varname, int parameter){
        if(interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
                // check if there exists a matching allocation for this method invocation
                if(alloc.newMethod.equals(method) && (alloc.newName == null || alloc.newName.equals(varname)) && parameter == alloc.parameter) {
                    if(alloc.value == value || (value != null && isPrimitiveOrWrapper(value) && value.equals(alloc.value))){
                        // get variable usage for parameter when method is called at call site
                        DefUseVariable result = chains.findUse(alloc.currentMethod, alloc.linenumber, value, alloc.isRemoved); // TODO Erkennung von Operationen in Methodenaufrufen?
                        // if this exists a method allocation is registered instead of a definition
                        if(result != null) {
                            alloc.newIndex = index;
                            alloc.newName = varname;
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
        // save parameter as variable definition when there is no method allocation
        DefUseVariable def = defs.contains(value, index, ln, parameter, method);
        if(def != null){
            defs.removeDef(def);
        } else {
            def = new DefUseVariable(ln, parameter, index, value, method, varname);
        }
        registerDef(def);
    }

    /**
     * Method parameter is registered at the entry of the called method. If this parameter was defined elsewhere and is
     * passed on to this method, this is not considered as a definition but registered as an allocation over the boundary
     * of methods. Otherwise, a definition is registered.
     * @param value value of parameter
     * @param index index with which this is stored in variable table
     * @param ln line number where the parameter is defined
     * @param method method name of this parameter
     * @param varname name of the parameter
     * @param parameter integer indicating which number parameter this is
     */
    protected static void registerSymbolicParameter(Object value, int index, int ln, String method, String varname, int parameter){
        if(interMethods.size() != 0){
            for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
                // check if there exists a matching allocation for this method invocation
                if(alloc.newMethod.equals(method) && (alloc.newName == null || alloc.newName.equals(varname)) && parameter == alloc.parameter) {
                    if(alloc.value == value || (value instanceof ConcSnumber && value.equals(alloc.value) || (value instanceof PartnerClass &&
                            ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && alloc.value instanceof PartnerClass &&
                            ((PartnerClass) alloc.value).__mulib__getId() instanceof Sint.ConcSint &&
                            ((PartnerClass) value).__mulib__getId() == ((PartnerClass) alloc.value).__mulib__getId()))){
                        // get variable usage for parameter when method is called at call site
                        // TODO assign symbolic values later on as allocation?
                        DefUseVariable result = chains.findSymbolicUse(alloc.currentMethod, alloc.linenumber, value, alloc.isRemoved); // TODO Erkennung von Operationen in Methodenaufrufen?
                        // if this exists a method allocation is registered instead of a definition
                        if(result != null) {
                            alloc.newIndex = index;
                            alloc.newName = varname;
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
        // save parameter as variable definition when there is no method allocation
        DefUseVariable def = symbolicDefs.symbolicContains(value, index, ln, parameter, method);
        if(def != null){
            symbolicDefs.removeDef(def);
        } else {
            def = new DefUseVariable(ln, parameter, index, value, method, varname);
        }
        registerDef(def);
    }

    /**
     * Method which is called from the instrumented source code whenever a method with only one parameter is invoked.
     * This registers the variable allocation at the call site.
     * @param value value of the parameter
     * @param linenumber line number where the method is invoked
     * @param currentMethod name of the method the new method is called from
     * @param newMethod name of the invoked method
     * @param parameter integer indicating which number parameter this is
     */
    public static void registerInterMethod(Object value, int linenumber, String currentMethod, String newMethod, int parameter){
        InterMethodAlloc m = new InterMethodAlloc(value, linenumber, currentMethod, newMethod);
        m.parameter = parameter;
        // if allocation is already registered, move to beginning to allocate more efficiently
        InterMethodAlloc a = interMethods.contains(m);
        if(a == null) {
            interMethods.addAlloc(m);
        } else {
            interMethods.moveToFirst(m);
        }
    }

    /**
     * Method which is called from the instrumented source code whenever a method with more than one parameter is invoked.
     * This registers the variable allocation at the call site for every parameter.
     * @param values values of the method parameter
     * @param linenumber line number where the method is invoked
     * @param currentMethod name of the method the new method is called from
     * @param newMethod name of the invoked method
     */
    public static void registerInterMethod(Object[] values, int linenumber, String currentMethod, String newMethod){
        // register variable allocation for every parameter
        for(int i=0;i<values.length;i++){
            registerInterMethod(values[i], linenumber,currentMethod, newMethod,i);
        }
    }

    /**
     * Find variable definition via the allocations in other methods.
     * @param method method where the usage has occured
     * @param index index depending on type of usage
     * @param name name of the variable
     * @return variable definition
     */
    protected static DefUseVariable getAllocDef(String method, int index, String name){
        DefUseVariable def;
        for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
            // if there exists an allocation for the given arguments
            if(alloc.newMethod.equals(method) && alloc.newName != null && alloc.newIndex == index && alloc.newName.equals(name)){
                // get last definition within the calling method
                if(alloc.isField) {
                    def = defs.getLastDefinitionFields(alloc.currentIndex, alloc.currentName, alloc.value, null);
                } else if(alloc.value instanceof SubstitutedVar){
                    def = symbolicDefs.getLastSymbolicDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value, alloc.currentName);
                } else {
                    def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value, alloc.currentName);
                }
                if(def == null){
                    /*
                    if no definition exists, get definitions in calling method recursively (when parameters are forwarded
                    over several methods.
                    */
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

    /**
     * Check whether the given Object is instance of a primitive type or its wrapper class
     * @param obj given Object
     * @return boolean if it is an instance of a primitive type or wrapper
     */
    protected static boolean isPrimitiveOrWrapper(Object obj){
        Class<?> type = obj.getClass();
        return type.isPrimitive() || type == Double.class || type == Float.class || type == Long.class
                || type == Integer.class || type == Short.class || type == Character.class
                || type == Byte.class || type == Boolean.class || type == String.class;
    }

    /**
     * Print number of identified DUCs
     */
    public static void check(){
        logger.info("Size: "+chains.getChainSize());
    }

    private List<SubstitutedVar> symbolics = new ArrayList<>();
    private void resolveLabels(SolverManager s) {
        for(DefUseVariable def : symbolicDefs.defs){
            if((def.getValue() instanceof SubstitutedVar) && !(def.getValue() instanceof ConcSnumber) && !(def.getValue() instanceof PartnerClass && ((PartnerClass) def.getValue()).__mulib__getId()
                    instanceof Sint.ConcSint)){
                // TODO ist das jetzt zB int oder ConcSint?
                def.setValue(s.getLabel(def.getValue()));
            }
        }
        for(DefUseVariable var: symbolicUsages){
            var.setValue(s.getLabel(var.getValue()));
            // TODO oder symbolische Vergleiche notwendig?
            DefUseVariable def = symbolicDefs.getLastDefinition(var.variableIndex, var.method, var.value, var.variableName);
            registerUse(def, var, var.variableIndex, var.variableName, var.method);
        }
        for (SubstitutedVar sv : symbolics) {
            if (sv instanceof Sprimitive) {
                if (sv instanceof ConcSnumber) {
                    // TODO Das hier muss nicht gelistet sein; - mit equals vergleichen, Wert auslesen
                }
            } if (sv instanceof PartnerClass) {
                // TODO Für Sarray und für PartnerClass (Sarray ist Subtype) gilt: WENN die Id konkret (ConcSint) ist
                //  dann kann man mit der ID den check nutzen. erstmal nicht '==', sondern:
                //  ((PartnerClass) sv).__mulib__getId() ==
                //  Man kann prüfen, ob eine ID symbolisch ist, indem man id auf Sym prüft, oder
                //  ((PartnerClass) sv).__mulib__defaultIsSymbolic()
            }
            Object realValue = s.getLabel(sv);
        }
    }
}
