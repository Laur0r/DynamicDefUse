package dacite.lsp.tvp;

import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.concurrent.CompletableFuture;

@JsonSegment("dacite")
public interface DaciteTreeViewService {

  CompletableFuture<TreeViewChildrenResult> treeViewChildren(TreeViewChildrenParams params);

  CompletableFuture<TreeViewParentResult> treeViewParent(TreeViewParentParams params);

  default void treeViewVisibilityDidChange(TreeViewVisibilityDidChangeParams params) {
    throw new UnsupportedOperationException();
  }

  default void treeViewNodeCollapseDidChange(TreeViewNodeCollapseDidChangeParams params) {
    throw new UnsupportedOperationException();
  }

  default void treeViewReveal(TextDocumentPositionParams params) {
    throw new UnsupportedOperationException();
  }

}
