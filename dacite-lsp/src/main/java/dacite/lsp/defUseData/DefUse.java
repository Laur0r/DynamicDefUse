package dacite.lsp.defUseData;

public abstract class DefUse {
    protected String name;
    protected int numberChains;

    protected String color;

    public String getName(){return name;}

    public void setNumberChains(int numberChains) {
        this.numberChains = numberChains;
    }

    public int getNumberChains() {
        return numberChains;
    }

    public void addNumberChains(int numberChains) {
        this.numberChains += numberChains;
    }

    public abstract int getLinenumber();
    public abstract int getInstruction();

    public String getColor(){return color;}
    public void setColor(String color){this.color=color;}

}
