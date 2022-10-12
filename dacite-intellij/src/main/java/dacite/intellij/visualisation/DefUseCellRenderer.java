package dacite.intellij.visualisation;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import dacite.intellij.defUseData.DefUseData;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

class DefUseCellRenderer extends DefaultTreeCellRenderer {
    DefUseTableModel model = new DefUseTableModel();
    JBTable table = new JBTable(model);

    JLabel name = new JLabel();
    JLabel number = new JLabel();

    DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

    public DefUseCellRenderer() {
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        defaultRenderer.setClosedIcon(AllIcons.FileTypes.JavaClass);
        defaultRenderer.setOpenIcon(AllIcons.FileTypes.JavaClass);
        this.number.setForeground(JBColor.GRAY);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) && (value instanceof DefUseNode)) {
            DefUseData[] e = ((DefUseNode) value).getUserObject();
            while(table.getRowCount() > e.length){
                ((DefUseTableModel) table.getModel()).removeRow(table.getRowCount()-1);
            }
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
            pane.setWheelScrollingEnabled(false);
            pane.setOverlappingScrollBar(false);
            pane.setAutoscrolls(false);
            table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight()*(table.getModel().getRowCount()+1)+2));
            pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            pane.setEnabled(true);
            pane.setSize(table.getPreferredSize());
            pane.setPreferredSize(table.getPreferredSize());
            returnValue = pane;
        } else if(value instanceof MethodNode){
            String name = ((MethodNode) value).getUserObject();
            String number = ((MethodNode) value).getNumberChains() + " chains";
            this.name.setText(name);
            this.name.setIcon(AllIcons.Nodes.Method);
            this.number.setText(number);
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
            labelPanel.add(this.name);
            labelPanel.add(Box.createHorizontalStrut(10)); //creates space between the JLabels
            labelPanel.add(this.number);

            returnValue = labelPanel;

        } else if(value instanceof ClassNode){
            String name = ((ClassNode) value).getUserObject();
            String number = ((ClassNode) value).getNumberChains() + " chains";
            this.name.setText(name);
            this.name.setIcon(AllIcons.Nodes.Class);
            this.number.setText(number);
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
            labelPanel.add(this.name);
            labelPanel.add(Box.createHorizontalStrut(10)); //creates space between the JLabels
            labelPanel.add(this.number);

            returnValue = labelPanel;
        } else if(value instanceof VariableNode){
            String name = ((VariableNode) value).getUserObject();
            String number = ((VariableNode) value).getNumberChains() + " chains";
            this.name.setText(name);
            this.name.setIcon(AllIcons.Nodes.Variable);
            this.number.setText(number);
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
            labelPanel.add(this.name);
            labelPanel.add(Box.createHorizontalStrut(10)); //creates space between the JLabels
            labelPanel.add(this.number);

            returnValue = labelPanel;
        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
        }
        return returnValue;
    }
}
