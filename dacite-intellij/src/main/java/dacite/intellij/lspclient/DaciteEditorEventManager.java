package dacite.intellij.lspclient;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.ui.JBColor;
import dacite.lsp.InlayHintDecoration;
import dacite.lsp.InlayHintDecorationParams;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;
import org.wso2.lsp4intellij.utils.DocumentUtils;

import java.awt.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

public class DaciteEditorEventManager extends EditorEventManager {

    private boolean inlays;
    private boolean pressedEscaped;
    public DaciteEditorEventManager(Editor editor, DocumentListener documentListener, EditorMouseListener mouseListener, EditorMouseMotionListener mouseMotionListener, LSPCaretListenerImpl caretListener, RequestManager requestmanager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
        super(editor, documentListener, mouseListener, mouseMotionListener, caretListener, requestmanager, serverOptions, wrapper);
        registerKey();
    }

    @Override
    public void documentOpened() {
        super.documentOpened();
        //addInlays();
    }

    @Override
    public void mouseEntered(){
        super.mouseEntered();
        if(!inlays){
            addInlays();
        }
    }

    public void registerKey(){
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent e) -> {
            int eventId = e.getID();
            if (eventId == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    this.pressedEscaped = true;
                }
            } else if (eventId == KeyEvent.KEY_RELEASED) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    this.pressedEscaped = false;
                }
            }
            return false;
        });
    }

    /**
     * Update Inlays for newly opened editor
     */
    protected void addInlays(){
        Font font = editor.getColorsScheme().getFont(EditorFontType.PLAIN);
        Range range = new Range(new Position(0,0), new Position(editor.getDocument().getLineCount()-1,0));
        InlayHintParams param = new InlayHintParams(getIdentifier(), range);
        // get InlayHints from Server
        CompletableFuture<java.util.List<InlayHint>> request =  getRequestManager().inlayHint(param);
        if (request != null) {
            try {
                java.util.List<InlayHint> res = request.get(getTimeout(REFERENCES), TimeUnit.MILLISECONDS);
                if (res != null && res.size() > 0) {
                    for(InlayHint hint: res){
                        InlayHintDecorationParams decParams = new InlayHintDecorationParams(getIdentifier(), hint.getPosition());
                        CompletableFuture<InlayHintDecoration> decorationRequest = ((DaciteLSPRequestManager) getRequestManager()).inlayHintDecoration(decParams);
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
                        int offset = editor.logicalPositionToOffset(lpos);
                        Either<String, List<InlayHintLabelPart>> label = hint.getLabel();
                        String hintWord = "";
                        if(label.getLeft() != null){
                            hintWord = label.getLeft();
                        }
                        editor.getInlayModel().getInlineElementsInRange(offset,offset+6).forEach(inlay -> {inlay.dispose();});
                        String finalHintWord = hintWord;
                        Font finalFont = font;
                        Color finalColor = color;
                        editor.getInlayModel().addInlineElement(offset, new EditorCustomElementRenderer() {
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
                                gr.drawString(finalHintWord, targetRegion.x, targetRegion.y+ editor.getAscent());
                            }
                        });
                        inlays = true;
                    }
                }
            } catch (TimeoutException | InterruptedException | JsonRpcException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Runnable getEditsRunnable(int version, List<Either<TextEdit, InsertReplaceEdit>> edits, String name, boolean setCaret) {
        if (version < this.documentEventManager.getDocumentVersion()) {
            LOG.warn(String.format("Edit version %d is older than current version %d", version, this.documentEventManager.getDocumentVersion()));
            return null;
        }
        if (edits == null) {
            LOG.warn("Received edits list is null.");
            return null;
        }
        if (editor.isDisposed()) {
            LOG.warn("Text edits couldn't be applied as the editor is already disposed.");
            return null;
        }
        Document document = editor.getDocument();
        if (!document.isWritable()) {
            LOG.warn("Document is not writable");
            return null;
        }

        return () -> {
            // Creates a sorted edit list based on the insertion position and the edits will be applied from the bottom
            // to the top of the document. Otherwise all the other edit ranges will be invalid after the very first edit,
            // since the document is changed.
            List<TextEdit> lspEdits = new ArrayList<>();
            if(!document.getText().isEmpty()){
                document.setText("");
            }
            edits.forEach(edit -> {
                int start = 0;
                int end = 0;
                String text = "";
                if(edit.isLeft()) {
                    text = edit.getLeft().getNewText();
                    Range range = edit.getLeft().getRange();
                    if (range != null) {
                        start = editor.logicalPositionToOffset(new LogicalPosition(range.getStart().getLine(),range.getStart().getCharacter()));
                        end = editor.logicalPositionToOffset(new LogicalPosition(range.getEnd().getLine(),range.getEnd().getCharacter()));
                    }
                } else if(edit.isRight()) {
                    text = edit.getRight().getNewText();
                    Range range = edit.getRight().getInsert();

                    if (range != null) {
                        start = editor.logicalPositionToOffset(new LogicalPosition(range.getStart().getLine(),range.getStart().getCharacter()));
                        end = editor.logicalPositionToOffset(new LogicalPosition(range.getEnd().getLine(),range.getEnd().getCharacter()));
                    } else if ((range = edit.getRight().getReplace()) != null) {
                        start = editor.logicalPositionToOffset(new LogicalPosition(range.getStart().getLine(),range.getStart().getCharacter()));
                        end = editor.logicalPositionToOffset(new LogicalPosition(range.getEnd().getLine(),range.getEnd().getCharacter()));
                    }
                }
                if (StringUtils.isEmpty(text)) {
                    document.deleteString(start, end);
                    if (setCaret) {
                        editor.getCaretModel().moveToOffset(start);
                    }
                } else {
                    text = text.replace(DocumentUtils.WIN_SEPARATOR, DocumentUtils.LINUX_SEPARATOR);
                    if (end >= 0) {
                        if (end - start <= 0) {
                            document.insertString(start, text);
                        } else {
                            document.replaceString(start, end, text);
                        }
                    } else if (start == 0) {
                        document.setText(text);
                    } else if (start > 0) {
                        document.insertString(start, text);
                    }
                    if (setCaret) {
                        editor.getCaretModel().moveToOffset(start + text.length());
                    }
                }
                FileDocumentManager.getInstance().saveDocument(editor.getDocument());
            });
        };
    }
}
