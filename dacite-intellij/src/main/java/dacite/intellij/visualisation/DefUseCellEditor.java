package dacite.intellij.visualisation;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.JBTable;
import dacite.intellij.DefUseData.DefUseData;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

class DefUseCellEditor extends AbstractCellEditor implements TreeCellEditor {
    TreeCellRenderer renderer = new DefUseCellRenderer();
    DefUseData[] data;
    JBTable table;
    DefUseTree tree;
    Component c;
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
                JBViewport port = ((JBViewport)pane.getComponent(0));
                table = (JBTable) port.getComponent(0);
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
                System.out.println(tree.getMaximumSize());
                System.out.println(l.getMaximumSize());
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
                if (node instanceof DefUseNode) {
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
        System.out.println("stop editing");
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        Object value = table.getValueAt(row, col);
        if(value instanceof Boolean){
            data[row].setChecked((boolean) value);
        }
        DefUseData defUse = data[table.getSelectedRow()];
        listener.editingStopped(new ChangeEvent(defUse));
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
                c.setSize(c.getPreferredSize());
                c.setSize(tree.getWidth()-58,c.getHeight());
                tree.invalidateNodeBounds();
                tree.repaint();
            }
        });
    }
}
