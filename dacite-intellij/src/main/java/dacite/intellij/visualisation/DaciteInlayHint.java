package dacite.intellij.visualisation;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.*;
import com.intellij.ide.presentation.Presentation;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutKt;
import com.intellij.util.ui.UIUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class DaciteInlayHint implements InlayHintsProvider {

    private SettingsKey<NoSettings> key = new SettingsKey<>("Dacite.hints");

    @NotNull
    @Override
    public SettingsKey getKey() {
        return key;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "Dacite";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Preview";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull Object o) {
        /*return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return LayoutKt.panel(new LCFlags[0], "LSP", builder -> {
                    return null;
                });
            }
        };*/
        return null;
    }

    @NotNull
    @Override
    public Object createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull Object o, @NotNull InlayHintsSink inlayHintsSink) {
        return new FactoryInlayHintsCollector(editor) {

            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                TextAttributes attributes = new TextAttributes(Color.CYAN, Color.BLACK, Color.LIGHT_GRAY, null, 1);
                InlayPresentation base = getFactory().smallText("help!!");
                //InlayPresentation att = new WithAttributesPresentation(base, attributes, editor,);
                InlayPresentation presentation = new ColorInlayPresentation("TEST", JBColor.RED,null,1);
                //BufferedImage image = UIUtil.createImage((Component)new JPanel(), 1,1,2);
                //((Graphics2D g2 = (Graphics2D) image.getGraphics();
                //presentation.paint(g2, attributes);
                inlayHintsSink.addInlineElement(10, false, presentation, false);
                inlayHintsSink.addInlineElement(50, false, base, false);
                return false;
            }
        };
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }
}
