package dacite.ls;

import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

import dacite.ls.feature.CommandRegistry;

public class LanguageServerImpl implements LanguageServer, LanguageClientAware {

  private TextDocumentService textDocumentService;
  private WorkspaceService workspaceService;

  private int errorCode = 1;

  public LanguageServerImpl() {
    this.textDocumentService = new TextDocumentServiceImpl();
    this.workspaceService = new WorkspaceServiceImpl();
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
    final InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());
    ServerCapabilities capabilities = initializeResult.getCapabilities();

    capabilities.setExecuteCommandProvider(new ExecuteCommandOptions(CommandRegistry.getCommands()));
    // TODO: Add Code Actions, Hover, Highlights, Document Symbol, Signature Help?

    capabilities.setTextDocumentSync(TextDocumentSyncKind.None);
    return CompletableFuture.supplyAsync(() -> initializeResult);
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
    Util.setClient(client);
  }

}
