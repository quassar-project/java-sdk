# Quassar Java SDK

Create your own domain-specific language (DSL) in [Quassar](https://quassar.io). Add the Quassar Maven plugin to your Java project to build a compiler or an interpreter for that DSL. [Configure Maven](#configure-maven)

## Choose how to execute your DSL

Configure the Maven plugin with the Quassar URL for your metamodel (M2). In both modes, the plugin generates a typed Java API for reading models (M1). Its `scaffold` option sets up the entry point where you implement the compiler or interpreter.

| Approach | What happens to M1 | `scaffold` |
| --- | --- | --- |
| **Compilation** | A generator renders M1 through `.itr` templates to produce source code in a target language such as Java or C. The target language's toolchain then builds that code for execution. | `maven-plugin` creates a Maven goal where you implement the generator. |
| **Interpretation** | A Java application loads M1 at runtime and acts on its declarations directly. | `application` creates a `Main.java` where you implement the interpreter. |

## Configure Maven

Add this build configuration to your `pom.xml`. Use Java 17 or later and Maven 3.9.9 or later.

```xml
<build>
	<plugins>
		<plugin>
			<groupId>quassar.io</groupId>
			<artifactId>maven-plugin</artifactId>
			<version>1.0.0</version>
			<extensions>true</extensions>
			<executions>
				<execution>
					<goals><goal>generate</goal></goals>
				</execution>
			</executions>
			<configuration>
				<package>example</package>
				<metamodel>https://quassar.io/models/[code]</metamodel>
				<scaffold>application</scaffold>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[code]` with the code of your metamodel (M2) in Quassar. The example uses the application scaffold. To scaffold a compiler, set `scaffold` to `maven-plugin` and add `<packaging>maven-plugin</packaging>` to the project POM.

Run `mvn generate-sources` to generate the Java API and create the starter files if your Java source directory is empty. [Maven plugin guide](docs/maven-plugin/getting-started.md)

## Find the right guide

| I want to… | Start here |
| --- | --- |
| Generate typed classes from a metamodel or scaffold a Maven generator. | [Maven Plugin](docs/maven-plugin/getting-started.md) |
| See a complete code generation example. | [Generate a Java HTTP client](docs/maven-plugin/web-service-client.md) |
| Write templates for text, JSON, or code. | [Template Engine](docs/template-engine/getting-started.md) · [Template examples](docs/template-engine/examples.md) |
| Load and query a Tara model through a generic Java API. | [Model Reader](docs/model-reader/getting-started.md) |

## Build the SDK from source

Use Java 17 or later and Maven 3.9.9 or later. From the repository root, run:

```sh
mvn clean install
```

This runs the tests and installs the SDK modules into your local Maven repository.

[MIT License](LICENSE)
