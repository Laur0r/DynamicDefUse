package dacite.lsp;

import org.eclipse.lsp4j.Position;

import java.awt.*;

public class InlayHintDecoration {
    private int[] color;
    private String fontStyle;

    public InlayHintDecoration(int[] color, String font){
        this.color = color;
        this.fontStyle = font;
    }

    public int[] getColor() {
        return color;
    }

    public String getFontStyle() {
        return fontStyle;
    }
}
