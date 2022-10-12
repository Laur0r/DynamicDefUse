package dacite.intellij.visualisation;

import javax.swing.tree.DefaultMutableTreeNode;

public class MethodNode extends DefaultMutableTreeNode {
    /**
     * @param resource
     */

    private int numberChains;

    public MethodNode(String resource) {
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

    public int getNumberChains() {return numberChains;}
    public void addNumberChains(int add){numberChains+=add;}

}
