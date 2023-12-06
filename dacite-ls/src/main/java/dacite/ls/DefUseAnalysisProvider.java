package dacite.ls;

import com.google.gson.JsonObject;

import dacite.lsp.defUseData.*;
import dacite.lsp.defUseData.transformation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

  private static XMLSolutions xmlSolutionsList;

  private static final String[] indexcolors = new String[]{
          "#434343", "#F6BE00", "#18c1d6", "#FF34FF", "#FF4A46", "#008941", "#006FA6", "#A30059",
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
    for (DefUseClass cl: defUseClasses) {
      for(DefUseMethod m: cl.getMethods()){
        for(DefUseVar var: m.getVariables()){
          for(Def def: var.getDefs()){
            String color;
            if(i<indexcolors.length){
              color = indexcolors[i];//Color.decode(indexcolors[i]);
            } else {
              Random random = new Random();
              color = "rgb("+random.nextInt(256)+","+ random.nextInt(256)+","+ random.nextInt(256)+")";
            }
            def.setColor(color);
          }
          i++;
        }
      }
    }
    logger.info(defUseClasses.toString());
  }

  public static void processXmlFileSymbolic(String path, File project) {
    try{
      String pathDir = path.substring(0, path.lastIndexOf("/"));
      Set<Class<?>> classes = parseClassesFromSolution(pathDir, project);
      classes.add(DefUseChains.class);
      classes.add(XMLSolution.class);
      classes.add(XMLSolutionMapping.class);
      classes.add(XMLSolutions.class);
      logger.info(classes.toString());
      JAXBContext jaxbContext = JAXBContext.newInstance(classes.toArray(new Class[]{}));
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      String chainFile = Files.readString(Paths.get(path));
      DefUseChains chainCollection = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(chainFile));
      String solutionFile = Files.readString(Path.of(pathDir+"/SymbolicSolutions.xml"));
      XMLSolutions solutionList = (XMLSolutions) jaxbUnmarshaller.unmarshal(new StringReader(solutionFile));
      xmlSolutionsList = solutionList;
      for(DefUseChain ch:chainCollection.getChains()){
        String solutionIds = ch.getSolutionIds();
        String[] array = solutionIds.split(",");
        List<XMLSolution> solutionsChain  = new ArrayList<>();
        for(String a:array){
          String[] input = a.split("_");
          String method = input[0];
          int index = Integer.parseInt(input[1]);
          solutionsChain.add(solutionList.getSolution(method, index));
        }
        ch.setSolution(solutionsChain);
      }
      notCoveredChains = chainCollection.getChains();
    } catch (Exception e){
      logger.error(e.getMessage(),e);
    }
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

    List<DefUseChain> chains = new ArrayList<>();
    for(DefUseChain chain: notCoveredChains){
      if(!defUseChains.contains(chain)){
        chains.add(chain);
      }
    }
    notCoveredChains = chains;
    logger.info(notCoveredChains.toString());
    notCoveredClasses = transformDefUse(notCoveredChains);

    for (DefUseClass cl: notCoveredClasses) {
      for (DefUseMethod m : cl.getMethods()) {
        for (DefUseVar var : m.getVariables()) {
          for(Def def: var.getDefs()){
            String color = "rgb(" + Color.red.getRed() + "," + Color.red.getGreen() + "," + Color.red.getBlue() + ")";
            def.setColor(color);
          }
        }
      }
    }
  }

  public static List<DefUseClass> getDefUseClasses() {
    return defUseClasses;
  }

  public static List<DefUseClass> getNotCoveredClasses() {
    return notCoveredClasses;
  }

  public static List<XMLSolution> getSolutions(){
    List<XMLSolution> solutions = new ArrayList<>();
    notCoveredChains.parallelStream().forEach(chain -> {
      List<XMLSolution> solution = chain.getSolution();
      solution.parallelStream().forEach(s -> {
        if(!solutions.contains(s)){
          solutions.add(s);
        }
      });
    });
    return solutions;
  }

  public static BitSet getBitSet(XMLSolution solution){
    BitSet bitset = new BitSet();
    for(DefUseChain chain :notCoveredChains){
      if(chain.getSolution().contains(solution)){
        bitset.set((int) chain.getId());
      }
    }
    //logger.info(String.valueOf(bitset));
    return bitset;
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

  public static void changeDefUseEditorHighlighting(JsonObject nodeProperties, Boolean newIsEditorHighlight) {
    if (nodeProperties.has("notCovered")) {
      logger.info("not Covered");
      changeDefUseEditorHighlightingClasses(notCoveredClasses, nodeProperties, newIsEditorHighlight);
    } else {
      changeDefUseEditorHighlightingClasses(defUseClasses, nodeProperties, newIsEditorHighlight);
    }

  }

  private static void changeDefUseEditorHighlightingClasses(List<DefUseClass> list, JsonObject nodeProperties, Boolean newIsEditorHighlight) {
    for(DefUseClass cl: list){
      boolean affected = false;
      if (nodeProperties.has("packageClass")) {
        var packageClass = nodeProperties.get("packageClass").getAsString();
        affected = cl.getName().equals(packageClass);
      }
      boolean affectedCl = affected;
      for(DefUseMethod m: cl.getMethods()){
        if (nodeProperties.has("method")) {
          var method = nodeProperties.get("method").getAsString();
          affected = affectedCl && m.getName().equals(method);
        }
        boolean affectedM = affected;
        for(DefUseVar var: m.getVariables()){
          if (nodeProperties.has("variable")) {
            var variable = nodeProperties.get("variable").getAsString();
            affected = affectedM && variable.equals(var.getName());
          }
          boolean affectedVar = affected;
          for(Def def: var.getDefs()){
            if (nodeProperties.has("defLocation") && nodeProperties.has("defInstruction")) {
              var defLocation = Integer.parseInt(nodeProperties.get("defLocation").getAsString().substring(1));
              int instruction = nodeProperties.get("defInstruction").getAsInt();
              affected = affectedVar && def.getLinenumber() == defLocation && def.getInstruction() == instruction;
            }
            boolean affectedDef = affected;
            for(Use use: def.getData()){
              if (nodeProperties.has("useLocation") && nodeProperties.has("index")) {
                int instruction = nodeProperties.get("useInstruction").getAsInt();
                String useLocation = nodeProperties.get("useLocation").getAsString();
                affected = affectedDef && use.getUseLocation().equals(useLocation) && use.getInstruction() == instruction;
                if (affected) {
                  logger.info("set highlighting uses");
                  def.setEditorHighlight(
                          // Standard implementations of the Tree View Protocol do not provide the additional parameter
                          // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                          newIsEditorHighlight == null ? !def.isEditorHighlight() : newIsEditorHighlight);
                  use.setEditorHighlight(
                          // Standard implementations of the Tree View Protocol do not provide the additional parameter
                          // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                          newIsEditorHighlight == null ? !use.isEditorHighlight() : newIsEditorHighlight);
                  logger.info(use.toString());
                }
              } else {
                if (affected) {
                  logger.info("set highlighting");
                  def.setEditorHighlight(
                          // Standard implementations of the Tree View Protocol do not provide the additional parameter
                          // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                          newIsEditorHighlight == null ? !def.isEditorHighlight() : newIsEditorHighlight);
                  use.setEditorHighlight(
                          // Standard implementations of the Tree View Protocol do not provide the additional parameter
                          // for newIsEditorHighlight. If it is null, we just toggle the boolean value of affected nodes
                          newIsEditorHighlight == null ? !use.isEditorHighlight() : newIsEditorHighlight);
                  logger.info(use.toString());
                }
              }
            }
          }
        }
      }
    }
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

  public static HashMap<Integer, List<DefUse>> getDefUseByLine(String packageName,
                                                               String className, boolean covered){
    HashMap<Integer, List<DefUse>> uniqueDefUseVariablesByLine = new HashMap<>();
    DefUseClass cl = getClass(packageName,className,covered);
    if(cl != null){
      for(DefUseMethod m:cl.getMethods()){
        for(DefUseVar var:m.getVariables()){
          for(Def def: var.getDefs()){
            addDefUseVariable(uniqueDefUseVariablesByLine, def);
            for(Use use: def.getData()){
              addDefUseVariable(uniqueDefUseVariablesByLine, use);
            }
          }
        }
      }
    }
    return uniqueDefUseVariablesByLine;
  }

  private static DefUseClass getClass(String packageName, String className, boolean covered){
    if(covered){
      for (DefUseClass cl: defUseClasses) {
        if (cl.getName().equals(packageName + "/" + className)){
          return cl;
        }
      }
    } else {
      for (DefUseClass cl: notCoveredClasses) {
        if (cl.getName().equals(packageName + "/" + className)){
          return cl;
        }
      }
    }
    return null;
  }

  private static void addDefUseVariable(HashMap<Integer, List<DefUse>> defUseVariableMap,
                                        DefUse defUseVariable) {
    if (defUseVariableMap.containsKey(defUseVariable.getLinenumber())) {
      // Check for uniqueness
      var existingDefUseVariables = defUseVariableMap.get(defUseVariable.getLinenumber());
      if (existingDefUseVariables.stream().noneMatch(defUseVariable::equals)) {
        existingDefUseVariables.add(defUseVariable);
      }
    } else {
      List<DefUse> defUseVariables = new ArrayList<>();
      defUseVariables.add(defUseVariable);
      defUseVariableMap.put(defUseVariable.getLinenumber(), defUseVariables);
    }
  }

  public static HashMap<String, List<DefUse>> groupByVariableNamesAndSortDefUse(
          List<DefUse> defUseVariables) {
    HashMap<String, List<DefUse>> defUseVariableMap = new HashMap<>();

    defUseVariables.forEach(defUseVariable -> {
      String name = defUseVariable.getName();

      if (defUseVariableMap.containsKey(name)) {
        var groupedDefUseVariables = defUseVariableMap.get(name);
        groupedDefUseVariables.add(defUseVariable);

        Comparator<DefUse> byVariableIndex = Comparator.comparingInt(DefUse::getInstruction);
        groupedDefUseVariables.sort(byVariableIndex);
      } else {
        List<DefUse> groupedDefUseVariables = new ArrayList<>();
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
      if(def.getObjectName() != null ){
        if(!varName.contains("[")) {
          varName = def.getObjectName() + "." + varName.substring(varName.indexOf(".") + 1);
        } else {
          varName = varName + def.getVariableIndex()+"]";
        }
      }
      Def definition = new Def(varName, defLocation, def.getInstruction(), def.getLinenumber());
      DefUseVar var = new DefUseVar(varName);
      if (!use.getVariableName().equals(varName)) {
        varName = use.getVariableName();
      }
      Use data = new Use(varName, useLocation, use.getLinenumber());
      data.setIndex(use.getVariableIndex());
      data.setUseInstruction(use.getInstruction());
      // if output already contains class, add data to existing class instance
      if (output.contains(defUseClass)) {
        DefUseClass instance = output.get(output.indexOf(defUseClass));
        if (instance.getMethods().contains(m)) {
          DefUseMethod mInstance = instance.getMethods().get(instance.getMethods().indexOf(m));
          if (mInstance.getVariables().contains(var)) {
            DefUseVar vInstance = mInstance.getVariables().get(mInstance.getVariables().indexOf(var));
            if(vInstance.getDefs().contains(definition)){
              Def dInstance = vInstance.getDefs().get(vInstance.getDefs().indexOf(definition));
              dInstance.addData(data);
            } else{
              definition.addData(data);
              vInstance.addDef(definition);
            }
          } else {
            definition.addData(data);
            var.addDef(definition);
            mInstance.addVariable(var);
          }
        } else {
          definition.addData(data);
          var.addDef(definition);
          m.addVariable(var);
          instance.addMethod(m);
        }
      } else { // if output does not contain class, create new class instance with data
        definition.addData(data);
        var.addDef(definition);
        m.addVariable(var);
        defUseClass.addMethod(m);
        output.add(defUseClass);
      }
    }
    for (DefUseClass cl : output) {
      for (DefUseMethod m : cl.getMethods()) {
        for (DefUseVar var : m.getVariables()) {
          for(Def def : var.getDefs()){
            def.setNumberChains(def.getData().size());
            var.addNumberChains(def.getNumberChains());
            //var.sort();
          }
          m.addNumberChains(var.getNumberChains());
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

  public static XMLSolutions getXmlSolutionsList(){return xmlSolutionsList;}

}
