package dacite.intellij.defUseData;

import java.util.ArrayList;

public class DefUseMethod {
    private String name;
    private ArrayList<DefUseData> data;

    public DefUseMethod(String name){
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

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            DefUseMethod method = (DefUseMethod) obj;
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
