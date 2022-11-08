package dacite.lsp.tvp;

import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class TreeViewParentResult {

  /**
   * The parent node URI or undefined when the parent is the root node.
   */
  private String uri;

  public TreeViewParentResult(String uri) {
    this.uri = uri;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("uri", this.uri);
    return b.toString();
  }

}
