package dacite.core.defuse;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

public class DefUseField extends DefUseVariable{

    @XmlTransient
    protected Object instance;

    protected String instanceName;

    public DefUseField(int linenumber, int instruction, int variableIndex, Object value, String method, String varname, Object instance, String instanceName){
        super(linenumber, instruction, variableIndex, value, method, varname);
        this.instance = instance;
        this.instanceName = instanceName;
    }

    public Object getInstance() {return this.instance;}
    @XmlElement
    public String getInstanceName() {return this.instanceName;}
    public void setInstanceName(String name) {instanceName = name;}

    @Override
    public String toString(){
        return super.toString() + ", instance: " + instanceName;
    }
}
