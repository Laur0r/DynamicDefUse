package dacite.core.analysis;

import dacite.lsp.defUseData.transformation.XMLSolution;
import de.wwu.mulib.search.executors.MulibExecutor;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.throwables.NotYetImplementedException;

import java.util.*;
import java.util.logging.Logger;

/**
 * Class responsible for analyzing the data flow
 */
public class SymbolicAnalyzer {

    static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    // all symbolic variable definitions that were passed so far
    protected static DefSet symbolicDefs;

    // all identified DUC that were passed so far
    public static ArrayDeque<DefUseVariable> symbolicUsages;

    protected static long counter;

    static{
        symbolicDefs = new DefSet();
        symbolicUsages = new ArrayDeque<>();
        counter = 0;
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
            DaciteAnalyzer.visitArrayUse(array, ((Sint.ConcSint) index).intVal(), value, linenumber, instruction, method);
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
            DaciteAnalyzer.visitArrayDef(array, ((Sint.ConcSint) index).intVal(), value, linenumber, instruction, method);
            return;
        }
        //// TODO
        throw new NotYetImplementedException();
    }

    /**
     * Given a usage and its most recent definition, these form a DUC and are added to chains. A Solution is added to the chain.
     * @param def most recent definition
     * @param use usage
     * @param index index based on type of definition/usage
     * @param name name of variable/field/element
     * @param method method where this usage occured
     * @param solution PathSolution with which this chain is passed
     */
    protected static void registerUse(DefUseVariable def, DefUseVariable use, int index, String name, String method, PathSolution solution){
        // if no definition was found for this method, find definition of allocations in other methods
        if(def == null && DaciteAnalyzer.interMethods.size() != 0){
            def = DaciteAnalyzer.getAllocDef(method, index, name, use.value);
        }
        // if a definition was found and DUC does not exist, add DUC
        if(def != null){
            DefUseChain chain = new DefUseChain(def, use);
            XMLSolution s = new XMLSolution();
            s.setSolution(solution);
            chain.setSolution(s);
            DefUseChain c = DaciteAnalyzer.chains.containsSimilar(chain);
            if(c == null){
                DaciteAnalyzer.chains.addChain(chain);
            } else if(!c.getSolutions().contains(s)){
                c.setSolution(s);
            }
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
    protected static void registerSymbolicParameter(Object value, int index, int ln, String method, String varname, int parameter){
        if(DaciteAnalyzer.interMethods.size() != 0){
            for(InterMethodAlloc alloc : DaciteAnalyzer.interMethods.interMethodAllocs){
                // check if there exists a matching allocation for this method invocation
                if(alloc.newMethod.equals(method) && (alloc.newName == null || alloc.newName.equals(varname)) && parameter == alloc.parameter) {
                    if(alloc.value == value || (value instanceof ConcSnumber && value.equals(alloc.value) || (value instanceof PartnerClass &&
                            ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && alloc.value instanceof PartnerClass &&
                            ((PartnerClass) alloc.value).__mulib__getId() instanceof Sint.ConcSint &&
                            ((PartnerClass) value).__mulib__getId() == ((PartnerClass) alloc.value).__mulib__getId()))){
                        // get variable usage for parameter when method is called at call site
                        // TODO assign symbolic values later on as allocation?
                        String newName = DaciteAnalyzer.getInterParameter(alloc.currentMethod.substring(0, alloc.currentMethod.lastIndexOf(".")), alloc.linenumber, parameter);
                        if(newName.isEmpty()){
                            continue;
                        }
                        DefUseVariable result = findSymbolicUse(alloc.currentMethod, alloc.linenumber, value, alloc.isRemoved);
                        if(result == null){
                            result = DaciteAnalyzer.chains.findUse(alloc.currentMethod, alloc.linenumber, value, newName, alloc.isRemoved);
                        }
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
        DaciteAnalyzer.registerDef(def);
    }

    protected static String removeSymbolicALoad(Object object, int linenumber, String method) {
        for (DefUseVariable use : symbolicUsages) {
            if (use.getLinenumber() == linenumber && use.getMethod().equals(method)) {
                if (use.getValue() == null && object == null) {
                    symbolicUsages.remove(use);
                    return use.getVariableName();
                } else if(use.getValue() != null && use.getValue() == object && !DaciteAnalyzer.isPrimitiveOrWrapper(object)) {
                    symbolicUsages.remove(use);
                    return use.getVariableName();
                }
            }
        }
        return "this";
    }
    protected static void addSymbolicUse(DefUseVariable use){
        use.setTimeRef(counter);
        counter++;
        symbolicUsages.addFirst(use);
    }

    protected static void addSymbolicDef(DefUseVariable def){
        def.setTimeRef(counter);
        counter++;
        symbolicDefs.addDef(def);
    }

    public static void resolveLabels(MulibExecutor mulibExecutor, PathSolution pathSolution, SolverManager s) {

        for(DefUseVariable def : symbolicDefs.defs){
            if((def.getValue() instanceof Substituted)){ //&& !(def.getValue() instanceof ConcSnumber) && !(def.getValue() instanceof PartnerClass && ((PartnerClass) def.getValue()).__mulib__getId()
                    //instanceof Sint.ConcSint)){
                // TODO ist das jetzt zB int oder ConcSint?
                def.setValue(s.getLabel(def.getValue()));
            }
        }
        for(DefUseVariable var: symbolicUsages){
            var.setValue(s.getLabel(var.getValue()));
            // TODO oder symbolische Vergleiche notwendig?
            DefUseVariable def = null;
            if(var instanceof DefUseField){
                def = symbolicDefs.getLastDefinitionFields(var.variableIndex, var.variableName,var.value, ((DefUseField) var).getInstance(), var.timeRef);
            } else {
                def = symbolicDefs.getLastDefinition(var.variableIndex, var.method, var.value, var.variableName, var.timeRef);
            }
            registerUse(def, var, var.variableIndex, var.variableName, var.method, pathSolution);
        }
        symbolicDefs = new DefSet();
        symbolicUsages = new ArrayDeque<>();
        for(DefUseChain chain: DaciteAnalyzer.chains.getDefUseChains()){
            XMLSolution solution = new XMLSolution();
            solution.setSolution(pathSolution);
            if(chain.getSolutions().size() == 0 || (chain.getPassedAgain() && !chain.getSolutions().contains(solution))){
                chain.setSolution(solution);
            }
            chain.setPassedAgain(false);
        }
    }

    public static void resetSymbolicValues() {
        symbolicDefs = new DefSet();
        symbolicUsages = new ArrayDeque<>();
    }

    protected static DefUseVariable findSymbolicUse(String method, int linenumber, Object value, boolean removed){
        for(DefUseVariable use : symbolicUsages){
            if(use.getLinenumber() == linenumber && use.getMethod().equals(method) &&
                    (use.getValue() == value || (value instanceof ConcSnumber && value.equals(use.getValue())) || (value instanceof PartnerClass &&
                            ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && use.getValue() instanceof PartnerClass &&
                            ((PartnerClass) use.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                            ((PartnerClass) value).__mulib__getId() == ((PartnerClass) use.getValue()).__mulib__getId()))) {
                if (!removed) {
                    symbolicUsages.remove(use);
                }
                return use;
            }
        }
        return null;
    }
}
