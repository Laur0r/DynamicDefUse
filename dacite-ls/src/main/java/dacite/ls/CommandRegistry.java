package dacite.ls;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
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
    highlight
  }

  public static List<String> getCommands() {
    return Arrays.stream(Command.values()).map(it -> COMMAND_PREFIX + it.name()).collect(Collectors.toList());
  }

  public static CompletableFuture<Object> execute(ExecuteCommandParams params, LanguageClient client) {
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

              DefUseAnalysisProvider.processXmlFile(projectDir + "file.xml");
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

            //DefUseAnalysisProvider.processXmlFile(projectDir + "file.xml");
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
            String projectDir = textDocumentPath.getParent().toString().split("src/")[0]+"/out/production/TestAnalysis1";
            File projectFile = new File(projectDir);
            List<TextEdit> c = generateSearchRegions(projectFile, packageName+"."+className);

            String uri = textDocumentUri.substring(0,textDocumentUri.lastIndexOf("/"));
            uri += "/DaciteSymbolicDriver.java";
            CreateFile createFile = new CreateFile(uri, new CreateFileOptions(true,false));
            List<TextEdit> edits = generateSearchRegions(projectFile, packageName+"."+className);
            /*int line = 0;
            String packageHeader = "package tryme;\n";
            Range range = new Range();
            range.setStart(new Position(line,0));
            range.setEnd(new Position(line, packageHeader.length()));
            line++;
            TextEdit textEdit = new TextEdit(range,packageHeader);
            edits.add(textEdit);

            String classHeader = "public class DaciteSymbolicDriver {\n";
            Range range0 = new Range();
            range0.setStart(new Position(line,0));
            range0.setEnd(new Position(line, classHeader.length()));
            line++;
            TextEdit textEdit0 = new TextEdit(range0,classHeader);
            edits.add(textEdit0);

            String m = " public static String driver(){\n";
            Range range1 = new Range();
            range1.setStart(new Position(line,0));
            range1.setEnd(new Position(line, m.length()));
            line++;
            TextEdit textEdit1 = new TextEdit(range1,m);
            edits.add(textEdit1);
            String end = "  return \"test\";}}";
            Range range2 = new Range();
            range2.setStart(new Position(line,0));
            range2.setEnd(new Position(line, end.length()));
            TextEdit textEdit2 = new TextEdit(range2,end);
            edits.add(textEdit2);*/
            TextDocumentEdit documentEdit = new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri,1), edits);
            List<Either<TextDocumentEdit, ResourceOperation>> changes = new ArrayList<>();
            changes.add(Either.forRight(createFile));
            changes.add(Either.forLeft(documentEdit));
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
    List<String> output = new ArrayList<>();
    String packageName = classname.substring(0, classname.lastIndexOf("."));
    ClassReader reader = null;
    try {
      //logger.info(classname);
      URL url = project.toURI().toURL();
      //logger.info(String.valueOf(url));
      URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
      //logger.info(String.valueOf(classLoader.getURLs()[0]));
      //Class myclass = classLoader.loadClass(classname);
      //logger.info(myclass.getName());
      InputStream input = classLoader.getResourceAsStream(classname.replace('.', '/') + ".class");
      //logger.info(input.toString());
      reader = new ClassReader(input);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ClassNode node = new ClassNode();
    reader.accept(node, 0);
    Map<String, List<String>> invokedMethods = new HashMap<>();
    for(MethodNode mnode : node.methods) {
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
    List<TextEdit> edits = new ArrayList<>();
    int line = 0;
    String packageHeader = "package tryme;"+System.getProperty("line.separator")+"import de.wwu.mulib.Mulib;"+System.getProperty("line.separator");
    Range range0 = new Range();
    range0.setStart(new Position(line,0));
    range0.setEnd(new Position(line, packageHeader.length()));
    line++;
    TextEdit textEdit0 = new TextEdit(range0,packageHeader);
    edits.add(textEdit0);

    String importHeader = "import de.wwu.mulib.Mulib;"+System.getProperty("line.separator");
    Range importRange = new Range();
    importRange.setStart(new Position(line,0));
    importRange.setEnd(new Position(line, importHeader.length()));
    line++;
    TextEdit textEditImport = new TextEdit(importRange,importHeader);
    edits.add(textEditImport);

    String classHeader = "public class DaciteSymbolicDriver {"+System.getProperty("line.separator");
    Range range = new Range();
    range.setStart(new Position(line,0));
    range.setEnd(new Position(line, classHeader.length()));
    line++;
    TextEdit textEdit = new TextEdit(range,classHeader);
    edits.add(textEdit);
    int counter = 0;
    for (Map.Entry<String, List<String>> entry : invokedMethods.entrySet()) {
      List<String> parameters = entry.getValue().subList(2,entry.getValue().size());
      String returnType = entry.getValue().get(1);
      String staticRef = entry.getValue().get(0);
      String method = entry.getKey();

      String m = " public static "+returnType+" driver"+counter+"(){"+System.getProperty("line.separator");
      Range range1 = new Range();
      range1.setStart(new Position(line,0));
      range1.setEnd(new Position(line, m.length()));
      line++;
      TextEdit textEdit1 = new TextEdit(range1,m);
      edits.add(textEdit1);

      for(int i=0; i<parameters.size();i++){
        String p = "  "+parameters.get(i) + " a"+i;
        switch (parameters.get(i)){
          case "int": p+="= Mulib.rememberedFreeInt(\"a"+i+"\");";break;
          case "double": p+="= Mulib.rememberedFreeDouble(\"a"+i+"\");";break;
          case "byte":p+="= Mulib.rememberedFreeByte(\"a"+i+"\");";break;
          case "boolean":p+="= Mulib.rememberedFreeBoolean(\"a"+i+"\");";break;
          case "short":p+="= Mulib.rememberedFreeShort(\"a"+i+"\");";break;
          case "long":p+="= Mulib.rememberedFreeLong(\"a"+i+"\");";break;
          default: p+="= Mulib.rememberedFreeObject(\"a"+i+"\", "+parameters.get(i)+".class);";
        }
        p += System.getProperty("line.separator");
        Range range2 = new Range();
        range2.setStart(new Position(line,0));
        range2.setEnd(new Position(line, p.length()));
        line++;
        TextEdit textEdit2 = new TextEdit(range2,p);
        edits.add(textEdit2);
      }
      if(staticRef.equals("object") && method.contains(".")){
        String namedClass = ""+method.substring(0,method.indexOf("."));
        String object = "  "+namedClass+" obj = new "+namedClass+"();"+System.getProperty("line.separator");
        Range range2 = new Range();
        range2.setStart(new Position(line,0));
        range2.setEnd(new Position(line, object.length()));
        line++;
        TextEdit textEdit2 = new TextEdit(range2,object);
        edits.add(textEdit2);
        method = "obj."+method.substring(method.indexOf(".")+1);
      }
      String methodS = "  ";
      if(!returnType.equals("void")){
        methodS+=returnType+" r0 = ";
      }

      methodS+=method+"(";
      for(int i=0; i<parameters.size();i++){
        methodS+="a"+i+",";
      }
      if(parameters.size()!=0){
        methodS = methodS.substring(0,methodS.length()-1);
      }
      methodS+=");"+System.getProperty("line.separator");
      Range range2 = new Range();
      range2.setStart(new Position(line,0));
      range2.setEnd(new Position(line, methodS.length()));
      line++;
      TextEdit textEdit2 = new TextEdit(range2,methodS);
      edits.add(textEdit2);

      String end = "";
      if(!returnType.equals("void")){
        end = "  return r0;";
      }
      end += " }"+System.getProperty("line.separator");
      Range range3 = new Range();
      range3.setStart(new Position(line,0));
      range3.setEnd(new Position(line, end.length()));
      line++;
      TextEdit textEdit3 = new TextEdit(range3,end);
      edits.add(textEdit3);
      counter ++;
    }
    String finalend = "}"+System.getProperty("line.separator");
    Range range3 = new Range();
    range3.setStart(new Position(line,0));
    range3.setEnd(new Position(line, finalend.length()));
    TextEdit textEdit3 = new TextEdit(range3,finalend);
    edits.add(textEdit3);
    return edits;
  }

}
