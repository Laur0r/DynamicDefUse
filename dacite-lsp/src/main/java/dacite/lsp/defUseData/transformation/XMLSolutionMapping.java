package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name="xmlsolutionmapping")
public class XMLSolutionMapping {

    private String method;

    private List<XMLSolution> solutions;

    public XMLSolutionMapping() {

    }

    public XMLSolutionMapping(String method, List<XMLSolution> solutions){
        this.method = method;
        this.solutions = solutions;
    }


    @XmlElement(name="xmlsolution")
    public List<XMLSolution> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<XMLSolution> solutions) {
        this.solutions = solutions;
    }

    @XmlElement(name="xmlsolutionmethod")
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
