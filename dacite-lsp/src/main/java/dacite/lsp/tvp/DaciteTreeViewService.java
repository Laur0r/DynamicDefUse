package dacite.lsp.tvp;

import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.concurrent.CompletableFuture;

@JsonSegment("dacite")
public interface DaciteTreeViewService {

  @JsonRequest
  CompletableFuture<TreeViewChildrenResult> treeViewChildren(TreeViewChildrenParams params);

  @JsonRequest
  CompletableFuture<TreeViewParentResult> treeViewParent(TreeViewParentParams params);

  @JsonNotification
  void treeViewDidChange(TreeViewDidChangeParams params);

  @JsonNotification
  default void treeViewVisibilityDidChange(TreeViewVisibilityDidChangeParams params) {
  }

  @JsonNotification
  default void treeViewNodeCollapseDidChange(TreeViewNodeCollapseDidChangeParams params) {
  }

  @JsonRequest
  default CompletableFuture<RevealResult> treeViewReveal(TextDocumentPositionParams params) {
    throw new UnsupportedOperationException();
  }
}
