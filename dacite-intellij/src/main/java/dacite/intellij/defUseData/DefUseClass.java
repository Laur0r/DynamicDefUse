package dacite.intellij.defUseData;

public class DefUseClass {
    private DefUseMethod[] methods;

    public DefUseClass(){

    }
    public void setMethods(DefUseMethod[] methods){
        this.methods = methods;
    }
    public DefUseMethod[] getMethods(){
        return this.methods;
    }
    public void addMethod(DefUseMethod method, int index){
        if(index < this.methods.length){
            methods[index] = method;
        }
    }
}
