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
import dacite.intellij.visualisation.DaciteToolWindowFactory;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.jetbrains.annotations.NotNull;
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

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

public class DaciteSymbolicAnalyzeAction extends AnAction {

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
    CompletableFuture<Object> result = requestManager.executeCommand(new ExecuteCommandParams("dacite.symbolicTrigger", List.of(file.getVirtualFile().getUrl())));
    /*if(result != null){
      try {
        //Object edit = result.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
        //System.out.println(edit.getClass().toString());
        //requestManager.applyEdit(new ApplyWorkspaceEditParams(edit));
      }
      catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException ex) {
        ex.printStackTrace();
      }
    }*/

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow("DaciteAnalysisToolWindow");
    DaciteToolWindowFactory factory = new DaciteToolWindowFactory();

    // One time registration of the tool window (does not add any content).
    /*if (toolWindow == null) {
      System.out.println("tool window not registered yet");
      RegisterToolWindowTask task = new RegisterToolWindowTask("DaciteAnalysisToolWindow", ToolWindowAnchor.RIGHT, null, false,true,true,true,factory, AllIcons.General.Modified,null );// null, null, null);
      toolWindow = toolWindowManager.registerToolWindow(task);
      toolWindow.show();
    } else {
      factory.createToolWindowContent(project,toolWindow);
    }*/

    // Using the event, implement an action. For example, create and show a dialog.
  }

}
