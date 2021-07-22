package defuse;

import java.util.ArrayList;

public class DefUseMethod {

    protected String name;
    protected int numberArguments;
    protected ArrayList<DefUseVariable> arguments;

    public DefUseMethod(String name, int number){
        this.name = name;
        this.numberArguments = number;
    }

    public String getName(){return name;}
}
