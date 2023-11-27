package dacite.core.analysis;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

public class DefUseFieldSymbolic extends DefUseSymbolic{

    @XmlTransient
    protected Object instance;

    // name of the instance
    protected String instanceName;

    public DefUseFieldSymbolic(int linenumber, int instruction, int variableIndex, Object value, String method, String varname, Object instance, String instanceName) {
        super(linenumber, instruction, variableIndex, value, method, varname);
        this.instance = instance;
        this.instanceName = instanceName;
    }

    public Object getInstance() {return this.instance;}
    @XmlElement
    public String getInstanceName() {return this.instanceName;}
    public void setInstanceName(String name) {instanceName = name;}
}
