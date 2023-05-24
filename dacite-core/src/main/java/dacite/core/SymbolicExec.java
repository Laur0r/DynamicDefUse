package dacite.core;

import dacite.core.defuse.DefUseAnalyser;
import dacite.core.defuse.DefUseField;
import dacite.core.defuse.DefUseVariable;
import dacite.core.defuse.ParameterCollector;
import dacite.core.instrumentation.Transformer;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.examples.sac22_mulib_benchmark.NQueens;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.print.DocFlavor;
import javax.tools.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        InputStream stream = ClassLoader.getSystemResourceAsStream("dacite_Sort.class");

        //logger.info(dir);
        File packagedir = new File(projectpath+packagename);
        URL url = null;
        Transformer transformer = new Transformer();
        Map<String,String> remap = new HashMap<>();
        for(File f: packagedir.listFiles()){
            if(!f.isDirectory()){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                remap.put(packagename+name, packagename+"dacite_"+name);
                if(!f.getName().contains("DaciteSymbolicDriver") && url == null){
                    url = Class.forName(packagename.replace("/",".")+name).getResource(name+".class");
                }
            }
        }
        String sourcePath = url.getPath().substring(0,url.getPath().indexOf(packagename));
        for(File f: packagedir.listFiles()){
            if(!f.isDirectory()){
                String name = f.getName().substring(0,f.getName().lastIndexOf("."));
                transformer.transformSymbolic(packagename.replace("/",".")+name, remap, sourcePath);
            }
        }


        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        /*List<File> sourceFileList = new ArrayList<File>();
        sourceFileList.add(file);
        StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );
        Iterable<? extends JavaFileObject> javaSource = fileManager.getJavaFileObjectsFromFiles( sourceFileList );
        Iterable<String> options = Arrays.asList("-d", sourcePath);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, javaSource);
        task.call();*/

        // Load and instantiate compiled class.
        Class<?> cls = Class.forName(packagename.replace("/",".")+"dacite_DaciteSymbolicDriver");
        Object instance = cls.getDeclaredConstructor().newInstance();
        logger.info(instance.toString());

        MulibConfig.MulibConfigBuilder builder = MulibConfig.builder().setTRANSF_WRITE_TO_FILE(true).setTRANSF_GENERATED_CLASSES_PATH(sourcePath);
        List<Class<?>> classes = new ArrayList<>(){};
        classes.add(DefUseAnalyser.class);
        classes.add(ParameterCollector.class);
        builder.setTRANSF_IGNORE_CLASSES(classes);
        Mulib.executeMulib("driver", cls, builder);


        /*try {
            junitCore.run(Class.forName(classname));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        long endTime   = System.nanoTime();
        long totalTime = (endTime - startTime) /1000000;
        logger.info(String.valueOf(totalTime));
        logger.info("run through symbolic method");
        DefUseAnalyser.check();
        // write xml file
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = null;
        try {
            xsw = xof.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream("file.xml")));
            xsw.writeStartDocument();
            xsw.writeStartElement("DefUseChains");

            for (DefUseChain chain : DefUseAnalyser.chains.getDefUseChains()) {
                xsw.writeStartElement("DefUseChain");
                xsw.writeStartElement("def");
                parseDefUseVariable(xsw, chain.getDef());
                xsw.writeEndElement();
                xsw.writeStartElement("use");
                parseDefUseVariable(xsw, chain.getUse());
                xsw.writeEndElement();
                xsw.writeEndElement();
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
            //format("file.xml");
        }
        catch (Exception e) {
            logger.info("Unable to write the file: " + e.getMessage());
        }*/

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

    private static void format(String file) {
        logger.info("hat String gebaut");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new InputStreamReader(new FileInputStream(file))));

            // Gets a new transformer instance
            javax.xml.transform.Transformer xformer = TransformerFactory.newInstance().newTransformer();
            // Sets XML formatting
            //xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            // Sets indent
            //xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source source = new DOMSource(document);
            StringWriter writer = new StringWriter();
            xformer.transform(source, new StreamResult(writer));
            logger.info("sollte Ergebnis geprintet haben");
            System.out.println(writer.getBuffer().toString());
        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> generateSearchRegions(String classname) throws IOException {
        List<String> output = new ArrayList<>();
        String packageName = classname.substring(0, classname.lastIndexOf("."));
        ClassReader reader = new ClassReader(classname);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        Map<String, List<String>> invokedMethods = new HashMap<>();
        for(MethodNode mnode : node.methods) {
            logger.info(mnode.name);
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
                                logger.info(methodins.owner + "." + methodins.name);
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : invokedMethods.entrySet()) {
            List<String> parameters = entry.getValue().subList(2,entry.getValue().size());
            String returnType = entry.getValue().get(1);
            String staticRef = entry.getValue().get(0);
            String method = entry.getKey();
            StringBuilder searchRegion = new StringBuilder("public static "+returnType+" driver(){" + System.getProperty("line.separator"));
            for(int i=0; i<parameters.size();i++){
                searchRegion.append(parameters.get(i)).append(" a").append(i);
                switch (parameters.get(i)){
                    case "int":searchRegion.append("= Mulib.namedFreeInt(").append("\"a").append(i).append("\");");break;
                    case "double":searchRegion.append("= Mulib.namedFreeDouble(").append("\"a").append(i).append("\");");break;
                    case "byte":searchRegion.append("= Mulib.namedFreeByte(").append("\"a").append(i).append("\");");break;
                    case "boolean":searchRegion.append("= Mulib.namedFreeBoolean(").append("\"a").append(i).append("\");");break;
                    case "short":searchRegion.append("= Mulib.namedFreeShort(").append("\"a").append(i).append("\");");break;
                    case "long":searchRegion.append("= Mulib.namedFreeLong(").append("\"a").append(i).append("\");");break;
                    default: searchRegion.append("= Mulib.namedFreeObject(").append("\"a").append(i).append("\", ")
                            .append(parameters.get(i)).append(".class);");
                }
                searchRegion.append(System.getProperty("line.separator"));
            }
            if(staticRef.equals("object") && method.contains(".")){
                String namedClass = method.substring(0,method.indexOf("."));
                searchRegion.append(namedClass).append(" obj = new ").append(namedClass).append("();");
                searchRegion.append(System.getProperty("line.separator"));
                method = "obj."+method.substring(method.indexOf(".")+1);
            }
            if(!returnType.equals("void")){
                searchRegion.append(returnType).append(" r0 = ");
            }

            searchRegion.append(method).append("(");
            for(int i=0; i<parameters.size();i++){
                searchRegion.append("a").append(i).append(",");
            }
            if(parameters.size()!=0){
                searchRegion.deleteCharAt(searchRegion.length()-1);
            }
            searchRegion.append(");").append(System.getProperty("line.separator"));
            if(!returnType.equals("void")){
                searchRegion.append("return r0;");
            }
            searchRegion.append("}");
            output.add(searchRegion.toString());
        }
        return output;
    }


}

