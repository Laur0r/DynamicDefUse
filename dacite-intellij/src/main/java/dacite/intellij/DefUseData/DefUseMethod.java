package dacite.intellij.DefUseData;

public class DefUseMethod {
    private DefUseData[] data;

    public DefUseMethod(){

    }

    public void setData(DefUseData[] data){
        this.data = data;
    }
    public DefUseData[] getData(){
        return this.data;
    }
    public void addData(DefUseData defuse, int index){
        if(index < this.data.length){
            data[index] = defuse;
        }
    }
}
