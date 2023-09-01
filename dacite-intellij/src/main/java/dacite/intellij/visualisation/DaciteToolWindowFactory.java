package dacite.intellij.visualisation;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.eclipse.lsp4j.*;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import dacite.lsp.defUseData.DefUseClass;

public class DaciteToolWindowFactory implements ToolWindowFactory {


    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
        RequestManager requestManager = null;
        if(wrapper.size() == 1){
            requestManager = wrapper.iterator().next().getRequestManager();
        }
        DaciteAnalysisToolWindow daciteAnalysisToolWindow = new DaciteAnalysisToolWindow(toolWindow, project, requestManager);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(daciteAnalysisToolWindow.getContent(), "Dacite Analysis", false);
        toolWindow.getContentManager().addContent(content);
    }

    public void changeToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow){
        toolWindow.getContentManager().removeAllContents(true);
        createToolWindowContent(project, toolWindow);
        toolWindow.show();
        removeInlayhints(project);
    }

    public void createToolWindowWithView(@NotNull Project project, @NotNull ToolWindow toolWindow){
        toolWindow.getContentManager().removeAllContents(true);
        Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
        RequestManager requestManager = null;
        if(wrapper.size() == 1){
            requestManager = wrapper.iterator().next().getRequestManager();
        }
        DaciteAnalysisToolWindow daciteAnalysisToolWindow = new DaciteAnalysisToolWindow(toolWindow, project, requestManager);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(daciteAnalysisToolWindow.addNotCoveredView(), "Dacite Analysis", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.show();
        removeInlayhints(project);
    }

    protected void removeInlayhints(Project project){
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors();
        for(FileEditor ed:fileEditors) {
            PsiAwareTextEditorImpl impl = (PsiAwareTextEditorImpl) ed;
            Editor eachEditor = impl.getEditor();
            // remove InlayHints from whole document
            int lastLine = eachEditor.getDocument().getLineCount() - 1;
            LogicalPosition pos = new LogicalPosition(lastLine, eachEditor.getDocument().getLineEndOffset(lastLine));
            eachEditor.getInlayModel().getInlineElementsInRange(0, eachEditor.logicalPositionToOffset(pos)).forEach(Disposable::dispose);
        }
    }
}
