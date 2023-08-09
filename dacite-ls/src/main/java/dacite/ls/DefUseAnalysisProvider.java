package dacite.ls;

import com.google.gson.JsonObject;

import dacite.lsp.defUseData.transformation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import dacite.lsp.defUseData.DefUseClass;
import dacite.lsp.defUseData.DefUseData;
import dacite.lsp.defUseData.DefUseMethod;
import dacite.lsp.defUseData.DefUseVar;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class DefUseAnalysisProvider {

  private static final Logger logger = LoggerFactory.getLogger(DefUseAnalysisProvider.class);

  private static String textDocumentUriTrigger;

  private static List<DefUseChain> defUseChains = new ArrayList<>();
  private static List<DefUseClass> defUseClasses = new ArrayList<>();

  private static List<DefUseChain> notCoveredChains = new ArrayList<>();

  private static List<DefUseClass> notCoveredClasses = new ArrayList<>();

  private static int maxNumberChains;

  private static final String[] indexcolors = new String[]{
          "#1c1c1c", "#F6BE00", "#18c1d6", "#FF34FF", "#FF4A46", "#008941", "#006FA6", "#A30059",
          "#C2A7AF", "#7A4900", "#0000A6", "#63FFAC", "#B79762", "#004D43", "#8FB0FF", "#997D87",
          "#5A0007", "#809693", "#FEFFE6", "#1B4400", "#4FC601", "#3B5DFF", "#4A3B53", "#FF2F80",
          "#61615A", "#BA0900", "#6B7900", "#00C2A0", "#FFAA92", "#FF90C9", "#B903AA", "#D16100",
          "#DDEFFF", "#000035", "#7B4F4B", "#A1C299", "#300018", "#0AA6D8", "#013349", "#00846F",
          "#372101", "#FFB500", "#C2FFED", "#A079BF", "#CC0744", "#C0B9B2", "#C2FF99", "#001E09",
          "#00489C", "#6F0062", "#0CBD66", "#EEC3FF", "#456D75", "#B77B68", "#7A87A1", "#788D66",
          "#885578", "#FAD09F", "#FF8A9A", "#D157A0", "#BEC459", "#456648", "#0086ED", "#886F4C",

          "#34362D", "#B4A8BD", "#00A6AA", "#452C2C", "#636375", "#A3C8C9", "#FF913F", "#938A81",
          "#575329", "#00FECF", "#B05B6F", "#8CD0FF", "#3B9700", "#04F757", "#C8A1A1", "#1E6E00",
          "#7900D7", "#A77500", "#6367A9", "#A05837", "#6B002C", "#772600", "#D790FF", "#9B9700",
          "#549E79", "#FFF69F", "#201625", "#72418F", "#BC23FF", "#99ADC0", "#3A2465", "#922329",
          "#5B4534", "#FDE8DC", "#404E55", "#0089A3", "#CB7E98", "#A4E804", "#324E72", "#6A3A4C",
          "#83AB58", "#001C1E", "#D1F7CE", "#004B28", "#C8D0F6", "#A3A489", "#806C66", "#222800",
          "#BF5650", "#E83000", "#66796D", "#DA007C", "#FF1A59", "#8ADBB4", "#1E0200", "#5B4E51",
          "#C895C5", "#320033", "#FF6832", "#66E1D3", "#CFCDAC", "#D0AC94", "#7ED379", "#012C58"
  };

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

    int i = 0;
    for (HashMap<DefUseVariable, List<DefUseVariable>> defuse : getVariableMapping(true).values()) {
      String color;
      if(i<indexcolors.length){
        color = indexcolors[i];//Color.decode(indexcolors[i]);
      } else {
        Random random = new Random();
        color = "rgb("+random.nextInt(256)+","+ random.nextInt(256)+","+ random.nextInt(256)+")";
      }

      defuse.forEach((def, uses) -> {
        def.setColor(color);
        uses.forEach(it -> {it.setColor(color); it.getChain().getDef().setColor(color);});
      });
      i++;
    }
  }

  public static void processXmlFileSymbolic(String path, File project) throws JAXBException, IOException {
    String pathDir = path.substring(0, path.lastIndexOf("/"));
    Set<Class<?>> classes = parseClassesFromSolution(pathDir, project);
    classes.add(DefUseChains.class);
    logger.info(classes.toString());
    JAXBContext jaxbContext = JAXBContext.newInstance(classes.toArray(new Class[]{}));
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    String test = Files.readString(Paths.get(path));
    var chainCollection = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(test));
    notCoveredChains = chainCollection.getChains();
  }

  public static void deriveNotCoveredChains(String path, File project) throws JAXBException, IOException {
    processXmlFileSymbolic(path, project);

    // Augment parsed information and pre-process
    notCoveredChains.forEach(chain -> {
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
    maxNumberChains = notCoveredChains.size();

    List<DefUseChain> chains = new ArrayList<>();
    for(DefUseChain chain: notCoveredChains){
      if(!defUseChains.contains(chain)){
        chains.add(chain);
      }
    }
    notCoveredChains = chains;
    logger.info(notCoveredChains.toString());
    notCoveredClasses = transformDefUse(notCoveredChains);

    for (HashMap<DefUseVariable, List<DefUseVariable>> defuse : getVariableMapping(false).values()) {
      String color = "rgb("+Color.red.getRed()+","+Color.red.getGreen()+","+Color.red.getBlue()+")";
      defuse.forEach((def, uses) -> {
        def.setColor(color);
        uses.forEach(it -> {it.setColor(color); it.getChain().getDef().setColor(color);});
      });
    }
  }

  public static List<DefUseChain> getDefUseChains() {
    return defUseChains;
  }

  public static List<DefUseClass> getDefUseClasses() {
    return defUseClasses;
  }

  public static List<DefUseClass> getNotCoveredClasses() {
    return notCoveredClasses;
  }

  public static Set<XMLSolution> getSolutions(){
    Set<XMLSolution> solutions = new HashSet<XMLSolution>();
    for(DefUseChain chain :notCoveredChains){
      XMLSolution solution = chain.getSolution();
      Object returnValue = solution.returnValue;
      if(returnValue == null){
        returnValue= new Object[0];
      }
      Map<String, Object> labels = new HashMap<>();
      if(solution.labels != null){
        labels.putAll(solution.labels);
      }
      XMLSolution compSolution = new XMLSolution(solution.exceptional, returnValue, labels);
      solutions.add(compSolution);
    }
    return solutions;
  }

  public static List<DefUseVariable> getDefUseVariables(boolean covered) {
    final List<DefUseVariable> defUseVariables = new ArrayList<>();
    if(covered) {
      for (DefUseChain defUseChain : defUseChains) {
        defUseVariables.add(defUseChain.getDef());
        defUseVariables.add(defUseChain.getUse());
      }
    } else {
      for (DefUseChain defUseChain : notCoveredChains) {
        defUseVariables.add(defUseChain.getDef());
        defUseVariables.add(defUseChain.getUse());
      }
    }
    return defUseVariables;
  }

  public static List<DefUseVariable> getUniqueDefUseVariables(boolean covered) {
    List<DefUseVariable> uniqueDefUseVariables = new ArrayList<>();
    var defUseMapping = getDefUseMapping(covered);
    uniqueDefUseVariables.addAll(defUseMapping.keySet());
    uniqueDefUseVariables.addAll(defUseMapping.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    return uniqueDefUseVariables;
  }

  public static List<DefUseVariable> getUniqueDefUseVariables(String packageName, String className, boolean covered) {
    return getUniqueDefUseVariables(covered).stream()
        .filter(it -> it.getPackageName().equals(packageName) && it.getClassName().equals(className))
        .collect(Collectors.toList());
  }

  public static void changeDefUseEditorHighlighting(JsonObject nodeProperties, Boolean newIsEditorHighlight) {
    if (nodeProperties.has("notCovered")) {
      changeDefUseEditorHighlighting(notCoveredChains, nodeProperties, newIsEditorHighlight);
    } else {
      changeDefUseEditorHighlighting(defUseChains, nodeProperties, newIsEditorHighlight);
    }

  }

  private static void changeDefUseEditorHighlighting(List<DefUseChain> list, JsonObject nodeProperties, Boolean newIsEditorHighlight) {
    list.forEach(chain -> {
      DefUseVariable def = chain.getDef();
      DefUseVariable use = chain.getUse();
      var affected = false;

      // class level filter
      if (nodeProperties.has("packageClass")) {
        var packageClass = nodeProperties.get("packageClass").getAsString();
        affected = def.matchesPackageClass(packageClass);
      }
      // method level filter
      if (nodeProperties.has("method")) {
        var method = nodeProperties.get("method").getAsString();
        affected = affected && def.getMethodName().equals(method);
      }
      // variable level filter
      if (nodeProperties.has("variable")) {
        var variable = nodeProperties.get("variable").getAsString();
        if(variable.contains("[")){
          //logger.info(variable+" defuse: "+defUseVariable.getVariableName()+defUseVariable.getVariableIndex()+"]");
          affected = affected && variable.equals(def.getVariableName()+def.getVariableIndex()+"]");
        } else {
          affected = affected && variable.equals(def.getVariableName());
        }

      }
      // def use level filter
      if (nodeProperties.has("defLocation") && nodeProperties.has("defInstruction")) {
        var defLocation = Integer.parseInt(nodeProperties.get("defLocation").getAsString().substring(1));
        int instruction = nodeProperties.get("defInstruction").getAsInt();
        affected = affected && def.getLinenumber() == defLocation && def.getInstruction() == instruction;
      }

      if (nodeProperties.has("useLocation") && nodeProperties.has("index")) {
        int index = nodeProperties.get("index").getAsInt();
        int instruction = nodeProperties.get("useInstruction").getAsInt();
        if(nodeProperties.get("useLocation").getAsString().contains(" ")){
          String useLocation = nodeProperties.get("useLocation").getAsString();
          String method = useLocation.substring(0,useLocation.indexOf(" "));
          int ln = Integer.parseInt(useLocation.substring(useLocation.indexOf(" ")+2));
          affected = affected && use.getMethodName().equals(method) && use.getLinenumber() == ln
                  && use.getVariableIndex() == index && use.getInstruction() == instruction;
        } else {
          int useLocation = Integer.parseInt(nodeProperties.get("useLocation").getAsString().substring(1));
          affected =
                  affected && use.getLinenumber() == useLocation && use.getVariableIndex() == index
                          && use.getInstruction() == instruction;
        }

      }

      if (affected) {
        def.setEditorHighlight(
                // Standard implementations of the Tree View Protocol do not provide the additional parameter
                // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                newIsEditorHighlight == null ? !def.isEditorHighlight() : newIsEditorHighlight);
        use.setEditorHighlight(
                // Standard implementations of the Tree View Protocol do not provide the additional parameter
                // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                newIsEditorHighlight == null ? !use.isEditorHighlight() : newIsEditorHighlight);
      }
    });
  }

  public static HashMap<DefUseVariable, List<DefUseVariable>> getDefUseMapping(boolean covered) {
    HashMap<DefUseVariable, List<DefUseVariable>> defUseMapping = new HashMap<>();

    List<DefUseVariable> defUseVars = getDefUseVariables(covered);
    Set<DefUseVariable> defs = new HashSet<>();
    Set<DefUseVariable> uses = new HashSet<>();
    for(DefUseVariable var:defUseVars){
      if(var.getRole() == DefUseVariableRole.DEFINITION) {
        if (defs.contains(var) && var.isEditorHighlight()) {
          defs.remove(var);
          defs.add(var);
        } else if(!defs.contains(var)){
          defs.add(var);
        }
      } else {
        if (uses.contains(var) && var.isEditorHighlight()) {
          uses.remove(var);
          uses.add(var);
        } else if(!uses.contains(var)){
          uses.add(var);
        }
      }
    }

    defs.forEach(def -> defUseMapping.put(def,
        uses.stream().filter(use -> use.getChain().getDef().equals(def)).collect(Collectors.toList())));

    return defUseMapping;
  }

  public static HashMap<String, HashMap<DefUseVariable,List<DefUseVariable>>> getVariableMapping(boolean covered) {
    HashMap<String, HashMap<DefUseVariable,List<DefUseVariable>>> variableMapping = new HashMap<>();

    var defUseVars = getDefUseVariables(covered);
    List<DefUseVariable> defs = new ArrayList<>();
    List<DefUseVariable> uses = new ArrayList<>();
    for(DefUseVariable var:defUseVars){
      if(var.getRole() == DefUseVariableRole.DEFINITION) {
        defs.add(var);
      } else {
        uses.add(var);
      }
    }

    for(DefUseVariable def: defs){
      List<DefUseVariable> sameVar = new ArrayList<>();
      sameVar.add(def);
      for(DefUseVariable def2: defs){
        if(def.getVariableName().equals(def2.getVariableName()) && (def.getVariableIndex() == def2.getVariableIndex())){
          sameVar.add(def2);
        }
      }
      HashMap<DefUseVariable,List<DefUseVariable>> mapVar = new HashMap<>();
      for(DefUseVariable var :sameVar){
        mapVar.put(var, uses.stream().filter(use -> use.getChain().getDef().equals(var)).collect(Collectors.toList()));
      }
      if(variableMapping.containsKey(sameVar.get(0).getMethod()+"."+sameVar.get(0).getVariableName())){
        String name = sameVar.get(0).getMethod()+"."+sameVar.get(0).getVariableName();
        HashMap<DefUseVariable, List<DefUseVariable>> defuses = variableMapping.get(name);
        mapVar.forEach((def2, uses2) -> {
          if(!defuses.containsKey(def2)){
            defuses.put(def2,uses2);
          }
        });
        defuses.putAll(mapVar);
        variableMapping.put(name, defuses);
      } else {
        variableMapping.put(sameVar.get(0).getMethod()+"."+sameVar.get(0).getVariableName(),mapVar);
      }
    }

    return variableMapping;
  }

  public static HashMap<Integer, List<DefUseVariable>> getUniqueDefUseVariablesByLine(String packageName,
      String className, boolean covered) {
    HashMap<Integer, List<DefUseVariable>> uniqueDefUseVariablesByLine = new HashMap<>();

    getUniqueDefUseVariables(packageName, className, covered).forEach(
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
      String name = defUseVariable.getVariableName();
      if(defUseVariable.getObjectName() != null ){
        if(!name.contains("[")) {
          name = defUseVariable.getObjectName() + "." + name.substring(name.indexOf(".") + 1);
        } else {
          name = name + defUseVariable.getVariableIndex()+"]";
        }
      }

      if (defUseVariableMap.containsKey(name)) {
        var groupedDefUseVariables = defUseVariableMap.get(name);
        groupedDefUseVariables.add(defUseVariable);

        Comparator<DefUseVariable> byVariableIndex = Comparator.comparingInt(DefUseVariable::getInstruction);
        groupedDefUseVariables.sort(byVariableIndex);
      } else {
        List<DefUseVariable> groupedDefUseVariables = new ArrayList<>();
        groupedDefUseVariables.add(defUseVariable);
        defUseVariableMap.put(name, groupedDefUseVariables);
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
      data.setUseInstruction(use.getInstruction());
      data.setDefInstruction(def.getInstruction());
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
          var.sort();
        }
        cl.addNumberChains(m.getNumberChains());
      }
    }
    return output;
  }

  public static String getTextDocumentUriTrigger() {
    return textDocumentUriTrigger;
  }

  public static void setTextDocumentUriTrigger(String textDocumentUriTrigger) {
    DefUseAnalysisProvider.textDocumentUriTrigger = textDocumentUriTrigger;
  }

  public static Set<Class<?>> parseClassesFromSolution(String path, File project){
    FileInputStream stream = null;
    Set<Class<?>> classes = new HashSet<>();
    try {
      stream = new FileInputStream(path+"/DaciteSolutionClasses.txt");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    String strLine;
    ArrayList<String> lines = new ArrayList<String>();
    try {
      while ((strLine = reader.readLine()) != null) {
        if(strLine.contains("[")){
          classes.add(Class.forName(strLine));
        } else {
          URL url = project.toURI().toURL();
          URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
          Class<?> clazz = classLoader.loadClass(strLine);
          classes.add(clazz);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return classes;
  }

}
