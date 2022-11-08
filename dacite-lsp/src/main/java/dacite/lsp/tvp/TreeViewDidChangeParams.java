package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

public class TreeViewDidChangeParams {

  /**
   * The nodes that have changed.
   */
  @NonNull
  private TreeViewNode[] nodes;

}
