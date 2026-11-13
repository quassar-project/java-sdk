# Quassar Java SDK

Every domain-specific language (DSL) in Quassar has a metamodel (M2) that defines its concepts, properties, and relationships. Models (M1) describe concrete examples using that language. Add the Quassar Maven plugin to your Java project to build a compiler or an interpreter for your DSL. [Quassar](https://quassar.io)

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
				<scaffold>[mode]</scaffold>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[code]` with the code of your metamodel (M2) in Quassar. Replace `[mode]` with `maven-plugin` for compilation or `application` for interpretation, as described below.

Run `mvn generate-sources` to generate the starter code. [Maven plugin guide](docs/maven-plugin/getting-started.md)

## Follow a use case

Configure the Maven plugin with the Quassar URL for your metamodel (M2). In both modes, the plugin generates a typed Java API for reading models (M1). Its `scaffold` option sets up the entry point where you implement the compiler or interpreter.

### 1. Generate an HTTP client

You build a **Maven plugin for your DSL**. In that plugin project, set `<packaging>maven-plugin</packaging>` and configure the **Quassar Maven plugin** with `<scaffold>maven-plugin</scaffold>`.

When you run `mvn generate-sources` in a project with an empty Java source directory, Quassar generates the typed model API and an editable Mojo with a `generate` goal. Implement that goal so it loads an M1 model and renders your `.itr` templates into source code for a target language such as Java or C.

Install or publish your DSL plugin, then add it to a **consumer project's POM**. Configure its `generate` goal to run during the consumer's build, before the generated code is compiled. The Quassar plugin helps build your DSL plugin, and your DSL plugin translates the consumer's models. [Generate a Java HTTP client step by step](docs/maven-plugin/web-service-client.md)

### 2. Interpret a service model at runtime

You build a **Java application that runs your DSL**. Configure the Quassar Maven plugin with `<scaffold>application</scaffold>`. In a project with an empty Java source directory, Quassar generates the typed model API and an editable `Main.java`.

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
