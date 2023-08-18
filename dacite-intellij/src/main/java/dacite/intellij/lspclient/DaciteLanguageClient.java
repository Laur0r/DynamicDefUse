package dacite.intellij.lspclient;

import dacite.lsp.tvp.TreeViewDidChangeParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;

public class DaciteLanguageClient extends DefaultLanguageClient {

    public DaciteLanguageClient(@NotNull ClientContext context) {
        super(context);
    }

    @JsonNotification("dacite/treeViewDidChange")
    public void treeViewDidChange(TreeViewDidChangeParams params) {
        System.out.println("notification received in Client!");
    }
}
