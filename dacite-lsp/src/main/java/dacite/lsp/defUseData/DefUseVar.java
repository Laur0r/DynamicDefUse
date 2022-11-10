package dacite.lsp.defUseData;

import java.util.ArrayList;

public class DefUseVar {
    private String name;
    private int numberChains;
    private ArrayList<DefUseData> data;

    public DefUseVar(String name){
        this.name = name;
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

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            DefUseVar method = (DefUseVar) obj;
            return method.name.equals(this.name);
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
