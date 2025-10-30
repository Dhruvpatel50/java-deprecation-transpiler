# Java API Deprecation Transpiler

## 🚀 Project Overview

The **Java API Deprecation Transpiler** is an intelligent command-line tool designed to automate the modernization of Java codebases. It addresses the common challenge of dealing with deprecated Java APIs by automatically detecting their usage within source files and replacing them with their modern, recommended equivalents.

Leveraging advanced compiler design principles, particularly Abstract Syntax Tree (AST) manipulation, this transpiler provides a robust and context-aware solution, significantly reducing manual refactoring effort and mitigating technical debt.

## ✨ Features

* **AST-Based Transformation:** Parses Java source code into an Abstract Syntax Tree (AST) for deep structural understanding, enabling precise and context-aware transformations.
* **Configurable Deprecation Rules:** Utilizes a `rules.json` file to define deprecated APIs and their modern replacements, making the transpiler easily extensible without code changes.
* **Automatic Import Management:** Automatically adds necessary `import` statements for new APIs used in replacements.
* **Comprehensive Coverage:** Designed to handle various types of deprecations, including:
    * Deprecated Types (e.g., `StringBuffer` -> `StringBuilder`, `Vector` -> `ArrayList`/`List`).
    * Deprecated Methods (e.g., `Date.getYear()` -> `java.time.Year` or `Calendar.get(Calendar.YEAR)`).
    * Deprecated Constructors (e.g., `new URL(String)` -> more robust `URI` parsing or specific `URL` constructors).
    * Complex API patterns (e.g., `String.split().length` -> `Arrays.asList(String.split()).size()`).
* **Verification Module:** Includes a dynamic Java compiler to compile and execute the transpiled code, ensuring functional correctness post-transformation.
* **Command-Line Interface:** Simple and straightforward execution from the terminal.

## 🎯 Motivation

Manual detection and replacement of deprecated APIs are:
* **Time-Consuming:** Especially in large codebases.
* **Error-Prone:** Easy to miss instances or introduce new bugs.
* **Inefficient:** Leads to significant technical debt and hinders future upgrades.

This project offers an automated, intelligent alternative to these challenges.

## ⚙️ Requirements

* **Java Development Kit (JDK):** Version 11 or higher (for `Files.writeString` and modern Java features).
* **Maven:** For dependency management and building the project.

## 🛠️ Installation & Setup

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/Dhruvpatel50/java-deprecation-transpiler.git
    cd java-deprecation-transpiler
    ```

2.  **Build with Maven:**
    This command will download all necessary dependencies (like JavaParser) and compile your project.
    ```bash
    mvn clean install
    ```
    This will create a `target/` directory containing the compiled `.jar` file.

## 🚀 Execution

1.  **Prepare an Input File:**
    Ensure you have a Java source file (e.g., `OldCode.java` in the project root) that contains deprecated API usages you want to transform.

2.  **Run the Transpiler:**
    Execute the transpiler from the command line, providing the path to your Java source file.

    ```bash
    java -jar target/java-deprecation-transpiler-1.0-SNAPSHOT.jar OldCode.java
    ```
    *(Note: The exact `.jar` name might vary based on your `pom.xml` configuration.)*

    **Example Output:**
    ```
    Processing file: OldCode.java
    AST created successfully.
    Deprecation rules loaded and visitor initialized.
    AST transformed based on deprecation rules.
    Transpiled code written to: OldCode_Transpiled.java

    --- Verification: Compiling and Running Transpiled Code ---
    --- Executing the test code ---
    Mutable string says: hello
    The year is: 2024  (or current year)
    URL Host: [www.google.com](https://www.google.com)
    Tokenizer has been replaced.
    Transpiled code compiled and ran successfully.
    --- Transpilation process complete ---
    ```

## ⚙️ Deprecation Rules (`rules.json`)

The `rules.json` file dictates which deprecated APIs are targeted and how they should be replaced. It's an array of rule objects, each with the following structure:

```json
[
  {
    "description": "Replace StringBuffer with StringBuilder",
    "kind": "Type",
    "match": "StringBuffer",
    "replacementExpr": "StringBuilder",
    "imports": []
  },
  {
    "description": "Replace Date.getYear() with Calendar.getInstance().get(Calendar.YEAR)",
    "kind": "MethodCall",
    "match": "getYear",
    "replacementExpr": "java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)",
    "imports": ["java.util.Calendar"]
  },
  {
    "description": "Replace new URL(String) with new URI(String).toURL()",
    "kind": "Constructor",
    "match": "URL",
    "argsCount": 1,
    "replacementExpr": "new java.net.URI(\"%s\").toURL()",
    "imports": ["java.net.URI"]
  },
  {
    "description": "Replace StringTokenizer with String.split()",
    "kind": "Type",
    "match": "StringTokenizer",
    "replacementExpr": "String[]",
    "initializerReplacementExpr": "%s.split(\" \")",
    "imports": []
  },
  {
    "description": "Replace Vector with ArrayList (Declaration)",
    "kind": "Type",
    "match": "Vector",
    "replacementExpr": "java.util.List",
    "initializerReplacementExpr": "new java.util.ArrayList<>()",
    "imports": ["java.util.List", "java.util.ArrayList"]
  }
]
