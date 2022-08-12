package dacite.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dacite.intellij.LanguageServerService;

public class AnalyzeAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    LanguageServerService languageServerService = project.getService(LanguageServerService.class);
    languageServerService.executeCommand("analyze");
  }

}
