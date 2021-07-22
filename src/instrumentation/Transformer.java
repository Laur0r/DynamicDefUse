package instrumentation;

import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicInterpreter;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;
import jdk.internal.org.objectweb.asm.util.CheckClassAdapter;
import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;


import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;

public class Transformer implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		Thread th = Thread.currentThread();
		if ("DefUseMain".equals(className)) {
			/*ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
			//TraceClassVisitor writer = new TraceClassVisitor(new PrintWriter(System.out));
			ClassVisitor visitor = new LogMethodClassVisitor(writer, className);
			reader.accept(visitor, 0);
			//writer.visitEnd();
			//System.out.println(writer.p.getText());
			return writer.toByteArray();
			//return null;*/

			ClassReader reader = new ClassReader(classfileBuffer);
			ClassNode node = new ClassNode();
			//TraceClassVisitor writer = new TraceClassVisitor(new PrintWriter(System.out));
			//ClassVisitor visitor = new LogMethodClassVisitor(writer, className);
			reader.accept(node, 0);
			for(MethodNode mnode : node.methods){
				System.out.println(mnode.name);
				int linenumber = 0;
				if ("<init>".equals(mnode.name) || "<clinit>".equals(mnode.name)) {
					continue;
				}
				InsnList insns = mnode.instructions;
				if (insns.size() == 0) {
					continue;
				}
				InsnList methodStart = new InsnList();
				Type[] types = Type.getArgumentTypes(mnode.desc);
				int typeindex = 0;
				for(int i =0; i< types.length+1; i++) {
					if (mnode.localVariables.size() < i || typeindex >= types.length) {
						break;
					}
					LocalVariableNode localVariable = mnode.localVariables.get(i);
					if (Type.getType(localVariable.desc).equals(types[typeindex])) {
						// TODO long und double nehme zwei Indexes ein -> +2
						boxing(types[typeindex], localVariable.index, methodStart, true);
						methodStart.add(new IntInsnNode(Opcodes.BIPUSH, localVariable.index));
						methodStart.add(new LdcInsnNode(mnode.name));
						methodStart.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitParameter", "(Ljava/lang/Object;ILjava/lang/String;)V", false));
						/*switch (localVariable.desc) {
							case "I":
								methodStart.add(new VarInsnNode(Opcodes.ILOAD, localVariable.index));
								methodStart.add(new IntInsnNode(Opcodes.BIPUSH, localVariable.index));
								methodStart.add(new LdcInsnNode(mnode.name));
								methodStart.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitParameter", "(IILjava/lang/String;)V", false));
								break;
							case "[Ljava/lang/String;":
								methodStart.add(new VarInsnNode(Opcodes.ALOAD, localVariable.index));
								methodStart.add(new IntInsnNode(Opcodes.BIPUSH, localVariable.index));
								methodStart.add(new LdcInsnNode(mnode.name));
								methodStart.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitParameter", "(Ljava/lang/Object;ILjava/lang/String;)V", false));
								break;
						}*/
						typeindex++;
					}
				}
				AbstractInsnNode firstIns = null;
				Iterator<AbstractInsnNode> j = insns.iterator();
				while (j.hasNext()) {
					AbstractInsnNode in = j.next();
					if(in.getPrevious() == null){
						firstIns = in;
					}
					int op = in.getOpcode();
					if (in instanceof VarInsnNode) {
						VarInsnNode varins = (VarInsnNode) in;
						if(op >= Opcodes.ILOAD && op < Opcodes.ISTORE){
							InsnList il = new InsnList();
							// TODO reusing of variable indexes?
							Type varType = Type.getType(mnode.localVariables.get(varins.var).desc);
							boxing(varType, varins.var, il, true);
							//il.add(new VarInsnNode(Opcodes.ALOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitUse", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						} else if(op > Opcodes.SALOAD && op < Opcodes.POP){
							InsnList il = new InsnList();
							// TODO reusing of variable indexes?
							Type varType = Type.getType(mnode.localVariables.get(varins.var).desc);
							boxing(varType, varins.var, il, true);
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitDef", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						}
						/*if(op == Opcodes.ILOAD){
							InsnList il = new InsnList();
							il.add(new VarInsnNode(Opcodes.ILOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitUse", "(IIILjava/lang/String;)V", false));
							insns.insert(in, il);
						} else if(op == Opcodes.ALOAD){
							InsnList il = new InsnList();
							il.add(new VarInsnNode(Opcodes.ALOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitUse", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						} else if(op == Opcodes.ISTORE){
							InsnList il = new InsnList();
							il.add(new VarInsnNode(Opcodes.ILOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitDef", "(IIILjava/lang/String;)V", false));
							insns.insert(in, il);
						} else if(op == Opcodes.ASTORE){
							InsnList il = new InsnList();
							il.add(new VarInsnNode(Opcodes.ALOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitDef", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						}*/
					} else if(in instanceof LineNumberNode){
						LineNumberNode lineins = (LineNumberNode) in;
						linenumber = lineins.line;
					} else if(in instanceof MethodInsnNode) {
						MethodInsnNode methodins = (MethodInsnNode) in;
						Type[] parameterTypes = Type.getArgumentTypes(methodins.desc);
						if(parameterTypes.length == 1){
							InsnList il = new InsnList();
							il.add(new InsnNode(Opcodes.DUP));
							boxing(parameterTypes[0], 0, il, false);
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new LdcInsnNode(methodins.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "registerInterMethod", "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;)V", false));
							insns.insertBefore(methodins, il);
						} else if(parameterTypes.length == 2){

						}
					}
				}
				insns.insertBefore(firstIns, methodStart);
				//mn.maxStack += 4;
			}
			//writer.visitEnd();
			//System.out.println(writer.p.getText());
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		}

		return null;
	}

	protected boolean isPushInstruction(int opcode){
		if(opcode > Opcodes.NOP && opcode < Opcodes.ISTORE){
			return true;
		} else if(opcode > Opcodes.POP2 && opcode < Opcodes.DUP2_X2){
			return true;
		} else if(opcode > Opcodes.SWAP && opcode < Opcodes.IINC){
			return true;
		}
		return false;
	}

	protected void boxing(Type type, int varIndex, InsnList il, boolean withLoad){
		switch (type.getSort()) {
			case Type.BOOLEAN:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
				break;
			case Type.BYTE:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
				break;
			case Type.CHAR:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",false));
				break;
			case Type.SHORT:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;",false));
				break;
			case Type.INT:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",false));
				break;
			case Type.FLOAT:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;",false));
				break;
			case Type.LONG:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false));
				break;
			case Type.DOUBLE:
				if(withLoad) il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), varIndex));
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
				break;
			default: if(withLoad) il.add(new VarInsnNode(Opcodes.ALOAD, varIndex));
		}
	}

}
