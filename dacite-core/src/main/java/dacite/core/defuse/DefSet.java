package dacite.core.defuse;

import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.ArrayDeque;

/**
 * Class representing all identified definitions
 */
public class DefSet {

    // all definitions as a Deque so that the most recent definition is at the beginning
    public ArrayDeque<DefUseVariable> defs = new ArrayDeque<>();

    /**
     * Get most recent variable definition for characteristics.
     * @param index of defined variable
     * @param method name where the variable was defined
     * @param value of defined variable
     * @param name of defined variable
     * @return existing variable definition otherwise null
     */
    public DefUseVariable getLastDefinition(int index, String method, Object value, String name){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)
                    && !(def instanceof DefUseField)){
                // to be able to compare integer and Integer due to transformer boxing
                if(def.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(def.getValue())
                        && def.getVariableName().equals(name)) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Get most recent variable definition for characteristics.
     * @param index of defined variable
     * @param method name where the variable was defined
     * @param value of defined variable
     * @param name of defined variable
     * @return existing variable definition otherwise null
     */
    public DefUseVariable getLastDefinition(int index, String method, Object value, String name, long time){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)
                    && !(def instanceof DefUseField) && time>def.timeRef){
                // to be able to compare integer and Integer due to transformer boxing
                if(def.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(def.getValue())
                        && def.getVariableName().equals(name)) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Get most recent variable definition for characteristics.
     * @param index of defined variable
     * @param method name where the variable was defined
     * @param value of defined variable
     * @param name of defined variable
     * @return existing variable definition otherwise null
     */
    public DefUseVariable getLastSymbolicDefinition(int index, String method, Object value, String name){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)
                    && !(def instanceof DefUseField)){
                // to be able to compare integer and Integer due to transformer boxing
                if((value instanceof ConcSnumber && value.equals(def.getValue())) || (value instanceof PartnerClass &&
                        ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && def.getValue() instanceof PartnerClass &&
                        ((PartnerClass) def.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                        ((PartnerClass) value).__mulib__getId() == ((PartnerClass) def.getValue()).__mulib__getId())
                        && def.getVariableName().equals(name)) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Get most recent array element or field definition for characteristics.
     * @param index of defined element
     * @param varname name of the defined element
     * @param value of the defined element
     * @param fieldInstance class instance of corresponding class or array the element was defined for
     * @return existing variable definition otherwise null
     */
    public DefUseVariable getLastDefinitionFields(int index, String varname, Object value, Object fieldInstance){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def instanceof DefUseField) {
                DefUseField field = (DefUseField) def;
                if(field.getVariableIndex() == index && (varname.equals(field.getVariableName()) || varname.equals(""))
                        && (fieldInstance == null || field.getInstance()== fieldInstance)){
                    // to be able to compare integer and Integer due to transformer boxing
                    if(def.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(field.getValue())){
                        output = def;
                        break;
                    }
                }
            }
        }
        return output;
    }

    public DefUseVariable getLastDefinitionFields(int index, String varname, Object value, Object fieldInstance, long time){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def instanceof DefUseField) {
                DefUseField field = (DefUseField) def;
                if(field.getVariableIndex() == index && (varname.contains(field.getVariableName()))
                        && (fieldInstance == null || field.getInstance()== fieldInstance)&& time>def.timeRef){
                    // to be able to compare integer and Integer due to transformer boxing
                    if(def.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(field.getValue())){
                        if(!varname.equals("") && field.getVariableName().equals("")){
                            def.setVariableName(varname);
                        }
                        output = def;
                        break;
                    }
                }
            }
        }
        return output;
    }

    /**
     * Get most recent array element or field definition for characteristics.
     * @param index of defined element
     * @param varname name of the defined element
     * @param value of the defined element
     * @param fieldInstance class instance of corresponding class or array the element was defined for
     * @return existing variable definition otherwise null
     */
    public DefUseVariable getLastSymbolicDefinitionFields(int index, String varname, Object value, Object fieldInstance){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def instanceof DefUseField) {
                DefUseField field = (DefUseField) def;
                if(field.getVariableIndex() == index && (varname.equals(field.getVariableName()) || varname.equals(""))
                        && (fieldInstance == null || field.getInstance()== fieldInstance || (fieldInstance instanceof PartnerClass &&
                        ((PartnerClass) fieldInstance).__mulib__getId() instanceof Sint.ConcSint && field.getInstance() instanceof PartnerClass &&
                        ((PartnerClass) field.getInstance()).__mulib__getId() instanceof Sint.ConcSint &&
                        ((PartnerClass) fieldInstance).__mulib__getId() == ((PartnerClass) field.getInstance()).__mulib__getId()))){
                    // to be able to compare integer and Integer due to transformer boxing
                    if(def.getValue() == value || value != null && (value instanceof ConcSnumber && value.equals(def.getValue())) || (value instanceof PartnerClass &&
                            ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && def.getValue() instanceof PartnerClass &&
                            ((PartnerClass) def.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                            ((PartnerClass) value).__mulib__getId() == ((PartnerClass) def.getValue()).__mulib__getId())){
                        output = def;
                        break;
                    }
                }
            }
        }
        return output;
    }

    public DefUseVariable getLastDefinitionArray(Object array, String arrayName){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue() == array && def.getVariableName().equals(arrayName)){
                output = def;
                break;
            }
        }
        return output;
    }

    /**
     * Get most recent definition of variable defining the same object
     * @param index of alias
     * @param varname of alias
     * @param value of alias object
     * @return existing alias definition otherwise null
     */
    public DefUseVariable getAliasDef(int index, String varname, Object value){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getVariableName().equals(varname)){
                // to be able to compare integer and Integer due to transformer boxing
                if(def.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(def.getValue())) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Get most recent definition of variable defining the same object
     * @param index of alias
     * @param varname of alias
     * @param value of alias object
     * @return existing alias definition otherwise null
     */
    public DefUseVariable getSymbolicAliasDef(int index, String varname, Object value){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getVariableName().equals(varname)){
                // to be able to compare integer and Integer due to transformer boxing
                if((value instanceof ConcSnumber && value.equals(def.getValue())) || (value instanceof PartnerClass &&
                        ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && def.getValue() instanceof PartnerClass &&
                        ((PartnerClass) def.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                        ((PartnerClass) value).__mulib__getId() == ((PartnerClass) def.getValue()).__mulib__getId())) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    /**
     * Check whether there exists an alias for this variable definition, i.e. a definition of a variable pointing to the same
     * object instance.
     * @param newDef given variable definition
     * @return existing alias definition otherwise null
     */
    public DefUseVariable hasAlias(DefUseVariable newDef){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue() == null){
                continue;
            }
            if(def.getValue() == newDef.getValue() && !(def.getVariableIndex() == newDef.getVariableIndex())
                    && !DaciteAnalyzer.isPrimitiveOrWrapper(def.getValue())){
                if(output == null || def.getLinenumber() > output.getLinenumber()){
                    output = def;
                }
            }
        }
        return output;
    }

    /**
     * Check whether there exists an alias for this variable definition, i.e. a definition of a variable pointing to the same
     * object instance.
     * @param newDef given variable definition
     * @return existing alias definition otherwise null
     */
    public DefUseVariable hasSymbolicAlias(DefUseVariable newDef){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue() == null){
                continue;
            }
            if((def.getValue() instanceof PartnerClass && ((PartnerClass) def.getValue()).__mulib__getId() instanceof
                            Sint.ConcSint && newDef.getValue() instanceof PartnerClass &&
                    ((PartnerClass) newDef.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                    ((PartnerClass) def.getValue()).__mulib__getId() == ((PartnerClass) newDef.getValue()).__mulib__getId())){
                if(output == null || def.getLinenumber() > output.getLinenumber()){
                    output = def;
                }
            }
        }
        return output;
    }

    /**
     * Check whether a variable definition with the given characteristics already exists.
     * @param value of defined variable
     * @param index of defined variable
     * @param ln line number where variable was defined in the source code
     * @param ins integer helping to differentiating instructions within a line
     * @param method name where the variable was defined
     * @return existing variable definition otherwise null
     */
    public DefUseVariable contains(Object value, int index, int ln, int ins, String method){
        for(DefUseVariable d: defs){
            if(d.getMethod().equals(method) && d.getVariableIndex() == index
                            && d.getLinenumber() == ln && d.getInstruction() == ins){
                    if(d.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(d.getValue())) {
                        return d;
                    }
            }
        }
        return null;
    }

    /**
     * Check whether a symbolic variable definition with the given characteristics already exists. Only applicable if the
     * symbolic variable is concolic or has a concolic id, otherwise a new definition will be added.
     * @param value of defined symbolic variable
     * @param index of defined symbolic variable
     * @param ln line number where variable was defined in the source code
     * @param ins integer helping to differentiating instructions within a line
     * @param method name where the variable was defined
     * @return existing variable definition otherwise null
     */
    public DefUseVariable symbolicContains(Object value, int index, int ln, int ins, String method){
        for(DefUseVariable d: defs){
            if(d.getMethod().equals(method) && d.getVariableIndex() == index
                    && d.getLinenumber() == ln && d.getInstruction() == ins){
                if((value instanceof ConcSnumber && value.equals(d.getValue())) || (value instanceof PartnerClass &&
                        ((PartnerClass) value).__mulib__getId() instanceof Sint.ConcSint && d.getValue() instanceof PartnerClass &&
                        ((PartnerClass) d.getValue()).__mulib__getId() instanceof Sint.ConcSint &&
                        ((PartnerClass) value).__mulib__getId() == ((PartnerClass) d.getValue()).__mulib__getId())){
                    return d;
                }
            }
        }
        return null;
    }

    /**
     * Check whether an array element or field definition with the given characteristics already exists.
     * @param value of defined variable
     * @param index of defined variable
     * @param varname of defined variable
     * @param ln line number where variable was defined in the source code
     * @param ins integer helping to differentiating instructions within a line
     * @param instance class instance of array or object for which an element or field was defined
     * @return existing definition otherwise null
     */
    public DefUseVariable containsField(Object value, int index, String varname, int ln, int ins, Object instance){
        for(DefUseVariable d: defs){
            if(d instanceof DefUseField){
                DefUseField field = (DefUseField) d;
                if(d.getVariableIndex() == index && d.getVariableName().equals(varname) && d.getLinenumber() == ln &&
                        d.getInstruction() == ins && (instance == null || field.getInstance() == instance)) {
                    if(field.getValue() == value || value != null && DaciteAnalyzer.isPrimitiveOrWrapper(value) && value.equals(field.getValue())){
                        return d;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check whether an array element or field definition with the given characteristics already exists.
     * @param value of defined variable
     * @param index of defined variable
     * @param varname of defined variable
     * @param ln line number where variable was defined in the source code
     * @param ins integer helping to differentiating instructions within a line
     * @param instance class instance of array or object for which an element or field was defined
     * @return existing definition otherwise null
     */
    public DefUseVariable containsSymbolicField(Object value, int index, String varname, int ln, int ins, Object instance){
        for(DefUseVariable d: defs){
            if(d instanceof DefUseField){
                DefUseField field = (DefUseField) d;
                if(d.getVariableIndex() == index && d.getVariableName().equals(varname) && d.getLinenumber() == ln &&
                        d.getInstruction() == ins && (instance == null || field.getInstance() == instance) ||
                        (instance instanceof PartnerClass && ((PartnerClass) instance).__mulib__getId()
                                instanceof Sint.ConcSint && field.getInstance() instanceof PartnerClass &&
                                ((PartnerClass) field.getInstance()).__mulib__getId() instanceof Sint.ConcSint &&
                                ((PartnerClass) instance).__mulib__getId() == ((PartnerClass) field.getInstance()).__mulib__getId())) {
                    if(field.getValue() == value || value != null && (value instanceof ConcSnumber && value.equals(field.getValue()))
                            || (value instanceof PartnerClass && ((PartnerClass) value).__mulib__getId() instanceof
                            Sint.ConcSint && field.getValue() instanceof PartnerClass && ((PartnerClass) field.getValue()).__mulib__getId()
                            instanceof Sint.ConcSint && ((PartnerClass) value).__mulib__getId() == ((PartnerClass) field.getValue()).__mulib__getId())){
                        return d;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set name for all array element definitions to "arrayname[" as they do not have an own name as fields or variables.
     * @param array instance
     * @param varname name of array
     */
    public void setArrayName(Object array, String varname){
        for(DefUseVariable d: defs) {
            if (d instanceof DefUseField) {
                DefUseField field = (DefUseField) d;
                if(field.getInstanceName() == null && array == field.getInstance()){
                    field.setInstanceName(varname);
                    field.setVariableName(varname+"[");
                }
            }
        }
    }

    /**
     * Remove definition from set
     * @param def removed definition
     */
    public void removeDef(DefUseVariable def){
        defs.remove(def);
    }

    /**
     * Add definition to set
     * @param def added definition
     */
    public void addDef(DefUseVariable def){defs.addFirst(def);}
}
