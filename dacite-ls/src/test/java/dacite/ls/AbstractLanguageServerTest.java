package dacite.ls;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Files;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class AbstractLanguageServerTest {

  protected DaciteLanguageServer languageServer;

  @AfterEach
  public void tearDown() {
    if (this.languageServer != null) {
      //this.languageServer.stopServer();
    }
  }

  protected InitializeResult initializeLanguageServer()
      throws ExecutionException, InterruptedException, URISyntaxException {
    this.languageServer = new DaciteLanguageServer();
    this.languageServer.connect(new TestLanguageClient());
    //this.languageServer.startServer();

    // Server's request
    var wsFile = Paths.get(AbstractLanguageServerTest.class.getResource("/workspace/").toURI()).toFile();
    var wsFolder = new WorkspaceFolder(wsFile.toURI().toString());

    var params = new InitializeParams();
    params.setProcessId(new Random().nextInt());
    params.setWorkspaceFolders(List.of(wsFolder));
    params.setInitializationOptions(Collections.emptyMap());

    var initializeResult = languageServer.initialize(params);
    assertThat(initializeResult).isCompleted();

    // Client's response
    languageServer.initialized(new InitializedParams());

    return initializeResult.get();
  }

  protected DaciteLanguageServer initializeLanguageServer(String fileName, String text)
      throws URISyntaxException, InterruptedException, ExecutionException {
    return initializeLanguageServer(new TextDocumentItem(fileName, DaciteLanguageServer.LANGUAGE_ID, 0, text));
  }

  protected DaciteLanguageServer initializeLanguageServer(File file)
      throws URISyntaxException, InterruptedException, ExecutionException, IOException {
    return initializeLanguageServer(new TextDocumentItem(file.toURI().toString(), DaciteLanguageServer.LANGUAGE_ID, 0,
        new String(Files.toByteArray(file))));
  }

  protected DaciteLanguageServer initializeLanguageServer(TextDocumentItem... textDocumentItems)
      throws URISyntaxException, InterruptedException, ExecutionException {
    // Initialize LS
    initializeLanguageServer();

    // Open document items
    for (var item : textDocumentItems) {
      languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(item));
    }

    return languageServer;
  }

  private final class TestLanguageClient implements LanguageClient {

    @Override
    public void telemetryEvent(Object object) {

    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {

    }

    @Override
    public void showMessage(MessageParams messageParams) {

    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
      return null;
    }

    @Override
    public void logMessage(MessageParams message) {

    }

  }

}
