package dacite.intellij.lspclient;

import com.intellij.openapi.diagnostic.Logger;

import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.tvp.*;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import dacite.lsp.InlayHintDecoration;
import dacite.lsp.DaciteExtendedLanguageServer;
import dacite.lsp.DaciteExtendedTextDocumentService;

public class DaciteLSPRequestManager extends DefaultRequestManager
    implements DaciteExtendedTextDocumentService, DaciteTreeViewService {

  private final Logger LOG = Logger.getInstance(DaciteLSPRequestManager.class);

  private final LanguageServerWrapper languageServerWrapper;
  private final DaciteExtendedTextDocumentService extendedTextDocumentService;
  private final DaciteTreeViewService treeViewService;

  public DaciteLSPRequestManager(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client,
      ServerCapabilities serverCapabilities) {
    super(wrapper, server, client, serverCapabilities);
    this.languageServerWrapper = wrapper;
    DaciteExtendedLanguageServer extendedServer = (DaciteExtendedLanguageServer) server;
    this.extendedTextDocumentService = extendedServer.getDaciteExtendedTextDocumentService();
    this.treeViewService = extendedServer.getDaciteTreeViewService();
  }

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        if (checkStatus()) {
            try {
                return getTextDocumentService().inlayHint(params);
            } catch (Exception e) {
                getWrapper().crashed(e);
                return null;
            }
        }
        return null;
    }

  @Override
  public CompletableFuture<InlayHintDecoration> inlayHintDecoration(InlayHintDecorationParams params) {
    if (checkStatus()) {
      try {
        return extendedTextDocumentService.inlayHintDecoration(params);
      } catch (Exception e) {
        crashed(e);
        return null;
      }
    }
    return null;
  }

  @Override
  public CompletableFuture<TreeViewChildrenResult> treeViewChildren(TreeViewChildrenParams params) {
    if (checkStatus()) {
      try {
        return treeViewService.treeViewChildren(params);
      } catch (Exception e) {
        crashed(e);
        return null;
      }
    }
    return null;
  }

  @Override
  public CompletableFuture<TreeViewParentResult> treeViewParent(TreeViewParentParams params) {
    if (checkStatus()) {
      try {
        return treeViewService.treeViewParent(params);
      } catch (Exception e) {
        crashed(e);
        return null;
      }
    }
    return null;
  }

  private void crashed(Exception e) {
    LOG.warn(e);
    languageServerWrapper.crashed(e);
  }

  @Override
  public void treeViewDidChange(TreeViewDidChangeParams params) {
    System.out.println("notification received!");
    treeViewService.treeViewDidChange(params);
  }

}
