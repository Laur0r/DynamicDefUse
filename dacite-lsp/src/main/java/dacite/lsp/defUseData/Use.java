package dacite.lsp.defUseData;

public class Use extends DefUseElement {
    private int index;

    public Use(String name, String useLocation, int linenumber){
        this.name = name;
        this.location = useLocation;
        this.linenumber = linenumber;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String toString(){
        String output = "\n"+name + ", " + location+" "+instruction+" "+color+" "+editorHighlight;
        return output;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            Use data = (Use) obj;
            return data.name.equals(this.name) &&
                    data.location.equals(this.location) && data.instruction == this.instruction;
        }
    }
}
