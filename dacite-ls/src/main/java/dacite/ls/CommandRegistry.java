package dacite.ls;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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
    switch (Command.valueOf(params.getCommand().replaceFirst(COMMAND_PREFIX, ""))) {
      case analyze:
        ProcessHandle.Info currentProcessInfo = ProcessHandle.current().info();
        List<String> newProcessCommandLine = new ArrayList<>();
        newProcessCommandLine.add(currentProcessInfo.command().get());

        newProcessCommandLine.add("-javaagent:lib/dacite-intellij-1.0-SNAPSHOT.jar=tryme/");
        newProcessCommandLine.add("-classpath");
        newProcessCommandLine.add(ManagementFactory.getRuntimeMXBean().getClassPath());
        newProcessCommandLine.add("DefUseMain");
        newProcessCommandLine.add("tryme.SatGcd");

        ProcessBuilder newProcessBuilder = new ProcessBuilder(newProcessCommandLine).redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT);
        try {
          Process newProcess = newProcessBuilder.start();
          logger.info("{}: process {} started", "executed command", newProcessBuilder.command());
          logger.info("process exited with status {}", newProcess.waitFor());
        } catch (Exception e) {
          logger.error(e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
      default:
        throw new RuntimeException("Not implemented");
    }
  }

}
