package dacite.ls;

import dacite.lsp.DaciteExtendedLanguageClient;
import dacite.lsp.DaciteLauncher;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ServerLauncher {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    // Create server launcher
    DaciteLanguageServer server = new DaciteLanguageServer();
    Launcher<DaciteExtendedLanguageClient> launcher = DaciteLauncher.createServerLauncher(server, System.in, System.out);

    // Connect to client
    DaciteExtendedLanguageClient client = launcher.getRemoteProxy();
    server.connect(client);

    // Start listening
    Future<?> startListening = launcher.startListening();
    startListening.get();
  }

}
