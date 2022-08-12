package dacite.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

public class LanguageServerStartupActivity implements StartupActivity {

  @NotNull
  final private Logger LOG = Logger.getInstance(LanguageServerService.class);

  @Override
  public void runActivity(@NotNull Project project) {
    LanguageServerService languageServerService = project.getService(LanguageServerService.class);

    (new Notification("dacite", "Dacite language server starting...", NotificationType.INFORMATION)).notify(project);
    languageServerService.start();
    (new Notification("dacite", "Dacite language server started", NotificationType.INFORMATION)).notify(project);
  }

}
