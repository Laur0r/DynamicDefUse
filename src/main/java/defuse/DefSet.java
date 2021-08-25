package defuse;

import java.util.ArrayDeque;

public class DefSet {

    public ArrayDeque<DefUseVariable> defs = new ArrayDeque<>();

    public DefUseVariable getLastDefinition(String index, String method, Object value){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex().equals(index) && def.getValue().equals(value)  && def.getMethod().equals(method)
            && !(def instanceof DefUseField)){
                output = def;
                break;
            }
        }
        return output;
    }

    public DefUseVariable getLastDefinition(String index, Object value, Object fieldInstance){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def instanceof DefUseField) {
                DefUseField field = (DefUseField) def;
                if(field.getVariableIndex().equals(index) && field.getValue().equals(value)){
                    if(fieldInstance == null || field.getInstance().equals(fieldInstance)){
                        output = def;
                        break;
                    }
                }
            }
        }
        return output;
    }

    public DefUseVariable hasAlias(DefUseVariable newDef){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue().equals(newDef.getValue()) && def.getMethod().equals(newDef.getMethod())
                    && !def.getVariableIndex().equals(newDef.getVariableIndex()) && !isPrimitiveOrWrapper(def.getValue())){
                if(output == null || def.getLinenumber() > output.getLinenumber()){
                    output = def;
                }
            }
        }
        return output;
    }

    public void addDef(DefUseVariable def){defs.addFirst(def);}

    public DefUseVariable contains(Object value, String index, int ln, String method){
        for(DefUseVariable d: defs){
            if(d.getValue().equals(value) && d.getMethod().equals(method) && d.getVariableIndex().equals(index)
            && d.getLinenumber() == ln){
                return d;
            }
        }
        return null;
    }

    public DefUseVariable contains(Object value, String index, int ln, Object instance){
        for(DefUseVariable d: defs){
            if(d instanceof DefUseField){
                DefUseField field = (DefUseField) d;
                if(field.getValue().equals(value) && d.getVariableIndex().equals(index)
                        && d.getLinenumber() == ln){
                    if(instance == null || field.getInstance().equals(instance)){
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

    protected boolean isPrimitiveOrWrapper(Object obj){
        Class<?> type = obj.getClass();
        return type.isPrimitive() || type == Double.class || type == Float.class || type == Long.class
                || type == Integer.class || type == Short.class || type == Character.class
                || type == Byte.class || type == Boolean.class || type == String.class;
    }
}
