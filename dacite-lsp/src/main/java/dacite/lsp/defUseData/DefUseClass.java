package dacite.lsp.defUseData;

import java.util.ArrayList;

public class DefUseClass extends DefUse{
    private ArrayList<DefUseMethod> methods;

    public DefUseClass(String name){
        this.name = name;
        methods = new ArrayList<>();
    }
    public void setMethods(ArrayList<DefUseMethod>  methods){
        this.methods = methods;
    }
    public ArrayList<DefUseMethod>  getMethods(){
        return this.methods;
    }
    public void addMethod(DefUseMethod method){
        methods.add(method);
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
            DefUseClass cl = (DefUseClass) obj;
            return cl.name.equals(this.name);
        }
    }

    public String toString(){
        String output = name + ": ";
        for(DefUseMethod m: methods){
            output += m.toString();
        }
        return output;
    }
}


