package dacite.lsp.defUseData.transformation;

import de.wwu.mulib.search.trees.Solution;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement
public class XMLSolution {
    @XmlElement
    public Object returnValue;

    //@XmlJavaTypeAdapter(SolutionAdapter.class)
    @XmlElement
    public Object[] labels;

    public XMLSolution(){

    }

    public void setSolution(Solution solution){
        this.returnValue = solution.returnValue;
        this.labels = solution.labels.getLabels();
    }


}
