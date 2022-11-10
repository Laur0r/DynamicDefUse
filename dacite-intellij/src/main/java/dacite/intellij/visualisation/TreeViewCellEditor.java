package dacite.intellij.visualisation;

import com.intellij.ui.components.JBCheckBox;
import com.thaiopensource.xml.dtd.om.Def;
import dacite.intellij.defUseData.DefUseData;
import dacite.intellij.defUseData.DefUseVar;
import dacite.lsp.tvp.TreeViewNode;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

class TreeViewCellEditor extends AbstractCellEditor implements TreeCellEditor {

    TreeCellRenderer renderer;
    CellEditorListener listener;
    JBCheckBox box;
    TreeViewNode treeNode;
    DefaultMutableTreeNode node;
    public TreeViewCellEditor() {
        renderer = new TreeViewCellRenderer();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree jTree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
        Component l = renderer.getTreeCellRendererComponent(jTree, value,true, expanded, leaf, row, true);
        node = (DefaultMutableTreeNode) value;
        treeNode = (TreeViewNode) node.getUserObject();
        JPanel panel = (JPanel) l;
        if(panel.getComponentCount() == 5){
            box = (JBCheckBox) panel.getComponent(4);
            if(box.getActionListeners().length == 0) {
                box.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        stopCellEditing();
                    }
                });
            }
        } else if(panel.getComponentCount()== 9){
            box = (JBCheckBox) panel.getComponent(8);
            box.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    stopCellEditing();
                }
            });
        }
        return l;
    }

    @Override
    public Object getCellEditorValue() {
        return treeNode;
    }

    @Override
    public boolean isCellEditable(EventObject eventObject) {
        return true;
    }

    public void addChangeListener(CellEditorListener listener){
        this.listener = listener;
    }

    @Override
    public boolean stopCellEditing() {
        boolean value = box.isSelected();
        treeNode.setEditorHighlight(value);
        listener.editingStopped(new ChangeEvent(node));
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject eventObject) {
        return true;
    }
}
