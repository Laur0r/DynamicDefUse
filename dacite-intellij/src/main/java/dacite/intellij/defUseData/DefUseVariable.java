package dacite.intellij.defUseData;

import javax.xml.bind.annotation.XmlElement;

public class DefUseVariable {
    private int linenumber;
    private String method;
    private int variableIndex;
    private String variableName;

    @XmlElement
    public int getLinenumber() {
        return linenumber;
    }

    public void setLinenumber(int linenumber) {
        this.linenumber = linenumber;
    }

    @XmlElement
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @XmlElement
    public int getVariableIndex() {
        return variableIndex;
    }

    public void setVariableIndex(int variableIndex) {
        this.variableIndex = variableIndex;
    }

    @XmlElement
    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }
}
