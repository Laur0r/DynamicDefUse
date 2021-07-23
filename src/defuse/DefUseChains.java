package defuse;

import java.util.HashSet;

public class DefUseChains {

    private HashSet<DefUseChain> defUseChains = new HashSet<DefUseChain>();

    public HashSet<DefUseChain> getDefUseChains() {
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

    public void setDefUseChains(HashSet<DefUseChain> defUseChains) {
        this.defUseChains = defUseChains;
    }

    public void addChain(DefUseChain chain) {
        defUseChains.add(chain);
    }

    public int getChainSize() {
        return defUseChains.size();
    }

    public String toString(){
        String output = "";
        for(DefUseChain chain : defUseChains){
            output += "\r\n"+chain.toString();
        }
        return output;
    }

    public DefUseVariable findUse(String method, int linenumber, Object value){
        for(DefUseChain chain : defUseChains){
            DefUseVariable use = chain.getUse();
            if(use.getLinenumber() == linenumber && use.getValue().equals(value) && use.getMethod().equals(method)){
                return use;
            }
        }
        return null;
    }
}
