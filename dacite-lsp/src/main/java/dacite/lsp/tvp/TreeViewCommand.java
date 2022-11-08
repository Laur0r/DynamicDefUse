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

}
