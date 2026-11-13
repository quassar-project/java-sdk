# Quassar Maven Plugin

Use the Quassar Maven plugin to build a compiler or an interpreter for your DSL. Both use the typed Java classes the plugin generates from your metamodel. Set `mode` to `compiler` to start a code generator or to `interpreter` to start a runtime interpreter. The plugin can also compile `.itr` templates into Java renderers during the Maven build. Use Java 17 and Maven 3.9.9 or later.

## 1. Prepare M2 and M1 in Quassar

In Quassar Forge, select **New DSL** and define the metamodel (M2). Then select your DSL in Quassar Studio and create a model (M1). Copy both download URLs in the form `https://quassar.io/models/[code]`. [Open Quassar Forge](https://quassar.io/forge) · [Open Quassar Studio](https://quassar.io/studio)

The plugin uses the metamodel name in the Quassar archive's manifest to name the generated Java model class.

## 2. Configure Maven

In your project's POM, set `maven.compiler.release` to `17` or later and add this build configuration.

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

Replace the example URL with the M2 download URL from Quassar. With `extensions` enabled, Maven adds the reader and template engine as project dependencies.

## 3. Choose compilation or interpretation

Replace `[mode]` with the mode for your tool. Run `mvn generate-sources` to generate the Java model API under `src/main/gen`. The plugin creates starter Java files when the main Java source directory is empty.

### Compilation

1. Set `<mode>compiler</mode>` and the project's `<packaging>maven-plugin</packaging>`. Run `mvn generate-sources` to create an editable Mojo with a `generate` goal.
2. Write `.itr` templates that describe the source code to generate and put them in `src/main/resources`. This guide assumes Java output. The same template engine can generate source code for any programming language. [Template Engine guide](../template-engine/getting-started.md)
3. Implement the goal to load M1 through the generated model class, use the Quassar Template Engine to render the templates with the model, and write the result to the consumer's generated sources directory.
4. Run `mvn install` in the DSL plugin project. In the consuming project, add your plugin's Maven coordinates and bind its `generate` goal to `generate-sources`, and set `<model>` in its configuration to the M1 download URL. Run `mvn compile` in the consumer project. [Consumer POM example](../../README.md#compiler)

### Interpretation

1. Set `<mode>interpreter</mode>`. Run `mvn generate-sources` to create an editable `Main.java`.
2. In `Main`, call the generated model class's `load` method with the M1 download URI.
3. Use the generated getters to read the model and implement its behavior in Java. Compile and run the application with the M1 you want to interpret.

Use `<mode>none</mode>` if you want to supply your own entry point.

If M2 is configured and the templates directory has no `.itr` files, both modes create `model-json.itr` as a starter example. The application starter prints the rendered result when one template is available. To generate Java from templates alone, omit `metamodel`.

When a metamodel and one template are available, the starter goal requires `<model>` and renders it to `target/generated-resources/model.json` by default. You can also pass the M1 URL with `-Dquassar.model` and change the output file with `-Dquassar.output`. Adapt the starter goal and its output for the code your compiler needs to generate.

## Configuration reference

| Parameter | Default |
| --- | --- |
| `package` | `generated` |
| `metamodel` | Optional M2 folder or ZIP URL |
| `mode` | `interpreter` |
| `templatesDirectory` | `${project.basedir}/src/main/resources` |
| `outputDirectory` | `${project.basedir}/src/main/gen` |

For property types, cardinality, and the generated Java API, see the [M2 guide](metamodel.md).
