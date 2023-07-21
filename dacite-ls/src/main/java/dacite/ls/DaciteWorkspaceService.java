package dacite.ls;

import dacite.lsp.DaciteExtendedLanguageClient;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

public class DaciteWorkspaceService implements WorkspaceService {

  private DaciteExtendedLanguageClient client;

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
  }

  @Override
  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    return CommandRegistry.execute(params, client);
  }

  public void setClient(LanguageClient client){ this.client = (DaciteExtendedLanguageClient) client;}

}
