package defuse;

public class DefUseChain {

    private DefUseVariable def;
    private DefUseVariable use;

    public DefUseChain(DefUseVariable def, DefUseVariable use){
        this.def = def;
        this.use = use;
    }

    public String toString(){
        String output = "";
        output += "   DefUse value "+def.getValue() +": Def Method "+def.getMethod()+" ln=" + def.getLinenumber() +" --> Use: Method "+use.getMethod()+" ln=" + use.getLinenumber();
        if(def.getAlias() != null){
            output += ", alias: Def: ln="+def.getAlias().getLinenumber();
        }
        return output;
    }

    public DefUseVariable getUse(){
        return use;
    }

    public DefUseVariable getDef(){
        return def;
    }

}
