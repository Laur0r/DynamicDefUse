package dacite.lsp.defUseData;

public class Use extends DefUse{
    private String useLocation;
    private int index;
    private int useInstruction;
    private boolean checked;

    private int linenumber;
    private boolean editorHighlight;

    public Use(String name, String useLocation, int linenumber){
        this.name = name;
        this.useLocation = useLocation;
        this.linenumber = linenumber;
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

    public int getLinenumber(){return linenumber;}
    public int getInstruction(){return useInstruction;}

    public boolean isEditorHighlight() {
        return editorHighlight;
    }

    public void setEditorHighlight(boolean editorHighlight) {
        this.editorHighlight = editorHighlight;
    }

    public String toString(){
        String output = "\n"+name + ", " + useLocation+" "+useInstruction+" "+color+" "+editorHighlight;
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
            Use data = (Use) obj;
            return data.name.equals(this.name) &&
                    data.useLocation.equals(this.useLocation) && data.useInstruction == this.useInstruction;
        }
    }
}
