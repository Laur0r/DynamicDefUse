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

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Set;

import dacite.intellij.visualisation.DaciteToolWindowFactory;

public class DaciteAnalyzeAction extends AnAction {

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
    requestManager.executeCommand(new ExecuteCommandParams("dacite.analyze", List.of(FileUtils.VFSToURI(file.getVirtualFile()))));

    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow("DaciteAnalysisToolWindow");
    DaciteToolWindowFactory factory = new DaciteToolWindowFactory();

    // One time registration of the tool window (does not add any content).
    if (toolWindow == null) {
      System.out.println("tool window not registered yet");
      RegisterToolWindowTask task = new RegisterToolWindowTask("DaciteAnalysisToolWindow", ToolWindowAnchor.RIGHT, null, false,true,true,true,factory, AllIcons.General.Modified,null );// null, null, null);
      toolWindow = toolWindowManager.registerToolWindow(task);
      toolWindow.show();
    } else {
      factory.changeToolWindowContent(project,toolWindow);
    }

    // Using the event, implement an action. For example, create and show a dialog.
  }

}
