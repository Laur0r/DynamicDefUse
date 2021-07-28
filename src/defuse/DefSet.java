package defuse;

import java.util.ArrayDeque;

public class DefSet {

    public ArrayDeque<DefUseVariable> defs = new ArrayDeque<>();

    public DefUseVariable getLastDefinition(int index, String method){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)){
                output = def;
                break;
            }
        }
        return output;
    }

    public DefUseVariable hasAlias(DefUseVariable newDef){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getValue().equals(newDef.getValue()) && def.getMethod().equals(newDef.getMethod())
                    && def.getVariableIndex() != newDef.getVariableIndex()){
                if(output == null || def.getLinenumber() > output.getLinenumber()){
                    output = def;
                }
            }
        }
        return output;
    }

    public void addDef(DefUseVariable def){defs.addFirst(def);}

    public DefUseVariable contains(Object value, int index, int ln, String method){
        for(DefUseVariable d: defs){
            if(d.getValue().equals(value) && d.getMethod().equals(method) && d.getVariableIndex() == index
            && d.getLinenumber() == ln){
                return d;
            }
        }
        return null;
    }

    public void removeDef(DefUseVariable def){
        defs.remove(def);
    }
}
