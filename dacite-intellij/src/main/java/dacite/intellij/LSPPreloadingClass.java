package dacite.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import dacite.intellij.lspclient.DaciteLSPExtensionManager;

import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProcessBuilderServerDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LSPPreloadingClass extends PreloadingActivity {
    public void preload(@NotNull ProgressIndicator indicator) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("dacite.dacite-intellij"));
        String workingDir = Objects.requireNonNull(plugin).getPluginPath().toString() + File.separator + "lib";
        String classpath = workingDir + File.separator + "*";

        // Determine java properties
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        // Put together the command to execute
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add("dacite.ls.ServerLauncher");

        // Setup process builder
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(new File(workingDir));
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        IntellijLanguageClient.addServerDefinition(new ProcessBuilderServerDefinition("java",builder));
        IntellijLanguageClient.addExtensionManager("java", new DaciteLSPExtensionManager());
    }
}
