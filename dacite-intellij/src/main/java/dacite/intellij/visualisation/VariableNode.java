package dacite.intellij.visualisation;

import dacite.intellij.defUseData.DefUseVar;

import javax.swing.tree.DefaultMutableTreeNode;

public class VariableNode extends DefaultMutableTreeNode {
    /**
     * @param resource
     */

    private int numberChains;
    public VariableNode(DefUseVar resource) {
        super(resource);
    }

    @Override
    public void setUserObject(Object userObject) {
        if(userObject instanceof DefUseVar) {
            super.setUserObject(userObject);
        }
    }

    @Override
    public DefUseVar getUserObject() {
        return (DefUseVar) super.getUserObject();
    }

    public void setNumberChains(int number) {numberChains = number;}

    public int getNumberChains() {return numberChains;}
    public void addNumberChains(int add){numberChains+=add;}

}
