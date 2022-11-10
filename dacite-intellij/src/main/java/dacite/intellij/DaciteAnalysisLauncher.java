package dacite.intellij;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import dacite.intellij.defUseData.*;
import dacite.intellij.defUseData.transformation.DefUseChain;
import dacite.intellij.defUseData.transformation.DefUseChains;
import dacite.intellij.defUseData.transformation.DefUseVariable;
import dacite.intellij.visualisation.DaciteToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        newProcessCommandLine.add("-javaagent:"+workingDir+"/dacite-core-0.1.0-SNAPSHOT.jar="+packagename+"/");
        newProcessCommandLine.add("-classpath");
        newProcessCommandLine.add(fullClasspath);
        newProcessCommandLine.add("dacite.core.DefUseMain");
        newProcessCommandLine.add(filename);
        ProcessBuilder newProcessBuilder = (new ProcessBuilder(newProcessCommandLine)).redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process newProcess = newProcessBuilder.start();
            System.out.format("%s: process %s started%n", "executed command", newProcessBuilder.command());
            System.out.format("process exited with status %s%n", newProcess.waitFor());
            //BufferedReader stdInput = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));
            /*String s = null;
            //System.out.println(stdInput.lines().count());
            while((s = stdInput.readLine()) != null) {
                if (s.contains("<?xml")) {*/
                    JAXBContext jaxbContext = JAXBContext.newInstance(DefUseChains.class);
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    String test = Files.readString(Paths.get("file.xml"));
                    DefUseChains chains = (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(test));// (DefUseChains) jaxbUnmarshaller.unmarshal(new StringReader(s));//
                    ArrayList<DefUseClass> list = transformDefUse(chains);
                    for (DefUseClass cl : list) {
                        System.out.println(cl.toString());
                    }
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    ToolWindow toolWindow = toolWindowManager.getToolWindow("DaciteAnalysisToolWindow");
                    DaciteToolWindowFactory factory = new DaciteToolWindowFactory(list);

                    // One time registration of the tool window (does not add any content).
                    if (toolWindow == null) {
                        System.out.println("tool window not registered yet");
                        RegisterToolWindowTask task = new RegisterToolWindowTask("DaciteAnalysisToolWindow", ToolWindowAnchor.RIGHT, null, false,true,true,true,factory, AllIcons.General.Modified,null );// null, null, null);
                        toolWindow = toolWindowManager.registerToolWindow(task);
                        toolWindow.show();
                    } else {
                        factory.createToolWindowContent(project,toolWindow);
                    }
                    /*break;
                } else {
                    System.out.println(s);
                }
            }*/
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
        ArrayList<DefUseClass> output = new ArrayList<DefUseClass>();
        // Go through all recognized DefUseChains
        for(DefUseChain chain:chains.getChains()){
            DefUseVariable use = chain.getUse();
            DefUseVariable def = chain.getDef();
            String useMethodPath = chain.getUse().getMethod();
            String defMethodPath = chain.getDef().getMethod();
            // Method String is Class.Method, retrieve name of class and method
            String useClassName = useMethodPath.substring(0, useMethodPath.lastIndexOf("."));
            String defClassName = defMethodPath.substring(0, defMethodPath.lastIndexOf("."));
            String useMethodName = useMethodPath.substring(useMethodPath.lastIndexOf(".")+1);
            String defMethodName = defMethodPath.substring(defMethodPath.lastIndexOf(".")+1);

            DefUseClass defUseClass = new DefUseClass(defClassName);
            DefUseMethod m = new DefUseMethod(defMethodName);
            String useLocation = useMethodPath +" line "+use.getLinenumber();
            String defLocation = "Line "+def.getLinenumber();
            if(defClassName.equals(useClassName)){
                if(defMethodName.equals(useMethodName)){
                    useLocation = "Line "+use.getLinenumber();
                }
            }
            String varName = def.getVariableName();
            DefUseVar var = new DefUseVar(def.getVariableName());
            if(!use.getVariableName().equals(varName)){
                if(use.getVariableName().contains("[")){
                    varName = use.getVariableName() + use.getVariableIndex() + "]";
                    var = new DefUseVar(varName);
                } else {
                    varName = varName + "/"+use.getVariableName();
                }
            } else if(varName.contains("[")){
                varName = varName + use.getVariableIndex() + "]";
                var = new DefUseVar(varName);
            }
            DefUseData data = new DefUseData(varName, defLocation, useLocation);
            data.setIndex(use.getInstruction());
            // if output already contains class, add data to existing class instance
            if(output.contains(defUseClass)){
                DefUseClass instance = output.get(output.indexOf(defUseClass));
                if(instance.getMethods().contains(m)){
                    DefUseMethod mInstance = instance.getMethods().get(instance.getMethods().indexOf(m));
                    if(mInstance.getVariables().contains(var)){
                        DefUseVar vInstance = mInstance.getVariables().get(mInstance.getVariables().indexOf(var));
                        vInstance.addData(data);
                    } else {
                        var.addData(data);
                        mInstance.addVariable(var);
                    }
                } else {
                    var.addData(data);
                    m.addVariable(var);
                    instance.addMethod(m);
                }
            } else { // if output does not contain class, create new class instance with data
                var.addData(data);
                m.addVariable(var);
                defUseClass.addMethod(m);
                output.add(defUseClass);
            }
        }
        return output;

    }
}
