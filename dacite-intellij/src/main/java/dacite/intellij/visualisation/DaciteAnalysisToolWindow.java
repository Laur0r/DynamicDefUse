package dacite.intellij.visualisation;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.thaiopensource.xml.dtd.om.Def;
import dacite.intellij.defUseData.DefUseClass;
import dacite.intellij.defUseData.DefUseData;
import dacite.intellij.defUseData.DefUseMethod;
import dacite.intellij.defUseData.DefUseVar;
import dacite.intellij.lspclient.DaciteLSPRequestManager;
import dacite.lsp.InlayHintDecoration;
import dacite.lsp.InlayHintDecorationParams;
import dacite.lsp.tvp.TreeViewChildrenParams;
import dacite.lsp.tvp.TreeViewChildrenResult;
import dacite.lsp.tvp.TreeViewNode;
import groovy.lang.Tuple;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.Color;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

public class DaciteAnalysisToolWindow {

    private JButton button;
    private JPanel myToolWindowContent;
    private DefUseTree tree;
    private Project project;
    private DaciteLSPRequestManager requestManager;
    private ArrayList<DefUseClass> data;

    public DaciteAnalysisToolWindow(ToolWindow toolWindow, ArrayList<DefUseClass> data, Project project, RequestManager requestManager) {
        this.data = data;
        this.project = project;
        this.requestManager = (DaciteLSPRequestManager) requestManager;
        button = new JButton("Highlight all");
        button.addActionListener(e -> highlightAll());
        myToolWindowContent = new JPanel();
        myToolWindowContent.setLayout(new BoxLayout(myToolWindowContent, BoxLayout.Y_AXIS));
        TreeViewNode root = new TreeViewNode("defUseChains", "", "root");
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode(root);
        createTreeViewChildren(top);
        Tree tree2 = new DefUseTree(top);
        TreeExpansionListener listener = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent treeExpansionEvent) {
                TreePath path = treeExpansionEvent.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if(node.isLeaf()){
                    int[] indexes = createTreeViewChildren(node);
                    ((DefaultTreeModel)tree2.getModel()).nodesWereInserted( node, indexes);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent treeExpansionEvent) {

            }
        };
        tree2.addTreeExpansionListener(listener);
        ((DefaultTreeModel)tree2.getModel()).setAsksAllowsChildren(true);
        tree2.setCellRenderer(new TreeViewCellRenderer());
        TreeViewCellEditor editor = new TreeViewCellEditor();
        editor.addChangeListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent changeEvent) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) changeEvent.getSource();
                updateChildrenNodes(node);

                //Todo sind inlays schon synchronisiert?
                addInlays();
            }

            @Override
            public void editingCanceled(ChangeEvent changeEvent) {

            }
        });
        tree2.setCellEditor(editor);
        tree2.setRootVisible(false);
        tree2.setEditable(true);
        JScrollPane treeView2 = new JBScrollPane(tree2);
        myToolWindowContent.add(treeView2, BorderLayout.CENTER);
        myToolWindowContent.add(button);
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    public void highlightAll(){
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        iterateTree(root);

    }

    public void iterateTree(DefaultMutableTreeNode node){
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                iterateTree(childNode);
            } else if(childNode instanceof DefUseNode){
                DefUseData[] data = ((DefUseNode) childNode).getUserObject();
                System.out.println("found DefUSeNode");
                for(int k=0; k<data.length;k++){
                    if(!data[k].isChecked()){
                        data[k].setChecked(true);
                    }
                }
            }
        }
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
    }

    private int[] createTreeViewChildren(DefaultMutableTreeNode top){
        int[] output = null;
        TreeViewNode parent = (TreeViewNode) top.getUserObject();
        TreeViewChildrenParams params = new TreeViewChildrenParams(parent.getViewId(), parent.getNodeUri());
        CompletableFuture<TreeViewChildrenResult> request = requestManager.treeViewChildren(params);
        if(request != null){
            try{
                TreeViewChildrenResult result = request.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
                output = new int[result.getNodes().length];
                int i = 0;
                for(TreeViewNode child : result.getNodes()){
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(child);
                    top.add(node);
                    output[i] = i;
                    i++;
                }
            } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return output;
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
                                    int[] colors = decoration.getColor();
                                    color = new Color(colors[0], colors[1], colors[2], colors[3]);
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
                            textEditor.getInlayModel().getInlineElementsInRange(offset,offset+6).forEach(inlay -> {inlay.dispose();});
                            String finalHintWord = hintWord;
                            Font finalFont = font;
                            Color finalColor = color;
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
                                    //gr.setColor(JBColor.LIGHT_GRAY);
                                    //gr.fillRect(targetRegion.x-3, targetRegion.y+1, calcWidthInPixels(inlay)+3, calcHeightInPixels(inlay)+3);
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
}