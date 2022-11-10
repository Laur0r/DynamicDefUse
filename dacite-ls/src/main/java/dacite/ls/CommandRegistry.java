package dacite.ls;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.gson.JsonPrimitive;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandRegistry {

  private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

  public static final String COMMAND_PREFIX = "dacite.";

  enum Command {
    analyze;
  }

  public static List<String> getCommands() {
    return Arrays.stream(Command.values()).map(it -> COMMAND_PREFIX + it.name()).collect(Collectors.toList());
  }

  public static CompletableFuture<Object> execute(ExecuteCommandParams params) {
    var command = Command.valueOf(params.getCommand().replaceFirst(COMMAND_PREFIX, ""));

    if (command == Command.analyze) {
      try {
        String textDocumentUri =
            params.getArguments().size() > 0 ? ((JsonPrimitive) params.getArguments().get(0)).getAsString() : null;

        if (textDocumentUri != null && textDocumentUri.startsWith("file://")) {
          // Extract project's root directory
          Path textDocumentPath = Paths.get(textDocumentUri.replace("file://", ""));
          String projectDir = textDocumentPath.getParent().toString().split("src/")[0];

          // Extract package and class name
          String javaCode = TextDocumentItemProvider.get(textDocumentUri).getText();
          CompilationUnit compilationUnit = StaticJavaParser.parse(javaCode);
          String className = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get().getName().asString();
          String packageName = compilationUnit.getPackageDeclaration().get().getName().asString();

          // Construct class path
          // * dacite-core (and thus the agent) is a dependency of dacite-ls and is thus contained in the class path
          //   of the running language server process
          // * We also add all jars that can be found within the project's root directory
          // * We also add all folders which contain .class files
          // * We also add folders we can find that typically contain .class files
          String fullClassPath = System.getProperty("java.class.path");
          fullClassPath += Files.find(Paths.get(projectDir), 50,
                  (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar")).map(it -> ":"+it.toString())
              .reduce(String::concat).orElse("");
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
              "dacite.core.DefUseMain",
              // Class to be analyzed
              packageName + "." + className);

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

          AnalysisProvider.processXmlFile(projectDir + "file.xml");

          return CompletableFuture.completedFuture(null);
        }
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
    throw new RuntimeException("Not implemented");
  }

  private static String findClassPathFolders(String basePath) {
    var files = new File(basePath).listFiles(File::isDirectory);
    if (files != null) {
      return Arrays.stream(files).map(File::toString).collect(Collectors.toList()).stream().map(it -> ":" + it).reduce(String::concat).orElse("");
    }

    return "";
  }

}
