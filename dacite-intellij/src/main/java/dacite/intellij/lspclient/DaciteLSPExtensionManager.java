package dacite.intellij.lspclient;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;

import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.listeners.EditorMouseListenerImpl;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;

import dacite.lsp.DaciteExtendedLanguageServer;

@SuppressWarnings("unchecked")
public class DaciteLSPExtensionManager implements LSPExtensionManager {

  @Override
  public <T extends DefaultRequestManager> T getExtendedRequestManagerFor(LanguageServerWrapper wrapper,
      LanguageServer server, LanguageClient client, ServerCapabilities serverCapabilities) {
    return (T) new DaciteLSPRequestManager(wrapper, server, client, serverCapabilities);
  }

  @Override
  public <T extends EditorEventManager> T getExtendedEditorEventManagerFor(Editor editor,
      DocumentListener documentListener, EditorMouseListenerImpl mouseListener,
      EditorMouseMotionListenerImpl mouseMotionListener, LSPCaretListenerImpl caretListener,
      RequestManager requestManager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
    return (T) new DaciteEditorEventManager(editor, documentListener, mouseListener, mouseMotionListener, caretListener,
        requestManager, serverOptions, wrapper);
  }

  @Override
  public Class<? extends LanguageServer> getExtendedServerInterface() {
    return DaciteExtendedLanguageServer.class;
  }

  @Override
  public LanguageClient getExtendedClientFor(ClientContext context) {
    return new DefaultLanguageClient(context);
  }

}
