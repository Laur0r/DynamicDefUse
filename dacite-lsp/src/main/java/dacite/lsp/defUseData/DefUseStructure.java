package dacite.lsp.defUseData;

public abstract class DefUseStructure {
    protected String name;
    protected int numberChains;

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

}
