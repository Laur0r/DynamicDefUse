package instrumentation;

import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.util.ArrayList;

public class PrintMessageMethodVisitor extends MethodVisitor {
	private final MethodNode methodNode;

	public PrintMessageMethodVisitor(int access, String name, String desc, String sign, String[] exceptions){
		this(new MethodNode(Opcodes.ASM5, access, name, desc, sign, exceptions));
	}

	private PrintMessageMethodVisitor(MethodNode mv) {
		super(Opcodes.ASM5, mv);
		methodNode = mv;
	}

	@Override
	public void visitParameter(String name, int access){
		mv.visitParameter(name, access);
	}

	@Override
	public void visitInsn(int opcode) {
		switch(opcode) {
			case Opcodes.RETURN: case Opcodes.ARETURN: case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN:
				case Opcodes.DRETURN: case Opcodes.ATHROW:
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "removeLinenumber", "()V", false);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "removeMethod", "()V", false);
		}
		super.visitInsn(opcode);
	}

	@Override
	public void visitVarInsn(int var1, int var2){
		switch (var1){
			case Opcodes.ILOAD:
				mv.visitIntInsn(Opcodes.BIPUSH, var2);
				mv.visitVarInsn(var1, var2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitIntUse", "(II)V", false);
				super.visitVarInsn(var1, var2);
				break;
			case Opcodes.ALOAD:
				mv.visitIntInsn(Opcodes.BIPUSH, var2);
				mv.visitVarInsn(var1, var2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitObjectUse", "(ILjava/lang/Object;)V", false);
				super.visitVarInsn(var1, var2);
				break;
			case Opcodes.ISTORE:
				super.visitVarInsn(var1, var2);
				mv.visitIntInsn(Opcodes.BIPUSH, var2);
				mv.visitVarInsn(Opcodes.ILOAD, var2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitIntDef", "(II)V", false);
				break;
			case Opcodes.ASTORE:
				super.visitVarInsn(var1, var2);
				mv.visitIntInsn(Opcodes.BIPUSH, var2);
				mv.visitVarInsn(Opcodes.ALOAD, var2);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitObjectDef", "(ILjava/lang/Object;)V", false);
				break;
			default: super.visitVarInsn(var1, var2);
		}
	}

	@Override
	public void visitLineNumber(int var1, Label var2) {
		if (this.mv != null) {
			mv.visitIntInsn(Opcodes.BIPUSH, var1);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "addLinenumber", "(I)V", false);
			this.mv.visitLineNumber(var1, var2);
		}

	}

	@Override
	public void visitEnd(){
		mv.visitEnd();
		methodNode.accept(mv);
	}
}
