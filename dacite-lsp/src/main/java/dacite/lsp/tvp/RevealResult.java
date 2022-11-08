package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class RevealResult {

  /**
   * The ID of the view that this node is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * The list of URIs for the node to reveal and all of its ancestor parents.
   * The node to reveal is at index 0, it's parent is at index 1 and so forth
   * up until the root node.
   */
  @NonNull
  private String[] uriChain;

}
