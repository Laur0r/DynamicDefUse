package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class DefUseChain {
    private DefUseVariable def;
    private DefUseVariable use;

    private XMLSolution solution;

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

    @XmlElement(name="xmlSolution")
    public XMLSolution getSolution() {
        return solution;
    }

    public void setSolution(XMLSolution solution) {
        this.solution = solution;
    }

    public String toString(){
        String output = "";
        output += "   DefUse var "+use.getVariableIndex()+": Def name="+def.getVariableName()+" Method "+def.getMethod()+" ln=" + def.getLinenumber()+" ins="+def.getInstruction()+" color="+def.getColor() +" --> Use: Method "+use.getMethod()+" ln=" + use.getLinenumber()+" ins="+use.getInstruction()+
                " name="+use.getVariableName();
        return output;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefUseChain that = (DefUseChain) o;
        return this.def.equals(that.def) && this.use.equals(that.use);
    }
}
