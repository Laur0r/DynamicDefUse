package dacite.intellij;

import com.intellij.openapi.diagnostic.Logger;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class LanguageClientImpl implements LanguageClient {

  @NotNull
  final private Logger LOG = Logger.getInstance(LanguageClientImpl.class);

  @Override
  public void telemetryEvent(Object object) {
    LOG.info(object.toString());
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
  public void logMessage(MessageParams messageParams) {
    String message = messageParams.getMessage();
    MessageType msgType = messageParams.getType();

    switch (msgType) {
      case Error:
        LOG.error(message);
        break;
      case Warning:
        LOG.warn(message);
        break;
      case Info:
      case Log:
        LOG.info(message);
        break;
      default:
        LOG.warn("Unknown message with type '" + msgType + "': " + message);
        break;
    }
  }

}
