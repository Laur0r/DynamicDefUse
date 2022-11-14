package dacite.intellij.lspclient;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.ui.JBColor;
import dacite.lsp.InlayHintDecoration;
import dacite.lsp.InlayHintDecorationParams;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.listeners.LSPCaretListenerImpl;

import java.awt.*;
import java.awt.Color;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.lsp4intellij.requests.Timeout.getTimeout;
import static org.wso2.lsp4intellij.requests.Timeouts.REFERENCES;

public class DaciteEditorEventManager extends EditorEventManager {

    private boolean inlays;
    public DaciteEditorEventManager(Editor editor, DocumentListener documentListener, EditorMouseListener mouseListener, EditorMouseMotionListener mouseMotionListener, LSPCaretListenerImpl caretListener, RequestManager requestmanager, ServerOptions serverOptions, LanguageServerWrapper wrapper) {
        super(editor, documentListener, mouseListener, mouseMotionListener, caretListener, requestmanager, serverOptions, wrapper);
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
                                int[] colors = decoration.getColor();
                                color = new Color(colors[0], colors[1], colors[2], colors[3]);
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
}
