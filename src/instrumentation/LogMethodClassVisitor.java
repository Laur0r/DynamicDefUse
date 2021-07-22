package instrumentation;

import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.Method;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

public class LogMethodClassVisitor extends ClassVisitor {
    private String className;

    public LogMethodClassVisitor(ClassVisitor cv, String pClassName) {
    	super(Opcodes.ASM5, cv);
    	className = pClassName;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
    		String signature, String[] exceptions) {
    	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if(mv != null && (name.contains("ifTest") || name.contains("main"))){
            mv.visitLdcInsn(name);
            Method m = new Method(name, desc);
            int arguments = m.getArgumentTypes().length;
            mv.visitIntInsn(Opcodes.BIPUSH, arguments);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "addMethod", "(Ljava/lang/String;I)V", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "newLinenumber", "()V", false);
            mv = new PrintMessageMethodVisitor(access, name, desc, signature, exceptions);
            mv.visitMaxs(-1,-1);
        }
        return mv;
    }

}
