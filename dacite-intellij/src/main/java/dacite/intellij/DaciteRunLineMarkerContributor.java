package dacite.intellij;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DaciteRunLineMarkerContributor extends RunLineMarkerContributor {

    private List<? extends CodeLens> codeLens;
    private String textDocument;

    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        int offset = element.getTextOffset();
        PsiDocumentManager m = PsiDocumentManager.getInstance(element.getProject());
        Document doc = m.getDocument(element.getContainingFile());
        int line = doc.getLineNumber(offset);
        LogicalPosition elemPos = new LogicalPosition(line + 1, offset - doc.getLineStartOffset(line));
        Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(element.getProject()));
        RequestManager requestManager = null;
        if (wrapper.size() == 1) {
            requestManager = wrapper.iterator().next().getRequestManager();
        }
        if (textDocument == null || !textDocument.equals(element.getContainingFile().getVirtualFile().getUrl())) {
            CodeLensParams param = new CodeLensParams(new TextDocumentIdentifier(element.getContainingFile().getVirtualFile().getUrl()));
            CompletableFuture<List<? extends CodeLens>> request = requestManager.codeLens(param);
            if (request != null) {
                try {
                    List<? extends CodeLens> list = request.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
                    textDocument = element.getContainingFile().getVirtualFile().getUrl();
                    codeLens = list;
                    for (CodeLens lens : list) {
                        LogicalPosition position = new LogicalPosition(lens.getRange().getStart().getLine() + 1, lens.getRange().getStart().getCharacter());
                        //int lensOffset = editor.logicalPositionToOffset(position);
                        if (position.equals(elemPos)) {
                            return new Info(null, null,
                                    ActionManager.getInstance().getAction(lens.getCommand().getCommand()));
                        }
                    }

                } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return null;
        } else {
            for (CodeLens lens : codeLens) {
                LogicalPosition position = new LogicalPosition(lens.getRange().getStart().getLine() + 1, lens.getRange().getStart().getCharacter());
                if (position.equals(elemPos)) {
                    return new Info(null, null,
                            ActionManager.getInstance().getAction(lens.getCommand().getCommand()));
                }
            }
            return null;
        }
    }
}
