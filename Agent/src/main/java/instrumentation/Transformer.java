package instrumentation;

// import defuse.ParameterCollector;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;



import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
		if ("DefUseMain".equals(className) || ("Increment").equals(className)) {
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
				int linenumber = 0;
				if ("<init>".equals(mnode.name) || "<clinit>".equals(mnode.name)) {
					continue;
				}
				System.out.println(mnode.name);
				InsnList insns = mnode.instructions;
				if (insns.size() == 0) {
					continue;
				}
				InsnList methodStart = new InsnList();
				Type[] types = Type.getArgumentTypes(mnode.desc);
				int typeindex = 0;
				for(int i =0; i< mnode.localVariables.size()*2; i++) {
					if (mnode.localVariables.size() < i || typeindex >= types.length) {
						break;
					}
					LocalVariableNode localVariable = null;
					for(LocalVariableNode lv: mnode.localVariables){
						if(lv.index == i){
							localVariable = lv;
							break;
						}
					}

					if (localVariable != null && Type.getType(localVariable.desc).equals(types[typeindex])) {
						boxing(types[typeindex], localVariable.index, methodStart, true);
						methodStart.add(new IntInsnNode(Opcodes.BIPUSH, localVariable.index));
						methodStart.add(new LdcInsnNode(mnode.name));
						methodStart.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitParameter", "(Ljava/lang/Object;ILjava/lang/String;)V", false));
						if(types[typeindex] == Type.DOUBLE_TYPE || types[typeindex] == Type.LONG_TYPE){
							i++;
						}
						typeindex++;

					}
				}
				AbstractInsnNode firstIns = null;
				Iterator<AbstractInsnNode> j = insns.iterator();
				while (j.hasNext()) {
					AbstractInsnNode in = j.next();
					if(in.getPrevious() == null && firstIns == null){
						firstIns = in;
					}
					int op = in.getOpcode();
					if (in instanceof VarInsnNode) {
						VarInsnNode varins = (VarInsnNode) in;
						if(op == Opcodes.ILOAD || op == Opcodes.LLOAD || op == Opcodes.FLOAD ||
								op == Opcodes.DLOAD || op == Opcodes.ALOAD){
							InsnList il = new InsnList();
							Type varType = getTypeFromOpcode(op);
							boxing(varType, varins.var, il, true);
							//il.add(new VarInsnNode(Opcodes.ALOAD, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitUse", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						} else if(op == Opcodes.ISTORE || op == Opcodes.LSTORE || op == Opcodes.FSTORE ||
								op == Opcodes.DSTORE || op == Opcodes.ASTORE){
							InsnList il = new InsnList();
							Type varType = getTypeFromOpcode(op);
							boxing(varType, varins.var, il, true);
							il.add(new IntInsnNode(Opcodes.BIPUSH, varins.var));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "visitDef", "(Ljava/lang/Object;IILjava/lang/String;)V", false));
							insns.insert(in, il);
						}
					} else if(in instanceof LineNumberNode){
						LineNumberNode lineins = (LineNumberNode) in;
						linenumber = lineins.line;
					} else if(in instanceof MethodInsnNode) {
						MethodInsnNode methodins = (MethodInsnNode) in;
						Type[] parameterTypes = Type.getArgumentTypes(methodins.desc);
						if(parameterTypes.length == 1){
							InsnList il = new InsnList();
							if(parameterTypes[0] == Type.DOUBLE_TYPE || parameterTypes[0] == Type.LONG_TYPE){
								il.add(new InsnNode(Opcodes.DUP2));
							} else {
								il.add(new InsnNode(Opcodes.DUP));
							}
							boxing(parameterTypes[0], 0, il, false);
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new LdcInsnNode(methodins.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "registerInterMethod", "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;)V", false));
							insns.insertBefore(methodins, il);
						} else if(parameterTypes.length != 0){
							InsnList il = new InsnList();
							int index  = mnode.maxLocals + 1;
							il.add(new IntInsnNode(Opcodes.SIPUSH, parameterTypes.length));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/ParameterCollector", "setParameter", "(I)V", false));
							for(int i = parameterTypes.length-1; i >= 0; i--){
								Type type = parameterTypes[i];
								il.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
								boxing(type, index, il, true);
								il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/ParameterCollector", "push", "(Ljava/lang/Object;)V", false));
								if(type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
									index = index + 2;
								} else {
									index++;
								}
							}
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/ParameterCollector", "getParameters", "()[Ljava/lang/Object;", false));
							il.add(new IntInsnNode(Opcodes.BIPUSH, linenumber));
							il.add(new LdcInsnNode(mnode.name));
							il.add(new LdcInsnNode(methodins.name));
							il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "defuse/DefUseAnalyser", "registerInterMethod", "([Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;)V", false));
							for(Type type: parameterTypes){
								if(type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
									index = index - 2;
								} else {
									index--;
								}
								il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
							}
							insns.insertBefore(methodins, il);
						}
					}
				}
				insns.insertBefore(firstIns, methodStart);
				//mn.maxStack += 4;
			}
			//writer.visitEnd();
			//System.out.println(writer.p.getText());
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
			try{
				node.accept(writer);
			} catch(Exception e){
				e.printStackTrace();
			}
			File outputfile = new File("Increment.class");
			try{
				OutputStream fos = new FileOutputStream(outputfile);
				fos.write(writer.toByteArray());
				fos.flush();
				fos.close();
			} catch (Exception e){
				e.printStackTrace();
			}
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

	protected Type getTypeFromOpcode(int op){
		switch (op){
			case Opcodes.ILOAD:
			case Opcodes.ISTORE:
				return Type.INT_TYPE;
			case Opcodes.LLOAD:
			case Opcodes.LSTORE:
				return Type.LONG_TYPE;
			case Opcodes.FLOAD:
			case Opcodes.FSTORE:
				return Type.FLOAT_TYPE;
			case Opcodes.DLOAD:
			case Opcodes.DSTORE:
				return Type.DOUBLE_TYPE;
			default: return Type.getType("Ljava/lang/Object;");
		}
	}

	protected void storeDepType(Type type, InsnList il){

	}

}
