package dacite.core.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.commons.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class Transformer implements ClassFileTransformer {

	static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private String dir;
	private String classname;

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		Thread th = Thread.currentThread();
		if (className.startsWith(dir) && !className.contains("DaciteSymbolicDriver")) {
			logger.info("transformed "+className);
			classname = className;
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);

			node = transformBasis(node, reader);

			byte[] output = null;
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
			try{
				node.accept(writer);
				output = writer.toByteArray();
			} catch(Exception e){
				e.printStackTrace();
			}

			File outputfile = new File("/home/l_troo01/Development/Forschung/ByteCode/test.class");
			try{
				OutputStream fos = new FileOutputStream(outputfile);
				fos.write(output);
				fos.flush();
				fos.close();
			} catch (Exception e){
				e.printStackTrace();
			}

			//logger.info("Transformer has class written");
			return output;
		}

		return null;
	}

	public void transformSymbolic(String input, Map<String, String> remap, String path) throws IOException {
		ClassReader reader = new ClassReader(input);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		node = transformBasis(node, reader);

		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
		String newName = node.name.substring(0,node.name.lastIndexOf("/")) + "/dacite_"+node.name.substring(node.name.lastIndexOf("/")+1);
		SimpleRemapper mapper = new SimpleRemapper(remap);
		ClassRemapper remapper = new ClassRemapper(writer, mapper);
		File outputfile = new File(path+newName+".class");
		try{
			node.accept(remapper);
			OutputStream fos = new FileOutputStream(outputfile);
			fos.write(writer.toByteArray());
			fos.flush();
			fos.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	protected ClassNode transformBasis(ClassNode node, ClassReader reader){
		for(MethodNode mnode : node.methods){
			int linenumber = 0;
			InsnList insns = mnode.instructions;
			if (insns.size() == 0) {
				continue;
			}
			if (mnode.name.equals("<init>")) {
				//continue;
			}

			AbstractInsnNode firstIns = insns.getFirst();
			Iterator<AbstractInsnNode> j = insns.iterator();
			int index = 0;
			int firstLinenumber = 0;
			while (j.hasNext()) {
				AbstractInsnNode in = j.next();
				int op = in.getOpcode();
				if (in instanceof VarInsnNode) {
					VarInsnNode varins = (VarInsnNode) in;
					String varname = "";
					for(LocalVariableNode lvariable :mnode.localVariables){
						if(lvariable.index == varins.var){
							varname = lvariable.name;
							break;
						}
					}
					InsnList il = instrumentVarInsn(varins, mnode, varname, op, linenumber, index);
					index++;
					if(il != null){
						insns.insert(in, il);
					}
				} else if(in instanceof InsnNode) {
					InsnList[] ilarray = instrumentInsn(mnode, op, linenumber, index);
					if(ilarray[0] != null) {
						insns.insertBefore(in, ilarray[0]);
					}
					if(ilarray[1] != null) {
						insns.insert(in, ilarray[1]);
					}
					index++;
				} else if (in instanceof FieldInsnNode) {
					FieldInsnNode fieldins = (FieldInsnNode) in;
					InsnList[] ilarray = instrumentFieldInsn(fieldins, classname+"."+mnode.name, op, linenumber, index);
					if(ilarray[0] != null) {
						insns.insertBefore(in, ilarray[0]);
					}
					if(ilarray[1] != null) {
						insns.insert(in, ilarray[1]);
					}
					index++;
				} else if(in instanceof IincInsnNode){
					IincInsnNode incIns = (IincInsnNode) in;
					InsnList il = new InsnList();
					String varname = "";
					for(LocalVariableNode lvariable :mnode.localVariables){
						if(lvariable.index == incIns.var){
							varname = lvariable.name;
							break;
						}
					}
					boxing(Type.INT_TYPE, incIns.var, il, true);
					il.add(new LdcInsnNode(incIns.var));
					il.add(new LdcInsnNode(linenumber));
					il.add(new LdcInsnNode(index));
					il.add(new LdcInsnNode(classname+"."+mnode.name));
					il.add(new LdcInsnNode(varname));
					il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitUse", "(Ljava/lang/Object;IIILjava/lang/String;Ljava/lang/String;)V", false));
					insns.insertBefore(in, il);
					boxing(Type.INT_TYPE, incIns.var, il, true);
					il.add(new LdcInsnNode(incIns.var));
					il.add(new LdcInsnNode(linenumber));
					il.add(new LdcInsnNode(index));
					il.add(new LdcInsnNode(classname+"."+mnode.name));
					il.add(new LdcInsnNode(varname));
					il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitDef", "(Ljava/lang/Object;IIILjava/lang/String;Ljava/lang/String;)V", false));
					insns.insert(in, il);
					index++;
				} else if(in instanceof LineNumberNode){
					LineNumberNode lineins = (LineNumberNode) in;
					linenumber = lineins.line;
					if(firstLinenumber == 0){
						firstLinenumber = linenumber-1;
					}
					index = 0;
				} else if(in instanceof MethodInsnNode) {
					MethodInsnNode methodins = (MethodInsnNode) in;
					InsnList il = instrumentMethodInsn(methodins, mnode, linenumber);
					if(il != null){
						insns.insertBefore(in, il);
					}
				}
			}
			// Register method Parameter for DefUse by aligning first local variables with parameter types
			InsnList methodStart = new InsnList();
			InsnList methodintermediate = new InsnList();
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
					boxing(types[typeindex], localVariable.index, methodintermediate, true);
					methodintermediate.add(new LdcInsnNode(localVariable.index));
					methodintermediate.add(new LdcInsnNode(firstLinenumber));
					methodintermediate.add(new LdcInsnNode(classname+"."+mnode.name));
					methodintermediate.add(new LdcInsnNode(localVariable.name));
					methodintermediate.add(new IntInsnNode(Opcodes.BIPUSH, typeindex));
					methodintermediate.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitParameter", "(Ljava/lang/Object;IILjava/lang/String;Ljava/lang/String;I)V", false));
					if(types[typeindex] == Type.DOUBLE_TYPE || types[typeindex] == Type.LONG_TYPE){
						i++;
					}
					typeindex++;
					if(methodStart.size() == 0){
						methodStart.insert(methodintermediate);
					} else {
						methodStart.insertBefore(methodStart.get(0), methodintermediate);
					}
					methodintermediate.clear();
				}
			}
			insns.insertBefore(firstIns, methodStart);
		}

		return node;
	}

	/**
	 * Convert primitive types to Class types. Necessary to avoid individual methods for all types for the DefUseAnalyser.
	 * Has the option to include loading of the variable to the stack before converting it. Otherwise it is assumed that
	 * the variable is already on top of the stack.
	 * @param type original type of the variable
	 * @param varIndex index of the local variable (if it is stored as local variable)
	 * @param il list of instructions to append this transformation
	 * @param withLoad option whether the laod should be included, true for include, false only conversion
	 */
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

	/**
	 * Get type of variable depending on instruction opcode
	 * @param op instruction opcode
	 * @return Type of variable
	 */
	protected Type getTypeFromOpcode(int op){
		switch (op){
			case Opcodes.ILOAD:
			case Opcodes.ISTORE:
			case Opcodes.IALOAD:
			case Opcodes.IASTORE:
				return Type.INT_TYPE;
			case Opcodes.LLOAD:
			case Opcodes.LSTORE:
			case Opcodes.LALOAD:
			case Opcodes.LASTORE:
				return Type.LONG_TYPE;
			case Opcodes.FLOAD:
			case Opcodes.FSTORE:
			case Opcodes.FALOAD:
			case Opcodes.FASTORE:
				return Type.FLOAT_TYPE;
			case Opcodes.DLOAD:
			case Opcodes.DSTORE:
			case Opcodes.DALOAD:
			case Opcodes.DASTORE:
				return Type.DOUBLE_TYPE;
			default: return Type.getType("Ljava/lang/Object;");
		}
	}

	/**
	 * Instrument Variable Instructions (Load and Store of variables). Includes pushing relevant parameter on the operating
	 * stack and calling the DefUseAnalyser
	 * @param varins the variable instruction
	 * @param mnode name of the current method
	 * @param op operating instruction as int
	 * @param linenumber current linenumber of source code
	 * @return instrumented instructions
	 */
	protected InsnList instrumentVarInsn(VarInsnNode varins, MethodNode mnode, String variableName, int op, int linenumber, int index){
		if((op == Opcodes.ILOAD || op == Opcodes.LLOAD || op == Opcodes.FLOAD ||
				op == Opcodes.DLOAD || op == Opcodes.ALOAD ||// && !mnode.name.equals("<init>")) ||
				op == Opcodes.ISTORE || op == Opcodes.LSTORE || op == Opcodes.FSTORE ||
				op == Opcodes.DSTORE || op == Opcodes.ASTORE)) {
			if(op == Opcodes.ALOAD && mnode.name.equals("<init>") && (variableName.equals("this") || variableName.equals(""))){
				return null;
			}
			String methodName = classname+"."+mnode.name;
			InsnList il = new InsnList();
			Type varType = getTypeFromOpcode(op);
			for(LocalVariableNode lv: mnode.localVariables){
				if(lv.index == varins.var){
					Type varType2 = Type.getType(lv.desc);
					if(varType == Type.INT_TYPE && (varType2 == Type.BYTE_TYPE || varType2 == Type.BOOLEAN_TYPE || varType2 == Type.CHAR_TYPE)){
						varType=varType2;
					}
					if(varType2.getSort() != varType.getSort() ){
						if(varType2.getSort() == Type.ARRAY){
							if(varType.getSort() != Type.OBJECT){
								variableName = "";
							}
						} else if(varType.getSort() == Type.ARRAY){
							if(varType2.getSort() != Type.OBJECT) {
								variableName = "";
							}
						} else if(varType2.getSort() == Type.OBJECT){
							if(varType.getSort() != Type.ARRAY){
								variableName = "";
							}
						} else if(varType.getSort() == Type.OBJECT){
							if(varType2.getSort() != Type.ARRAY){
								variableName = "";
							}
						} else {
							variableName = "";
						}
					}
					break;
				}
			}

			boxing(varType, varins.var, il, true);
			il.add(new LdcInsnNode(varins.var));
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(index));
			il.add(new LdcInsnNode(methodName));
			il.add(new LdcInsnNode(variableName));
			if (op == Opcodes.ILOAD || op == Opcodes.LLOAD || op == Opcodes.FLOAD ||
					op == Opcodes.DLOAD || op == Opcodes.ALOAD) {
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitUse", "(Ljava/lang/Object;IIILjava/lang/String;Ljava/lang/String;)V", false));
			} else {
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitDef", "(Ljava/lang/Object;IIILjava/lang/String;Ljava/lang/String;)V", false));
			}
			return il;
		}
		return null;
	}

	/**
	 * Instrument Simple Instructions (Load and Store of array elements and return statements). Includes pushing relevant
	 * parameter on the operating stack and calling the DefUseAnalyser.
	 * @param mnode the node of the current method
	 * @param op operating instruction as int
	 * @param linenumber current linenumber of source code
	 * @return instrumented instructions as array, first element for instructions before the current instruction,
	 * second element for instructions after
	 */
	protected InsnList[] instrumentInsn(MethodNode mnode, int op, int linenumber, int instruction){
		InsnList[] output = new InsnList[2];
		if(op == Opcodes.IALOAD || op == Opcodes.LALOAD || op == Opcodes.FALOAD ||
				op == Opcodes.DALOAD || op == Opcodes.AALOAD) {
			// load of array element
			InsnList il2 = new InsnList();
			il2.add(new InsnNode(Opcodes.DUP2));
			output[0] = il2;
			InsnList il = new InsnList();
			Type varType = getTypeFromOpcode(op);
			if(varType == Type.DOUBLE_TYPE || varType == Type.LONG_TYPE){
				il.add(new InsnNode(Opcodes.DUP2_X2));
			} else {
				il.add(new InsnNode(Opcodes.DUP_X2));
			}
			boxing(varType, 0, il, false);
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(instruction));
			il.add(new LdcInsnNode(classname +"."+mnode.name));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitArrayUse", "(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/String;)V", false));
			output[1] = il;
		} else if(op == Opcodes.IASTORE || op == Opcodes.LASTORE || op == Opcodes.FASTORE ||
				op == Opcodes.DASTORE || op == Opcodes.AASTORE){
			// store of array element
			// there are three values as parameters on stack -> not possible with dup, needs storing in local variables
			InsnList il = new InsnList();
			int index  = mnode.maxLocals + 1;
			mnode.maxLocals = mnode.maxLocals + 3;
			// storing parameters in local variables
			Type varType1 = getTypeFromOpcode(op);
			il.add(new VarInsnNode(varType1.getOpcode(Opcodes.ISTORE), index));
			if(varType1 == Type.DOUBLE_TYPE || varType1 == Type.LONG_TYPE) {
				index = index + 2;
			} else {
				index++;
			}
			il.add(new VarInsnNode(Opcodes.ISTORE, index));
			index++;
			il.add(new VarInsnNode(Opcodes.ASTORE, index));
			// retrieving parameters for DefUseAnalyser invocation
			int indexEnd = index;
			il.add(new VarInsnNode(Opcodes.ALOAD, index));
			index--;
			il.add(new VarInsnNode(Opcodes.ILOAD, index));
			index--;
			boxing(varType1, index, il, true);
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(instruction));
			il.add(new LdcInsnNode(classname+"."+mnode.name));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitArrayDef", "(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/String;)V", false));
			// retrieving parameters for original store instruction
			il.add(new VarInsnNode(Opcodes.ALOAD, indexEnd));
			indexEnd--;
			il.add(new VarInsnNode(Opcodes.ILOAD, indexEnd));
			indexEnd--;
			il.add(new VarInsnNode(varType1.getOpcode(Opcodes.ILOAD), indexEnd));
			output[0] = il;
		}
		return output;
	}

	/**
	 * Instrument Field Variable Instructions (Load and Store of class variables). Includes pushing relevant parameter
	 * on the operating stack and calling the DefUseAnalyser
	 * @param fieldins the field variable instruction
	 * @param methodName name of the current method
	 * @param op operating instruction as int
	 * @param linenumber current linenumber of source code
	 * @return instrumented instructions as array, first element for instructions before the current instruction,
	 * second element for instructions after
	 */
	protected InsnList[] instrumentFieldInsn(FieldInsnNode fieldins, String methodName, int op, int linenumber, int index){
		InsnList[] output = new InsnList[2];
		// load field variable
		if(op == Opcodes.GETFIELD){
			InsnList il2 = new InsnList();
			il2.add(new InsnNode(Opcodes.DUP));
			output[0] = il2;
			InsnList il = new InsnList();
			Type varType = Type.getType(fieldins.desc);
			// push duplicated value behind above duplicated value so that the different values can be used as parameters
			if(varType == Type.DOUBLE_TYPE || varType == Type.LONG_TYPE){
				il.add(new InsnNode(Opcodes.DUP2_X1));
			} else {
				il.add(new InsnNode(Opcodes.DUP_X1));
			}
			boxing(varType, 0, il, false);
			il.add(new LdcInsnNode(fieldins.owner+"."+fieldins.name));
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(index));
			il.add(new LdcInsnNode(methodName));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitFieldUse", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;IILjava/lang/String;)V", false));
			output[1] = il;
		} else if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC){
			// static field variables behave similar to variable uses and definitions -> no ALOAD necessary
			InsnList il = new InsnList();
			Type varType = Type.getType(fieldins.desc);
			if(varType == Type.DOUBLE_TYPE || varType == Type.LONG_TYPE){
				il.add(new InsnNode(Opcodes.DUP2));
			} else {
				il.add(new InsnNode(Opcodes.DUP));
			}
			boxing(varType, 0, il, false);
			il.add(new LdcInsnNode(fieldins.name));
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(index));
			il.add(new LdcInsnNode(methodName));
			il.add(new LdcInsnNode(fieldins.owner));
			if(op == Opcodes.GETSTATIC){
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitStaticFieldUse", "(Ljava/lang/Object;Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", false));
				output[1] = il;
			} else {
				// Putfield needs instrumentation before instruction otherwise value is no longer on stack
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitStaticFieldDef", "(Ljava/lang/Object;Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V", false));
				output[0] = il;
			}
		} else if(op == Opcodes.PUTFIELD){
			// store field variable
			InsnList il = new InsnList();
			Type varType = Type.getType(fieldins.desc);
			if(varType == Type.DOUBLE_TYPE || varType == Type.LONG_TYPE){
				// TODO
				//il.add(new InsnNode(Opcodes.DUP2));
			} else {
				il.add(new InsnNode(Opcodes.DUP2));
			}
			boxing(varType, 0, il, false);
			il.add(new LdcInsnNode(fieldins.owner+"."+fieldins.name));
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(index));
			il.add(new LdcInsnNode(methodName));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "visitFieldDef", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;IILjava/lang/String;)V", false));
			output[0] = il;
		}
		return output;
	}

	/**
	 * Instrument method invocation instructions. Includes duplicating parameter of invoked method
	 * on the operating stack and calling the DefUseAnalyser
	 * @param methodins the method invocation instruction
	 * @param mnode node of the current method
	 * @param linenumber current linenumber of source code
	 * @return list of instrumented instructions
	 */
	protected InsnList instrumentMethodInsn(MethodInsnNode methodins, MethodNode mnode, int linenumber){
		Type[] parameterTypes = Type.getArgumentTypes(methodins.desc);
		if(parameterTypes.length == 1){
			// if new method has only one parameter, duplicating can be done with dup
			InsnList il = new InsnList();
			if(parameterTypes[0] == Type.DOUBLE_TYPE || parameterTypes[0] == Type.LONG_TYPE){
				il.add(new InsnNode(Opcodes.DUP2));
			} else {
				il.add(new InsnNode(Opcodes.DUP));
			}
			boxing(parameterTypes[0], 0, il, false);
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(classname+"."+mnode.name));
			il.add(new LdcInsnNode(methodins.owner+"."+methodins.name));
			il.add(new IntInsnNode(Opcodes.BIPUSH, 0));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "registerInterMethod", "(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;I)V", false));
			return il;
		} else if(parameterTypes.length != 0){
			// if there are more parameters, duplicating has to be done by storing values in local variables and retrieving values
			InsnList il = new InsnList();
			int index  = mnode.maxLocals + 1;
			il.add(new IntInsnNode(Opcodes.SIPUSH, parameterTypes.length));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/ParameterCollector", "setParameter", "(I)V", false));
			for(int i = parameterTypes.length-1; i >= 0; i--){
				Type type = parameterTypes[i];
				il.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
				boxing(type, index, il, true);
				// to avoid having different methods for each number of parameters, method parameters are collected in array
				il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/ParameterCollector", "push", "(Ljava/lang/Object;)V", false));
				if(type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
					index = index + 2;
				} else {
					index++;
				}
			}
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/ParameterCollector", "getParameters", "()[Ljava/lang/Object;", false));
			il.add(new LdcInsnNode(linenumber));
			il.add(new LdcInsnNode(classname+"."+mnode.name));
			il.add(new LdcInsnNode(methodins.owner+"."+methodins.name));
			il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dacite/core/defuse/DefUseAnalyser", "registerInterMethod", "([Ljava/lang/Object;ILjava/lang/String;Ljava/lang/String;)V", false));
			// retrieving values for original method invocation instruction
			for(Type type: parameterTypes){
				if(type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE) {
					index = index - 2;
				} else {
					index--;
				}
				il.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
			}
			return il;
		}
		return null;
	}

	public void setDir(String dir){
		this.dir = dir;
	}
}
