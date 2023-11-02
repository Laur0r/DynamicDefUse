package dacite.lsp.defUseData;

import java.util.ArrayList;

public class DefUseDef {

    private String name;
    private int numberChains;

    private String defLocation;

    private int defInstruction;
    private ArrayList<DefUseData> data;

    public DefUseDef(String name, String defLocation, int instruction){
        this.name = name;
        this.defLocation = defLocation;
        this.defInstruction = instruction;
        data = new ArrayList<>();
    }

    public void setData(ArrayList<DefUseData>  data){
        this.data = data;
    }
    public ArrayList<DefUseData>  getData(){
        return this.data;
    }
    public void addData(DefUseData defuse){
        data.add(defuse);
    }
    public String getName(){return name;}

    public void setNumberChains(int numberChains) {
        this.numberChains = numberChains;
    }

    public int getNumberChains() {
        return numberChains;
    }

    public int getDefInstruction() {
        return defInstruction;
    }

    public String getDefLocation() {
        return defLocation;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            DefUseDef def = (DefUseDef) obj;
            return def.name.equals(this.name) && def.defLocation.equals(this.defLocation) && def.defInstruction == this.defInstruction;
        }
    }

    public String toString(){
        String output = "\n" +name + ": ";
        for(DefUseData d: data){
            output += d.toString();
        }
        return output;
    }
}
