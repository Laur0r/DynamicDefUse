package dacite.lsp.defUseData.transformation;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="DefUseChains")
public class DefUseChains {
    private List<DefUseChain> chains;
    public DefUseChains(){

    }
    @XmlElement(name="DefUseChain")
    public List<DefUseChain> getChains(){
        return chains;
    }

    public void setChains(List<DefUseChain> chains){
        this.chains = chains;
    }
}
