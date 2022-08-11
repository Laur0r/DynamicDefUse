package dacite.ls;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

import dacite.ls.feature.CommandRegistry;

public class WorkspaceServiceImpl implements WorkspaceService {

  @Override
  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    return CommandRegistry.execute(params);
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {

  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

  }

}