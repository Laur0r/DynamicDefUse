package dacite.intellij.DefUseData;

public class DefUseData {
    private String name;
    private String defLocation;
    private String useLocation;
    private boolean checked;

    public DefUseData(String name, String defLocation, String useLocation){
        this.name = name;
        this.defLocation = defLocation;
        this.useLocation = useLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefLocation() {
        return defLocation;
    }

    public void setDefLocation(String defLocation) {
        this.defLocation = defLocation;
    }

    public String getUseLocation() {
        return useLocation;
    }

    public void setUseLocation(String useLocation) {
        this.useLocation = useLocation;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
