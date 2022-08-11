package dacite.intellij.visualisation;

import javax.swing.tree.DefaultMutableTreeNode;

public class ClassNode extends DefaultMutableTreeNode {
    /**
     * @param resource
     */
    public ClassNode(String resource) {
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
