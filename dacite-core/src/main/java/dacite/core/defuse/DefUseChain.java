package dacite.core.defuse;

import jakarta.xml.bind.annotation.XmlElement;

public class DefUseChain {
    private DefUseVariable def;
    private DefUseVariable use;

    public DefUseChain(DefUseVariable def, DefUseVariable use){
        this.def = def;
        this.use = use;
    }

    public String toString(){
        String output = "";
        output += "   DefUse var "+use.getVariableIndex()+" value "+def.getValue() +": Def Method "+def.getMethod()+" ln=" + def.getLinenumber()+" ins="+def.getInstruction() +" --> Use: Method "+use.getMethod()+" ln=" + use.getLinenumber()+" ins="+use.getInstruction()+
        " name="+use.getVariableName();
        return output;
    }
    @XmlElement
    public DefUseVariable getUse(){
        return use;
    }
    @XmlElement
    public DefUseVariable getDef(){
        return def;
    }

    public boolean equals(DefUseChain chain){
        return chain.getUse().equals(this.use) && chain.getDef().equals(this.def);
    }

}
