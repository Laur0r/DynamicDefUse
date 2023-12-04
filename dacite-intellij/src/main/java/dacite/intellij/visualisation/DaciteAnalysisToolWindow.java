package dacite.intellij.visualisation;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.INIT;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import dacite.intellij.lspclient.DaciteLSPRequestManager;
import dacite.lsp.InlayHintDecoration;
import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.tvp.TreeViewChildrenParams;
import dacite.lsp.tvp.TreeViewChildrenResult;
import dacite.lsp.tvp.TreeViewCommand;
import dacite.lsp.tvp.TreeViewNode;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.utils.FileUtils;

public class DaciteAnalysisToolWindow {
    private JPanel myToolWindowContent;
    private Project project;
    private DaciteLSPRequestManager requestManager;

    private Tree tree;
    private ToolWindow toolWindow;

    private AnAction symbAction;

    public DaciteAnalysisToolWindow(ToolWindow toolWindow, Project project, RequestManager requestManager) {
        this.project = project;
        this.requestManager = (DaciteLSPRequestManager) requestManager;
        this.toolWindow = toolWindow;
        myToolWindowContent = new JPanel();
        myToolWindowContent.setLayout(new BoxLayout(myToolWindowContent, BoxLayout.Y_AXIS));
        tree = createTree("defUseChains");
        JScrollPane treeView = new JBScrollPane(tree);
        String borderTitle = "Covered DUCs";
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border etchedTitledBorder = BorderFactory.createTitledBorder(etchedBorder, borderTitle);
        treeView.setBorder(etchedTitledBorder);
        myToolWindowContent.add(treeView, BorderLayout.CENTER);
        JButton button = new JButton("Run Symbolic Analysis");
        symbAction = new AnAction("Run Dacite Symbolic Analysis", "Symbolic analysis of def-use chains", AllIcons.Actions.StartMemoryProfile) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) tree.getModel().getRoot()).getChildAt(0);
                String uri = ((TreeViewNode) node.getUserObject()).getContextValue();
                Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
                RequestManager requestManager = null;
                if (wrapper.size() == 1) {
                    requestManager = wrapper.iterator().next().getRequestManager();
                }
                CompletableFuture<Object> result = requestManager.executeCommand(new ExecuteCommandParams("dacite.symbolicDriver",List.of(uri)));
            }
        };

        toolWindow.setTitleActions(List.of(symbAction));

    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    public void updateChildrenNodes(DefaultMutableTreeNode node){
        TreeViewNode iniView = (TreeViewNode) node.getUserObject();
        if(node.getChildCount() != 0){
            Enumeration<TreeNode> nodes = node.children();
            while(nodes.hasMoreElements()){
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) nodes.nextElement();
                TreeViewNode treeView = (TreeViewNode) child.getUserObject();
                treeView.setEditorHighlight(iniView.isEditorHighlight());
                if(child.getChildCount() != 0){
                    updateChildrenNodes(child);
                }
            }
        }
        myToolWindowContent.repaint();
    }

    private void
    createTreeViewChildren(DefaultMutableTreeNode top){
        TreeViewNode parent = (TreeViewNode) top.getUserObject();
        TreeViewChildrenParams params = new TreeViewChildrenParams(parent.getViewId(), parent.getNodeUri());
        CompletableFuture<TreeViewChildrenResult> request = requestManager.treeViewChildren(params);
        if(request != null){
            try{
                TreeViewChildrenResult result = request.get(100000, TimeUnit.MILLISECONDS);
                for(TreeViewNode child : result.getNodes()){
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(child);
                    top.add(node);
                    createTreeViewChildren(node);
                }
            } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void addInlays(){
        FileEditorManager manager = FileEditorManager.getInstance(project);
        Editor textEditor = manager.getSelectedTextEditor();
        Font font = textEditor.getColorsScheme().getFont(EditorFontType.PLAIN);
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors();
        for(FileEditor ed:fileEditors){
            PsiAwareTextEditorImpl impl = (PsiAwareTextEditorImpl) ed;
            Editor eachEditor = impl.getEditor();
            // get InlayHints from Server
            Range range = new Range(new Position(0,0), new Position(eachEditor.getDocument().getLineCount()-1,0));
            InlayHintParams param = new InlayHintParams(new TextDocumentIdentifier(ed.getFile().getUrl()), range);
            CompletableFuture<List<InlayHint>> request =  requestManager.inlayHint(param);
            int lastLine = eachEditor.getDocument().getLineCount()-1;
            LogicalPosition pos = new LogicalPosition(lastLine, eachEditor.getDocument().getLineEndOffset(lastLine));
            eachEditor.getInlayModel().getInlineElementsInRange(0,eachEditor.logicalPositionToOffset(pos)).forEach(inlay -> {inlay.dispose();});
            if (request != null) {
                try {
                    List<InlayHint> res = request.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
                    if (res != null && res.size() > 0) {
                        for(InlayHint hint: res){
                            InlayHintDecorationParams decParams = new InlayHintDecorationParams(new TextDocumentIdentifier(ed.getFile().getUrl()), hint.getPosition());
                            CompletableFuture<InlayHintDecoration> decorationRequest = ((DaciteLSPRequestManager) requestManager).inlayHintDecoration(decParams);
                            Color color = JBColor.BLUE;
                            if (decorationRequest != null) {
                                try {
                                    InlayHintDecoration decoration = decorationRequest.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
                                    font = new Font(decoration.getFontStyle(), Font.PLAIN, font.getSize());
                                    String colors = decoration.getColor();
                                    if(!colors.contains(",")){
                                        color = Color.decode(colors);
                                    } else{
                                        String[] list = colors.split(",");
                                        list[0] = list[0].substring(list[0].indexOf("(")+1);
                                        list[list.length-1] = list[list.length-1].substring(0,list[list.length-1].length()-1);
                                        color = new Color(Integer.parseInt(list[0].trim()), Integer.parseInt(list[1].trim()), Integer.parseInt(list[2].trim()));
                                    }
                                } catch (TimeoutException | InterruptedException | JsonRpcException |
                                         ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                            LogicalPosition lpos = new LogicalPosition(hint.getPosition().getLine(), hint.getPosition().getCharacter());
                            int offset = eachEditor.logicalPositionToOffset(lpos);
                            Either<String, List<InlayHintLabelPart>> label = hint.getLabel();
                            String hintWord = "";
                            if(label.getLeft() != null){
                                hintWord = label.getLeft();
                            }
                            String finalHintWord = hintWord;
                            Font finalFont = font;
                            Color finalColor = color;
                            eachEditor.getInlayModel().getInlineElementsInRange(offset,offset+3).forEach(inlay -> {inlay.dispose();});
                            eachEditor.getInlayModel().addInlineElement(offset, new EditorCustomElementRenderer() {
                                @Override
                                public int calcWidthInPixels(@NotNull Inlay inlay) {
                                    AffineTransform a = finalFont.getTransform();
                                    FontRenderContext frc = new FontRenderContext(a,true,true);
                                    return (int) finalFont.getStringBounds(finalHintWord,frc).getWidth()+3;
                                }

                                @Override
                                public int calcHeightInPixels(@NotNull Inlay inlay) {
                                    AffineTransform a = finalFont.getTransform();
                                    FontRenderContext frc = new FontRenderContext(a,true,true);
                                    return (int) finalFont.getStringBounds(finalHintWord,frc).getHeight();
                                }

                                @Override
                                public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle targetRegion, @NotNull TextAttributes textAttributes) {
                                    Graphics2D gr = (Graphics2D) g;
                                    gr.setColor(new Color(238,238,238));
                                    gr.fillRect(targetRegion.x-3, targetRegion.y+1, calcWidthInPixels(inlay)+3, calcHeightInPixels(inlay)+3);
                                    gr.setFont(finalFont);
                                    gr.setColor(finalColor);
                                    gr.drawString(finalHintWord, targetRegion.x, targetRegion.y+ eachEditor.getAscent());
                                }
                            });
                        }
                    }
                } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public Tree createTree(String rootName){
        TreeViewNode root = new TreeViewNode(rootName, "", "root");
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode(root);
        createTreeViewChildren(top);
        Tree tree = new Tree(top);
        tree.setCellRenderer(new TreeViewCellRenderer());
        TreeViewCellEditor editor = new TreeViewCellEditor();
        editor.addChangeListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent changeEvent) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) changeEvent.getSource();
                updateChildrenNodes(node);

                TreeViewNode iniView = (TreeViewNode) node.getUserObject();
                TreeViewCommand command = iniView.getCommand();
                if (command != null) {
                    // Tell server to make corresponding inlay hints visible
                    var args = new ArrayList<>(command.getArguments());
                    args.add(iniView.isEditorHighlight());
                    requestManager.executeCommand(new ExecuteCommandParams(
                            command.getCommand(),
                            args
                    ));

                    // Retrieve new inlay hints from server
                    addInlays();
                }
            }

            @Override
            public void editingCanceled(ChangeEvent changeEvent) {

            }
        });
        tree.setCellEditor(editor);
        tree.setRootVisible(false);
        tree.setEditable(true);
        return tree;
    }

    public JPanel addNotCoveredView(){
        Tree tree2 = createTree("notCoveredDUC");
        JScrollPane treeView2 = new JBScrollPane(tree2);
        String borderTitle = "Not covered DUCs";
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border etchedTitledBorder = BorderFactory.createTitledBorder(etchedBorder, borderTitle);
        treeView2.setBorder(etchedTitledBorder);
        myToolWindowContent.add(treeView2, BorderLayout.PAGE_START);
        myToolWindowContent.setComponentZOrder(treeView2, 0);

        AnAction action = new AnAction("Generate JUnit Tests for Non-Covered DUCs", "Symbolic analysis of def-use chains", AllIcons.Toolwindows.ToolWindowCoverage) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) tree.getModel().getRoot()).getChildAt(0);
                String uri = ((TreeViewNode) node.getUserObject()).getContextValue();
                Set<LanguageServerWrapper> wrapper = IntellijLanguageClient.getAllServerWrappersFor(FileUtils.projectToUri(project));
                RequestManager requestManager = null;
                if (wrapper.size() == 1) {
                    requestManager = wrapper.iterator().next().getRequestManager();
                }
                CompletableFuture<Object> result = requestManager.executeCommand(new ExecuteCommandParams("dacite.generateTestCases",List.of(uri)));
            }
        };

        toolWindow.setTitleActions(List.of(action, symbAction));
        return myToolWindowContent;
    }
}
