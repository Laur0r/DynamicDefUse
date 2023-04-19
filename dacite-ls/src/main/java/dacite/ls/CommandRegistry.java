package dacite.ls;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dacite.lsp.DaciteExtendedLanguageClient;
import dacite.lsp.tvp.TreeViewDidChangeParams;
import dacite.lsp.tvp.TreeViewNode;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageClient;
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
    analyze,
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

            DefUseAnalysisProvider.processXmlFile(projectDir + "file.xml");
            DefUseAnalysisProvider.setTextDocumentUriTrigger(textDocumentUri);
            TreeViewNode node = new TreeViewNode("defUseChains","", "");
            TreeViewDidChangeParams paramsChange = new TreeViewDidChangeParams(new TreeViewNode[]{node});
            client.treeViewDidChange(paramsChange);
          }
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
      case highlight:
        try {
          if (args.size() > 0) {
            var nodeProperties = (JsonObject) args.get(0);
            Boolean newIsEditorHighlight = null;
            if (args.size() == 2) {
              newIsEditorHighlight = ((JsonPrimitive) args.get(1)).getAsBoolean();
            }
            DefUseAnalysisProvider.changeDefUseEditorHighlighting(nodeProperties, newIsEditorHighlight);
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

}
