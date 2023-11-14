package dacite.core.analysis;

import java.util.ArrayDeque;

/**
 * Class representing list of allocations. These are saved as Deque so that the most recent allocation is at the
 * beginning of the list.
 */
public class InterMethodAllocDequeue {
    public ArrayDeque<InterMethodAlloc> interMethodAllocs = new ArrayDeque<>();

    public void addAlloc(InterMethodAlloc alloc){
        interMethodAllocs.addFirst(alloc);
    }
    public InterMethodAlloc contains(InterMethodAlloc alloc){
        for(InterMethodAlloc a: interMethodAllocs){
            if(a.newMethod.equals(alloc.newMethod) && a.currentMethod.equals(alloc.currentMethod) && a.linenumber == alloc.linenumber
            && (a.value == alloc.value || a.value != null && DaciteAnalyzer.isPrimitiveOrWrapper(a.value) && a.value.equals(alloc.value))){
                return a;
            }
        }
        return null;
    }

    public void moveToFirst(InterMethodAlloc alloc){
        interMethodAllocs.remove(alloc);
        interMethodAllocs.addFirst(alloc);
    }
    public int size(){
        return interMethodAllocs.size();
    }
}
