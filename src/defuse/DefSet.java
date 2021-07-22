package defuse;

import java.util.HashSet;

public class DefSet {

    public HashSet<DefUseVariable> defs = new HashSet<>();

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

    public void addDef(DefUseVariable def){defs.add(def);}
}
