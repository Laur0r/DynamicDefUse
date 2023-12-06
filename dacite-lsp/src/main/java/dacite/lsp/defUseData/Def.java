package dacite.lsp.defUseData;

import java.util.ArrayList;

public class Def extends DefUseElement {
    private ArrayList<Use> data;

    public Def(String name, String defLocation, int instruction, int linenumber){
        this.name = name;
        this.location = defLocation;
        this.instruction = instruction;
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
            return def.name.equals(this.name) && def.location.equals(this.location) && def.instruction == this.instruction;
        }
    }

    public String toString(){
        String output = "\n" +name + " "+location+": ";
        for(Use d: data){
            output += d.toString();
        }
        return output;
    }
}
