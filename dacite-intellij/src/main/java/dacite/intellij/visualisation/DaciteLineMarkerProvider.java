package dacite.intellij.visualisation;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.execution.lineMarker.RunLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.MarkupEditorFilter;
import com.intellij.openapi.editor.markup.MarkupEditorFilterFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.util.Function;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class DaciteLineMarkerProvider implements LineMarkerProvider {
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier &&
                element.getText().equals("DaciteSymbolicDriver")) {
            final DefaultActionGroup actionGroup = new DefaultActionGroup();
            actionGroup.add(ActionManager.getInstance().getAction("dacite.analyzeSymbolic"));
            Function<PsiElement, String> tooltipProvider = element1 -> {return "Run Dacite Symbolic Analysis";};
            return new RunLineMarkerInfo(element, AllIcons.Actions.StartMemoryProfile, tooltipProvider, actionGroup);
        }
        return null;
    }

    static class RunLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        private final DefaultActionGroup myActionGroup;
        private final AnAction mySingleAction;

        RunLineMarkerInfo(PsiElement element, Icon icon, Function<? super PsiElement, @Nls String> tooltipProvider, DefaultActionGroup actionGroup) {
            super(element, element.getTextRange(), icon, tooltipProvider, null, GutterIconRenderer.Alignment.CENTER,
                    () -> tooltipProvider.fun(element));
            myActionGroup = actionGroup;
            if (myActionGroup.getChildrenCount() == 1) {
                mySingleAction = myActionGroup.getChildActionsOrStubs()[0];
            } else {
                mySingleAction = null;
            }
        }

        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new LineMarkerGutterIconRenderer<>(this) {
                @Override
                public AnAction getClickAction() {
                    return mySingleAction;
                }

                @Override
                public boolean isNavigateAction() {
                    return true;
                }

                @Override
                public ActionGroup getPopupMenuActions() {
                    return myActionGroup;
                }
            };
        }

        @NotNull
        @Override
        public MarkupEditorFilter getEditorFilter() {
            return MarkupEditorFilterFactory.createIsNotDiffFilter();
        }

        @Override
        public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
            return info instanceof RunLineMarkerInfo && info.getIcon() == getIcon();
        }

        @Override
        public Icon getCommonIcon(@NotNull List<? extends MergeableLineMarkerInfo<?>> infos) {
            return getIcon();
        }
    }
}
