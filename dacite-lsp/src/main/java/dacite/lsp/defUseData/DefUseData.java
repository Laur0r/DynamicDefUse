package dacite.lsp.defUseData;

public class DefUseData {
    private String name;
    private String defLocation;
    private String useLocation;
    private int index;
    private int useInstruction;
    private int defInstruction;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getUseInstruction() {
        return useInstruction;
    }

    public void setUseInstruction(int instruction) {
        this.useInstruction = instruction;
    }

    public int getDefInstruction() {
        return defInstruction;
    }

    public void setDefInstruction(int instruction) {
        this.defInstruction = instruction;
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

    public String toString(){
        String output = "\n"+name + ", " + defLocation + ", " + useLocation+" "+index+"; ";
        return output;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            DefUseData data = (DefUseData) obj;
            return data.name.equals(this.name) && data.defLocation.equals(this.defLocation) &&
                    data.useLocation.equals(this.useLocation);
        }
    }
}
