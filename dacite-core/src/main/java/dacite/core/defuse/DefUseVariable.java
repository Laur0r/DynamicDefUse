package dacite.core.defuse;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * Class representing a variable definition or usage
 */
public class DefUseVariable {
    // line number of source code where this variable is defined or used
    protected int linenumber;
    // integer helping to differentiating instructions within a line
    protected int instruction;
    // name of the defined/ used variable
    protected String variableName;
    // index with which this is stored in variable table
    protected int variableIndex;
    // value of the defined/ used variable
    @XmlTransient
    protected Object value;
    // name of the method where the variable was defined/used
    protected String method;
    // stores whether this variable has an alias
    protected boolean alias;

    public DefUseVariable(int linenumber, int instruction, int variableIndex, Object value, String method, String variableName){
        this.linenumber = linenumber;
        this.variableIndex = variableIndex;
        this.value = value;
        this.method = method;
        this.instruction = instruction;
        this.variableName = variableName;
        this.alias = false;
    }

    public void setLinenumber(int linenumber){
        this.linenumber = linenumber;
    }
    @XmlElement
    public int getLinenumber(){return linenumber;}

    public void setInstruction(int ins){
        this.instruction = ins;
    }
    @XmlElement
    public int getInstruction(){return instruction;}

    public void setVariableName(String variableName){
        this.variableName = variableName;
    }
    public String getVariableName(){return variableName;}

    public void setVariableIndex(int variableIndex){
        this.variableIndex = variableIndex;
    }
    @XmlElement
    public int getVariableIndex(){return variableIndex;}

    public void setValue(Object value){
        this.value = value;
    }
    @XmlTransient
    public Object getValue(){return value;}

    public void setMethod(String method){
        this.method = method;
    }
    @XmlElement
    public String getMethod(){return method;}

    public boolean isAlias(){return alias;}
    public void setAlias(Boolean alias){this.alias=alias;}

    public boolean equals(DefUseVariable var){
        if(var.getLinenumber() == this.linenumber && var.getInstruction() == this.instruction &&
                var.getVariableIndex() == this.variableIndex && var.getVariableName().equals(this.variableName) && var.getMethod().equals(this.method)){
            return this.value == null && var.getValue() == null || var.getValue() != null && var.getValue() == this.value;
        } else return false;
    }

    @Override
    public String toString(){
        return "name: " + variableName + ", ln: "+linenumber + ", method: "+ method;
    }
}
