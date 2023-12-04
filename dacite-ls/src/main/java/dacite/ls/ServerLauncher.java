package dacite.ls;

import dacite.lsp.DaciteExtendedLanguageClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ServerLauncher {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    // Create server launcher
    DaciteLanguageServer server = new DaciteLanguageServer();
    Launcher<DaciteExtendedLanguageClient> launcher = createServerLauncher(server, System.in, System.out);

    // Connect to client
    DaciteExtendedLanguageClient client = launcher.getRemoteProxy();
    server.connect(client);

    // Start listening
    Future<?> startListening = launcher.startListening();
    startListening.get();
  }

  /**
   * Create a new Launcher for a language server and an input and output stream.
   *
   * @param server - the server that receives method calls from the remote client
   * @param in - input stream to listen for incoming messages
   * @param out - output stream to send outgoing messages
   */
  public static Launcher<DaciteExtendedLanguageClient> createServerLauncher(LanguageServer server, InputStream in, OutputStream out) {
    return new LSPLauncher.Builder<DaciteExtendedLanguageClient>()
            .setLocalService(server)
            .setRemoteInterface(DaciteExtendedLanguageClient.class)
            .setInput(in)
            .setOutput(out)
            .create();
  }

}
