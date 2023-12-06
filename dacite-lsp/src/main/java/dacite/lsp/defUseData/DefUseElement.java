package dacite.lsp.defUseData;

public abstract class DefUseElement extends DefUseStructure {
    protected int linenumber;
    protected boolean editorHighlight;
    protected int instruction;
    protected String location;

    protected String color;

    public int getLinenumber(){return linenumber;}
    public int getInstruction(){return instruction;}
    public void setInstruction(int instruction){this.instruction=instruction;}
    public boolean isEditorHighlight() {
        return editorHighlight;
    }
    public void setEditorHighlight(boolean editorHighlight) {
        this.editorHighlight = editorHighlight;
    }
    public String getColor(){return color;}
    public void setColor(String color){this.color=color;}
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
