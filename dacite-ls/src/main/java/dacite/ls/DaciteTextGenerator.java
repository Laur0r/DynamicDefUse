package dacite.ls;

import dacite.lsp.defUseData.transformation.XMLSolution;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import de.wwu.mulib.tcg.testsetreducer.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DaciteTextGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DaciteTextGenerator.class);

    public static List<TextEdit> generateSearchRegions(File project, String classname){
        Map<String, List<String>> invokedMethods = CodeAnalyser.analyseJUnitTest(project,classname);
        List<TextEdit> edits = new ArrayList<>();
        int line = 0;
        String ls = System.lineSeparator();
        String packageName = classname.substring(0, classname.lastIndexOf("."));
        String packageHeader = "package "+packageName+";"+ls;
        line = createTextEditAndIncrementLine(edits, line, packageHeader);

        String importHeader = "import de.wwu.mulib.Mulib;"+ls+ls;
        line = createTextEditAndIncrementLine(edits, line, importHeader);

        String driverComment = "/* This class serves as a search region and prepares the input values for the symbolic execution. */"+ls;
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

        Map<String, List<String>> invokedMethods = CodeAnalyser.analyseJUnitTest(project,packageName +"."+classname);
        int counter = 0;
        for (String methodString : invokedMethods.keySet()) {
            testingClassName = methodString.substring(0, methodString.lastIndexOf("."));
            String testingMethodName = methodString.substring(methodString.lastIndexOf(".") + 1);

            List<TestCase> testCaseList = new ArrayList<>();
            List<XMLSolution> list = DefUseAnalysisProvider.getXmlSolutionsList().getSolutionList(testingMethodName);
            for(XMLSolution solution : solutions){
                if(!list.contains(solution)){
                    continue;
                }
                TcgConfig config = TcgConfig.builder().setTestClassPostfix("Dacite")
                        .setTestSetReducer(new SequentialCombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer())).
                        build();
                TestCase testCase = new TestCase(solution.exceptional,solution.labels,solution.returnValue, DefUseAnalysisProvider.getBitSet(solution),config);
                testCaseList.add(testCase);
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
}
