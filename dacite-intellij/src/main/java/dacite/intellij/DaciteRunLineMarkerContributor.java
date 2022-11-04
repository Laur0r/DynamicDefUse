package dacite.intellij;

import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DaciteRunLineMarkerContributor extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {
        PsiClass test = JUnitUtil.getTestClass(element); // TODO requestmanager Return check
        if(test == null){
            return null;
        }
        return new Info(null, null,
                    ActionManager.getInstance().getAction("dacite.intellij.actions.DaciteAnalyzeAction"));
    }

}
