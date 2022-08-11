package dacite.ls;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ServerLauncher {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    // Create server launcher
    LanguageServerImpl server = new LanguageServerImpl();
    Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);

    // Connect to client
    LanguageClient client = launcher.getRemoteProxy();
    server.connect(client);

    // Start listening
    Future<?> startListening = launcher.startListening();
    startListening.get();
  }

}
