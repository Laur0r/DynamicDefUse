package dacite.intellij.visualisation;

import com.intellij.ui.treeStructure.Tree;

import java.lang.reflect.Field;

import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeNode;

public class DefUseTree extends Tree {
    public DefUseTree(TreeNode root) {
        super(root);
    }

    public void invalidateNodeBounds() {
        invalidateNodeBoundsViaSideEffect();
        //invalidateNodeBoundsViaRefection();
    }

    public void invalidateNodeBoundsViaSideEffect() {
        if (ui instanceof BasicTreeUI) {
            BasicTreeUI basicTreeUI = (BasicTreeUI) ui;
            basicTreeUI.setLeftChildIndent(basicTreeUI.getLeftChildIndent());
        }
    }

    public void invalidateNodeBoundsViaRefection() {

        if (ui instanceof BasicTreeUI) {

            try {
                Field field = BasicTreeUI.class.getDeclaredField("treeState");
                field.setAccessible(true);

                AbstractLayoutCache treeState = (AbstractLayoutCache) field.get(ui);

                if (treeState != null) {
                    treeState.invalidateSizes();
                }
            } catch (Exception e) {
            }
        }
    }
}
