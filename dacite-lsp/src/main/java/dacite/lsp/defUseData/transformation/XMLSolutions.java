package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name="xmlsolutions")
public class XMLSolutions {


    private List<XMLSolution> xmlSolutions;

    @XmlElement(name="xmlsolution")
    public List<XMLSolution> getXmlSolutions() {
        return xmlSolutions;
    }

    public void setXmlSolutions(List<XMLSolution> xmlSolutions) {
        this.xmlSolutions = xmlSolutions;
    }
}
