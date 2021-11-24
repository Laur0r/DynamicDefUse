package defuse;

public class DefUseField extends DefUseVariable{

    protected Object instance;

    public DefUseField(int linenumber, int instruction, String variableIndex, Object value, String method, Object instance){
        super(linenumber, instruction, variableIndex, value, method);
        this.instance = instance;
    }

    public Object getInstance() {return this.instance;}
}
