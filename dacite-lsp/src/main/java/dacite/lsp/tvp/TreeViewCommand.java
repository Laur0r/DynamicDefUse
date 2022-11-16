package dacite.lsp.tvp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.util.List;

public class TreeViewCommand {

  /**
   * The title of the command, the client is free to not display this title in the UI.
   */
  @NonNull
  private String title;

  /**
   * The identifier of the command that should be executed by the client.
   */
  @NonNull
  private String command;

  /**
   * A description of what this command does.
   */
  private String tooltip;

  /**
   * Optional arguments to invoke the command with.
   */
  private List<Object> arguments;

  public TreeViewCommand(String title, String command, List<Object> arguments) {
    this.title = title;
    this.command = command;
    this.arguments = arguments;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public List<Object> getArguments() {
    return arguments;
  }

  public void setArguments(List<Object> arguments) {
    this.arguments = arguments;
  }
}
