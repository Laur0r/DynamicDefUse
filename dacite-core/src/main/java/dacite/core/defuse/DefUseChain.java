package dacite.core.defuse;

import dacite.lsp.defUseData.transformation.XMLSolution;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class representing a DefUseChain (DUC)
 */
public class DefUseChain {

    private static final AtomicLong count = new AtomicLong(0);
    private final long id;
    // the definition
    private DefUseVariable def;
    // the usage
    private DefUseVariable use;

    private boolean passedAgain;

    protected List<XMLSolution> solutions;

    public DefUseChain(DefUseVariable def, DefUseVariable use){
        this.def = def;
        this.use = use;
        this.id = count.incrementAndGet();
        solutions = new ArrayList<>();
    }

    public void setSolution(XMLSolution solution){
        this.solutions.add(solution);
    }
    public List<XMLSolution> getSolutions(){return solutions;}

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

    @XmlElement
    public long getId(){return id;}

    public void setPassedAgain(boolean passed) {this.passedAgain = passed;}

    public boolean getPassedAgain(){return passedAgain;}

    public boolean equals(DefUseChain chain){
        return chain.getUse().equals(this.use) && chain.getDef().equals(this.def);
    }

}
