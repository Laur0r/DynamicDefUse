package dacite.lsp;

import org.eclipse.lsp4j.Position;

import java.awt.*;

public class InlayHintDecoration {
    private String color;
    private String fontStyle;

    public InlayHintDecoration(String color, String font){
        this.color = color;
        this.fontStyle = font;
    }

    public String getColor() {
        return color;
    }

    public String getFontStyle() {
        return fontStyle;
    }
}
