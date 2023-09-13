package dacite.lsp.defUseData.transformation;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name="xmlsolutions")
public class XMLSolutions {


    private List<XMLSolutionMapping> xmlSolutions = new ArrayList<>();

    @XmlElement(name="xmlsolutionmapping")
    public List<XMLSolutionMapping> getXmlSolutions() {
        return xmlSolutions;
    }

    public void setXmlSolutions(List<XMLSolutionMapping> xmlSolutions) {
        this.xmlSolutions = xmlSolutions;
    }

    public void addXMLSolution(String method, List<XMLSolution> solutions){
        List<XMLSolution> uniqueSolutions = new ArrayList<>();
        List<XMLSolutionMapping> duplicateSolutions = new ArrayList<>();
        if(xmlSolutions.isEmpty()){
            XMLSolutionMapping mapping = new XMLSolutionMapping(method, solutions);
            xmlSolutions.add(mapping);
            return;
        }
        for(XMLSolution s:solutions){
            for(XMLSolutionMapping mapping: xmlSolutions) {
                List<XMLSolution> entrySolutions = mapping.getSolutions();
                String entryMethod = mapping.getMethod();
                if(entrySolutions.contains(s)){
                    entrySolutions.remove(s);
                    boolean contained = false;
                    for(XMLSolutionMapping mappingDup: duplicateSolutions){
                        if(mappingDup.getMethod().equals(method+","+entryMethod)){
                            mappingDup.getSolutions().add(s);
                            contained = true;
                        }
                    }
                    if(contained){
                        XMLSolutionMapping m = new XMLSolutionMapping(method+","+entryMethod, List.of(s));
                        duplicateSolutions.add(m);
                    }
                } else {
                    uniqueSolutions.add(s);
                }
            }
        }
        if(uniqueSolutions.size() != 0){
            XMLSolutionMapping m = new XMLSolutionMapping(method, uniqueSolutions);
            xmlSolutions.add(m);
        }
    }

    public boolean containsSolution(XMLSolution solution){
        for(XMLSolutionMapping mapping: xmlSolutions){
            List<XMLSolution> solutions = mapping.getSolutions();
            if(solutions.contains(solution)){
                return true;
            }
        }
        return false;
    }

    public String getMethod(XMLSolution solution){
        for(XMLSolutionMapping mapping : xmlSolutions){
            List<XMLSolution> solutions = mapping.getSolutions();
            String method = mapping.getMethod();
            if(solutions.contains(solution)){
                return method;
            }
        }
        return "";
    }

    public XMLSolution getSolution(String method, int index){
        for(XMLSolutionMapping mapping : xmlSolutions) {
            List<XMLSolution> solutions = mapping.getSolutions();
            String m = mapping.getMethod();
            if(method.equals(m)){
                return solutions.get(index);
            }
        }
        return null;
    }

    public List<XMLSolution> getSolutionList(String method){
        for(XMLSolutionMapping mapping : xmlSolutions) {
            List<XMLSolution> solutions = mapping.getSolutions();
            String m = mapping.getMethod();
            if(m.contains(method)){
                return solutions;
            }
        }
        return null;
    }
}
