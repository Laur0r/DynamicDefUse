package dacite.ls;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dacite.lsp.DaciteExtendedLanguageClient;
import dacite.lsp.defUseData.transformation.XMLSolution;
import dacite.lsp.tvp.TreeViewDidChangeParams;
import dacite.lsp.tvp.TreeViewNode;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandRegistry {

  private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

  public static final String COMMAND_PREFIX = "dacite.";

  enum Command {
    analyze,
    analyzeSymbolic,
    symbolicTrigger,
    generateTestCases,
    highlight
  }

  public static List<String> getCommands() {
    return Arrays.stream(Command.values()).map(it -> COMMAND_PREFIX + it.name()).collect(Collectors.toList());
  }

  public static CompletableFuture<Object> execute(ExecuteCommandParams params, DaciteExtendedLanguageClient client) {
    logger.info("{}", params);

    var command = Command.valueOf(params.getCommand().replaceFirst(COMMAND_PREFIX, ""));
    var args = params.getArguments();

    switch (command) {
        case analyze:
          try {
            String textDocumentUri = args.size() > 0 ? ((JsonPrimitive) args.get(0)).getAsString() : null;
            if (textDocumentUri != null && textDocumentUri.startsWith("file://")) {
              // Extract project's root directory
              Path textDocumentPath = Paths.get(textDocumentUri.replace("file://", ""));
              String projectDir = textDocumentPath.getParent().toString().split("src/")[0];

              // Extract package and class name
              var analyser = new CodeAnalyser(TextDocumentItemProvider.get(textDocumentUri).getText());
              String className = analyser.extractClassName();
              String packageName = analyser.extractPackageName();

              // Construct class path
              // * dacite-core (and thus the agent) is a dependency of dacite-ls and is thus contained in the class path
              //   of the running language server process
              // * We also add all jars that can be found within the project's root directory
              // * We also add all folders which contain .class files
              // * We also add folders we can find that typically contain .class files
              String fullClassPath = System.getProperty("java.class.path");
              fullClassPath += Files.find(Paths.get(projectDir), 50,
                      (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))
                  .map(it -> ":" + it.toString()).reduce(String::concat).orElse("");
              fullClassPath += findClassPathFolders(projectDir + "out/production"); // IntelliJ
              fullClassPath += findClassPathFolders(projectDir + "build/classes/java/main"); // gradle
              fullClassPath += findClassPathFolders(projectDir + "target/classes"); // maven

              // As dacite-core must be within the constructed class path we can extract the corresponding jar
              String javaAgentJar = Arrays.stream(fullClassPath.split(":")).filter(it -> it.contains("dacite-core"))
                  .findFirst().get();

              // Construct command arguments
              var commandArgs = List.of(
                  // Java binary
                  System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                  // Java agent
                  "-javaagent:" + javaAgentJar + "=" + packageName.replace(".", "/"),
                  // Classpath
                  "-classpath", fullClassPath,
                  // Main class in dacite-core
                  "dacite.core.DaciteLauncher",
                  "dacite",
                      // Projectpath
                      projectDir + "src/",
                      // Packagename
                      packageName +"/",
                  // Class to be analyzed
                  className);

              // Start process in project's root directory
              ProcessBuilder pb = (new ProcessBuilder(commandArgs)).redirectError(ProcessBuilder.Redirect.INHERIT)
                  .directory(new File(projectDir));
              Process process = pb.start();
              logger.info("{}: process {} started", "executed command", pb.command());
              logger.info("process exited with status {}", process.waitFor());

              // Get standard output
            /*
            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
              String readLine;
              while ((readLine = bufferedReader.readLine()) != null) {
                processOutput.append(readLine + System.lineSeparator());
              }
            }
            String stdOut = processOutput.toString().trim();
             */

              DefUseAnalysisProvider.processXmlFile(projectDir + "coveredDUCs.xml", true);
              DefUseAnalysisProvider.setTextDocumentUriTrigger(textDocumentUri);
              TreeViewNode node = new TreeViewNode("defUseChains","", "");
              TreeViewDidChangeParams paramsChange = new TreeViewDidChangeParams(new TreeViewNode[]{node});
              client.treeViewDidChange(paramsChange);
            }
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
          return CompletableFuture.completedFuture(null);

      case analyzeSymbolic:
        try {
          String textDocumentUri = args.size() > 0 ? ((JsonPrimitive) args.get(0)).getAsString() : null;
          logger.info("in analyze Symbolic");
          if (textDocumentUri != null && textDocumentUri.startsWith("file://")) {
            // Extract project's root directory
            Path textDocumentPath = Paths.get(textDocumentUri.replace("file://", ""));
            String projectDir = textDocumentPath.getParent().toString().split("src/")[0];

            // Extract package and class name
            var analyser = new CodeAnalyser(TextDocumentItemProvider.get(textDocumentUri).getText());
            String className = analyser.extractClassName();
            String packageName = analyser.extractPackageName().replace(".", "/");

            // Construct class path
            // * dacite-core (and thus the agent) is a dependency of dacite-ls and is thus contained in the class path
            //   of the running language server process
            // * We also add all jars that can be found within the project's root directory
            // * We also add all folders which contain .class files
            // * We also add folders we can find that typically contain .class files
            String fullClassPath = System.getProperty("java.class.path");
            fullClassPath += Files.find(Paths.get(projectDir), 50,
                            (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))
                    .map(it -> ":" + it.toString()).reduce(String::concat).orElse("");
            fullClassPath += findClassPathFolders(projectDir + "out/production"); // IntelliJ
            fullClassPath += findClassPathFolders(projectDir + "build/classes/java/main"); // gradle
            fullClassPath += findClassPathFolders(projectDir + "target/classes"); // maven

            // As dacite-core must be within the constructed class path we can extract the corresponding jar
            String javaAgentJar = Arrays.stream(fullClassPath.split(":")).filter(it -> it.contains("dacite-core"))
                    .findFirst().get();

            // Construct command arguments
            var commandArgs = List.of(
                    // Java binary
                    System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                    // Java agent
                    //"-javaagent:" + javaAgentJar + "=" + packageName,
                    // Classpath
                    "-classpath", fullClassPath,
                    // Main class in dacite-core
                    "dacite.core.DaciteLauncher",
                    "symbolic",
                    // Projectpath
                    projectDir + "src/",
                    // Packagename
                    packageName +"/",
                    // Classname
                    className+".java");

            // Start process in project's root directory
            ProcessBuilder pb = (new ProcessBuilder(commandArgs)).redirectError(ProcessBuilder.Redirect.INHERIT)
                    .directory(new File(projectDir));
            Process process = pb.start();
            logger.info("{}: process {} started", "executed command", pb.command());
            logger.info("process exited with status {}", process.waitFor());

            DefUseAnalysisProvider.deriveNotCoveredChains(projectDir + "SymbolicDUCs.xml");
            TreeViewNode node = new TreeViewNode("notCoveredDUC","", "");
            TreeViewDidChangeParams paramsChange = new TreeViewDidChangeParams(new TreeViewNode[]{node});
            client.treeViewDidChange(paramsChange);
          }
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
        return CompletableFuture.completedFuture(null);

        case symbolicTrigger:
          String textDocumentUri = args.size() > 0 ? ((JsonPrimitive) args.get(0)).getAsString() : null;
          if (textDocumentUri != null && textDocumentUri.startsWith("file://")) {
            CodeAnalyser analyser = new CodeAnalyser(TextDocumentItemProvider.get(textDocumentUri).getText());
            //analyser.methodVisitor();
            String className = analyser.extractClassName();
            String packageName = analyser.extractPackageName();

            // Extract project's root directory
            Path textDocumentPath = Paths.get(textDocumentUri.replace("file://", ""));
            String projectDirStart = textDocumentPath.getParent().toString();
            projectDirStart = projectDirStart.substring(0, projectDirStart.lastIndexOf("/src/"));
            String projectName = projectDirStart.substring(projectDirStart.lastIndexOf("/")+1);
            String projectDir = projectDirStart+"/out/production/"+projectName;
            File projectFile = new File(projectDir);
            if(!projectFile.exists()){
              projectDir = projectDirStart+"/build/classes/java/main/"+projectName;
              projectFile = new File(projectDir);
            }
            if(!projectFile.exists()){
              projectDir = projectDirStart+"/target/classes/"+projectName;
              projectFile = new File(projectDir);
            }
            if(!projectFile.exists()){
              throw new RuntimeException("Class directory not found for executed File");
            }
            String uri = textDocumentUri.substring(0,textDocumentUri.lastIndexOf("/"));
            uri += "/DaciteSymbolicDriverFor" + className + ".java";
            CreateFile createFile = new CreateFile(uri, new CreateFileOptions(true,false));
            List<TextEdit> edits = generateSearchRegions(projectFile, packageName+"."+className);

            TextDocumentEdit documentEdit = new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri,1), edits);
            List<Either<TextDocumentEdit, ResourceOperation>> changes = new ArrayList<>();
            changes.add(Either.forRight(createFile));
            changes.add(Either.forLeft(documentEdit));
            WorkspaceEdit edit = new WorkspaceEdit();
            edit.setDocumentChanges(changes);
            client.applyEdit(new ApplyWorkspaceEditParams(edit));
            return CompletableFuture.completedFuture(null);
          }

      case generateTestCases:
        logger.info("generateTestCases");
        textDocumentUri = args.size() > 0 ? ((JsonPrimitive) args.get(0)).getAsString() : null;
        if (textDocumentUri != null && textDocumentUri.startsWith("file://")) {
          CodeAnalyser analyser = new CodeAnalyser(TextDocumentItemProvider.get(textDocumentUri).getText());
          //analyser.methodVisitor();
          String className = analyser.extractClassName();
          String packageName = analyser.extractPackageName();

          // Extract project's root directory
          Path textDocumentPath = Paths.get(textDocumentUri.replace("file://", ""));
          String projectDirStart = textDocumentPath.getParent().toString();
          projectDirStart = projectDirStart.substring(0, projectDirStart.lastIndexOf("/src/"));
          String projectName = projectDirStart.substring(projectDirStart.lastIndexOf("/")+1);
          String projectDir = projectDirStart+"/out/production/"+projectName;
          File projectFile = new File(projectDir);
          if(!projectFile.exists()){
            projectDir = projectDirStart+"/build/classes/java/main/"+projectName;
            projectFile = new File(projectDir);
          }
          if(!projectFile.exists()){
            projectDir = projectDirStart+"/target/classes/"+projectName;
            projectFile = new File(projectDir);
          }
          if(!projectFile.exists()){
            throw new RuntimeException("Class directory not found for executed File");
          }

          String uri = textDocumentUri.substring(0,textDocumentUri.lastIndexOf("/"));
          List<Either<TextDocumentEdit, ResourceOperation>> changes = generateTestCases(projectFile, packageName, className, uri);
          WorkspaceEdit edit = new WorkspaceEdit();
          edit.setDocumentChanges(changes);
          client.applyEdit(new ApplyWorkspaceEditParams(edit));
          return CompletableFuture.completedFuture(null);
        }

        case highlight:
          try {
            if (args.size() > 0) {
              var nodeProperties = (JsonObject) args.get(0);
              Boolean newIsEditorHighlight = null;
              if (args.size() == 2) {
                newIsEditorHighlight = ((JsonPrimitive) args.get(1)).getAsBoolean();
              }
              DefUseAnalysisProvider.changeDefUseEditorHighlighting(nodeProperties, newIsEditorHighlight);
              logger.info("highlight", nodeProperties);
            }
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
          return CompletableFuture.completedFuture(null);
    }

    throw new RuntimeException("Not implemented");
  }

  private static String findClassPathFolders(String basePath) {
    var files = new File(basePath).listFiles(File::isDirectory);
    if (files != null) {
      return Arrays.stream(files).map(File::toString).collect(Collectors.toList()).stream().map(it -> ":" + it)
          .reduce(String::concat).orElse("");
    }

    return "";
  }

  private static List<TextEdit> generateSearchRegions(File project, String classname){
    Map<String, List<String>> invokedMethods = analyseJUnitTest(project,classname);
    List<TextEdit> edits = new ArrayList<>();
    int line = 0;
    String ls = System.lineSeparator();
    String packageHeader = "package tryme;"+ls; //// TODO Edit package name
    line = createTextEditAndIncrementLine(edits, line, packageHeader);

    String importHeader = "import de.wwu.mulib.Mulib;"+ls+ls;
    line = createTextEditAndIncrementLine(edits, line, importHeader);

    String driverComment = "/* This class serves as a search region and prepares the input values for the symbolic execution. */"+ls;
    line = createTextEditAndIncrementLine(edits, line, driverComment);

    String classHeader = "public class DaciteSymbolicDriverFor" + classname.substring(classname.lastIndexOf('.') + 1) + " {"+ls;
    line = createTextEditAndIncrementLine(edits, line, classHeader);
    int counter = 0;
    for (Map.Entry<String, List<String>> entry : invokedMethods.entrySet()) {
      List<String> parameters = entry.getValue().subList(2,entry.getValue().size());
      String returnType = entry.getValue().get(1);
      String staticRef = entry.getValue().get(0);
      final String method = entry.getKey();
      String indent = "    ";

      String m = indent + "public static "+returnType+" driver"+counter+"() {"+ls;
      line = createTextEditAndIncrementLine(edits, line, m);
      String commentInput = indent.repeat(2) + "/* Input values */"+ls;
      line = createTextEditAndIncrementLine(edits, line, commentInput);


      boolean isNonStaticMethod = staticRef.equals("object") && method.contains(".");
      final String objName = isNonStaticMethod ? "arg0" : null;
      String[] parameterNames = new String[parameters.size()];
      int parameterNumber = isNonStaticMethod ? 1 : 0;
      for (int i = 0; i < parameterNames.length; i++) {
        parameterNames[i] = "arg" + parameterNumber;
        parameterNumber++;
      }
      boolean containsArray = false;
      for(int i = 0; i < parameters.size(); i++){
        String parameter = parameters.get(i);
        String p = indent.repeat(2) + parameter + " " +  parameterNames[i];
        String mulibRememberPrefix = " = Mulib.rememberedFree";
        switch (parameter) {
          case "int": p+= mulibRememberPrefix + "Int(\""+parameterNames[i]+"\");";break;
          case "double": p+= mulibRememberPrefix + "Double(\""+parameterNames[i]+"\");";break;
          case "byte":p+= mulibRememberPrefix + "Byte(\""+parameterNames[i]+"\");";break;
          case "boolean":p+= mulibRememberPrefix + "Boolean(\""+parameterNames[i]+"\");";break;
          case "short":p+= mulibRememberPrefix + "Short(\""+parameterNames[i]+"\");";break;
          case "long":p+= mulibRememberPrefix + "Long(\""+parameterNames[i]+"\");";break;
          case "char": p+= mulibRememberPrefix + "Char(\""+parameterNames[i]+"\");"; break;
          default: p+= mulibRememberPrefix + "Object(\""+parameterNames[i]+"\", " + parameter + ".class);";
        }
        if(parameter.contains("[")){
          containsArray = true;
        }
        p += ls;
        line = createTextEditAndIncrementLine(edits, line, p);
      }

      if(containsArray) {
        String arrayComment = indent.repeat(2)+"// To restrict the array size use: Mulib.assume(array.lenght<...);"+ls;
        line = createTextEditAndIncrementLine(edits, line, arrayComment);
      }

      final String methodCall;
      if (isNonStaticMethod) {
        String namedClass = method.substring(0, method.lastIndexOf("."));
        String object =
                String.format("%s%s %s = Mulib.rememberedFreeObject(\"%s\", %s.class);", indent.repeat(2), namedClass, objName, objName, namedClass) + ls;
        line = createTextEditAndIncrementLine(edits, line, object);
        methodCall = objName + "." +method.substring(method.lastIndexOf(".")+1);
      } else {
        methodCall = method;
      }
      StringBuilder methodS = new StringBuilder(indent.repeat(2));
      if(!returnType.equals("void")){
        methodS.append(returnType).append(" ").append("r0 = ");
      }

      methodS.append(methodCall).append("(");
      int argStart = isNonStaticMethod ? 1 : 0;
      for(int i=argStart; i<(parameters.size()+argStart);i++){
        methodS.append("arg").append(i).append(",");
      }
      if(!parameters.isEmpty()){
        methodS = new StringBuilder(methodS.substring(0, methodS.length() - 1));
      }
      methodS.append(");").append(ls);
      line = createTextEditAndIncrementLine(edits, line, methodS.toString());
      for (int i = 0; i < parameters.size(); i++) {
        // Remember state of input-object after executing method
        String parameterType = parameters.get(i);
        String parameterName = parameterNames[i];
        String rememberCall = null;
        switch (parameterType) {
          case "int":
          case "double":
          case "byte":
          case "boolean":
          case "short":
          case "long":
          case "char": break;
          default: rememberCall = String.format("%sMulib.remember(%s, \"%sAfterExec\");%s", indent.repeat(2), parameterName, parameterName, ls);
        }
        if (rememberCall != null) {
          line = createTextEditAndIncrementLine(edits, line, rememberCall);
        }
      }
      if (isNonStaticMethod) {
        String rememberObj = String.format("%sMulib.remember(%s, \"%sAfterExec\");%s", indent.repeat(2), objName, objName, ls);
        line = createTextEditAndIncrementLine(edits, line, rememberObj);
      }
      String end = "";
      if(!returnType.equals("void")){
        end = indent.repeat(2) + "return r0;" + ls;
      }
      end = end + indent + "}" + ls;
      line = createTextEditAndIncrementLine(edits, line, end);
      counter ++;
    }
    String finalEnd = "}"+ls;
    createTextEditAndIncrementLine(edits, line, finalEnd);
    return edits;
  }

  private static int createTextEditAndIncrementLine(List<TextEdit> edits, int line, String object) {
    Range range = new Range();
    range.setStart(new Position(line,0));
    range.setEnd(new Position(line, object.length()));
    TextEdit textEdit2 = new TextEdit(range,object);
    edits.add(textEdit2);
    return line + 2;
  }

  private static Map<String, List<String>> analyseJUnitTest(File project, String classname){
    String packageName = classname.substring(0, classname.lastIndexOf("."));
    ClassReader reader;
    try {
      //logger.info(classname);
      URL url = project.toURI().toURL();
      logger.info(String.valueOf(url));
      URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
      InputStream input = classLoader.getResourceAsStream(classname.replace('.', '/') + ".class");
      //logger.info(input.toString());
      reader = new ClassReader(input);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ClassNode classNode = new ClassNode();
    reader.accept(classNode, 0);
    Map<String, List<String>> invokedMethods = new HashMap<>();
    for(MethodNode mnode : classNode.methods) {
      //logger.info(mnode.name);
      if (mnode.visibleAnnotations != null) {
        for (AnnotationNode an : mnode.visibleAnnotations) {
          if (an.desc.equals("Lorg/junit/Test;")) {
            InsnList insns = mnode.instructions;
            Iterator<AbstractInsnNode> j = insns.iterator();
            while (j.hasNext()) {
              AbstractInsnNode in = j.next();
              if (in instanceof MethodInsnNode) {
                MethodInsnNode methodins = (MethodInsnNode) in;
                if(methodins.owner.contains(packageName) && !methodins.name.equals("<init>")){
                  String name = methodins.owner + "." + methodins.name;
                  if(methodins.owner.contains("/")){
                    name = methodins.owner.substring(methodins.owner.lastIndexOf("/")+1)+"." + methodins.name;
                  }
                  Type[] types = Type.getArgumentTypes(methodins.desc);
                  List<String> list = new ArrayList<>();
                  if(methodins.getOpcode() == Opcodes.INVOKESTATIC){
                    list.add("static");
                  } else if(methodins.getOpcode() == Opcodes.INVOKEINTERFACE){
                    list.add("interface");
                  } else {
                    list.add("object");
                  }
                  String returnType = Type.getReturnType(methodins.desc).getClassName();
                  list.add(returnType);
                  List<String> list2 = Arrays.stream(types).map(Type::getClassName).collect(Collectors.toList());
                  list.addAll(list2);
                  invokedMethods.put(name, list);
                }
                //logger.info(methodins.owner + "." + methodins.name);
              }
            }
          }
        }
      }
    }
    return invokedMethods;
  }

  private static List<Either<TextDocumentEdit, ResourceOperation>> generateTestCases(File project, String packageName, String classname, String uri){
    Set<XMLSolution> solutions = DefUseAnalysisProvider.getSolutions();
    List<TestCase> testCaseList = new ArrayList<>();
    for(XMLSolution solution : solutions){
      TcgConfig config = TcgConfig.builder().build();
      TestCase testCase = new TestCase(solution.exceptional,solution.labels,solution.returnValue, new BitSet(),config);
      testCaseList.add(testCase);
    }
    Map<String, List<String>> invokedMethods = analyseJUnitTest(project,packageName +"."+classname);
    Iterator<Map.Entry<String, List<String>>> iterator = invokedMethods.entrySet().iterator();
    Map.Entry<String, List<String>> firstMethod = iterator.next(); // TODO Loop for multiple drivers

    String testingClassName = firstMethod.getKey().substring(0,firstMethod.getKey().lastIndexOf("."));
    String testingMethodName = firstMethod.getKey().substring(firstMethod.getKey().lastIndexOf(".")+1);
    List<String> parameters = firstMethod.getValue();
    parameters = parameters.subList(1, parameters.size()-1);
    logger.info(parameters.toString());

    Class myclass = null;
    try {
      URL url = project.toURI().toURL();
      logger.info(String.valueOf(url));
      URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
      myclass = classLoader.loadClass(packageName+"."+testingClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    Method[] methods = myclass.getDeclaredMethods();
    Method methodUnderTest = null;
    for(Method method : methods){
      if(method.getName().equals(testingMethodName)){
        methodUnderTest = method;
      }
    }
    TestCases testCases = new TestCases(testCaseList,methodUnderTest);
    TcgConfig config = TcgConfig.builder().build();
    TestCasesStringGenerator tcg = new TestCasesStringGenerator(testCases, config);
    String test = tcg.generateTestClassStringRepresentation();
    logger.info(test);
    List<TextEdit> testEdits = new ArrayList<>();
    createTextEditAndIncrementLine(testEdits, 0, test);

    uri += testingClassName + "DaciteTest.java";
    CreateFile createFile = new CreateFile(uri, new CreateFileOptions(true,false));

    TextDocumentEdit documentEdit = new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri,1), testEdits);
    List<Either<TextDocumentEdit, ResourceOperation>> changes = new ArrayList<>();
    changes.add(Either.forRight(createFile));
    changes.add(Either.forLeft(documentEdit));
    return changes;
  }

}
