# Dacite: DAta-flow Coverage for Imperative TEsting

## Subprojects

* `dacite-core`: core library implementing the instrumentation and agent
* `dacite-exmaples`: examples used as benchmarks
* `dacite-ls`: language server implementation
* `dacite-intellij`: plugin integrating Dacite into the IntelliJ IDE
* `dacite-vscode`: plugin integrating Dacite into Visual Studio Code

## Important Gradle Tasks

* Running the benchmarking examples:
  ```sh
  $ ./gradlew dacite-examples:run
  ```
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
