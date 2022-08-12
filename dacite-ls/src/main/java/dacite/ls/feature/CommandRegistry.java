package dacite.ls.feature;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;

import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import dacite.ls.Util;

public class CommandRegistry {

  enum Command {
    analyze;
  }

  public static List<String> getCommands() {
    return Arrays.stream(Command.values()).map(Enum::name).collect(Collectors.toList());
  }

  public static CompletableFuture<Object> execute(ExecuteCommandParams params) {
    switch (Command.valueOf(params.getCommand())) {
      case analyze:
        Util.getClient().logMessage(new MessageParams(MessageType.Info, "Running... " + params));

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
          System.out.format("%s: process %s started%n", "executed command", newProcessBuilder.command());
          System.out.format("process exited with status %s%n", newProcess.waitFor());
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
      default:
        throw new RuntimeException("Not implemented");
    }
  }

}
