package dacite.intellij.visualisation;

import dacite.intellij.DefUseData.DefUseData;

import javax.swing.tree.DefaultMutableTreeNode;

public class DefUseNode extends DefaultMutableTreeNode {

        /**
         * @param resource
         */
        public DefUseNode(DefUseData[] resource) {
            super(resource);
        }

        @Override
        public void setUserObject(Object userObject) {
            if(userObject instanceof DefUseData[]) {
                super.setUserObject(userObject);
            }
        }

        /*public void setName(String name) {
            if (getUserObject() != null) {
                getUserObject().setName(name);
            }
        }

        public String getName() {
            if (getUserObject() != null) {
                return getUserObject().getName();
            }
            return null;
        }
    public void setDefLocation(String location) {
        if (getUserObject() != null) {
            getUserObject().setDefLocation(location);
        }
    }

    public String getDefLocation() {
        if (getUserObject() != null) {
            return getUserObject().getDefLocation();
        }
        return null;
    }

    public void setUseLocation(String location) {
        if (getUserObject() != null) {
            getUserObject().setUseLocation(location);
        }
    }

    public String getUseLocation() {
        if (getUserObject() != null) {
            return getUserObject().getUseLocation();
        }
        return null;
    }*/

    public void setChecked(boolean checked, int index) {
        if (getUserObject() != null) {
            getUserObject()[index].setChecked(checked);
        }
    }

    public boolean isChecked(int index) {
        if (getUserObject() != null) {
            return getUserObject()[index].isChecked();
        }
        return false;
    }

        @Override
        public DefUseData[] getUserObject() {
            return (DefUseData[]) super.getUserObject();
        }


}
