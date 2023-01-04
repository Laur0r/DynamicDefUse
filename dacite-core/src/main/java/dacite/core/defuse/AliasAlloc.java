package dacite.core.defuse;


import java.util.ArrayList;

/**
 * Class mapping all aliases for one object
 */
public class AliasAlloc {

    // names of different aliases
    public ArrayList<String> varNames;
    //indexes for aliases
    public ArrayList<Integer> varIndexes;

    public AliasAlloc(String vn1, String vn2, int vi1, int vi2){
        this.varNames = new ArrayList<>();
        this.varNames.add(vn1);
        this.varNames.add(vn2);
        this.varIndexes = new ArrayList<>();
        this.varIndexes.add(vi1);
        this.varIndexes.add(vi2);
    }

    public void addAlias(String name, int index){
        varNames.add(name);
        varIndexes.add(index);
    }

}
