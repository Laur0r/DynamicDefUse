package dacite.intellij.visualisation;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import dacite.intellij.defUseData.DefUseClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DaciteToolWindowFactory implements ToolWindowFactory {

    private ArrayList<DefUseClass> data;

    public DaciteToolWindowFactory(ArrayList<DefUseClass> data){
        super();
        this.data = data;
    }
    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DaciteAnalysisToolWindow daciteAnalysisToolWindow = new DaciteAnalysisToolWindow(toolWindow, data, project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(daciteAnalysisToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);

    }
}
