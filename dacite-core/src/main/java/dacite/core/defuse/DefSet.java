package dacite.core.defuse;

import java.util.ArrayDeque;

public class DefSet {

    public ArrayDeque<DefUseVariable> defs = new ArrayDeque<>();

    public DefUseVariable getLastDefinition(int index, String method, Object value, String name){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)
                    && !(def instanceof DefUseField)){
                if(def.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(def.getValue())
                        && def.getVariableName().equals(name)) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    public DefUseVariable getLastDefinitionFields(int index, String varname, Object value, Object fieldInstance){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def instanceof DefUseField) {
                DefUseField field = (DefUseField) def;
                if(field.getVariableIndex() == index && (varname.equals(field.getVariableName()) || varname.equals(""))
                        && (fieldInstance == null || field.getInstance()== fieldInstance)){
                    if(def.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(field.getValue())){
                        output = def;
                        break;
                    }
                }
            }
        }
        return output;
    }

    public DefUseVariable getAliasDef(int index, String varname, Object value){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getVariableName().equals(varname)){
                if(def.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(def.getValue())) {
                    output = def;
                    break;
                }
            }
        }
        return output;
    }

    public DefUseVariable hasAlias(DefUseVariable newDef){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue() == null){
                continue;
            }
            if(def.getValue() == newDef.getValue() && !(def.getVariableIndex() == newDef.getVariableIndex())
                    && !DefUseAnalyser.isPrimitiveOrWrapper(def.getValue())){
                if(output == null || def.getLinenumber() > output.getLinenumber()){
                    output = def;
                }
            }
        }
        return output;
    }

    public void addDef(DefUseVariable def){defs.addFirst(def);}

    public DefUseVariable contains(Object value, int index, int ln, int ins, String method){
        for(DefUseVariable d: defs){
            if(d.getMethod().equals(method) && d.getVariableIndex() == index
                            && d.getLinenumber() == ln && d.getInstruction() == ins){
                    if(d.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(d.getValue())) {
                        return d;
                    }
            }
        }
        return null;
    }

    public DefUseVariable containsField(Object value, int index, String varname, int ln, int ins, Object instance){
        for(DefUseVariable d: defs){
            if(d instanceof DefUseField){
                DefUseField field = (DefUseField) d;
                if(d.getVariableIndex() == index && d.getVariableName().equals(varname) && d.getLinenumber() == ln &&
                        d.getInstruction() == ins && (instance == null || field.getInstance() == instance)) {
                    if(field.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(field.getValue())){
                        return d;
                    }
                }
            }
        }
        return null;
    }

    public void removeDef(DefUseVariable def){
        defs.remove(def);
    }

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
}
