package dacite.intellij.visualisation;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import dacite.lsp.tvp.TreeViewNode;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class TreeViewCellRenderer extends DefUseCellRenderer{

    JLabel number = new JLabel();
    JLabel defLabel = new JLabel("Def: ");
    JLabel useLabel = new JLabel("Use: ");
    JLabel def = new JLabel();
    JLabel use = new JLabel();

    JBCheckBox box = new JBCheckBox();
    boolean defuse;

    public TreeViewCellRenderer() {
        this.number.setForeground(JBColor.GRAY);
        this.defLabel.setForeground(JBColor.GRAY);
        this.useLabel.setForeground(JBColor.GRAY);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component returnValue = null;
        if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
            TreeViewNode e = (TreeViewNode) ((DefaultMutableTreeNode) value).getUserObject();
            String label = e.getLabel();
            if(e.getIcon() != null){
                switch (e.getIcon()){
                    case "class": this.name.setIcon(AllIcons.Nodes.Class); defuse = false; break;
                    case "method": this.name.setIcon(AllIcons.Nodes.Method); defuse = false; break;
                    case "variable": this.name.setIcon(AllIcons.Nodes.Variable); defuse = false; break;
                    default:
                        defuse = true;
                }
            } else{
                this.name.setIcon(null);
                defuse = true;
            }
            if(!defuse || label.equals("root")){
                JPanel labelPanel = new JPanel();
                labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
                if(label.contains(" ")){
                    name.setText(label.substring(0,label.indexOf(" ")));
                    String chains = label.substring(label.indexOf(" "));
                    number.setText(chains);
                } else {
                    name.setText(label);
                    number.setText("");
                }
                labelPanel.add(name);
                labelPanel.add(Box.createHorizontalStrut(10)); //creates space between the JLabels
                labelPanel.add(number);
                labelPanel.add(Box.createHorizontalStrut(10));
                box.setSelected(e.isEditorHighlight());
                labelPanel.add(box);
                returnValue=labelPanel;
            } else {
                JPanel labelPanel = new JPanel();
                labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
                String var = label.substring(0,label.indexOf(" "));
                String deftext = label.substring(label.indexOf(" "),label.indexOf("-")-1);
                String usetext = label.substring(label.lastIndexOf("-")+2);
                name.setText(var);
                def.setText(deftext);
                labelPanel.add(name);
                labelPanel.add(Box.createHorizontalStrut(10));
                labelPanel.add(defLabel);
                labelPanel.add(def);
                labelPanel.add(Box.createHorizontalStrut(10));
                labelPanel.add(useLabel);
                use.setText(usetext);
                labelPanel.add(use);
                labelPanel.add(Box.createHorizontalStrut(10));
                box.setSelected(e.isEditorHighlight());
                labelPanel.add(box);
                returnValue=labelPanel;
            }

        }
        if (returnValue == null) {
            returnValue = defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
        }
        return returnValue;
    }
}
