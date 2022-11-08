package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class TreeViewParentParams {

  /**
   * The ID of the view that the nodeUri is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * The URI of the child node.
   */
  @NonNull
  private String nodeUri;

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

}
