package dacite.lsp.defUseData;

import java.util.ArrayList;

public class Def extends DefUse{
    private String defLocation;
    private int defInstruction;
    private int linenumber;
    private boolean editorHighlight;
    private ArrayList<Use> data;

    public Def(String name, String defLocation, int instruction, int linenumber){
        this.name = name;
        this.defLocation = defLocation;
        this.defInstruction = instruction;
        this.linenumber = linenumber;
        data = new ArrayList<>();
    }

    public void setData(ArrayList<Use>  data){
        this.data = data;
    }
    public ArrayList<Use>  getData(){
        return this.data;
    }
    public void addData(Use defuse){
        data.add(defuse);
    }

    public int getDefInstruction() {
        return defInstruction;
    }

    public String getDefLocation() {
        return defLocation;
    }

    public int getLinenumber(){return linenumber;}
    public int getInstruction(){return defInstruction;}
    public boolean isEditorHighlight() {
        return editorHighlight;
    }

    public void setEditorHighlight(boolean editorHighlight) {
        this.editorHighlight = editorHighlight;
    }
    @Override
    public void setColor(String color) {
        this.color = color;
        for(Use use: data){
            use.setColor(color);
        }
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            Def def = (Def) obj;
            return def.name.equals(this.name) && def.defLocation.equals(this.defLocation) && def.defInstruction == this.defInstruction;
        }
    }

    public String toString(){
        String output = "\n" +name + " "+defLocation+": ";
        for(Use d: data){
            output += d.toString();
        }
        return output;
    }
}
