package dacite.intellij.visualisation;

import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
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
import com.intellij.util.NotNullProducer;
import dacite.intellij.defUseData.DefUseClass;
import dacite.intellij.defUseData.DefUseData;
import dacite.intellij.defUseData.DefUseMethod;
import dacite.intellij.defUseData.DefUseVar;
import dacite.intellij.lspclient.DaciteLSPRequestManager;
import dacite.lsp.InlayHintDecoration;
import dacite.lsp.InlayHintDecorationParams;
import groovy.lang.Tuple;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.Color;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
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
    private RequestManager requestManager;
    private ArrayList<DefUseClass> data;

    public DaciteAnalysisToolWindow(ToolWindow toolWindow, ArrayList<DefUseClass> data, Project project, RequestManager requestManager) {
        this.data = data;
        this.project = project;
        this.requestManager = requestManager;
        button = new JButton("Highlight all");
        button.addActionListener(e -> highlightAll());
        myToolWindowContent = new JPanel();
        myToolWindowContent.setLayout(new BoxLayout(myToolWindowContent, BoxLayout.Y_AXIS));
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode("root");
        createNodes(top);
        tree = new DefUseTree(top);
        tree.setRootVisible(false);
        tree.setEditable(true);
        DefUseCellEditor cellEditor = new DefUseCellEditor(tree);
        DefUseCellRenderer renderer = new DefUseCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setIcon(AllIcons.Actions.GroupByClass);
        tree.setCellRenderer(renderer);
        tree.setCellEditor(cellEditor);
        //addInlays();
        cellEditor.addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent changeEvent) {
                Object obj = changeEvent.getSource();
                if(obj instanceof DefUseVar){

                } else if(obj instanceof DefUseData){

                }
                addInlays();
                //textEditor.getInlayModel().getInlineElementsInRange(getOffSets(textEditor, 6, "b").get(0),getOffSets(textEditor, 6, "b").get(1)).forEach(inlay -> {inlay.dispose();});
                /*DefUseData data = (DefUseData) changeEvent.getSource();
                String varName = data.getName();
                String def = data.getDefLocation();
                String use = data.getUseLocation();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                DefaultMutableTreeNode node = findNode(root, data);
                if(node == null){
                    throw new RuntimeException("Could not find checked entry in tree");
                }
                ClassNode classNode = (ClassNode) node.getParent().getParent();
                String className = classNode.getUserObject();
                int useline = Integer.parseInt(use.substring(use.lastIndexOf(" ")+1))-1;
                int defline = Integer.parseInt(def.substring(def.lastIndexOf(" ")+1))-1;
                if(data.isChecked()){
                    jumpToFile(project, defline, useline, varName, className);
                } else {
                    System.out.println("not checked");
                    textEditor = manager.getSelectedTextEditor();
                    RangeHighlighter[] highlighters = textEditor.getMarkupModel().getAllHighlighters();
                    boolean useRemoved = false;
                    boolean defRemoved = false;
                    for (RangeHighlighter high : highlighters) {
                        if (useRemoved && defRemoved) {
                            break;
                        }
                        Tuple<Integer> defOffsets = getOffSets(textEditor, defline, varName);
                        int startOffsetDef = defOffsets.get(0);
                        int endOffsetDef = defOffsets.get(1);
                        if (high.getStartOffset() == startOffsetDef && high.getEndOffset() == endOffsetDef && !defRemoved) {
                            System.out.println("remove def Highlights");
                            textEditor.getMarkupModel().removeHighlighter(high);
                            defRemoved = true;
                            continue;
                        }
                        Tuple<Integer> useOffsets = getOffSets(textEditor, useline, varName);
                        int startOffsetUse = useOffsets.get(0);
                        int endOffsetUse = useOffsets.get(1);
                        if (high.getStartOffset() == startOffsetUse && high.getEndOffset() == endOffsetUse && !useRemoved) {
                            textEditor.getMarkupModel().removeHighlighter(high);
                            useRemoved = true;
                        }
                    }
                }*/
            }

            @Override
            public void editingCanceled(ChangeEvent changeEvent) {

            }
        });
        JScrollPane treeView = new JBScrollPane(tree);
        myToolWindowContent.add(treeView, BorderLayout.CENTER);
        myToolWindowContent.add(button);
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    public void jumpToFile(Project project, int defLine, int useLine, String varName, String className){
        VirtualFile[] vFiles = ProjectRootManager.getInstance(project).getContentRoots();
        System.out.println(vFiles[0].getChildren().length);
        System.out.println(vFiles[0].getChildren()[0].getPath());
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(project.getBasePath() +"/src/"+ className + ".java");
        if(vf == null){
            System.out.println("noFile");
            System.out.println("update");
        } else{
            FileEditorManager manager = FileEditorManager.getInstance(project);
            manager.openFile(vf, true);
            Editor textEditor = manager.getSelectedTextEditor();
            LogicalPosition position = new LogicalPosition(defLine,0);
            textEditor.getCaretModel().moveToLogicalPosition(position);
            Tuple<Integer> defOffsets = getOffSets(textEditor, defLine, varName);
            PsiFile psi = PsiManager.getInstance(project).findFile(vf);
            PsiElement element = psi.findElementAt(textEditor.getDocument().getLineStartOffset(defLine));
            int startOffsetDef = defOffsets.get(0);
            int endOffsetDef = defOffsets.get(1);
            textEditor.getMarkupModel().addRangeHighlighter(startOffsetDef, endOffsetDef, 9999, new TextAttributes(null, JBColor.CYAN, null, null, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE);

            LogicalPosition usePosition = new LogicalPosition(useLine,0);
            textEditor.getCaretModel().moveToLogicalPosition(usePosition);
            Tuple<Integer> useOffsets = getOffSets(textEditor, useLine, varName);
            int startOffsetUse = useOffsets.get(0);
            int endOffsetUse = useOffsets.get(1);
            textEditor.getMarkupModel().addRangeHighlighter(startOffsetUse, endOffsetUse, 9999, new TextAttributes(null, JBColor.MAGENTA, null, null, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE);
        }
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

    public DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, DefUseData data){
        DefaultMutableTreeNode output = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                output = findNode(childNode, data);
                if(output !=null){
                    return output;
                }
            } else if(childNode instanceof DefUseNode){
                DefUseData[] d = ((DefUseNode) childNode).getUserObject();
                if(Arrays.asList(d).contains(data)){
                    output = childNode;
                    return output;
                }
            }
        }
        return output;
    }

    public Tuple<Integer> getOffSets(Editor textEditor, int line, String varName){
        TextRange defRange = new TextRange(textEditor.getDocument().getLineStartOffset(line),textEditor.getDocument().getLineEndOffset(line));
        String lineTextDef = textEditor.getDocument().getText(defRange);
        int index = lineTextDef.indexOf(varName);
        int startOffsetDef = textEditor.getDocument().getLineStartOffset(line) + index;
        int endOffsetDef = startOffsetDef + varName.length();
        return new Tuple<>(startOffsetDef, endOffsetDef);
    }

    /*public void setTableData() {
        model.addRow("x", "Class.method line 2", "Class.method line 3");
        model.addRow("y", "Class.method line 3", "Class.method line 4");
    }*/
    public void setProject(Project project){
        this.project = project;
    }
    public void setData(ArrayList<DefUseClass> data){
        this.data = data;
    }

    private void createNodes(DefaultMutableTreeNode top) {
        for(DefUseClass dfClass: data){
            ClassNode cnode = new ClassNode(dfClass.getName());
            for(DefUseMethod dfMethod:dfClass.getMethods()){
                MethodNode mnode = new MethodNode(dfMethod.getName());
                for(DefUseVar dfVariable : dfMethod.getVariables()){
                    DefUseVar var = new DefUseVar(dfVariable.getName());
                    VariableNode vnode = new VariableNode(var);
                    DefUseData[] data = dfVariable.getData().toArray(new DefUseData[0]);
                    vnode.setNumberChains(data.length);
                    DefUseNode dnode = new DefUseNode(data);
                    vnode.add(dnode);
                    mnode.add(vnode);
                    mnode.addNumberChains(data.length);
                }
                cnode.add(mnode);
                cnode.addNumberChains(mnode.getNumberChains());
            }
            top.add(cnode);
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