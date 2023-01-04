package dacite.core.defuse;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Class representing a list of DUCs
 */
@XmlRootElement(name="DefUseChains")
public class DefUseChains {

    private ArrayList<DefUseChain> defUseChains = new ArrayList<DefUseChain>();

    @XmlElement(name="DefUseChain")
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

    /**
     * Checking whether the list of chains contains a similar DUC with the same index, name, method and linenumber
     * @param chain DUC to compare
     * @return boolean whether it contains a similar DUC
     */
    public boolean containsSimilar(DefUseChain chain){
        for(DefUseChain c :defUseChains){
            if(chain.getDef().variableIndex == c.getDef().variableIndex && chain.getDef().method.equals(c.getDef().method)
                    && chain.getDef().linenumber == c.getDef().linenumber && chain.getDef().instruction == c.getDef().instruction
                    && chain.getDef().getVariableName().equals(c.getDef().getVariableName()) && chain.getUse().method.equals(c.getUse().method)
                    && chain.getUse().linenumber == c.getUse().linenumber && chain.getUse().instruction == c.getUse().instruction
                    && chain.getUse().getVariableName().equals(c.getUse().getVariableName())){
                return true;
            }
        }
        return false;
    }

    /**
     * Find variable usage with given characteristics. Necessary in the context of variable allocations over the
     * boundaries of methods.
     * @param method name of method where the usage occurred
     * @param linenumber where the usage occurred
     * @param value of variable which was used
     * @param removed boolean indicating whether this usage was already removed from chains
     * @return variable usage
     */
    public DefUseVariable findUse(String method, int linenumber, Object value, boolean removed){
        for(DefUseChain chain : defUseChains){
            DefUseVariable use = chain.getUse();
            if(use.getLinenumber() == linenumber && use.getMethod().equals(method) &&
                    (use.getValue() == value || value != null && DefUseAnalyser.isPrimitiveOrWrapper(value) && value.equals(use.getValue()))){
                if(!removed){
                    defUseChains.remove(chain);
                }
                return use;
            }
        }
        return null;
    }

    /**
     * Remove object usage. To access an array element or object field, first the class instance is loaded in bytecode.
     * However, this is not a usage as this is only used for the element or field access. Thus, the identified chain using
     * the instance is removed.
     * @param object class instance which was loaded
     * @param linenumber where the usage occured
     * @param method name where the usage occured
     * @return variable name of removed usage
     */
    public String removeAload(Object object, int linenumber, String method){
        for(DefUseChain chain : defUseChains) {
            DefUseVariable use = chain.getUse();
            if (use.getLinenumber() == linenumber && use.getMethod().equals(method)){
                if(use.getValue() == null && object == null){
                    defUseChains.remove(chain);
                    return chain.getUse().getVariableName();
                } else if(use.getValue() != null && use.getValue() == object && !DefUseAnalyser.isPrimitiveOrWrapper(object)) {
                    defUseChains.remove(chain);
                    return chain.getUse().getVariableName();
                }
            }
        }
        return "this";
    }
}
