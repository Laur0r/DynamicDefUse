package dacite.core.defuse;

public class InterMethodAlloc {

    public Object value;
    public int linenumber;
    public String currentMethod;
    public String newMethod;
    public int newIndex;
    public String newName;
    public int currentIndex;
    public String currentName;
    public boolean isField;
    public boolean isRemoved;

    public InterMethodAlloc(Object value, int linenumber, String cM, String nM){
        this.value = value;
        this.linenumber = linenumber;
        this.currentMethod = cM;
        this.newMethod = nM;
    }
}
