package dacite;

import defuse.DefUseAnalyser;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

@Mojo(name="defuse")
public class DefUseMojo extends AbstractMojo{

    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${plugin}", readonly = true ) // Maven 3 only
    private PluginDescriptor plugin;

    @Parameter( readonly = true, defaultValue = "${plugin.artifacts}" )
    private List<Artifact> pluginDependencies;

    @Parameter( property = "defuse.analysisDir")
    private String analysisDir;

    @Parameter( property = "defuse.analysisJunittest")
    private String analysisJunittest;

    public void execute() throws MojoExecutionException {
        ProcessHandle.Info currentProcessInfo = ProcessHandle.current().info();
        List<String> newProcessCommandLine = new LinkedList<>();
        newProcessCommandLine.add(currentProcessInfo.command().get());
        List<String> classpaths= null;
        String classpath = "";
        try {
            classpaths = project.getCompileClasspathElements();
            for(String cp:classpaths){
                classpath = classpath + cp + ":";
            }
        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();
        }

        newProcessCommandLine.add("-javaagent:target/dynamic-defuse-1.0-SNAPSHOT.jar="+analysisDir+"/");
        newProcessCommandLine.add("-classpath");
        for(Artifact a: pluginDependencies){
            classpath = classpath + a.getFile().getAbsolutePath() + ":";
        }
        StringUtils.chop(classpath);
        newProcessCommandLine.add(classpath);
        newProcessCommandLine.add(DefUseMain.class.getName());
        newProcessCommandLine.add(analysisDir + "."+analysisJunittest);

        ProcessBuilder newProcessBuilder = new ProcessBuilder(newProcessCommandLine).redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT);
        try{
            Process newProcess = newProcessBuilder.start();
            System.out.format("%s: process %s started%n", "TestMojo", newProcessBuilder.command());
            DefUseAnalyser.check();
            System.out.format("process exited with status %s%n", newProcess.waitFor());
            DefUseAnalyser.check();
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        ProcessHandle.Info currentProcessInfo = ProcessHandle.current().info();
        List<String> newProcessCommandLine = new LinkedList<>();
        newProcessCommandLine.add(currentProcessInfo.command().get());
        Properties p = System.getProperties();
        System.out.println("test path");
        System.out.println(p.getProperty("java.class.path"));
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        newProcessCommandLine.add("-javaagent:target/dynamic-defuse-1.0-SNAPSHOT.jar=execution/");
        newProcessCommandLine.add("-classpath");
        newProcessCommandLine.add(ManagementFactory.getRuntimeMXBean().getClassPath());
        newProcessCommandLine.add(DefUseMain.class.getName());

        ProcessBuilder newProcessBuilder = new ProcessBuilder(newProcessCommandLine).redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT);
        try{
            Process newProcess = newProcessBuilder.start();
            System.out.format("%s: process %s started%n", "TestMojo", newProcessBuilder.command());
            System.out.format("process exited with status %s%n", newProcess.waitFor());
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
