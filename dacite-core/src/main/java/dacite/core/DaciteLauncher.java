package dacite.core;

public class DaciteLauncher {

    public static void main(String[] args) throws Exception {
        if (args != null && args.length == 4) {
            String option = args[0];
            String projectdir = args[1];
            String packagename = args[2];
            String classname = args[3];

            // forward method invocation depending on given argument
            if (option.equals("dacite")) {
                DaciteDynamicExecutor.exec(projectdir, packagename, classname);
            }
            else if (option.equals("symbolic")) {
                DaciteSymbolicExecutor.exec(projectdir, packagename, classname);
            }
        } else {
            throw new RuntimeException("args must contain four arguments, containing the execution parameter, projectdir, packagename and classname.");
        }
    }
}
