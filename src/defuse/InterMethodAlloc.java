package defuse;

public class InterMethodAlloc {

    public Object value;
    public int linenumber;
    public String currentMethod;
    public String newMethod;
    public int newIndex;
    public int currentIndex;

    public InterMethodAlloc(Object value, int linenumber, String cM, String nM){
        this.value = value;
        this.linenumber = linenumber;
        this.currentMethod = cM;
        this.newMethod = nM;
    }
}
