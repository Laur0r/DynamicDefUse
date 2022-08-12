package dacite.intellij.defUseData;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

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
