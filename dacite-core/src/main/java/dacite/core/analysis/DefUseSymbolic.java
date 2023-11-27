package dacite.core.analysis;

public class DefUseSymbolic extends DefUseVariable {
    protected long timeRef;

    public DefUseSymbolic(int linenumber, int instruction, int variableIndex, Object value, String method, String varname){
        super(linenumber, instruction, variableIndex, value, method, varname);
        this.timeRef = timeRef;
    }

    public void setTimeRef(long timeRef){
        this.timeRef = timeRef;
    }

    public long getTimeRef(){
        return this.timeRef;
    }
}
