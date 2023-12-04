package dacite.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DaciteSymbolicDriverAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    // Using the event, evaluate the context, and enable or disable the action.
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    System.out.println("action performed");
    Project project = e.getProject();
    PsiFile file = e.getData(PlatformCoreDataKeys.PSI_FILE);

    Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
    RequestManager requestManager = null;
    if (wrapper.size() == 1) {
      requestManager = wrapper.iterator().next().getRequestManager();
    }
    CompletableFuture<Object> result = requestManager.executeCommand(new ExecuteCommandParams("dacite.symbolicDriver", List.of(FileUtils.VFSToURI(file.getVirtualFile()))));
  }

}
