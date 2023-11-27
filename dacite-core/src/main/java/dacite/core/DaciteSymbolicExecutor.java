package dacite.core;

import dacite.core.analysis.*;
import dacite.core.instrumentation.DaciteTransformer;
import dacite.lsp.defUseData.transformation.XMLSolution;
import dacite.lsp.defUseData.transformation.XMLSolutionMapping;
import dacite.lsp.defUseData.transformation.XMLSolutions;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.solving.Solvers;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.yaml.snakeyaml.Yaml;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

public class DaciteSymbolicExecutor extends DaciteDynamicExecutor {

    static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void exec(String projectpath, String packagename, String classname) throws Exception {
        Yaml yaml = new Yaml();
        File packagedir = new File(projectpath+packagename);
        URL urlPackage = packagedir.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{urlPackage});
        Map<String, Object> config = null;
        try{
            InputStream inputStream = classLoader.getResourceAsStream("Dacite_config.yaml");
            config = yaml.load(inputStream);
            LinkedHashMap<String, String> daciteConfig = config.get("dacite_config") == null? null : (LinkedHashMap<String, String>) config.get("dacite_config");
            if(daciteConfig != null){
                logger.info(daciteConfig.toString());
                if(daciteConfig.containsKey("package")){
                    packagedir = new File(projectpath+daciteConfig.get("package").replace(".","/"));
                    packagename = daciteConfig.get("package").replace(".","/")+"/";
                }
            } else {
                logger.info("no dacite_config");
            }
        } catch (Exception e){
            logger.info("no dacite config found");
        }


        File file = new File(projectpath+packagename+classname);
        logger.info("file1 "+file.getPath()+" "+file.exists());

        // Get sourcepath
        URL url = null;
        DaciteTransformer transformer = new DaciteTransformer();
        transformer.setPath(projectpath);
        transformer.setDir(packagename.substring(0,packagename.length()-2));
        if(packagedir.listFiles() == null){
            throw new RuntimeException("there are not files in "+projectpath+packagename);
        }
        for(File f: Objects.requireNonNull(packagedir.listFiles())){
            if(!f.isDirectory()){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                if(!f.getName().contains("DaciteSymbolicDriver")){
                    url = Class.forName(packagename.replace("/",".")+name).getResource(name+".class");
                    break;
                }
            }
        }
        if(url == null){
            throw new RuntimeException("Source path of classes not found in "+projectpath+packagename);
        }
        String sourcePath = url.getPath().substring(0,url.getPath().indexOf(packagename));

        // Compile source file.
        List<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(file);
        compileFiles(sourceFileList, sourcePath);

        // Transform all files for Analysis
        for(File f: Objects.requireNonNull(packagedir.listFiles())){
            if(!f.isDirectory() && f.getName().contains(".java")){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                transformer.transformSymbolic(packagename.replace("/",".")+name, sourcePath);
            }
        }

        // Load and instantiate compiled class.
        Class<?> cls = Class.forName(packagename.replace("/",".")+classname.substring(0,classname.indexOf(".")));//+"dacite_"
        Object instance = cls.getDeclaredConstructor().newInstance();
        logger.info(instance.toString());

        // Set Mulib configs
        int BUDGET_INCR_ACTUAL_CP = 64;
        int BUDGET_FIXED_ACTUAL_CP = 64;
        int BUDGET_GLOBAL_TIME_IN_SECONDS = 10;
        boolean CONCOLIC = false;
        String SEARCH_STRATEGY = "IDDFS";
        if (config != null) {
            LinkedHashMap<String, Object> mulibConfig = config.get("mulib_config") == null? null : (LinkedHashMap<String, Object>) config.get("mulib_config");
            if (mulibConfig != null){
                if (mulibConfig.containsKey("BUDGET_INCR_ACTUAL_CP")) {
                    BUDGET_INCR_ACTUAL_CP = (Integer) mulibConfig.get("BUDGET_INCR_ACTUAL_CP");
                }
                if (mulibConfig.containsKey("BUDGET_FIXED_ACTUAL_CP")) {
                    BUDGET_FIXED_ACTUAL_CP = (Integer) mulibConfig.get("BUDGET_FIXED_ACTUAL_CP");
                }
                if (mulibConfig.containsKey("BUDGET_GLOBAL_TIME_IN_SECONDS")) {
                    BUDGET_GLOBAL_TIME_IN_SECONDS = (Integer) mulibConfig.get("BUDGET_GLOBAL_TIME_IN_SECONDS");
                }
                if (mulibConfig.containsKey("SEARCH_CONCOLIC")) {
                    CONCOLIC = (Boolean) mulibConfig.get("SEARCH_CONCOLIC");
                }
                if (mulibConfig.containsKey("SEARCH_STRATEGY")) {
                    SEARCH_STRATEGY = (String) mulibConfig.get("SEARCH_STRATEGY");
                }
            }
        }
        SearchStrategy searchStrategy = SearchStrategy.valueOf(SEARCH_STRATEGY);

