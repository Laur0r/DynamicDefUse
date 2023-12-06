package dacite.ls;

import dacite.lsp.defUseData.transformation.XMLSolution;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import de.wwu.mulib.tcg.testsetreducer.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class DaciteTextGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DaciteTextGenerator.class);

    public static List<TextEdit> generateSearchRegions(File project, String classname){
        Map<String, List<String>> invokedMethods = analyseJUnitTest(project,classname);
        List<TextEdit> edits = new ArrayList<>();
        int line = 0;
        String ls = System.lineSeparator();
        String packageName = classname.substring(0, classname.lastIndexOf("."));
        String packageHeader = "package "+packageName+";"+ls;
        line = createTextEditAndIncrementLine(edits, line, packageHeader);

        String importHeader = "import de.wwu.mulib.Mulib;"+ls+ls;
        line = createTextEditAndIncrementLine(edits, line, importHeader);

        String driverComment = "/*" + ls;
        driverComment += " * This class serves as a search region and prepares the input values for the symbolic execution." + ls;
        driverComment += " * Mulib can be used via the static methods in the 'de.wwu.mulib.Mulib' class." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * For instance, Mulib.freeInt() generates a new symbolic int value." + ls;
        driverComment += " * Other methods for initializing symbolic primitives include {freeDouble(), freeFloat(), freeLong(), freeShort(), freeByte(), freeChar(), freeBoolean()}." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * A value can be 'remembered' using Mulib.remember(value, nameToRememberBy). There are also shortcuts such as Mulib.rememberedFreeInt(nameToRememberBy)." + ls;
        driverComment += " * Remembered values should be called 'argX' or 'argXAfterExec'. They can then be used to assert a state before executing a method or after executing a method, respectively." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * If an input parameter is named, the 'X' in the name used for remembering should correspond to the parameter number of the method under test." + ls;
        driverComment += " * Mulib.freeObject(Class-Object) can be used to either initialize an object the fields of which are lazily initialized to symbolic values or symbolic arrays." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * Exemplary uses are: " + ls;
        driverComment += " *  (1) Mulib.freeObject(A.class), which creates an instance of type A for which all fields are symbolic;" + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " *  (2) Mulib.freeObject(int[].class) which creates an array for which all elements are symbolic values and for which the length has not yet been determined." + ls;
        driverComment += " *  (3) Mulib.freeObject(A[][].class) which creates an array of arrays with elements of type A, all of which are symbolic." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * Note that no constructor is executed while initializing these values." + ls;
        driverComment += " * Via Mulib.assume(constraint) constraints can be added. For instance, given a symbolic int i, Mulib.assume(i < 42) will assure that i never is equal or larger than 42." + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);
        driverComment = " * The value range of primitive values can be set immediately using, e.g., Mulib.freeInt(lowerBound, upperBound)." + ls;
        driverComment += "*/" + ls;
        line = createTextEditAndIncrementLine(edits, line, driverComment);

        String classHeader = "public class DaciteSymbolicDriverFor" + classname.substring(classname.lastIndexOf('.') + 1) + " {"+ls;
        line = createTextEditAndIncrementLine(edits, line, classHeader);
        int counter = 0;
        for (Map.Entry<String, List<String>> entry : invokedMethods.entrySet()) {
            List<String> parameters = entry.getValue().subList(2,entry.getValue().size());
            String returnType = entry.getValue().get(1);
            String staticRef = entry.getValue().get(0);
            final String method = entry.getKey();
            String indent = "    ";

            String m = indent + "public static "+returnType+" dacite_symbolic_driver_"+method.substring(method.lastIndexOf(".")+1)+"() {"+ls;
            line = createTextEditAndIncrementLine(edits, line, m);
            String commentInput = indent.repeat(2) + "/* Input values */"+ls;
            line = createTextEditAndIncrementLine(edits, line, commentInput);


            boolean isNonStaticMethod = staticRef.equals("object") && method.contains(".");
            final String objName = isNonStaticMethod ? "arg0" : null;
            String[] parameterNames = new String[parameters.size()];
            int parameterNumber = isNonStaticMethod ? 1 : 0;
            for (int i = 0; i < parameterNames.length; i++) {
                parameterNames[i] = "arg" + parameterNumber;
                parameterNumber++;
            }
            boolean containsArray = false;
            for(int i = 0; i < parameters.size(); i++){
                String parameter = parameters.get(i);
                String p = indent.repeat(2) + parameter + " " +  parameterNames[i];
                String mulibRememberPrefix = " = Mulib.rememberedFree";
                switch (parameter) {
                    case "int": p+= mulibRememberPrefix + "Int(\""+parameterNames[i]+"\");";break;
                    case "double": p+= mulibRememberPrefix + "Double(\""+parameterNames[i]+"\");";break;
                    case "byte":p+= mulibRememberPrefix + "Byte(\""+parameterNames[i]+"\");";break;
                    case "boolean":p+= mulibRememberPrefix + "Boolean(\""+parameterNames[i]+"\");";break;
                    case "short":p+= mulibRememberPrefix + "Short(\""+parameterNames[i]+"\");";break;
                    case "long":p+= mulibRememberPrefix + "Long(\""+parameterNames[i]+"\");";break;
                    case "char": p+= mulibRememberPrefix + "Char(\""+parameterNames[i]+"\");"; break;
                    default: p+= mulibRememberPrefix + "Object(\""+parameterNames[i]+"\", " + parameter + ".class);";
                }
                if(parameter.contains("[")){
                    containsArray = true;
                }
                p += ls;
                line = createTextEditAndIncrementLine(edits, line, p);
            }

            if(containsArray) {
                String arrayComment = indent.repeat(2)+"// To restrict the array size use: Mulib.assume(array.length<...);"+ls;
                line = createTextEditAndIncrementLine(edits, line, arrayComment);
            }

            final String methodCall;
            if (isNonStaticMethod) {
                String namedClass = method.substring(0, method.lastIndexOf("."));
                String object =
                        String.format("%s%s %s = Mulib.rememberedFreeObject(\"%s\", %s.class);", indent.repeat(2), namedClass, objName, objName, namedClass) + ls;
                line = createTextEditAndIncrementLine(edits, line, object);
                methodCall = objName + "." +method.substring(method.lastIndexOf(".")+1);
            } else {
                methodCall = method;
            }
            StringBuilder methodS = new StringBuilder(indent.repeat(2));
            if(!returnType.equals("void")){
                methodS.append(returnType).append(" ").append("r0 = ");
            }

            methodS.append(methodCall).append("(");
            int argStart = isNonStaticMethod ? 1 : 0;
            for(int i=argStart; i<(parameters.size()+argStart);i++){
                methodS.append("arg").append(i).append(",");
            }
            if(!parameters.isEmpty()){
                methodS = new StringBuilder(methodS.substring(0, methodS.length() - 1));
            }
            methodS.append(");").append(ls);
            line = createTextEditAndIncrementLine(edits, line, methodS.toString());
            for (int i = 0; i < parameters.size(); i++) {
                // Remember state of input-object after executing method
                String parameterType = parameters.get(i);
                String parameterName = parameterNames[i];
                String rememberCall = null;
                switch (parameterType) {
                    case "int":
                    case "double":
                    case "byte":
                    case "boolean":
                    case "short":
                    case "long":
                    case "char": break;
                    default: rememberCall = String.format("%sMulib.remember(%s, \"%sAfterExec\");%s", indent.repeat(2), parameterName, parameterName, ls);
                }
                if (rememberCall != null) {
                    line = createTextEditAndIncrementLine(edits, line, rememberCall);
                }
            }
            if (isNonStaticMethod) {
                String rememberObj = String.format("%sMulib.remember(%s, \"%sAfterExec\");%s", indent.repeat(2), objName, objName, ls);
                line = createTextEditAndIncrementLine(edits, line, rememberObj);
            }
            String end = "";
            if(!returnType.equals("void")){
                end = indent.repeat(2) + "return r0;" + ls;
            }
            end = end + indent + "}" + ls;
            line = createTextEditAndIncrementLine(edits, line, end);
            counter ++;
        }
        String finalEnd = "}"+ls;
        createTextEditAndIncrementLine(edits, line, finalEnd);
        return edits;
    }

    public static int createTextEditAndIncrementLine(List<TextEdit> edits, int line, String object) {
        Range range = new Range();
        range.setStart(new Position(line,0));
        range.setEnd(new Position(line, object.length()));
        TextEdit textEdit2 = new TextEdit(range,object);
        edits.add(textEdit2);
        return line + 2;
    }

    public static List<Either<TextDocumentEdit, ResourceOperation>> generateTestCases(File project, String packageName, String classname, String uri){
        List<XMLSolution> solutions = DefUseAnalysisProvider.getSolutions();
        String testingClassName = "";
        List<TextEdit> testEdits = new ArrayList<>();
        List<Either<TextDocumentEdit, ResourceOperation>> changes = new ArrayList<>();

        Map<String, List<String>> invokedMethods = analyseJUnitTest(project,packageName +"."+classname);
        int counter = 0;
        for (String methodString : invokedMethods.keySet()) {
            testingClassName = methodString.substring(0, methodString.lastIndexOf("."));
            String testingMethodName = methodString.substring(methodString.lastIndexOf(".") + 1);

            List<TestCase> testCaseList = new ArrayList<>();
            List<XMLSolution> list = DefUseAnalysisProvider.getXmlSolutionsList().getSolutionList(testingMethodName);
            for(XMLSolution solution : solutions){
                if(list == null || !list.contains(solution)){
                    continue;
                }
                TcgConfig config = TcgConfig.builder().setTestClassPostfix("Dacite")
                        .setTestSetReducer(new SequentialCombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer())).
                        build();
                TestCase testCase = new TestCase(solution.exceptional,solution.labels,solution.returnValue, DefUseAnalysisProvider.getBitSet(solution),config);
                testCaseList.add(testCase);
            }
            if(testCaseList.size()==0){
                continue;
            }

            Class<?> myclass = null;
            try {
                URL url = project.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
                myclass = classLoader.loadClass(packageName + "." + testingClassName);
            } catch (ClassNotFoundException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
            Method[] methods = myclass.getDeclaredMethods();
            Method methodUnderTest = null;
            for (Method method : methods) {
                if (method.getName().equals(testingMethodName)) {
                    methodUnderTest = method;
                }
            }
            TestCases testCases = new TestCases(testCaseList, methodUnderTest);
            TcgConfig config = TcgConfig.builder().setTestClassPostfix("Dacite").
                    setTestSetReducer(
                            new CompetingTestSetReducer(
                                    new SequentialCombinedTestSetReducer(
                                            new SimpleForwardsTestSetReducer(),
                                            new SimpleBackwardsTestSetReducer()),
                                    new SimpleGreedyTestSetReducer()
                            )).build();
            TestCasesStringGenerator tcg = new TestCasesStringGenerator(testCases, config);
            String test = tcg.generateTestClassStringRepresentation();
            createTextEditAndIncrementLine(testEdits, 0, test);

            uri += "/Test"+testingClassName + "Dacite"+counter+".java";
            CreateFile createFile = new CreateFile(uri, new CreateFileOptions(true,false));

            TextDocumentEdit documentEdit = new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri,1), testEdits);
            changes.add(Either.forRight(createFile));
            changes.add(Either.forLeft(documentEdit));
            counter++;
        }
        return changes;
    }

    public static Map<String, List<String>> analyseJUnitTest(File project, String classname){
        String packageName = classname.substring(0, classname.lastIndexOf(".")).replace(".","/");
        ClassReader reader;
        try {
            //logger.info(classname);
            URL url = project.toURI().toURL();
            logger.info(String.valueOf(url));
            URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
            InputStream input = classLoader.getResourceAsStream(classname.replace('.', '/') + ".class");
            //logger.info(input.toString());
            reader = new ClassReader(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        Map<String, List<String>> invokedMethods = new HashMap<>();
        for(MethodNode mnode : classNode.methods) {
            if (mnode.visibleAnnotations != null) {
                for (AnnotationNode an : mnode.visibleAnnotations) {
                    if (an.desc.equals("Lorg/junit/Test;")) {
                        InsnList insns = mnode.instructions;
                        Iterator<AbstractInsnNode> j = insns.iterator();
                        while (j.hasNext()) {
                            AbstractInsnNode in = j.next();
                            if (in instanceof MethodInsnNode) {
                                MethodInsnNode methodins = (MethodInsnNode) in;
                                if(methodins.owner.contains(packageName) && !methodins.name.equals("<init>")){
                                    String name = methodins.owner + "." + methodins.name;
                                    if(methodins.owner.contains("/")){
                                        name = methodins.owner.substring(methodins.owner.lastIndexOf("/")+1)+"." + methodins.name;
                                    }
                                    Type[] types = Type.getArgumentTypes(methodins.desc);
                                    List<String> list = new ArrayList<>();
                                    if(methodins.getOpcode() == Opcodes.INVOKESTATIC){
                                        list.add("static");
                                    } else if(methodins.getOpcode() == Opcodes.INVOKEINTERFACE){
                                        list.add("interface");
                                    } else {
                                        list.add("object");
                                    }
                                    String returnType = Type.getReturnType(methodins.desc).getClassName();
                                    list.add(returnType);
                                    List<String> list2 = Arrays.stream(types).map(Type::getClassName).collect(Collectors.toList());
                                    list.addAll(list2);
                                    invokedMethods.put(name, list);
                                }
                            }
                        }
                    }
                }
            }
        }
        return invokedMethods;
    }
}
