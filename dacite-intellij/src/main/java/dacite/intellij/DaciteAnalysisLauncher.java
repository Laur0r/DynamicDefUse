package dacite.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
        ProcessBuilder newProcessBuilder = (new ProcessBuilder(newProcessCommandLine)).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process newProcess = newProcessBuilder.start();
            System.out.format("%s: process %s started%n", "executed command", newProcessBuilder.command());
            System.out.format("process exited with status %s%n", newProcess.waitFor());
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
}
