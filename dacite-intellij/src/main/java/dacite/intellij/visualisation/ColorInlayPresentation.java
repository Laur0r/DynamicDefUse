package dacite.intellij.visualisation;

import com.intellij.codeInsight.hints.presentation.BasePresentation;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ColorInlayPresentation extends BasePresentation {

    int fontStyle;
    Color textColor;
    Color backgroundColor;
    String base;

    public ColorInlayPresentation(String base, Color text, Color background, int font){
        this.base = base;
        this.backgroundColor = background;
        this.textColor = text;
        this.fontStyle = font;
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
        graphics2D.drawString(base, 0, 10);
        graphics2D.setColor(textColor);
        graphics2D.setBackground(backgroundColor);
    }
}
