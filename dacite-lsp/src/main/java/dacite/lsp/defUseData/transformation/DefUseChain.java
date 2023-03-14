package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;

public class DefUseChain {
    private DefUseVariable def;
    private DefUseVariable use;

    @XmlElement
    public DefUseVariable getDef() {
        return def;
    }

    public void setDef(DefUseVariable def) {
        this.def = def;
    }

    @XmlElement
    public DefUseVariable getUse() {
        return use;
    }

    public void setUse(DefUseVariable use) {
        this.use = use;
    }

    public String toString(){
        String output = "";
        output += "   DefUse var "+use.getVariableIndex()+": Def name="+def.getVariableName()+" Method "+def.getMethod()+" ln=" + def.getLinenumber()+" ins="+def.getInstruction() +" --> Use: Method "+use.getMethod()+" ln=" + use.getLinenumber()+" ins="+use.getInstruction()+
                " name="+use.getVariableName();
        return output;
    }
}
