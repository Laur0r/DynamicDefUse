package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.util.List;

public class DefUseChain {
    private DefUseVariable def;
    private DefUseVariable use;

    private long id;

    private String solutionIds;

    private List<XMLSolution> solution;

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
    public List<XMLSolution> getSolution() {
        return solution;
    }

    public void setSolution(List<XMLSolution> solution) {
        this.solution =solution;
    }

    @XmlElement
    public String getSolutionIds() {
        return solutionIds;
    }

    public void setSolutionIds(String solutionIds) {
        this.solutionIds =solutionIds;
    }

    @XmlElement
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String toString(){
        String output = "";
        output += "  id="+id+ " DefUse var "+use.getVariableIndex()+": Def name="+def.getVariableName()+" Method "+def.getMethod()+" ln=" + def.getLinenumber()+" ins="+def.getInstruction()+" color="+def.getColor() +" --> Use: Method "+use.getMethod()+" ln=" + use.getLinenumber()+" ins="+use.getInstruction()+
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
