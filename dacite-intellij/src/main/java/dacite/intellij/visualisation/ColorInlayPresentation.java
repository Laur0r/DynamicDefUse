package dacite.intellij.visualisation;

import com.intellij.codeInsight.hints.presentation.BasePresentation;
import com.intellij.codeInsight.hints.presentation.InlayTextMetricsStorage;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Graphics2D;

public class ColorInlayPresentation extends BasePresentation {

    int fontStyle;
    Color textColor;
    Color backgroundColor;
    String base;

    EditorImpl editor;

    private Key<InlayTextMetricsStorage> TEXT_METRICS_STORAGE = Key.create("InlayTextMetricsStorage");

    public ColorInlayPresentation(String base, Color text, Color background, int font){
        this.base = base;
        this.backgroundColor = background;
        this.textColor = text;
        this.fontStyle = font;
        //this.editor = editor;
    }

    private InlayTextMetricsStorage getTextMetricStorage(EditorImpl editor) {
        InlayTextMetricsStorage storage = editor.getUserData(TEXT_METRICS_STORAGE);
        if (storage == null) {
            InlayTextMetricsStorage newStorage = new InlayTextMetricsStorage(editor);
            editor.putUserData(TEXT_METRICS_STORAGE, newStorage);
            return newStorage;
        }
        return storage;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public int getWidth() {
        return 10;
    }

    @Override
    public void paint(@NotNull Graphics2D graphics2D, @NotNull TextAttributes textAttributes) {
        //graphics2D.drawString(base, 0, 10);
        //InlayTextMetricsStorage storage = getTextMetricStorage(editor);
        //int baseline = storage.getFontMetrics(true).getFontBaseline();
        graphics2D.setBackground(backgroundColor);
        graphics2D.setColor(textColor);
        graphics2D.drawString(base, 0, 50);
    }
}
