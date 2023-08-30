package dacite.core;

import dacite.core.defuse.*;
import dacite.core.instrumentation.Transformer;
import dacite.lsp.defUseData.transformation.XMLSolution;
import dacite.lsp.defUseData.transformation.XMLSolutions;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import de.wwu.mulib.tcg.testsetreducer.CombinedTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleBackwardsTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleForwardsTestSetReducer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class SymbolicExec {

    static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void exec(String[] args) throws Exception {
        if(args.length != 3){
            throw new IllegalArgumentException("Not the expected amount of arguments given for SymbolicExec.exec()");
        }
        String projectpath = args[0];
        String packagename = args[1];
        String classname = args[2];
        File file = new File(projectpath+packagename+classname);
        logger.info("file1 "+file.getPath()+" "+file.exists());

        //logger.info(dir);
        File packagedir = new File(projectpath+packagename);
        URL url = null;
        Transformer transformer = new Transformer();
        transformer.setDir(projectpath+packagename.substring(0,packagename.length()-2));
        Map<String,String> remap = new HashMap<>();
        for(File f: packagedir.listFiles()){
            if(!f.isDirectory()){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                //remap.put(packagename+name, packagename+"dacite_"+name);
                if(!f.getName().contains("DaciteSymbolicDriver") && url == null){
                    url = Class.forName(packagename.replace("/",".")+name).getResource(name+".class");
                }
            }
        }
        String sourcePath = url.getPath().substring(0,url.getPath().indexOf(packagename));

        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(file);
        StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
        Iterable<? extends JavaFileObject> javaSource = fileManager.getJavaFileObjectsFromFiles( sourceFileList );
        Iterable<String> options = Arrays.asList("-d", sourcePath, "--release", "11");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, javaSource);
        task.call();

        for(File f: packagedir.listFiles()){
            if(!f.isDirectory()){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                transformer.transformSymbolic(packagename.replace("/",".")+name, remap, sourcePath);
            }
        }

        // Load and instantiate compiled class.
        Class<?> cls = Class.forName(packagename.replace("/",".")+classname.substring(0,classname.indexOf(".")));//+"dacite_"
        Object instance = cls.getDeclaredConstructor().newInstance();
        logger.info(instance.toString());

        MulibConfig.MulibConfigBuilder builder =
                MulibConfig.builder()
                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(sourcePath)
//                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
//                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setSEARCH_MAIN_STRATEGY(SearchStrategy.IDDSAS)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setBUDGET_INCR_ACTUAL_CP(8)
                        .setTRANSF_USE_DEFAULT_MODEL_CLASSES(true)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setBUDGET_GLOBAL_TIME_IN_SECONDS(5)
                        .setBUDGET_FIXED_ACTUAL_CP(32)
                        //.setVALS_SYMSINT_DOMAIN(-10000000, 1000000)
                        //.setBUDGET_MAX_EXCEEDED(150_000)
                        .setCALLBACK_BACKTRACK((a0, a1, a2) -> DefUseAnalyser.resetSymbolicValues())
                        .setCALLBACK_FAIL((a0, a1, a2) -> DefUseAnalyser.resetSymbolicValues())
                        .setCALLBACK_EXCEEDED_BUDGET((a0, a1, a2) -> DefUseAnalyser.resetSymbolicValues())
                        //.setBUDGET_FIXED_ACTUAL_CP(16)
                        .setTRANSF_TREAT_SPECIAL_METHOD_CALLS(true)
                        .setTRANSF_IGNORE_CLASSES(List.of(DefUseAnalyser.class, ParameterCollector.class))
                        .setCALLBACK_PATH_SOLUTION(DefUseAnalyser::resolveLabels)
                ;
        Mulib.getPathSolutions(cls, "driver0", builder); //// TODO Use loop for different driver-methods
        DefUseAnalyser.check();

        logger.info("run through symbolic method");
        DefUseAnalyser.check();
        // write xml file
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = null;
        xof.setProperty("escapeCharacters", false);
        try {
            xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("SymbolicDUCs.xml")));
            xsw.writeStartDocument();
            xsw.writeStartElement("DefUseChains");
            if(DefUseAnalyser.chains.getDefUseChains().size() >0) {
                getClassesOfSolution(DefUseAnalyser.chains.getDefUseChains().get(0).getSolutions());
            }
            List<XMLSolution> solutions = getSolutions(DefUseAnalyser.chains);
            parseSolution(xof, solutions);

            for (DefUseChain chain : DefUseAnalyser.chains.getDefUseChains()) {
                xsw.writeStartElement("DefUseChain");
                xsw.writeStartElement("id");
                xsw.writeCharacters(String.valueOf(chain.getId()));
                xsw.writeEndElement();
                xsw.writeStartElement("def");
                parseDefUseVariable(xsw, chain.getDef());
                xsw.writeEndElement();
                xsw.writeStartElement("use");
                parseDefUseVariable(xsw, chain.getUse());
                xsw.writeEndElement();
                xsw.writeStartElement("solutionIds");
                parseSolutionIds(xsw, chain.getSolutions(), solutions);
                xsw.writeEndElement();
                xsw.writeEndElement();
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();

            /*Set<Class<?>> classes = parseClassesFromSolution();
            classes.add(dacite.lsp.defUseData.transformation.DefUseChains.class);
            classes.add(XMLSolution.class);
            classes.add(XMLSolutions.class);
            logger.info(classes.toString());
            JAXBContext jaxbContext = JAXBContext.newInstance(classes.toArray(new Class[]{}));
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            String test = Files.readString(Path.of("SymbolicDUCs.xml"));
            var chainCollection = (dacite.lsp.defUseData.transformation.DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(test));
            String test2 = Files.readString(Path.of("SymbolicSolutions.xml"));
            XMLSolutions solutionList = (XMLSolutions) jaxbUnmarshaller.unmarshal(new StringReader(test2));
            for(dacite.lsp.defUseData.transformation.DefUseChain ch:chainCollection.getChains()){
                String solutionIds = ch.getSolutionIds();
                String[] array = solutionIds.split(",");
                List<XMLSolution> solutionsChain  = new ArrayList<>();
                for(String a:array){
                    int index = Integer.parseInt(a);
                    solutionsChain.add(solutionList.getXmlSolutions().get(index));
                }
                ch.setSolution(solutionsChain);
            }
            List<TestCase> testCaseList = new ArrayList<>();
            for(XMLSolution solution : solutions){
                TcgConfig config = TcgConfig.builder().setTestClassPostfix("Dacite").build();
                TestCase testCase = new TestCase(solution.exceptional,solution.labels,solution.returnValue, getBitSet(solution, chainCollection),config);
                testCaseList.add(testCase);
            }
            Class<?> myclass = Class.forName("dacite.core.tests.Fibonacci");
            Method[] methods = myclass.getDeclaredMethods();
            Method methodUnderTest = null;
            for(Method method : methods){
                if(method.getName().equals("fibonacci")){
                    methodUnderTest = method;
                }
            }
            TestCases testCases = new TestCases(testCaseList,methodUnderTest);
            TcgConfig config = TcgConfig.builder().setTestClassPostfix("Dacite")
                    .setTestSetReducer(new CombinedTestSetReducer(new SimpleForwardsTestSetReducer(),
                            new SimpleBackwardsTestSetReducer())).build();
            TestCasesStringGenerator tcg = new TestCasesStringGenerator(testCases, config);
            String testing = tcg.generateTestClassStringRepresentation();
            System.out.println("test");*/
        }
        catch (Exception e) {
            logger.info("Unable to write the file: " + e.getMessage());
        }

        sourceFileList = new ArrayList<File>();
        for(File f: packagedir.listFiles()) {
            if (!f.isDirectory()) {
                sourceFileList.add(f);
            }
        }
        javaSource = fileManager.getJavaFileObjectsFromFiles( sourceFileList );
        JavaCompiler.CompilationTask taskRecompile = compiler.getTask(null, fileManager, null, options, null, javaSource);
        taskRecompile.call();

    }

    private static void parseDefUseVariable(XMLStreamWriter xsw, DefUseVariable var){
        try {
            xsw.writeStartElement("linenumber");
            xsw.writeCharacters(String.valueOf(var.getLinenumber()));
            xsw.writeEndElement();
            xsw.writeStartElement("method");
            xsw.writeCharacters(String.valueOf(var.getMethod()));
            xsw.writeEndElement();
            xsw.writeStartElement("variableIndex");
            xsw.writeCharacters(String.valueOf(var.getVariableIndex()));
            xsw.writeEndElement();
            xsw.writeStartElement("instruction");
            xsw.writeCharacters(String.valueOf(var.getInstruction()));
            xsw.writeEndElement();
            xsw.writeStartElement("variableName");
            xsw.writeCharacters(String.valueOf(var.getVariableName()));
            xsw.writeEndElement();
            if(var instanceof DefUseField){
                xsw.writeStartElement("objectName");
                xsw.writeCharacters(String.valueOf(((DefUseField)var).getInstanceName()));
                xsw.writeEndElement();
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private static void parseSolution(XMLOutputFactory xof, List<XMLSolution> solutions){
        try {
            if(solutions.size() != 0){
                XMLStreamWriter xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("SymbolicSolutions.xml")));
                xsw.writeStartDocument();
                xsw.writeStartElement("xmlsolutions");
                Set<Class<?>> classes = parseClassesFromSolution();
                classes.add(XMLSolution.class);
                for(XMLSolution s: solutions){
                    JAXBContext jaxbContextR = JAXBContext.newInstance(classes.toArray(Class[]::new));
                    Marshaller jaxbMarshallerR = jaxbContextR.createMarshaller();
                    jaxbMarshallerR.setProperty(Marshaller.JAXB_FRAGMENT, true);
                    StringWriter swR = new StringWriter();
                    jaxbMarshallerR.marshal(s, swR);
                    xsw.writeCharacters(swR.toString());
                }
                xsw.writeEndElement();
                xsw.writeEndDocument();
                xsw.flush();
                xsw.close();
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseSolutionIds(XMLStreamWriter xsw, List<XMLSolution> solutions, List<XMLSolution> allSolutions) {
        try {
            StringBuilder solutionInd = new StringBuilder();
            for(int i =0; i<solutions.size(); i++){
                XMLSolution s = solutions.get(i);
                if(i != 0){
                    solutionInd.append(",");
                }
                solutionInd.append(allSolutions.indexOf(s));
            }
            xsw.writeCharacters(solutionInd.toString());
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<XMLSolution> getSolutions(DefUseChains chains){
        List<XMLSolution> solutions = new ArrayList<>();
        for(DefUseChain chain :chains.getDefUseChains()){
            List<XMLSolution> solution = chain.getSolutions();
            for(XMLSolution s: solution){
                if(!solutions.contains(s)){
                    solutions.add(s);
                }
            }
        }
        return solutions;
    }

    private static void getClassesOfSolution(List<XMLSolution> solutions){
        try {
            if(solutions.size() != 0){
                XMLSolution solution = solutions.get(0);
                Set<String> classes = new HashSet<>();
                for (Map.Entry<String, Object> o : solution.labels.entrySet()) {
                    if (!(o.getValue() == null)) {
                        Class clazz = o.getValue().getClass();
                        classes.add(clazz.getName());
                        if(!isPrimitiveOrPrimitiveWrapperOrString(clazz)){
                            for(Field f: clazz.getDeclaredFields()){
                                if(!f.getType().isPrimitive()){
                                    classes.add(f.getType().getName());
                                }
                            }
                        }
                    }
                }
                if(solution.returnValue != null){
                    classes.add(solution.returnValue.getClass().getName());
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter("DaciteSolutionClasses.txt"));
                for (String className : classes) {
                    writer.write(className);
                    writer.newLine();
                }
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<Class<?>> parseClassesFromSolution(){
        FileInputStream stream = null;
        Set<Class<?>> classes = new HashSet<>();
        try {
            stream = new FileInputStream("DaciteSolutionClasses.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String strLine;
        ArrayList<String> lines = new ArrayList<String>();
        try {
            while ((strLine = reader.readLine()) != null) {
                // Classloader works only for Objects not arrays
                if(strLine.contains("[") || strLine.equals("int")){
                    classes.add(Class.forName(strLine));
                } else {
                    classes.add(SymbolicExec.class.getClassLoader().loadClass(strLine));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    public static boolean isPrimitiveOrPrimitiveWrapperOrString(Class<?> type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class;
    }

}

