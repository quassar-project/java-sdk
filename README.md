# Quassar Java SDK

To build a compiler or an interpreter for your DSL, add the following Quassar Maven plugin configuration to your `pom.xml`. Use Java 17 or later and Maven 3.9.9 or later.

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
				<scaffold>[mode]</scaffold>
			</configuration>
		</plugin>
	</plugins>
</build>
```

## Create your models

To create a model (M1), you need a domain-specific language (DSL). In Quassar, you create that language by defining its metamodel (M2), which specifies its concepts, properties, and relationships. Once the DSL is available, use it to create models for your domain. [Open Quassar](https://quassar.io/)

Replace `[code]` with the code of your metamodel (M2) in Quassar.

## Choose how to process your models

Decide what should happen when your tool reads an M1 model. If you want to produce source code for a later build, choose compilation. If you want a Java application to act directly on the model at runtime, choose interpretation. Both use the typed Java API generated from M2.

### 1. Compilation

Build a Maven plugin for your DSL that turns each model (M1) into source code in the consuming project's generated sources directory.

```text
Model (M1) → Your DSL's Maven plugin → Source code in the generated sources directory
```

In your DSL plugin project, set `<packaging>maven-plugin</packaging>` and replace `[mode]` with `maven-plugin` in the Quassar plugin configuration.

When you run `mvn generate-sources` in a project with an empty Java source directory, Quassar generates the typed model API and an editable Mojo with a `generate` goal. Implement that goal so it loads an M1 model and renders your `.itr` templates into source code for a target language such as Java or C.

Install or publish your DSL plugin, then add it to a **consumer project's POM**. Configure its `generate` goal to run during the consumer's build, before the generated code is compiled. [Generate a Java HTTP client step by step](docs/maven-plugin/web-service-client.md)

### 2. Interpretation

You build a **Java application that runs your DSL**. Replace `[mode]` with `application` and run `mvn generate-sources`. In a project with an empty Java source directory, Quassar generates the typed model API and an editable `Main.java`.

Implement the interpreter in the application. At runtime, it loads M1 through the generated API and carries out the behavior declared by the model. [Interpret an HTTP endpoint step by step](docs/maven-plugin/web-service-interpreter.md)

## Find the right guide

| I want to… | Start here |
| --- | --- |
| Generate typed classes from a metamodel or scaffold a Maven generator. | [Maven Plugin](docs/maven-plugin/getting-started.md) |
| Write templates for text, JSON, or code. | [Template Engine](docs/template-engine/getting-started.md) · [Template examples](docs/template-engine/examples.md) |
| Load and query a Tara model through a generic Java API. | [Model Reader](docs/model-reader/getting-started.md) |

## Build the SDK from source

Use Java 17 or later and Maven 3.9.9 or later. From the repository root, run:

```sh
mvn clean install
```

This runs the tests and installs the SDK modules into your local Maven repository.

[MIT License](LICENSE)
