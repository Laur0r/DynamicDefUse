package defuse;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class DefUseChains {

    private ArrayList<DefUseChain> defUseChains = new ArrayList<DefUseChain>();

    public ArrayList<DefUseChain> getDefUseChains() {
        return defUseChains;
    }

    public void setDefUseChains(ArrayList<DefUseChain> defUseChains) {
        this.defUseChains = defUseChains;
    }

    public void addChain(DefUseChain chain) {
        defUseChains.add(chain);
    }

    public int getChainSize() {
        return defUseChains.size();
    }

    public boolean contains(DefUseChain chain) {
        for(DefUseChain c :defUseChains)
            if(c.equals(chain)){
                return true;
            }
        return false;
    }

    public String toString(){
        String output = "";
        for(DefUseChain chain : defUseChains){
            output += "\r\n"+chain.toString();
        }
        return output;
    }

    public String[] findUse(String method, int linenumber, Object value){
        for(DefUseChain chain : defUseChains){
            DefUseVariable use = chain.getUse();
            if(use.getLinenumber() == linenumber && use.getValue().equals(value) && use.getMethod().equals(method)){
                DefUseVariable def = chain.getDef();
                defUseChains.remove(chain);
                if(def instanceof DefUseField){
                    return new String[] {use.getVariableIndex(), "true"};
                } else {
                    return new String[] {use.getVariableIndex(), "false"};
                }
            }
        }
        return null;
    }

    public void removeAload(Object object, int linenumber, String method){
        for(DefUseChain chain : defUseChains) {
            DefUseVariable use = chain.getUse();
            if (use.getLinenumber() == linenumber && use.getValue().equals(object) && use.getMethod().equals(method)) {
                if(!isPrimitiveOrWrapper(object)){
                    defUseChains.remove(chain);
                    return;
                }
            }
        }
    }

    protected boolean isPrimitiveOrWrapper(Object obj){
        Class<?> type = obj.getClass();
        return type.isPrimitive() || type == Double.class || type == Float.class || type == Long.class
                || type == Integer.class || type == Short.class || type == Character.class
                || type == Byte.class || type == Boolean.class || type == String.class;
    }
}
