package dacite.intellij.visualisation;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import dacite.intellij.DefUseData.DefUseData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.lang.reflect.Method;

class DefUseCellRenderer extends DefaultTreeCellRenderer {
    DefUseTableModel model = new DefUseTableModel();
    JBTable table = new JBTable(model);

    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

    public DefUseCellRenderer() {
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        defaultRenderer.setClosedIcon(AllIcons.FileTypes.JavaClass);
        defaultRenderer.setOpenIcon(AllIcons.FileTypes.JavaClass);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) && (value instanceof DefUseNode)) {
            DefUseData[] e = ((DefUseNode) value).getUserObject();
            for(int i=0; i<e.length;i++){
                if(table.getRowCount()>i) {
                    table.getModel().setValueAt(e[i].getName(),i,0);
                    table.getModel().setValueAt(e[i].getDefLocation(),i,1);
                    table.getModel().setValueAt(e[i].getUseLocation(),i,2);
                    // set value only if there was a change -> fires table change
                    if(e[i].isChecked() && !(Boolean)table.getModel().getValueAt(i,3)) {
                        table.getModel().setValueAt(e[i].isChecked(), i, 3);
                    } else if(!e[i].isChecked() && (Boolean)table.getModel().getValueAt(i,3)) {
                        table.getModel().setValueAt(e[i].isChecked(), i, 3);
                    }
                } else {
                    ((DefUseTableModel) table.getModel()).addRow(e[i].getName(), e[i].getDefLocation(), e[i].getUseLocation());
                }
            }
            table.setEnabled(true);
            JBScrollPane pane = new JBScrollPane(table);
            pane.setEnabled(true);
            returnValue = pane;
        } else if(value instanceof MethodNode){
            defaultRenderer.setClosedIcon(AllIcons.Nodes.Method);
            defaultRenderer.setOpenIcon(AllIcons.Nodes.Method);
            defaultRenderer.setLeafIcon(AllIcons.Nodes.Method);
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);

        } else if(value instanceof ClassNode){
            defaultRenderer.setClosedIcon(AllIcons.Nodes.Class);
            defaultRenderer.setOpenIcon(AllIcons.Nodes.Class);
            defaultRenderer.setLeafIcon(AllIcons.Nodes.Class);
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
        }
        return returnValue;
    }
}
