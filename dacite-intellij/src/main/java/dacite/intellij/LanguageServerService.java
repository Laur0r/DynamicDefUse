package dacite.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Service
public final class LanguageServerService {

  @NotNull
  private final Logger LOG = Logger.getInstance(this.getClass());

  private final Project project;

  @Nullable
  private ProcessBuilder builder;

  @Nullable
  private Process process;

  private LanguageClient client;
  private LanguageServer server;
  private Future<?> startListeningFuture;

  private CompletableFuture<InitializeResult> initializeFuture;

  public LanguageServerService(Project project) {
    this.project = project;

    // Determine plugin's lib folder in which JARs reside
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("dacite.dacite-intellij"));
    String workingDir = Objects.requireNonNull(plugin).getPluginPath().toString() + File.separator + "lib";
    String classpath = workingDir + File.separator + "*";

    // Determine java properties
    String javaHome = System.getProperty("java.home");
    String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

    // Put together the command to execute
    List<String> command = new ArrayList<>();
    command.add(javaBin);
    command.add("-cp");
    command.add(classpath);
    command.add("dacite.ls.ServerLauncher");

    // Setup process builder
    this.builder = new ProcessBuilder(command);
    this.builder.directory(new File(workingDir));
    this.builder.redirectError(ProcessBuilder.Redirect.INHERIT);

    // Make sure to stop process in the end
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public void start() {
    LOG.info("Starting dacite language server from project '" + this.project.getName() + "'");

    if (this.process != null && this.process.isAlive()) {
      LOG.info("Dacite language server already started: " + process);
      return;
    }

    // Start language server process
    try {
      this.process = Objects.requireNonNull(this.builder).start();

      if (!this.process.isAlive()) {
        throw new IOException();
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to start dacite language server");
    }
    LOG.info("Dacite language server process started: " + process);

    // Create client launcher
    //this.client = new LanguageClientImpl();
    Launcher<LanguageServer> launcher = Launcher.createLauncher(this.client, LanguageServer.class,
        process.getInputStream(), process.getOutputStream());

    // Connect to server
    this.server = launcher.getRemoteProxy();
    this.startListeningFuture = launcher.startListening();

    // Initialize
    WorkspaceClientCapabilities workspaceClientCapabilities = new WorkspaceClientCapabilities();
    workspaceClientCapabilities.setExecuteCommand(new ExecuteCommandCapabilities());

    TextDocumentClientCapabilities textDocumentClientCapabilities = new TextDocumentClientCapabilities();
    textDocumentClientCapabilities.setSynchronization(new SynchronizationCapabilities(true, true, true));

    InitializeParams initParams = new InitializeParams();
    initParams.setWorkspaceFolders(List.of(new WorkspaceFolder(this.project.getBasePath())));
    initParams.setCapabilities(
        new ClientCapabilities(workspaceClientCapabilities, textDocumentClientCapabilities, null));
    this.initializeFuture = server.initialize(initParams);
  }

  public void executeCommand(String command) {
    ExecuteCommandParams params = new ExecuteCommandParams();
    params.setCommand(command);

    server.getWorkspaceService().executeCommand(params);
  }

  public void stop() {
    if (process != null) {
      process.destroy();
    }
  }

}
