package dacite.intellij.visualisation;

import com.google.errorprone.annotations.Var;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.JBTable;
import dacite.intellij.defUseData.DefUseData;
import dacite.intellij.defUseData.DefUseVar;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

class DefUseCellEditor extends AbstractCellEditor implements TreeCellEditor {
    TreeCellRenderer renderer = new DefUseCellRenderer();
    DefUseData[] data;
    JBTable table;
    DefUseTree tree;
    Component c;
    JBCheckBox box;
    DefUseVar variable;
    boolean editTable = false;
    CellEditorListener listener;

    public DefUseCellEditor(DefUseTree tree) {
        this.tree = tree;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean isSelected, boolean expanded, boolean leaf, int row) {
        Component l = renderer.getTreeCellRendererComponent(tree, value,
                true, expanded, leaf, row, true);
        if (value instanceof DefUseNode) {
                data = ((DefUseNode) value).getUserObject();
                c = l;
                JBScrollPane pane = ((JBScrollPane)l);
                JBViewport port =  (JBViewport) pane.getViewport();
                table = (JBTable) port.getComponent(0);
                editTable = true;
                table.addComponentListener(new ComponentListener() {
                         public void componentResized(ComponentEvent e) {somethingChanged();                                               }
                         public void componentShown(ComponentEvent e) {somethingChanged();}
                         public void componentMoved(ComponentEvent e) {}
                         public void componentHidden(ComponentEvent e) {}
                });

                table.getModel().addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent tableModelEvent) {
                        if(tableModelEvent.getColumn() == 3){
                            stopCellEditing();
                        }
                    }
                });
        } else if(value instanceof VariableNode){
            JPanel panel = (JPanel) l;
            variable = ((VariableNode) value).getUserObject();
            box = (JBCheckBox) panel.getComponent(4);
            editTable = false;
            box.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    //if(itemEvent.getStateChange() == ItemEvent.SELECTED) {//checkbox has been selected
                        stopCellEditing();
                    //} else {//checkbox has been deselected
                        //do something...
                    //};
                }
            });
        }
        return l;
    }


    @Override
    public Object getCellEditorValue() {
        return data;
    }

    @Override
    public boolean isCellEditable(EventObject eventObject) {
        if (eventObject instanceof MouseEvent) {
            MouseEvent mEvt = (MouseEvent)eventObject;
            if (mEvt.getClickCount() == 1) {
                TreePath path = tree.getPathForLocation(mEvt.getX(), mEvt.getY());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node instanceof DefUseNode || node instanceof VariableNode) {
                    System.out.println("isEditable !");
                    return true;
                }
            }
        }
        System.out.println("is not editable!!");
        return false;
    }

    @Override
    public boolean shouldSelectCell(EventObject eventObject) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        //System.out.println("stop editing");
        if(editTable){
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            Object value = table.getValueAt(row, col);
            if(value instanceof Boolean && data.length == table.getRowCount()){
                data[row].setChecked((boolean) value);
            }
            DefUseData defUse = data[table.getSelectedRow()];
            listener.editingStopped(new ChangeEvent(defUse));
        } else {
            boolean value = box.isSelected();
            variable.setChecked(value);
            listener.editingStopped(new ChangeEvent(variable));
        }
        return true;
    }

    @Override
    public void cancelCellEditing() {

    }

    @Override
    public void addCellEditorListener(CellEditorListener cellEditorListener) {
        listener = cellEditorListener;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener cellEditorListener) {

    }

    private void somethingChanged() {
        /*System.out.println("something changed");
        c.setSize(c.getPreferredSize());
        c.setSize(c.getWidth()+100,c.getHeight());
        tree.invalidateNodeBounds();
        tree.repaint();*/
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO: skip if size is not changing
                //((JBScrollPane) c).setLayout(new BorderLayout());
                //c.setSize(c.getPreferredSize());
                c.setSize(tree.getWidth()-58,c.getHeight());
                tree.invalidateNodeBounds();
                tree.repaint();
            }
        });
    }
}
