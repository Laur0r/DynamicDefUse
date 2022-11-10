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
}
