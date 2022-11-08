package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class TreeViewNodeCollapseDidChangeParams {

  /**
   * The ID of the view that this node is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * The URI of the node that was collapsed or expanded.
   */
  @NonNull
  private String nodeUri;

  /**
   * True if the node is collapsed, false if the node was expanded.
   */
  @NonNull
  private boolean collapsed;

}
