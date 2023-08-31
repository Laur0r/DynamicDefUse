package dacite.ls;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dacite.lsp.DaciteExtendedLanguageClient;
import dacite.lsp.defUseData.transformation.XMLSolution;
import dacite.lsp.tvp.TreeViewDidChangeParams;
import dacite.lsp.tvp.TreeViewNode;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import de.wwu.mulib.tcg.testsetreducer.CombinedTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleBackwardsTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleForwardsTestSetReducer;
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
                  "-javaagent:" + javaAgentJar + "=" +projectDir + "src/;"+ packageName.replace(".", "/"),
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

              DefUseAnalysisProvider.processXmlFile(projectDir + "coveredDUCs.xml");
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

            File projectFile = getProjectClassDir(textDocumentUri);
            DefUseAnalysisProvider.deriveNotCoveredChains(projectDir + "SymbolicDUCs.xml", projectFile);
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

            File projectFile = getProjectClassDir(textDocumentUri);
            String uri = textDocumentUri.substring(0,textDocumentUri.lastIndexOf("/"));
            uri += "/DaciteSymbolicDriverFor" + className + ".java";
            CreateFile createFile = new CreateFile(uri, new CreateFileOptions(true,false));
            List<TextEdit> edits = DaciteTextGenerator.generateSearchRegions(projectFile, packageName+"."+className);

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

          String className = analyser.extractClassName();
          String packageName = analyser.extractPackageName();
          File projectFile = getProjectClassDir(textDocumentUri);
          String uri = textDocumentUri.substring(0,textDocumentUri.lastIndexOf("/"));

          List<Either<TextDocumentEdit, ResourceOperation>> changes = DaciteTextGenerator.generateTestCases(projectFile, packageName, className, uri);
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
              logger.info("highlight"+ nodeProperties);
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
  private static File getProjectClassDir(String textDocumentUri){
    // Extract project's source directory
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
    return projectFile;
  }

}
