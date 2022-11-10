package dacite.ls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import dacite.lsp.defUseData.DefUseClass;
import dacite.lsp.defUseData.DefUseData;
import dacite.lsp.defUseData.DefUseMethod;
import dacite.lsp.defUseData.DefUseVar;
import dacite.lsp.defUseData.transformation.DefUseChain;
import dacite.lsp.defUseData.transformation.DefUseChains;
import dacite.lsp.defUseData.transformation.DefUseVariable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class AnalysisProvider {

  private static final Logger logger = LoggerFactory.getLogger(AnalysisProvider.class);

  private static DefUseChains defUseChains;

  public static DefUseChains getDefUseChains() {
    return defUseChains;
  }

  public static ArrayList<DefUseClass> getDefUseClasses() {
    var defUseClasses = transformDefUse(defUseChains);
    for (DefUseClass cl : defUseClasses) {
      logger.info(cl.toString());
    }
    logger.info(defUseClasses.toString());

    return defUseClasses;
  }

  public static void processXmlFile(String path) throws JAXBException, IOException {
    JAXBContext jaxbContext = JAXBContext.newInstance(DefUseChains.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    String test = Files.readString(Paths.get(path));
    defUseChains = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(test));
  }

  public static ArrayList<DefUseClass> transformDefUse(DefUseChains chains) {
    if (chains == null || chains.getChains().size() == 0) {
      return null;
    }
    ArrayList<DefUseClass> output = new ArrayList<DefUseClass>();
    // Go through all recognized DefUseChains
    for (DefUseChain chain : chains.getChains()) {
      DefUseVariable use = chain.getUse();
      DefUseVariable def = chain.getDef();
      String useMethodPath = chain.getUse().getMethod();
      String defMethodPath = chain.getDef().getMethod();
      // Method String is Class.Method, retrieve name of class and method
      String useClassName = useMethodPath.substring(0, useMethodPath.lastIndexOf("."));
      String defClassName = defMethodPath.substring(0, defMethodPath.lastIndexOf("."));
      String useMethodName = useMethodPath.substring(useMethodPath.lastIndexOf(".") + 1);
      String defMethodName = defMethodPath.substring(defMethodPath.lastIndexOf(".") + 1);

      DefUseClass defUseClass = new DefUseClass(defClassName);
      DefUseMethod m = new DefUseMethod(defMethodName);
      String useLocation = useMethodPath + " line " + use.getLinenumber();
      String defLocation = "Line " + def.getLinenumber();
      if (defClassName.equals(useClassName)) {
        if (defMethodName.equals(useMethodName)) {
          useLocation = "Line " + use.getLinenumber();
        }
      }
      String varName = def.getVariableName();
      DefUseVar var = new DefUseVar(def.getVariableName());
      if (!use.getVariableName().equals(varName)) {
        if (use.getVariableName().contains("[")) {
          varName = use.getVariableName() + use.getVariableIndex() + "]";
          var = new DefUseVar(varName);
        } else {
          varName = varName + "/" + use.getVariableName();
        }
      } else if (varName.contains("[")) {
        varName = varName + use.getVariableIndex() + "]";
        var = new DefUseVar(varName);
      }
      DefUseData data = new DefUseData(varName, defLocation, useLocation);
      data.setIndex(use.getInstruction());
      // if output already contains class, add data to existing class instance
      if (output.contains(defUseClass)) {
        DefUseClass instance = output.get(output.indexOf(defUseClass));
        if (instance.getMethods().contains(m)) {
          DefUseMethod mInstance = instance.getMethods().get(instance.getMethods().indexOf(m));
          if (mInstance.getVariables().contains(var)) {
            DefUseVar vInstance = mInstance.getVariables().get(mInstance.getVariables().indexOf(var));
            vInstance.addData(data);
          } else {
            var.addData(data);
            mInstance.addVariable(var);
          }
        } else {
          var.addData(data);
          m.addVariable(var);
          instance.addMethod(m);
        }
      } else { // if output does not contain class, create new class instance with data
        var.addData(data);
        m.addVariable(var);
        defUseClass.addMethod(m);
        output.add(defUseClass);
      }
    }
    return output;
  }

}
