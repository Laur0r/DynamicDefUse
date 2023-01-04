package dacite.core.defuse;

/**
 * Class representing the allocation between variable definitions and usages over the boundary of methods
 */
public class InterMethodAlloc {

    // value of the variable
    public Object value;
    // linenumber where the variable was passed from on method to the next, i.e. method invocation
    public int linenumber;
    // name of method at the call site, i.e. where the new method is invoked
    public String currentMethod;
    // name of the newly invoked method, i.e. called method
    public String newMethod;
    // index with which the variable is stored for the called method
    public int newIndex;
    // variable name for the called method
    public String newName;
    // index with which the variable is stored for the method at call site
    public int currentIndex;
    // variable name for the method at call site
    public String currentName;
    // if variable is an array element or field
    public boolean isField;
    public boolean isRemoved;

    public int parameter;

    public InterMethodAlloc(Object value, int linenumber, String cM, String nM){
        this.value = value;
        this.linenumber = linenumber;
        this.currentMethod = cM;
        this.newMethod = nM;
    }
}
