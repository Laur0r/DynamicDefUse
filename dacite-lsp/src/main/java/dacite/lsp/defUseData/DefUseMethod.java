package dacite.lsp.defUseData;

import java.util.ArrayList;

public class DefUseMethod extends DefUse{
    private ArrayList<DefUseVar> variables;

    public DefUseMethod(String name){
        this.name = name;
        variables = new ArrayList<>();
    }

    public void setVariables(ArrayList<DefUseVar>  var){
        this.variables = var;
    }
    public ArrayList<DefUseVar> getVariables(){
        return this.variables;
    }
    public void addVariable(DefUseVar defuse){
        variables.add(defuse);
    }
    public int getLinenumber(){return 0;}
    public int getInstruction(){return 0;}

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
        for(DefUseVar d: variables){
            output += d.toString();
        }
        return output;
    }
}
