# Quassar Maven Plugin

Use the Quassar Maven plugin to build a compiler or an interpreter for your DSL. Both use the typed Java classes the plugin generates from your metamodel. Set `scaffold` to `maven-plugin` to start a code generator or to `application` to start a runtime interpreter. The plugin can also compile `.itr` templates into Java renderers during the Maven build. Use Java 17 and Maven 3.9.9 or later.

This quickstart uses a web service DSL to show how the plugin turns M2 into Java accessors and how a template reads endpoint data from M1. The [client generation example](web-service-client.md) takes the same model through to a Java client that sends HTTP requests.

## 1. Prepare M2 and M1 in Quassar

M2 describes the language. M1 contains the objects written in that language. Quassar provides both at `https://quassar.io/models/[code]`. Use the M2 code in the Maven configuration and the M1 code when loading the model in Java.

Create a metamodel named `WebServices`. An endpoint has an HTTP method, a path, and a list of parameters.

```tara
dsl metta

Concept Endpoint
	def string method
	def string path
	has Parameter

Concept Parameter as component
	def string location
	def string valueType
	def boolean required
```

Quassar includes a manifest in the downloaded archive. Maven uses the metamodel name from that manifest to generate `WebServicesModel`.

Create an M1 model with one endpoint. `userId` is part of the path, and clients can pass `includeDetails` in the query string.

```tara
Endpoint(method = "GET", path = "/users/{userId}") GetUser
	Parameter(location = "path", valueType = "string", required = true) userId
	Parameter(location = "query", valueType = "boolean", required = false) includeDetails
```

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
				<scaffold>none</scaffold>
			</configuration>
		</plugin>
	</plugins>
</build>
```

Replace the example URL with the M2 download URL from Quassar. With `extensions` enabled, Maven adds the reader and template engine as project dependencies.

The tutorial sets `scaffold` to `none` so you can add the Java and template code yourself.

## 3. Generate and use the Java classes

Run `mvn compile`. The plugin writes generated Java under `src/main/gen`. With the package in this example, model classes are under `example.model`.

Replace the URL below with the M1 download URL from Quassar. It identifies the model that declares the route.

```java
import example.model.WebServicesModel;
import java.net.URI;

var services = WebServicesModel.load(URI.create("https://quassar.io/models/[code]"));
var endpoint = services.endpoints().get(0);

System.out.println(endpoint.method() + " " + endpoint.path());
for (var parameter : endpoint.parameters())
	System.out.println(parameter.identifier() + " required=" + parameter.required());
```

The model exposes root endpoints. Parameters are components of an endpoint, so you access them through `endpoint.parameters()`.

## 4. Render with a template

Save this as `src/main/resources/endpoint-summary.itr`. During compilation Maven generates `EndpointSummaryTemplate` from the file name.

```itr
rule type(Endpoint)
	$method $path
	$parameter...[$NL]

rule type(Parameter)
	$identifier ($location, $valueType, required=$required)
```

Create `src/main/java/example/Main.java` and replace the URL with the M1 download URL.

```java
package example;

import example.model.WebServicesModel;
import example.templates.EndpointSummaryTemplate;
import java.io.IOException;
import java.net.URI;

public final class Main {
	public static void main(String[] args) throws IOException {
		var services = WebServicesModel.load(URI.create("https://quassar.io/models/[code]"));
		System.out.println(EndpointSummaryTemplate.render(services.endpoints().get(0)));
	}
}
```

Compile and run the application.

```sh
mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=example.Main
```

The result describes the route and its parameters.

```text
GET /users/{userId}
userId (path, string, required=true)
includeDetails (query, boolean, required=false)
```

Maven compiles each template into its own Java class, so the generated renderer can run without the original `.itr` file. Use the same endpoint data in your own templates to generate client code or server stubs.

## Choose compilation or interpretation

Set `scaffold` in the plugin's `configuration` to choose the entry point for your DSL tool. In both modes, the plugin generates Java accessors from M2. Your code then decides how to process M1. Starter files are created when the main Java source directory is empty. Existing sources and templates take precedence.

| Approach | Value | What it creates |
| --- | --- | --- |
| Compilation | `maven-plugin` | An editable Mojo with a `generate` goal. Use it to load M1 and render `.itr` templates that emit source code in a target language such as Java or C. Set the project's packaging to `maven-plugin`. A metamodel named `WebServices` produces `WebServicesMojo.java`. |
| Interpretation | `application` (default) | An editable `Main.java` where you load M1 and implement the behavior declared by the model at runtime. |
| Supply your own entry point | `none` | Only generated Java from the configured metamodel and existing templates. |

If M2 is configured and the templates directory has no `.itr` files, both scaffolds create `model-json.itr` as a starter example. The application starter prints the rendered result when one template is available. To generate Java from templates alone, omit `metamodel`.

The generated Maven goal reads `m1` and writes `target/generated-resources/model.json` when a metamodel and one template are available. Set `-Dquassar.source` and `-Dquassar.output` to use other paths.

## Configuration reference

| Parameter | Default |
| --- | --- |
| `package` | `generated` |
| `metamodel` | Optional M2 folder or ZIP URL |
| `scaffold` | `application` |
| `templatesDirectory` | `${project.basedir}/src/main/resources` |
| `outputDirectory` | `${project.basedir}/src/main/gen` |

For property types, cardinality, and the generated Java API, see the [M2 guide](metamodel.md).
