package dacite.intellij.visualisation;

import javax.swing.tree.DefaultMutableTreeNode;

public class VariableNode extends DefaultMutableTreeNode {
    /**
     * @param resource
     */
    public VariableNode(String resource) {
        super(resource);
    }

    @Override
    public void setUserObject(Object userObject) {
        if(userObject instanceof String) {
            super.setUserObject(userObject);
        }
    }

    @Override
    public String getUserObject() {
        return (String) super.getUserObject();
    }

}
