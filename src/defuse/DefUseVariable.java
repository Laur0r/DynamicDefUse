package defuse;

public class DefUseVariable {

    private int linenumber;
    private int variableIndex;
    private Object value;
    private String method;

    public DefUseVariable(int linenumber, int variableIndex, Object value, String method){
        this.linenumber = linenumber;
        this.variableIndex = variableIndex;
        this.value = value;
        this.method = method;
    }

    public void setLinenumber(int linenumber){
        this.linenumber = linenumber;
    }

    public int getLinenumber(){return linenumber;}

    public void getVariableIndex(int variableIndex){
        this.variableIndex = variableIndex;
    }

    public int getVariableIndex(){return variableIndex;}

    public void setValue(Object value){
        this.value = value;
    }

    public Object getValue(){return value;}

    public void setMethod(String method){
        this.method = method;
    }

    public String getMethod(){return method;}
}
