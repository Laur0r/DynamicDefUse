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