        // Symbolic Execution with Mulib
        MulibConfig.MulibConfigBuilder builder =
                MulibConfig.builder()
//                        .setTRANSF_WRITE_TO_FILE(true)
                        .setTRANSF_GENERATED_CLASSES_PATH(sourcePath)
                        .setTRANSF_USE_DEFAULT_MODEL_CLASSES(true)
                        .setTRANSF_TREAT_SPECIAL_METHOD_CALLS(true)
                        .setTRANSF_IGNORE_CLASSES(List.of(DaciteAnalyzer.class, ParameterCollector.class))
                        .setTRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR(List.of(DaciteAnalyzer.class, ParameterCollector.class))
//                        .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(true)
//                        .setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(true)
//                        .setTRANSF_VALIDATE_TRANSFORMATION(true)
                        .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false)
                        .setSEARCH_MAIN_STRATEGY(searchStrategy)
                        .setSEARCH_CONCOLIC(CONCOLIC)
                        .setVALS_SYMSINT_DOMAIN(-7,7)
                        .setBUDGET_INCR_ACTUAL_CP(BUDGET_INCR_ACTUAL_CP)
                        .setBUDGET_GLOBAL_TIME_IN_SECONDS(BUDGET_GLOBAL_TIME_IN_SECONDS)
                        .setBUDGET_FIXED_ACTUAL_CP(BUDGET_FIXED_ACTUAL_CP)
                        .setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS)
                        .setSOLVER_GLOBAL_TYPE(Solvers.Z3_INCREMENTAL)
                        .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true)
                        .setCALLBACK_BACKTRACK((a0, a1, a2) -> SymbolicAnalyzer.resetSymbolicValues())
                        .setCALLBACK_FAIL((a0, a1, a2) -> SymbolicAnalyzer.resetSymbolicValues())
                        .setCALLBACK_EXCEEDED_BUDGET((a0, a1, a2) -> SymbolicAnalyzer.resetSymbolicValues())
                        .setCALLBACK_PATH_SOLUTION(SymbolicAnalyzer::resolveLabels)
                ;

        XMLSolutions solutionMapping = new XMLSolutions();
        DefUseChains chains = new DefUseChains();
        for(Method method: cls.getDeclaredMethods()){
            if(method.getName().contains("dacite_symbolic_driver")){
                Mulib.getPathSolutions(cls, method.getName(), builder);
                solutionMapping.addXMLSolution(method.getName().substring(method.getName().lastIndexOf("_")+1), getSolutions(DaciteAnalyzer.chains));
                chains.mergeChains(DaciteAnalyzer.chains);
                DaciteAnalyzer.chains = new DefUseChains();
            }
        }

        logger.info("run through symbolic method");
        logger.info("Size: "+chains.getChainSize());

        // Write xml files for output
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = null;
        xof.setProperty("escapeCharacters", false);
        try {
            xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("SymbolicDUCs.xml")));
            xsw.writeStartDocument();
            xsw.writeStartElement("DefUseChains");

            getClassesOfSolution(solutionMapping);
            parseSolution(xof, solutionMapping);

            for (DefUseChain chain : chains.getDefUseChains()) {
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
                parseSolutionIds(xsw, chain.getSolutions(), solutionMapping);
                xsw.writeEndElement();
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

        sourceFileList = new ArrayList<File>();
        for(File f: Objects.requireNonNull(packagedir.listFiles())) {
            if (!f.isDirectory() && f.getName().contains(".java")) {
                sourceFileList.add(f);
            }
        }
        compileFiles(sourceFileList, sourcePath);
    }

    private static void parseSolution(XMLOutputFactory xof, XMLSolutions solutions){
        try {
            if(!solutions.getXmlSolutions().isEmpty()){
                XMLStreamWriter xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("SymbolicSolutions.xml")));
                xsw.writeStartDocument();
                xsw.writeStartElement("xmlsolutions");
                Set<Class<?>> classes = parseClassesFromSolution();
                classes.add(XMLSolution.class);
                for(XMLSolutionMapping mapping : solutions.getXmlSolutions()){
                    xsw.writeStartElement("xmlsolutionmapping");
                    List<XMLSolution> entrySolutions = mapping.getSolutions();
                    String entryMethod = mapping.getMethod();
                    xsw.writeStartElement("xmlsolutionmethod");
                    xsw.writeCharacters(entryMethod);
                    xsw.writeEndElement();
                    for (XMLSolution s : entrySolutions) {
                        JAXBContext jaxbContextR = JAXBContext.newInstance(classes.toArray(Class[]::new));
                        Marshaller jaxbMarshallerR = jaxbContextR.createMarshaller();
                        jaxbMarshallerR.setProperty(Marshaller.JAXB_FRAGMENT, true);
                        StringWriter swR = new StringWriter();
                        jaxbMarshallerR.marshal(s, swR);
                        xsw.writeCharacters(swR.toString());
                    }
                    xsw.writeEndElement();
                }
                xsw.writeEndElement();
                xsw.writeEndDocument();
                xsw.flush();
                xsw.close();
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (XMLStreamException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseSolutionIds(XMLStreamWriter xsw, List<XMLSolution> solutions, XMLSolutions allSolutions) {
        try {
            StringBuilder solutionInd = new StringBuilder();
            for(int i =0; i<solutions.size(); i++){
                XMLSolution s = solutions.get(i);
                if(i != 0){
                    solutionInd.append(",");
                }
                for(XMLSolutionMapping mapping : allSolutions.getXmlSolutions()){
                    List<XMLSolution> entrySolutions = mapping.getSolutions();
                    String entryMethod = mapping.getMethod();
                    if(entrySolutions.contains(s)){
                        solutionInd.append(entryMethod).append("_").append(entrySolutions.indexOf(s));
                    }
                }
            }
            xsw.writeCharacters(solutionInd.toString());
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<XMLSolution> getSolutions(DefUseChains chains){
        List<XMLSolution> solutions = new ArrayList<>();
        chains.getDefUseChains().parallelStream().forEach(chain -> {
            List<XMLSolution> solution = chain.getSolutions();
            solution.parallelStream().forEach(s -> {
                if(!solutions.contains(s)){
                    solutions.add(s);
                }
            });
        });
        return solutions;
    }


    private static void getClassesOfSolution(XMLSolutions solutionsMapped){
        Set<String> classes = new HashSet<>();
        for(XMLSolutionMapping mapping : solutionsMapped.getXmlSolutions()){
            List<XMLSolution> solutions = mapping.getSolutions();
            if(solutions.size() != 0) {
                XMLSolution solution = solutions.get(0);

                for (Map.Entry<String, Object> o : solution.labels.entrySet()) {
                    if (!(o.getValue() == null)) {
                        Class<?> clazz = o.getValue().getClass();
                        classes.add(clazz.getName());
                        if (!isPrimitiveOrPrimitiveWrapperOrString(clazz)) {
                            for (Field f : clazz.getDeclaredFields()) {
                                if (!f.getType().isPrimitive()) {
                                    classes.add(f.getType().getName());
                                }
                            }
                        }
                    }
                }
                if (solution.returnValue != null) {
                    classes.add(solution.returnValue.getClass().getName());
                }
            }
        }

        try {
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String strLine;
            while ((strLine = reader.readLine()) != null) {
                // Classloader works only for Objects not arrays
                if(strLine.contains("[") || strLine.equals("int")){
                    classes.add(Class.forName(strLine));
                } else {
                    classes.add(DaciteSymbolicExecutor.class.getClassLoader().loadClass(strLine));
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
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

