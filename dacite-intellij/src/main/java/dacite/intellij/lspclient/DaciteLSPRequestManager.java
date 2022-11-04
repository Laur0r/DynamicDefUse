package dacite.intellij.lspclient;

import dacite.intellij.defUseData.DefUseClass;
import dacite.intellij.defUseData.transformation.DefUseChains;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DaciteLSPRequestManager extends DefaultRequestManager {
    public DaciteLSPRequestManager(LanguageServerWrapper wrapper, LanguageServer server, LanguageClient client, ServerCapabilities serverCapabilities) {
        super(wrapper, server, client, serverCapabilities);
    }

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        return super.inlayHint(params);
    }

    public CompletableFuture<List<DefUseClass>> daciteAnalysis(DefUseChains chains){
        return null; //TODO return server.daciteAnalysis
    }
}
