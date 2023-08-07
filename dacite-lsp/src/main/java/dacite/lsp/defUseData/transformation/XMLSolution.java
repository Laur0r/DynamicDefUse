package dacite.lsp.defUseData.transformation;

import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.XsiNilLoader;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class XMLSolution {

    @XmlElement
    public boolean exceptional;
    @XmlElement
    public Object returnValue;

    @XmlElement
    public Object[] returnValueArray;

    @XmlElement
    //@XmlJavaTypeAdapter(XMLLabelAdapter.class)
    public Map<String, Object> labels;

    @XmlElement
    public Map<String, Object[]> labelsArray;

    public XMLSolution(){

    }

    public XMLSolution(boolean exceptional, Object returnValue, Map<String, Object> labels){
        this.exceptional = exceptional;
        this.returnValue = returnValue;
        this.labels = labels;
    }

    public void setSolution(PathSolution solution){
        exceptional = solution instanceof ExceptionPathSolution;
        if(solution.getSolution().returnValue.getClass().isArray()){
            returnValueArray = new Object[Array.getLength(solution.getSolution().returnValue)];
            for(int i=0; i< Array.getLength(solution.getSolution().returnValue); i++){
                returnValueArray[i] = Array.get(solution.getSolution().returnValue,i);
            }
        } else {
            this.returnValue = solution.getSolution().returnValue;
        }
        labels = new HashMap<>();
        labelsArray = new HashMap<>();
        for(Map.Entry<String,Object> entry : solution.getSolution().labels.getIdToLabel().entrySet()){
            String key = entry.getKey();
            Object obj = entry.getValue();
            if(obj.getClass().isArray()){
                Object[] array = new Object[Array.getLength(obj)];
                for(int i=0; i< Array.getLength(obj); i++){
                    array[i] = Array.get(obj,i);
                }
                labelsArray.put(key, array);
            } else{
                labels.put(key, obj);
            }
        }
    }


}
