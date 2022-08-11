# Dacite: DAta-flow Coverage for Imperative TEsting

## Subprojects

* `dacite-core`: core library implementing the instrumentation and agent
* `dacite-exmaples`: examples used as benchmarks
* `dacite-ls`: language server implementation
* `dacite-intellij`: plugin integrating Dacite into the IntelliJ IDE

## Gradle Tasks

* Running the benchmarking examples:
  ```sh
  $ ./gradlew dacite-examples:run
  ```
* Running the IntelliJ plugin:
  ```sh
  $ ./gradlew dacite-intellij:runIde
  ```
