package dacite.core;

import dacite.core.defuse.*;
import dacite.core.instrumentation.Transformer;
import dacite.lsp.defUseData.transformation.XMLSolution;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;
import de.wwu.mulib.tcg.TestCasesStringGenerator;
import jakarta.xml.bind.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.tools.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
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
        Iterable<String> options = Arrays.asList("-d", sourcePath);
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
                        .setGLOBAL_SEARCH_STRATEGY(SearchStrategy.IDDSAS)
                        .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING)
                        .setINCR_ACTUAL_CP_BUDGET(16)
                        .setTRANSF_USE_DEFAULT_MODEL_CLASSES(true)
                        .setHIGH_LEVEL_FREE_ARRAY_THEORY(true)
                        //.setSECONDS_PER_INVOCATION(5)
                        .setFIXED_ACTUAL_CP_BUDGET(16)
                        .setTRANSF_TREAT_SPECIAL_METHOD_CALLS(true)
                        .setTRANSF_IGNORE_CLASSES(List.of(DefUseAnalyser.class, ParameterCollector.class))
                        .setPATH_SOLUTION_CALLBACK(DefUseAnalyser::resolveLabels)
                ;
        Mulib.executeMulib("driver0", cls, builder); //// TODO Use loop for different driver-methods
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
                getClassesOfSolution(DefUseAnalyser.chains.getDefUseChains().get(0).getSolution());
            }

            for (DefUseChain chain : DefUseAnalyser.chains.getDefUseChains()) {
                xsw.writeStartElement("DefUseChain");
                xsw.writeStartElement("def");
                parseDefUseVariable(xsw, chain.getDef());
                xsw.writeEndElement();
                xsw.writeStartElement("use");
                parseDefUseVariable(xsw, chain.getUse());
                xsw.writeEndElement();
                parseSolution(xsw, chain.getSolution());
                xsw.writeEndElement();
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
        }
        catch (Exception e) {
            logger.info("Unable to write the file: " + e.getMessage());
        }

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

    private static void parseSolution(XMLStreamWriter xsw, XMLSolution solution){
        try {
            Set<Class<?>> classes = parseClassesFromSolution();
            classes.add(XMLSolution.class);
            JAXBContext jaxbContextR = JAXBContext.newInstance(classes.toArray(Class[]::new));
            Marshaller jaxbMarshallerR = jaxbContextR.createMarshaller();
            jaxbMarshallerR.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter swR = new StringWriter();
            jaxbMarshallerR.marshal(solution, swR);
            xsw.writeCharacters(swR.toString());
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static void getClassesOfSolution(XMLSolution solution){
        try {
            Set<String> classes = new HashSet<>();
            for (Map.Entry<String, Object> o : solution.labels.entrySet()) {
                if (!(o.getValue() == null)) {
                    classes.add(o.getValue().getClass().getName());
                }
            }
            for(Map.Entry<String, Object[]> o : solution.labelsArray.entrySet()){
                if (!(o.getValue() == null)) {
                    classes.add(o.getValue().getClass().getName());
                }
            }
            if(solution.returnValue != null){
                classes.add(solution.returnValue.getClass().getName());
            }
            if(solution.returnValueArray!= null){
                classes.add(solution.returnValueArray.getClass().getName());
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter("DaciteSolutionClasses.txt"));
            for (String className : classes) {
                writer.write(className);
                writer.newLine();
            }
            writer.close();
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
                if(strLine.contains("[") && strLine.contains(";")){
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


}

