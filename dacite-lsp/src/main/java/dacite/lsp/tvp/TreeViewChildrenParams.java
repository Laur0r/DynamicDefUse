package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class TreeViewChildrenParams {

  /**
   * The ID of the view that this node is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * The URI of the parent node or undefined when listing the root node.
   */
  private String nodeUri;

  public TreeViewChildrenParams(String viewId, String nodeUri){
    this.viewId = viewId;
    this.nodeUri = nodeUri;
  }

  public String getViewId() {
    return viewId;
  }

  public void setViewId(String viewId) {
    this.viewId = viewId;
  }

  public String getNodeUri() {
    return nodeUri;
  }

  public void setNodeUri(String nodeUri) {
    this.nodeUri = nodeUri;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("viewId", this.viewId);
    b.add("nodeUri", this.nodeUri);
    return b.toString();
  }

  @Override
  @Pure
  @SuppressWarnings("UnstableApiUsage")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + ((this.nodeUri == null) ? 0 : this.nodeUri.hashCode());
  }

}
