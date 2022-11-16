package dacite.ls;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import dacite.lsp.defUseData.DefUseClass;
import dacite.lsp.defUseData.DefUseData;
import dacite.lsp.defUseData.DefUseMethod;
import dacite.lsp.defUseData.DefUseVar;
import dacite.lsp.defUseData.transformation.DefUseChain;
import dacite.lsp.defUseData.transformation.DefUseChains;
import dacite.lsp.defUseData.transformation.DefUseVariable;
import dacite.lsp.defUseData.transformation.DefUseVariableRole;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class DefUseAnalysisProvider {

  private static final Logger logger = LoggerFactory.getLogger(DefUseAnalysisProvider.class);

  private static List<DefUseChain> defUseChains = new ArrayList<>();
  private static List<DefUseClass> defUseClasses = new ArrayList<>();

  public static void processXmlFile(String path) throws JAXBException, IOException {
    JAXBContext jaxbContext = JAXBContext.newInstance(DefUseChains.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    String test = Files.readString(Paths.get(path));
    var chainCollection = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(test));
    defUseChains = chainCollection.getChains();
    defUseClasses = transformDefUse(defUseChains);

    // Augment parsed information and pre-process
    defUseChains.forEach(chain -> {
      var def = chain.getDef();
      var use = chain.getUse();

      // Set backlink
      def.setChain(chain);
      use.setChain(chain);

      // Set role
      def.setRole(DefUseVariableRole.DEFINITION);
      use.setRole(DefUseVariableRole.USAGE);

      // Set visibility of highlights
      def.setEditorHighlight(false);
      use.setEditorHighlight(false);
    });

    getDefUseMapping().forEach((def, uses) -> {
      // Set random color
      Random random = new Random();
      var color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
      def.setColor(color);
      uses.forEach(it -> it.setColor(color));
    });
  }

  public static List<DefUseChain> getDefUseChains() {
    return defUseChains;
  }

  public static List<DefUseClass> getDefUseClasses() {
    return defUseClasses;
  }

  public static List<DefUseVariable> getDefUseVariables() {
    final List<DefUseVariable> defUseVariables = new ArrayList<>();
    for (DefUseChain defUseChain : defUseChains) {
      defUseVariables.add(defUseChain.getDef());
      defUseVariables.add(defUseChain.getUse());
    }
    return defUseVariables;
  }

  public static List<DefUseVariable> getUniqueDefUseVariables() {
    List<DefUseVariable> uniqueDefUseVariables = new ArrayList<>();
    var defUseMapping = getDefUseMapping();
    uniqueDefUseVariables.addAll(defUseMapping.keySet());
    uniqueDefUseVariables.addAll(defUseMapping.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    return uniqueDefUseVariables;
  }

  public static List<DefUseVariable> getUniqueDefUseVariables(String packageName, String className) {
    return getUniqueDefUseVariables().stream()
        .filter(it -> it.getPackageName().equals(packageName) && it.getClassName().equals(className))
        .collect(Collectors.toList());
  }

  public static void changeDefUseEditorHighlighting(JsonObject nodeProperties, boolean newIsEditorHighlight) {
    getDefUseVariables().forEach(defUseVariable -> {
      var affected = false;

      // class level filter
      if (nodeProperties.has("packageClass")) {
        var packageClass = nodeProperties.get("packageClass").getAsString();
        affected = defUseVariable.matchesPackageClass(packageClass);
      }
      // method level filter
      if (nodeProperties.has("method")) {
        var method = nodeProperties.get("method").getAsString();

        affected = affected && defUseVariable.getMethodName().equals(method);
      }
      // variable level filter
      if (nodeProperties.has("variable")) {
        var variable = nodeProperties.get("variable").getAsString();

        affected = affected && defUseVariable.getVariableName().equals(variable);
      }
      // def use level filter
      if (defUseVariable.getRole() == DefUseVariableRole.DEFINITION && nodeProperties.has("defLocation")) {
        var defLocation = Integer.parseInt(nodeProperties.get("defLocation").getAsString().substring(1));

        affected = affected && defUseVariable.getLinenumber() == defLocation;
      } else if (defUseVariable.getRole() == DefUseVariableRole.USAGE && nodeProperties.has("defLocation")
          && nodeProperties.has("useLocation") && nodeProperties.has("index")) {
        var useLocation = Integer.parseInt(nodeProperties.get("useLocation").getAsString().substring(1));
        var index = nodeProperties.get("index").getAsInt();
        var instruction = nodeProperties.get("instruction").getAsInt();

        affected = affected && defUseVariable.getLinenumber() == useLocation
            && defUseVariable.getVariableIndex() == index && defUseVariable.getInstruction() == instruction;
      }

      if (affected) {
        defUseVariable.setEditorHighlight(newIsEditorHighlight);
      }
    });
  }

  public static HashMap<DefUseVariable, List<DefUseVariable>> getDefUseMapping() {
    HashMap<DefUseVariable, List<DefUseVariable>> defUseMapping = new HashMap<>();

    var defUseVars = getDefUseVariables();
    var defs = defUseVars.stream().filter(it -> it.getRole() == DefUseVariableRole.DEFINITION)
        .collect(Collectors.toSet());
    var uses = defUseVars.stream().filter(it -> it.getRole() == DefUseVariableRole.USAGE).collect(Collectors.toSet());

    defs.forEach(def -> defUseMapping.put(def,
        uses.stream().filter(use -> use.getChain().getDef().equals(def)).collect(Collectors.toList())));

    return defUseMapping;
  }

  public static HashMap<Integer, List<DefUseVariable>> getUniqueDefUseVariablesByLine(String packageName,
      String className) {
    HashMap<Integer, List<DefUseVariable>> uniqueDefUseVariablesByLine = new HashMap<>();

    getUniqueDefUseVariables(packageName, className).forEach(
        defUseVariable -> addDefUseVariable(uniqueDefUseVariablesByLine, defUseVariable));

    return uniqueDefUseVariablesByLine;
  }

  private static void addDefUseVariable(HashMap<Integer, List<DefUseVariable>> defUseVariableMap,
      DefUseVariable defUseVariable) {
    if (defUseVariableMap.containsKey(defUseVariable.getLinenumber())) {
      // Check for uniqueness
      var existingDefUseVariables = defUseVariableMap.get(defUseVariable.getLinenumber());
      if (existingDefUseVariables.stream().noneMatch(defUseVariable::equals)) {
        existingDefUseVariables.add(defUseVariable);
      }
    } else {
      List<DefUseVariable> defUseVariables = new ArrayList<>();
      defUseVariables.add(defUseVariable);
      defUseVariableMap.put(defUseVariable.getLinenumber(), defUseVariables);
    }
  }

  public static HashMap<String, List<DefUseVariable>> groupByVariableNamesAndSort(
      List<DefUseVariable> defUseVariables) {
    HashMap<String, List<DefUseVariable>> defUseVariableMap = new HashMap<>();

    defUseVariables.forEach(defUseVariable -> {
      if (defUseVariableMap.containsKey(defUseVariable.getVariableName())) {
        var groupedDefUseVariables = defUseVariableMap.get(defUseVariable.getVariableName());
        groupedDefUseVariables.add(defUseVariable);

        Comparator<DefUseVariable> byVariableIndex = Comparator.comparingInt(DefUseVariable::getVariableIndex);
        groupedDefUseVariables.sort(byVariableIndex);
      } else {
        List<DefUseVariable> groupedDefUseVariables = new ArrayList<>();
        groupedDefUseVariables.add(defUseVariable);
        defUseVariableMap.put(defUseVariable.getVariableName(), groupedDefUseVariables);
      }
    });

    return defUseVariableMap;
  }

  public static ArrayList<DefUseClass> transformDefUse(List<DefUseChain> chains) {
    if (chains == null || chains.size() == 0) {
      return null;
    }
    ArrayList<DefUseClass> output = new ArrayList<>();
    // Go through all recognized DefUseChains
    for (DefUseChain chain : chains) {
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
      String useLocation = useMethodPath + " L" + use.getLinenumber();
      String defLocation = "L" + def.getLinenumber();
      if (defClassName.equals(useClassName)) {
        if (defMethodName.equals(useMethodName)) {
          useLocation = "L" + use.getLinenumber();
        } else {
          useLocation = useMethodName + " L" + use.getLinenumber();
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
      data.setIndex(use.getVariableIndex());
      data.setInstruction(use.getInstruction());
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
    for (DefUseClass cl : output) {
      for (DefUseMethod m : cl.getMethods()) {
        for (DefUseVar var : m.getVariables()) {
          var.setNumberChains(var.getData().size());
          m.addNumberChains(var.getNumberChains());
        }
        cl.addNumberChains(m.getNumberChains());
      }
    }
    return output;
  }

}
