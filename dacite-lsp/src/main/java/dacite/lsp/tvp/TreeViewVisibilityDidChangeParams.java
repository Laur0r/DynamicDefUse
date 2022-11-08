package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class TreeViewVisibilityDidChangeParams {

  /**
   * The ID of the view that this node is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * True if the node is visible in the editor UI, false otherwise.
   */
  @NonNull
  private boolean visible;

}
