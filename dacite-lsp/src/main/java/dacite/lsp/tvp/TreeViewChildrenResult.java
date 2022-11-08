package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class TreeViewChildrenResult {

  /**
   * The child nodes of the requested parent node.
   */
  @NonNull
  private TreeViewNode[] nodes;

  public TreeViewChildrenResult(TreeViewNode[] nodes) {
    this.nodes = nodes;
  }

  public TreeViewNode[] getNodes() {
    return nodes;
  }

  public void setNodes(TreeViewNode[] nodes) {
    this.nodes = nodes;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("nodes", this.nodes);
    return b.toString();
  }

}
