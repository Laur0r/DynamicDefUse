package dacite.lsp;

import dacite.lsp.tvp.DaciteTreeViewService;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.InputStream;
import java.io.OutputStream;

public class DaciteLauncher {

    private DaciteLauncher() {}

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