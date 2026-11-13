# Quassar Java SDK

Use the Quassar Java SDK to build a compiler or an interpreter for models written in your own domain-specific language (DSL).

## 1. Create your DSL and models

Open Quassar Forge and select **New DSL**. To create the DSL, define its metamodel (M2) with the concepts, properties, and relationships that models can express. [Open Quassar Forge](https://quassar.io/forge)

Once your DSL is available, select it in Quassar Studio and create a model (M1). Use the concepts defined in M2 to describe a specific case in your domain. The same DSL can support many models. [Open Quassar Studio](https://quassar.io/studio)

## 2. Use your models from Java

Decide how your tool should process M1. Choose compilation to generate source code for a later build, or interpretation to have a Java application act directly on the model at runtime. Both use the typed Java API generated from M2.

Add the Quassar Maven plugin to your `pom.xml`. Use Java 17 or later and Maven 3.9.9 or later.

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
				<mode>[mode]</mode>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[code]` with the M2 code so the plugin can generate the Java code for your DSL. Use the M1 URL in your compiler or interpreter to load a specific model. Choose the value for `[mode]` below.

### Compiler

Build a Maven plugin for your DSL that turns each model (M1) into source code in the consuming project's generated sources directory.

```text
Model (M1) → Your DSL's Maven plugin → Source code in the generated sources directory
```

In the `pom.xml` of the plugin you are building for your DSL, add `<packaging>maven-plugin</packaging>` directly under `<project>`, alongside `<artifactId>` and `<version>`.

```xml
<project>
	<modelVersion>4.0.0</modelVersion>
	<groupId>example</groupId>
	<artifactId>my-dsl-maven-plugin</artifactId>
	<version>1.0.0</version>
	<packaging>maven-plugin</packaging>
	<!-- Add the <build> block shown above here. -->
</project>
```

In that same POM, set `<mode>compiler</mode>` inside the Quassar plugin's `<configuration>` in the `<build>` block above.

When you run `mvn generate-sources` in a project with an empty Java source directory, Quassar generates the typed model API and an editable Mojo with a `generate` goal. Implement that goal so it loads an M1 model and uses the Quassar Template Engine to render your `.itr` templates. The templates define the source code your compiler produces. This guide assumes Java output, and you can write templates for any other programming language. [Template Engine guide](docs/template-engine/getting-started.md) · [Template examples](docs/template-engine/examples.md)

Run `mvn install` in your DSL plugin project to install it locally. Then add that plugin to the **consumer project's `pom.xml`**. Use the coordinates of your DSL plugin and set its `model` parameter to the M1 download URL from Quassar.

```xml
<build>
	<plugins>
		<plugin>
			<groupId>example</groupId>
			<artifactId>my-dsl-maven-plugin</artifactId>
			<version>1.0.0</version>
			<executions>
				<execution>
					<phase>generate-sources</phase>
					<goals><goal>generate</goal></goals>
				</execution>
			</executions>
			<configuration>
				<model>https://quassar.io/models/[code]</model>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace `[code]` with the M1 code and run `mvn compile` in the consumer project. Maven runs your DSL plugin before compiling the Java sources. [Code generation walkthrough](docs/maven-plugin/getting-started.md#compilation)

### Interpreter

Build a Java application that loads a model (M1) and executes the behavior it describes at runtime.

```text
Model (M1) → Your Java interpreter → Runtime behavior
```

1. Set `<mode>interpreter</mode>` in the Quassar plugin configuration and run `mvn generate-sources`. The plugin generates a typed Java API from M2 and creates an editable `Main.java` if the Java source directory is empty. [Maven Plugin guide](docs/maven-plugin/getting-started.md#interpretation)
2. In `Main`, pass the M1 download URI to the generated model class's `load` method. Use the generated getters to access the model's root concepts, then follow their children and references to read the data your interpreter needs.
3. Implement what each concept does in Java. For example, if a concept declares a sequence of steps, your interpreter reads those steps from M1 and calls the Java operation for each one in order. Run the application with the URL of the model you want to interpret.

If your interpreter needs to query a model generically, use the Model Reader API to look up objects by type or identifier. [Model Reader guide](docs/model-reader/getting-started.md)

## Build the SDK from source

Use Java 17 or later and Maven 3.9.9 or later. From the repository root, run:

```sh
mvn clean install
```

This runs the tests and installs the SDK modules into your local Maven repository.

[MIT License](LICENSE)
