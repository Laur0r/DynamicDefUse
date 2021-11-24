package defuse;

public class DefUseVariable {

    protected int linenumber;
    protected int instruction;
    protected String variableIndex;
    protected Object value;
    protected String method;
    protected DefUseVariable alias;

    public DefUseVariable(int linenumber, int instruction, String variableIndex, Object value, String method){
        this.linenumber = linenumber;
        this.variableIndex = variableIndex;
        this.value = value;
        this.method = method;
        this.instruction = instruction;
        this.alias = null;
    }

    public void setLinenumber(int linenumber){
        this.linenumber = linenumber;
    }

    public int getLinenumber(){return linenumber;}

    public void setInstruction(int ins){
        this.instruction = ins;
    }

    public int getInstruction(){return instruction;}

    public void setVariableIndex(String variableIndex){
        this.variableIndex = variableIndex;
    }

    public String getVariableIndex(){return variableIndex;}

    public void setValue(Object value){
        this.value = value;
    }

    public Object getValue(){return value;}

    public void setMethod(String method){
        this.method = method;
    }

    public String getMethod(){return method;}

    public DefUseVariable getAlias(){return alias;}

    public void setAlias(DefUseVariable alias){this.alias=alias;}

    public boolean equals(DefUseVariable var){
        if(this.value == null && var.getValue() == null){
            return var.getLinenumber() == this.linenumber && var.getInstruction() == this.instruction &&
                    var.getVariableIndex().equals(this.variableIndex) && var.getMethod().equals(this.method);
        } else if(var.getValue() != null){
            return var.getLinenumber() == this.linenumber && var.getInstruction() == this.instruction &&
                    var.getVariableIndex().equals(this.variableIndex)
                    && var.getValue().equals(this.value) && var.getMethod().equals(this.method);
        } else return false;
    }
}
