package defuse;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class DefUseChains {

    private ArrayList<DefUseChain> defUseChains = new ArrayList<DefUseChain>();

    public ArrayList<DefUseChain> getDefUseChains() {
        return defUseChains;
    }

    /*public HashSet<DefUseChain> copyChains() {
        HashSet<DefUseChain> copy = new HashSet<DefUseChain>();
        for (DefUseChain chain : defUseChains) {
            DefUseChain defCopy = (DefUseChain) chain.clone();
            copy.add(defCopy);
        }
        return copy;
    }*/

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
}
