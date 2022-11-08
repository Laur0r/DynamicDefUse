package dacite.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NotNull;

import dacite.intellij.DaciteAnalysisLauncher;

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
    String filename = "";
    String packagename = "";
    if (file instanceof PsiJavaFile) {
      PsiJavaFile jfile = (PsiJavaFile) file;
      packagename = jfile.getPackageName();
      filename = packagename + "." + jfile.getName();
      if (packagename.contains(".")) {
        packagename = packagename.replace(".", "/");
      }
      filename = filename.substring(0, filename.lastIndexOf("."));
    }
    DaciteAnalysisLauncher.launch(project, packagename, filename);

    // Using the event, implement an action. For example, create and show a dialog.
  }

}
