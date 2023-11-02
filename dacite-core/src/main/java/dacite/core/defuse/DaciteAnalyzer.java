package dacite.core.defuse;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class responsible for analyzing the data flow
 */
public class DaciteAnalyzer {

    static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // all identified DUC that were passed so far
    public static DefUseChains chains;
    // all variable definitions that were passed so far
    protected static DefSet defs;

    // allocation of variables over the boundary of methods
    protected static InterMethodAllocDequeue interMethods;
    // allocation of aliases
    protected static Map<Object,AliasAlloc> aliases;

    protected static Map<String, CompilationUnit> sourceCode;

    protected static Map<String, List<MethodCallExpr>> sourceCodeMethodCalls;

    protected static Map<String, List<MethodDeclaration>> sourceCodeMethodDeclaration;

    static{
        chains = new DefUseChains();
        defs = new DefSet();
        interMethods = new InterMethodAllocDequeue();
        aliases = new HashMap<Object, AliasAlloc>();
        sourceCode = new HashMap<String, CompilationUnit>();
        sourceCodeMethodCalls = new HashMap<String, List<MethodCallExpr>>();
        sourceCodeMethodDeclaration = new HashMap<String, List<MethodDeclaration>>();
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
        if(value instanceof Substituted){
            DefUseVariable symbolicDef = SymbolicAnalyzer.symbolicDefs.symbolicContains(value, index, linenumber, instruction, method);
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
        if(value instanceof Substituted){
            // can be compared similar to concrete values
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                DefUseVariable symbolicDef = SymbolicAnalyzer.symbolicDefs.getLastSymbolicDefinition(index, method, value, varname);
                if(symbolicDef != null && symbolicDef.isAlias()){
                    AliasAlloc alloc = aliases.get(symbolicDef.getValue());
                    for(int i = 0; i<alloc.varNames.size(); i++){
                        DefUseVariable alias = defs.getSymbolicAliasDef(alloc.varIndexes.get(i), alloc.varNames.get(i), value);
                        if(alias.getLinenumber() >
                                symbolicDef.getLinenumber()){
                            symbolicDef = alias;
                        }
                    }
                }
                registerUse(symbolicDef, use, index,varname, method);
            } else {
                // Evaluation has to be made later when use.value has a concrete value
                SymbolicAnalyzer.addSymbolicUse(use);
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
        if(value instanceof Substituted){
            // can be compared similar to concrete values
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = SymbolicAnalyzer.symbolicDefs.getLastSymbolicDefinitionFields(-1, name, value, null);
            } else {
                SymbolicAnalyzer.addSymbolicUse(use);
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
        if(value instanceof Substituted){
            def = SymbolicAnalyzer.symbolicDefs.containsSymbolicField(value, -1, name, linenumber, instruction,null);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                SymbolicAnalyzer.symbolicDefs.removeDef(def);
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
        if(value instanceof Substituted){
            instanceName = SymbolicAnalyzer.removeSymbolicALoad(instance, linenumber, method);
            def = SymbolicAnalyzer.symbolicDefs.containsSymbolicField(value, -1, name, linenumber, instruction, instance);
            if(def != null){
                SymbolicAnalyzer.symbolicDefs.removeDef(def);
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
        DefUseVariable use = null;
        DefUseVariable def = null;
        if(value instanceof Substituted){
            String instanceName = SymbolicAnalyzer.removeSymbolicALoad(instance, linenumber, method);
            use = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = SymbolicAnalyzer.symbolicDefs.getLastSymbolicDefinitionFields(-1, name, value, instance);
                if(def != null && def.isAlias()){
                    AliasAlloc alloc = aliases.get(def.getValue());
                    for(int i = 0; i<alloc.varNames.size(); i++){
                        DefUseVariable alias = SymbolicAnalyzer.symbolicDefs.getSymbolicAliasDef(-1, name, value);
                        if(alias.getLinenumber() > def.getLinenumber()){
                            def = alias;
                        }
                    }
                }
            } else {
                SymbolicAnalyzer.addSymbolicUse(use);
                return;
            }
        } else {
            // get most recent field definition for this usage
            String instanceName = chains.removeAload(instance, linenumber, method);
            use = new DefUseField(linenumber, instruction, -1, value, method, name, instance, instanceName);
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
        if(value instanceof Substituted){
            if(value instanceof ConcSnumber || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId()
                    instanceof Sint.ConcSint)){
                def = SymbolicAnalyzer.symbolicDefs.getLastSymbolicDefinitionFields(index, "", value, array);
            } else {
                SymbolicAnalyzer.addSymbolicUse(use);
                return;
            }
        } else {
            def = defs.getLastDefinitionFields(index, "", value, array);
            if(def == null){
                DefUseVariable defArray = defs.getLastDefinitionArray(array, arrayName);
                DefUseField defField = new DefUseField(defArray.linenumber, 0,use.variableIndex,use.value,defArray.method,
                        arrayName+"[",array, arrayName);
                defs.addDef(defField);
                def=defField;
            }
        }

        if(def != null && def.variableName.equals("")){
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
        if(value instanceof Substituted){
            def = SymbolicAnalyzer.symbolicDefs.containsSymbolicField(value, index, "", linenumber, instruction, array);
            // if definition already exists, it is removed and added to the beginning to keep track of the most recent definition
            if(def != null){
                SymbolicAnalyzer.symbolicDefs.removeDef(def);
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
        int ln = getLinenumberParameter(method, linenumber);
        linenumber = ln == 0? linenumber : ln;
        if(value instanceof Substituted){
            SymbolicAnalyzer.registerSymbolicParameter(value, index, linenumber, method, varname, parameter);
        } else {
            registerParameter(value, index, linenumber, method, varname, parameter);
        }
    }

    public static void visitSourceCode(String className, String path){
        if(!sourceCode.containsKey(className)){
            File file = new File(path+className+".java");
            try {
                if (file.exists()) {
                    String code = Files.readString(file.toPath());
                    CompilationUnit unit = StaticJavaParser.parse(code);
                    sourceCode.put(className, unit);
                }
            } catch (Exception e){
                logger.warning(e.getMessage());
            }
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
            DefUseChain c = chains.containsSimilar(chain);
            if(c == null){
                chains.addChain(chain);
            } else {
                c.setPassedAgain(true);
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
            if(def.getValue() instanceof Substituted){
                alias = SymbolicAnalyzer.symbolicDefs.hasSymbolicAlias(def);
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
        if(def.getValue() instanceof Substituted){
            SymbolicAnalyzer.addSymbolicDef(def);
        } else {
            defs.addDef(def);
        }
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
                        String newName = getInterParameter(alloc.currentMethod.substring(0, alloc.currentMethod.lastIndexOf(".")), alloc.linenumber, parameter);
                        if(newName.isEmpty()){
                            continue;
                        }
                        DefUseVariable result = chains.findUse(alloc.currentMethod, alloc.linenumber, value, newName, alloc.isRemoved);
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

    protected static String getInterParameter(String className, int linenumber, int parameter){
        List<MethodCallExpr> methods;
        if(sourceCodeMethodCalls.containsKey(className)) {
            methods = sourceCodeMethodCalls.get(className);
        } else {
            CompilationUnit unit = sourceCode.get(className);
            if(unit == null){
                return "";
            }
            methods = new ArrayList<>(unit.findAll(MethodCallExpr.class));
            sourceCodeMethodCalls.put(className, methods);
        }

        List<MethodCallExpr> lineMethodCalls = methods.stream().filter(it -> {
            var range = it.getRange().orElse(null);
            return range != null && range.begin.line == linenumber;
        }).collect(Collectors.toList());
        if(lineMethodCalls.size()==1){
            MethodCallExpr method = lineMethodCalls.get(0);
            Node argument = method.getArgument(parameter);
            if(argument instanceof BinaryExpr || argument instanceof IntegerLiteralExpr){
                return "";
            } else {
                return argument.toString();
            }
        }
        return "";
    }

    protected static int getLinenumberParameter(String methodString, int ln){
        String className = methodString.substring(0, methodString.lastIndexOf("."));
        String method = methodString.substring(methodString.lastIndexOf(".")+1);
        int linenumber = 0;

        List<MethodDeclaration> methods;
        if(sourceCodeMethodDeclaration.containsKey(className)) {
            methods = sourceCodeMethodDeclaration.get(className);
        } else {
            CompilationUnit unit = sourceCode.get(className);
            if(unit == null){
                return linenumber;
            }
            methods = new ArrayList<>(unit.findAll(MethodDeclaration.class));
            sourceCodeMethodDeclaration.put(className, methods);
        }

        List<MethodDeclaration> nameMethodCalls = methods.stream().filter(it -> it.getNameAsString().equals(method)).collect(Collectors.toList());
        for(Node n:nameMethodCalls){
            Range range = n.getRange().orElse(null);
            if(range!= null && range.begin.line > linenumber && ln >= range.begin.line){
                linenumber = range.begin.line;
            }
        }
        return linenumber;
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
                } else if(alloc.value instanceof Substituted){
                    def = SymbolicAnalyzer.symbolicDefs.getLastSymbolicDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value, alloc.currentName);
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
     * Find variable definition via the allocations in other methods.
     * @param method method where the usage has occured
     * @param index index depending on type of usage
     * @param name name of the variable
     * @return variable definition
     */
    protected static DefUseVariable getAllocDef(String method, int index, String name, Object subValue){
        DefUseVariable def;
        for(InterMethodAlloc alloc : interMethods.interMethodAllocs){
            // if there exists an allocation for the given arguments
            if(alloc.newMethod.equals(method) && alloc.newName != null && alloc.newIndex == index && alloc.newName.equals(name)){
                // get last definition within the calling method
                if(alloc.isField) {
                    def = defs.getLastDefinitionFields(alloc.currentIndex, alloc.currentName, alloc.value, null);
                } else if(alloc.value instanceof Substituted){
                    def = SymbolicAnalyzer.symbolicDefs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, subValue, alloc.currentName);
                } else {
                    def = defs.getLastDefinition(alloc.currentIndex, alloc.currentMethod, alloc.value, alloc.currentName);
                }
                if(def == null){
                    /*
                    if no definition exists, get definitions in calling method recursively (when parameters are forwarded
                    over several methods.
                    */
                    if(!alloc.currentMethod.equals(method)){
                        def = getAllocDef(alloc.currentMethod, alloc.currentIndex, alloc.currentName, subValue);
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
     *
     * @return
     */
    public static String check(){
        logger.info("Size: "+chains.getChainSize());
        return null;
    }
}
