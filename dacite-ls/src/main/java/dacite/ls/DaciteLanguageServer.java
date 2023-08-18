package dacite.ls;

import com.google.gson.JsonObject;

import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import dacite.lsp.DaciteExtendedLanguageServer;
import dacite.lsp.DaciteExtendedTextDocumentService;
import dacite.lsp.tvp.DaciteTreeViewService;

public class DaciteLanguageServer implements DaciteExtendedLanguageServer, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(DaciteLanguageServer.class);

  public static final String LANGUAGE_ID = "java";

  private final DaciteTextDocumentService textDocumentService;
  private final DaciteWorkspaceService workspaceService;

  private LanguageClient client;

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
    capabilities.setCodeLensProvider(new CodeLensOptions(false));
    capabilities.setInlayHintProvider(true);

    // Tree View Protocol Extension
    var experimental = new JsonObject();
    experimental.addProperty("treeViewProvider", true);
    capabilities.setExperimental(experimental);

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
  public TextDocumentService getTextDocumentService() {
    return this.textDocumentService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return this.workspaceService;
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
    this.workspaceService.setClient(client);
  }

  @Override
  public DaciteExtendedTextDocumentService getDaciteExtendedTextDocumentService() {
    return this.textDocumentService;
  }

  @Override
  public DaciteTreeViewService getDaciteTreeViewService() {
    return this.textDocumentService;
  }

}
