package dacite.lsp.defUseData;

import java.util.ArrayList;

public class DefUseVar extends DefUse {
    private ArrayList<Def> defs;

    public DefUseVar(String name){
        this.name = name;
        defs = new ArrayList<>();
    }

    public void setDefs(ArrayList<Def>  data){
        this.defs = data;
    }
    public ArrayList<Def>  getDefs(){
        return this.defs;
    }
    public void addDef(Def defuse){
        defs.add(defuse);
    }
    public int getLinenumber(){return 0;}
    public int getInstruction(){return 0;}

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        } else {
            DefUseVar method = (DefUseVar) obj;
            return method.name.equals(this.name);
        }
    }

    public String toString(){
        String output = "\n" +name + ": ";
        for(Def d: defs){
            output += d.toString();
        }
        return output;
    }

    /*public void sort(){
        data = quicksort(0, data.size()-1, false);
        int l = 0;
        int r = 0;
        while(r<data.size()){
            DefUseData element = data.get(l);
            int value = Integer.parseInt(element.getDefLocation().substring(element.getDefLocation().lastIndexOf('L')+1));
            for(int i=l+1; i<data.size();i++){
                DefUseData element2 = data.get(i);
                int value2 = Integer.parseInt(element2.getDefLocation().substring(element2.getDefLocation().lastIndexOf('L')+1));
                if(value != value2){
                    r = i-1;
                    break;
                } else if(i == data.size()-1){
                    r = i;
                }
            }
            data = quicksort(l,r,true);
            l = r+1;
            r = r+1;
        }
    }

    public ArrayList<DefUseData> quicksort(int l, int r, boolean dim){
        int t;
        if (l < r) {
            t = divide(l, r, dim);
            quicksort(l, t, dim);
            quicksort(t + 1, r, dim);
        }
        return data;
    }

    public int divide(int l, int r, boolean dim) {
        DefUseData element = data.get((l + r) / 2);
        int pivot;
        if (dim) {
            pivot = Integer.parseInt(element.getUseLocation().substring(element.getUseLocation().lastIndexOf('L') + 1));
        } else {
            pivot = Integer.parseInt(element.getDefLocation().substring(element.getDefLocation().lastIndexOf('L') + 1));
        }
        int i = l - 1;
        int j = r + 1;
        DefUseData elementI;
        int valueI;
        DefUseData elementJ;
        int valueJ;
        while (true) {
            do {
                i++;
                elementI = data.get(i);
                if (dim) {
                    valueI = Integer.parseInt(elementI.getUseLocation().substring(elementI.getUseLocation().lastIndexOf('L') + 1));
                } else {
                    valueI = Integer.parseInt(elementI.getDefLocation().substring(elementI.getDefLocation().lastIndexOf('L') + 1));
                }
            }
            while (valueI < pivot);
            do {
                j--;
                elementJ = data.get(j);
                if (dim) {
                    valueJ = Integer.parseInt(elementJ.getUseLocation().substring(elementJ.getUseLocation().lastIndexOf('L') + 1));
                } else {
                    valueJ = Integer.parseInt(elementJ.getDefLocation().substring(elementJ.getDefLocation().lastIndexOf('L') + 1));
                }
            } while (valueJ > pivot);
            if (i < j) {
                DefUseData a = data.get(i);
                data.set(i, elementJ);
                data.set(j, a);
            } else {
                return j;
            }
        }
    }*/
}
