package dacite.intellij.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import dacite.intellij.visualisation.DaciteAnalysisToolWindow;
import dacite.intellij.visualisation.DaciteToolWindowFactory;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DaciteSymbolicAnalysisAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    // Using the event, evaluate the context, and enable or disable the action.
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    System.out.println("action performed");
    long start = System.currentTimeMillis();
    Project project = e.getProject();
    PsiFile file = e.getData(PlatformCoreDataKeys.PSI_FILE);

    Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
    RequestManager requestManager = null;
    if (wrapper.size() == 1) {
      requestManager = wrapper.iterator().next().getRequestManager();
    }
    CompletableFuture<Object> result = requestManager.executeCommand(new ExecuteCommandParams("dacite.analyzeSymbolic", List.of(file.getVirtualFile().getUrl())));

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow("DaciteAnalysisToolWindow");
    DaciteToolWindowFactory factory = new DaciteToolWindowFactory();

    // One time registration of the tool window (does not add any content).
    if (toolWindow != null) {
      factory.createToolWindowWithView(project,toolWindow);
    }
    long end = System.currentTimeMillis();
    System.out.println("Execution time in ms: "+(end-start));

  }

}
