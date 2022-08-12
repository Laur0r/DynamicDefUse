package dacite.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import dacite.intellij.defUseData.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DaciteAnalysisLauncher {
    public static void launch(Project project, String packagename, String filename){
        Module @NotNull [] modules = ModuleManager.getInstance(project).getModules();
        String fullClasspath = "";
        for(Module m: modules){
            fullClasspath += getFullClassPath(m);
            System.out.println(fullClasspath);
        }

        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("dacite.dacite-intellij"));
        String workingDir = Objects.requireNonNull(plugin).getPluginPath().toString() + File.separator + "lib";
        String pluginClasspath = workingDir + File.separator + "*";
        fullClasspath += File.pathSeparator + pluginClasspath;

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        ProcessHandle.Info currentProcessInfo = ProcessHandle.current().info();
        fullClasspath += currentProcessInfo.command().get();
        List<String> newProcessCommandLine = new LinkedList();
        newProcessCommandLine.add(javaBin);
        newProcessCommandLine.add("-javaagent:"+workingDir+"/dacite-core-1.0-SNAPSHOT.jar="+packagename+"/");
        newProcessCommandLine.add("-classpath");
        newProcessCommandLine.add(fullClasspath);
        newProcessCommandLine.add("dacite.core.DefUseMain");
        newProcessCommandLine.add(filename);
        ProcessBuilder newProcessBuilder = (new ProcessBuilder(newProcessCommandLine)).redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process newProcess = newProcessBuilder.start();
            System.out.format("%s: process %s started%n", "executed command", newProcessBuilder.command());
            System.out.format("process exited with status %s%n", newProcess.waitFor());
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));
            String s = null;
            while((s = stdInput.readLine()) != null) {
                if (s.contains("<?xml")) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(DefUseChains.class);
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    DefUseChains chains = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(s));
                    ArrayList<DefUseClass> list = transformDefUse(chains);
                    for (DefUseClass cl : list) {
                        System.out.println(cl.toString());
                    }
                    break;
                } else {
                    System.out.println(s);
                }
            }
        } catch (Exception var5) {
            System.out.println(var5.getMessage());
        }


    }
    public static String getFullClassPath(Module m){
        String cp = "";

        for(VirtualFile vf : OrderEnumerator.orderEntries(m).recursively().getClassesRoots()){
            String entry = new File(vf.getPath()).getAbsolutePath();
            if(entry.endsWith("!/")){ //not sure why it happens in the returned paths
                entry = entry.substring(0,entry.length()-2);
            }
            if(entry.endsWith("!")){
                entry = entry.substring(0,entry.length()-1);
            }
            if(entry.contains("!")){
                continue;
            }
            cp += File.pathSeparator + entry;
        }
        return cp;
    }

    public static ArrayList<DefUseClass> transformDefUse(DefUseChains chains){
        if(chains == null || chains.getChains().size() == 0){
            return null;
        }
        System.out.println(chains.getChains().size());
        ArrayList<DefUseClass> output = new ArrayList<DefUseClass>();
        for(DefUseChain chain:chains.getChains()){
            DefUseVariable use = chain.getUse();
            DefUseVariable def = chain.getDef();
            String useMethodPath = chain.getUse().getMethod();
            String defMethodPath = chain.getUse().getMethod();
            String useClassName = useMethodPath.substring(0, useMethodPath.lastIndexOf("."));
            String defClassName = defMethodPath.substring(0, defMethodPath.lastIndexOf("."));
            String methodName = useMethodPath.substring(useMethodPath.lastIndexOf(".")+1);
            DefUseClass defUseClass = new DefUseClass(useClassName);
            if(output.contains(defUseClass)){
                DefUseClass instance = output.get(output.indexOf(defUseClass));
                DefUseMethod m = new DefUseMethod(methodName);
                String useLocation = "Line "+use.getLinenumber();
                String defLocation = defClassName+" line "+def.getLinenumber();
                if(defClassName.equals(useClassName)){
                    defLocation = "Line "+def.getLinenumber();
                }
                String varName = use.getVariableName();
                if(!def.getVariableName().equals(varName)){
                    varName = def.getVariableName() + "/"+varName;
                }
                DefUseData data = new DefUseData(varName, defLocation, useLocation);
                data.setIndex(use.getVariableIndex());
                if(instance.getMethods().contains(m)){
                    DefUseMethod mInstance = instance.getMethods().get(instance.getMethods().indexOf(m));
                    mInstance.addData(data);
                } else {
                    DefUseMethod meth = new DefUseMethod(methodName);
                    meth.addData(data);
                    instance.addMethod(meth);
                }
            } else {
                DefUseMethod method = new DefUseMethod(methodName);
                String useLocation = "Line "+use.getLinenumber();
                String defLocation = defClassName+" line "+def.getLinenumber();
                if(defClassName.equals(useClassName)){
                    defLocation = "Line "+def.getLinenumber();
                }
                String varName = use.getVariableName();
                if(!def.getVariableName().equals(varName)){
                    varName = def.getVariableName() + "/"+varName;
                }
                DefUseData data = new DefUseData(varName, defLocation, useLocation);
                data.setIndex(use.getVariableIndex());
                method.addData(data);
                defUseClass.addMethod(method);
                output.add(defUseClass);
            }
        }
        return output;

    }
}
