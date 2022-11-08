package dacite.lsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.services.LanguageServer;

import dacite.lsp.tvp.DaciteTreeViewService;

public interface DaciteExtendedLanguageServer extends LanguageServer {

  @JsonDelegate
  DaciteExtendedTextDocumentService getDaciteExtendedTextDocumentService();

  @JsonDelegate
  DaciteTreeViewService getDaciteTreeViewService();

}
