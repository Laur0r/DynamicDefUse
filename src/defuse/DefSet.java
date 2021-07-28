package defuse;

import java.util.HashSet;

public class DefSet {

    public HashSet<DefUseVariable> defs = new HashSet<>();

    // TODO check getDefinitions for loops

    public DefUseVariable getLastDefinition(int index, String method){
        DefUseVariable output = null;
        for(DefUseVariable def : defs){
            if(def.getVariableIndex() == index && def.getMethod().equals(method)){
                if(output != null) {
                    if(def.getLinenumber() > output.getLinenumber()) {
                        output = def;
                    }
                } else {
                    output = def;
                }
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

    public void addDef(DefUseVariable def){defs.add(def);}
}
