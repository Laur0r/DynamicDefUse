package dacite.intellij.lspclient;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.editor.EditorEventManagerBase;
import org.wso2.lsp4intellij.listeners.DocumentListenerImpl;
import org.wso2.lsp4intellij.listeners.EditorMouseListenerImpl;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.requests.WorkspaceEditHandler;
import org.wso2.lsp4intellij.utils.ApplicationUtils;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.wso2.lsp4intellij.utils.ApplicationUtils.invokeLater;
import static org.wso2.lsp4intellij.utils.ApplicationUtils.writeAction;
import static org.wso2.lsp4intellij.utils.DocumentUtils.toEither;

public class DaciteWorkspaceEditHandler extends WorkspaceEditHandler {

    private static Logger LOG = Logger.getInstance(WorkspaceEditHandler.class);

    public static boolean applyEdit(WorkspaceEdit edit, String name) {
        return applyEditComplete(edit, name, new ArrayList<>());
    }

    public static boolean applyEditComplete(WorkspaceEdit edit, String name, List<VirtualFile> toClose) {
        final String newName = (name != null) ? name : "LSP edits";
        if (edit != null) {
            Map<String, List<TextEdit>> changes = edit.getChanges();
            List<Either<TextDocumentEdit, ResourceOperation>> dChanges = edit.getDocumentChanges();
            boolean[] didApply = new boolean[]{true};

            Project[] curProject = new Project[]{null};
            List<VirtualFile> openedEditors = new ArrayList<>();

            //Get the runnable of edits for each editor to apply them all in one command
            List<Runnable> toApply = new ArrayList<>();
            if (dChanges != null) {
                dChanges.forEach(tEdit -> {
                    if (tEdit.isLeft()) {
                        TextDocumentEdit textEdit = tEdit.getLeft();
                        VersionedTextDocumentIdentifier doc = textEdit.getTextDocument();
                        int version = doc.getVersion() != null ? doc.getVersion() : Integer.MAX_VALUE;
                        String uri = FileUtils.sanitizeURI(doc.getUri());
                        EditorEventManager manager = EditorEventManagerBase.forUri(uri);
                        if (manager != null) {
                            curProject[0] = manager.editor.getProject();
                            toApply.add(manager.getEditsRunnable(version, toEither(textEdit.getEdits()), newName, true));
                        } else {
                            toApply.add(
                                    manageUnopenedEditor(textEdit.getEdits(), uri, version, openedEditors, curProject,
                                            newName));
                        }
                    } else if (tEdit.isRight()) {
                        ResourceOperation resourceOp = tEdit.getRight();
                        if(!(resourceOp instanceof CreateFile)){
                            return;
                        }
                        CreateFile create = (CreateFile) resourceOp;
                        Project[] projects = ProjectManager.getInstance().getOpenProjects();
                        //Infer the project from the uri
                        Project project = Stream.of(projects)
                                .map(p -> new ImmutablePair<>(FileUtils.VFSToURI(ProjectUtil.guessProjectDir(p)), p))
                                .filter(p -> create.getUri().startsWith(p.getLeft())).sorted(Collections.reverseOrder())
                                .map(ImmutablePair::getRight).findFirst().orElse(projects[0]);
                        String uri = FileUtils.sanitizeURI(create.getUri());
                        String fileName = VfsUtil.extractFileName(uri);
                        Application application = ApplicationManager.getApplication();
                        Runnable run = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaFileType.INSTANCE,"");
                                    String dir = VfsUtil.getParentDir(uri);
                                    VirtualFile fileDir = VfsUtil.findFileByURL(URI.create(dir).toURL());
                                    PsiDirectory psiDir = PsiManager.getInstance(project).findDirectory(fileDir);
                                    if(psiDir != null){
                                        PsiFile existing = psiDir.findFile(fileName);
                                        if(existing == null){
                                            psiDir.add(file);
                                        }
                                    }
                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                        invokeLater(() -> writeAction(() -> {
                            CommandProcessor.getInstance()
                                    .executeCommand(curProject[0], run, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT,
                                            false);
                        }));

                    } else {
                        LOG.warn("Null edit");
                    }
                });

            } else if (changes != null) {
                changes.forEach((key, lChanges) -> {
                    String uri = FileUtils.sanitizeURI(key);

                    EditorEventManager manager = EditorEventManagerBase.forUri(uri);
                    if (manager != null) {
                        curProject[0] = manager.editor.getProject();
                        toApply.add(manager.getEditsRunnable(Integer.MAX_VALUE, toEither(lChanges), newName, true));
                    } else {
                        toApply.add(manageUnopenedEditor(lChanges, uri, Integer.MAX_VALUE, openedEditors, curProject,
                                newName));
                    }
                });
            }
            if (toApply.contains(null)) {
                LOG.warn("Didn't apply, null runnable");
                didApply[0] = false;
            } else {
                Runnable runnable = () -> toApply.forEach(Runnable::run);
                invokeLater(() -> writeAction(() -> {
                    CommandProcessor.getInstance()
                            .executeCommand(curProject[0], runnable, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT,
                                    false);
                    openedEditors.forEach(f -> FileEditorManager.getInstance(curProject[0]).closeFile(f));
                    toClose.forEach(f -> FileEditorManager.getInstance(curProject[0]).closeFile(f));
                }));
            }
            return didApply[0];
        } else {
            return false;
        }
    }

    private static Runnable manageUnopenedEditor(List<TextEdit> edits, String uri, int version,
                                                 List<VirtualFile> openedEditors, Project[] curProject, String name) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        //Infer the project from the uri
        Project project = Stream.of(projects)
                .map(p -> new ImmutablePair<>(FileUtils.VFSToURI(ProjectUtil.guessProjectDir(p)), p))
                .filter(p -> uri.startsWith(p.getLeft())).sorted(Collections.reverseOrder())
                .map(ImmutablePair::getRight).findFirst().orElse(projects[0]);

        ApplicationUtils.invokeLater(()->writeAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = null;
                try {
                    file = LocalFileSystem.getInstance().findFileByIoFile(new File(new URI(FileUtils.sanitizeURI(uri))));
                } catch (URISyntaxException e) {
                    LOG.warn(e);
                }
                VirtualFile finalFile = file;
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                final Editor editor = fileEditorManager.openTextEditor(descriptor, false);
                openedEditors.add(finalFile);
                curProject[0] = editor.getProject();
                EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
                if (manager != null) {
                    Runnable runnable = manager.getEditsRunnable(version, toEither(edits), name, true);
                    ApplicationUtils.invokeLater(()->writeAction(()->CommandProcessor.getInstance().executeCommand(curProject[0], runnable, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT,
                            false)));
                } else {
                    IntellijLanguageClient.editorOpened(editor);
                    Runnable run = new Runnable() {
                        @Override
                        public void run() {
                            EditorEventManager manager = EditorEventManagerBase.forEditor(editor);
                            Runnable runnable = manager.getEditsRunnable(version, toEither(edits), name, true);
                            ApplicationUtils.invokeLater(()->writeAction(()->CommandProcessor.getInstance().executeCommand(curProject[0], runnable, name, "LSPPlugin", UndoConfirmationPolicy.DEFAULT,
                                    false)));
                        }
                    };
                    ApplicationManager.getApplication().invokeLater(run);
                }
            }
        }));         // .computableWriteAction(() -> fileEditorManager.openTextEditor(descriptor, false));

        return null;
    }
}
