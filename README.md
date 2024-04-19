# Dacite: DAta-flow Coverage for Imperative TEsting

## Subprojects

* `dacite-core`: core library implementing the instrumentation and agent
* `dacite-exmaples`: examples used as benchmarks
* `dacite-ls`: language server implementation
* `dacite-intellij`: plugin integrating Dacite into the IntelliJ IDE
* `dacite-vscode`: plugin integrating Dacite into Visual Studio Code
* `mulib`: integrated plugin for symbolic execution (see own [Github repository](https://github.com/NoItAll/mulib)

## Setup for Mulib to be able to start Dacite

A binary of Z3 is needed: https://github.com/Z3Prover/z3/releases

For Ubuntu: https://github.com/Z3Prover/z3/releases/download/z3-4.8.8/z3-4.8.8-x64-ubuntu-16.04.zip

Add the path to `LD_LIBRARY_PATH` so that the Z3 binary can be found. In Ubuntu, e.g.: 
`export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/lib/z3-4.8.8-x64-ubuntu-16.04/bin/"`

In `build.gradle` the path `implementation files('lib/z3-4.8.8-x64-ubuntu-16.04/bin/com.microsoft.z3.jar')` 
is assumed to hold the .jar suitable for your operating system. If this is not the case, it too must be changed.

## Important Gradle Tasks

* Running the IntelliJ plugin:
  ```sh
  $ ./gradlew dacite-intellij:runIde
  ```
* Running the IntelliJ plugin:
  ```sh
  $ ./gradlew dacite-vscode:runCode
  ```

## Dacite Config
To customize the Dacite data-flow analysis, symbolic execution and test case generation, a config file named `Dacite_config.yaml` can be defined within the executed Java project.
There are different configurations possibilities:
* `dacite_config`: To restrict the scope of the data-flow analysis, the package name including all to-be-analyzed files can be specified within the config file with the keyword `package`. This considers all files within this package and subordinate packages. Per default, all files within the package of the triggered test case are analyzed.
* `mulib_config`: To customize the symbolic execution of Mulib, different configurations can be adapted (see [Mulib](https://github.com/NoItAll/mulib)). With the keyword `budget_incr_actual_cp` the number of incremental choice points can be specified as an integer the symbolic execution executes before terminating (default=64). Analogously, the fixed number of choice points can be limited with `budget_fixed_actual_cp` (default=64). A limit for the symbolic execution time can be specified in seconds with `budget_global_time_in_seconds` (default=10). Moreover, with `search_concolic` the symbolic search including concolic values can be activated given `true`. Last but not least, a search strategy can be specified with `search_strategy`. This is either `BFS` (Breadth-first search), `DFS` (Depth-first search), `IDDFS` (Iterative deepening depth-first search), `IDDSAS` (Iterative deepening deepest shared ancestor search), and `DSAS` (Depth-first search with deepest ancestor) with the default of `IDDFS`.
* `test_config`: For the test reduction and generation, different configuration are also available. There are different heuristic strategies to reduce the test cases. Basic strategies can be specified with the keyword `reducer` giving `forward` (Simple forward test reduction), `backward` (simple backward test reduction), and `greedy` (simple greedy test reduction). These can also be combined with sequential or competitive reducers using `reducer_combine` with `sequential` or `competitive` respectively and the `reducer_list` containing a comma-separated list of the basic strategies to be combined. Furthermore, for the test generation methods regarding the to-be-tested program can be assumed by specifying `true` for `assumeGetters`, `assumeSetter`, and `assumeEquals`.

An exemplary config file can look like this:
```
dacite_config:
  package: somepackagename.test
mulib_config:
  budget_global_time_in_seconds:120
  search_strategy: DFS
test_config:
  reducer: greedy
  assumeGetters: true
  assumeSetters: true
```

## Acknowledgements

Parts of the implementation were inspired by the following open-source projects:

* [Apache Camel Language Server](https://github.com/camel-tooling/camel-language-server)

  Licensed under Apache License 2.0
* [Apache Camel Visual Studio Code Extension](https://github.com/camel-tooling/camel-lsp-client-vscode)

  Licensed under Apache License 2.0
* [Metals](https://github.com/scalameta/metals)

  Licensed under Apache License 2.0
* [Metals Visual Studio Code Extension](https://github.com/scalameta/metals-vscode)

  Licensed under Apache License 2.0
