package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class TreeViewNode {

  /**
   * The ID of the view that this node is associated with.
   */
  @NonNull
  private String viewId;

  /**
   * The URI of this node, or undefined if node is a root of the tree.
   */
  private String nodeUri;

  /**
   * The title to display for this node.
   */
  private String label;

  /**
   * An optional command to trigger when the user clicks on node.
   */
  private TreeViewCommand command;

  /**
   * An optional SVG icon to display next to the label of this node.
   */
  private String icon;

  /**
   * An optional description of this node that is displayed when the user hovers over this node.
   */
  private String tooltip;

  /**
   * Whether this tree node should be collapsed, expanded or if it has no children.
   * <p>
   * Can be one of "collapsed", "expanded" or "none".
   */
  private String collapseState;

  public TreeViewNode(String viewId, String nodeUri, String label) {
    this.viewId = viewId;
    this.nodeUri = nodeUri;
    this.label = label;
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

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public TreeViewCommand getCommand() {
    return command;
  }

  public void setCommand(TreeViewCommand command) {
    this.command = command;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public String getCollapseState() {
    return collapseState;
  }

  public void setCollapseState(String collapseState) {
    this.collapseState = collapseState;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("viewId", this.viewId);
    b.add("nodeUri", this.nodeUri);
    b.add("label", this.label);
    b.add("command", this.command);
    b.add("icon", this.icon);
    b.add("tooltip", this.tooltip);
    b.add("collapseState", this.collapseState);
    return b.toString();
  }

}
