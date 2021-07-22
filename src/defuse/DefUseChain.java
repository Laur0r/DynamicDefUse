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
        output += "   Method "+def.getMethod()+"/"+use.getMethod()+", Def: ln=" + def.getLinenumber() + ", Use: ln=" + use.getLinenumber();
        return output;
    }

    public DefUseVariable getUse(){
        return use;
    }

    public DefUseVariable getDef(){
        return def;
    }

}
