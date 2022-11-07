package dacite.ls;

import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import dacite.ls.feature.CommandRegistry;

public class DaciteLanguageServer implements LanguageServer, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(DaciteLanguageServer.class);

  public static final String LANGUAGE_ID = "java";

  private final org.eclipse.lsp4j.services.TextDocumentService textDocumentService;
  private final org.eclipse.lsp4j.services.WorkspaceService workspaceService;

  private int errorCode = 1;

  public DaciteLanguageServer() {
    this.textDocumentService = new DaciteTextDocumentService();
    this.workspaceService = new DaciteWorkspaceService();
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
    logger.info("initialize: {}", initializeParams);

    var capabilities = new ServerCapabilities();

    var syncOptions = new TextDocumentSyncOptions();
    syncOptions.setChange(TextDocumentSyncKind.Full);
    syncOptions.setOpenClose(true);
    capabilities.setTextDocumentSync(syncOptions);
    capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(CommandRegistry.getCommands()));
    capabilities.setCodeActionProvider(true);
    capabilities.setInlayHintProvider(true);

    return CompletableFuture.supplyAsync(() -> new InitializeResult(capabilities));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    this.errorCode = 0;
    return CompletableFuture.supplyAsync(() -> Boolean.TRUE);
  }

  @Override
  public void exit() {
    System.exit(this.errorCode);
  }

  @Override
  public org.eclipse.lsp4j.services.TextDocumentService getTextDocumentService() {
    return this.textDocumentService;
  }

  @Override
  public org.eclipse.lsp4j.services.WorkspaceService getWorkspaceService() {
    return this.workspaceService;
  }

  @Override
  public void connect(LanguageClient client) {
    Util.setClient(client);
  }

}
