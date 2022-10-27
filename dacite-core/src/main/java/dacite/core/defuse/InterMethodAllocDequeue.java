package dacite.core.defuse;

import java.util.ArrayDeque;

public class InterMethodAllocDequeue {
    public ArrayDeque<InterMethodAlloc> interMethodAllocs = new ArrayDeque<>();

    public void addAlloc(InterMethodAlloc alloc){
        interMethodAllocs.addFirst(alloc);
    }
    public InterMethodAlloc contains(InterMethodAlloc alloc){
        for(InterMethodAlloc a: interMethodAllocs){
            if(a.newMethod.equals(alloc.newMethod) && a.currentMethod.equals(alloc.currentMethod) && a.linenumber == alloc.linenumber
            && (a.value == alloc.value || a.value != null && isPrimitiveOrWrapper(a.value) && a.value.equals(alloc.value))){
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

    protected boolean isPrimitiveOrWrapper(Object obj){
        Class<?> type = obj.getClass();
        return type.isPrimitive() || type == Double.class || type == Float.class || type == Long.class
                || type == Integer.class || type == Short.class || type == Character.class
                || type == Byte.class || type == Boolean.class || type == String.class;
    }
}
