# Quassar Java SDK

Create your own domain-specific language (DSL) in [Quassar](https://quassar.io). Add the Quassar Maven plugin to your Java project to build a compiler or an interpreter for that DSL. [Configure Maven](#configure-maven)

## Choose how to execute your DSL

Configure the Maven plugin with the Quassar URL for your metamodel (M2). In both modes, the plugin generates a typed Java API for reading models (M1). Its `scaffold` option sets up the entry point where you implement the compiler or interpreter.

| Approach | What happens to M1 | `scaffold` |
| --- | --- | --- |
| **Compilation** | A generator renders M1 through `.itr` templates to produce source code in a target language such as Java or C. The target language's toolchain then builds that code for execution. | `maven-plugin` creates a Maven goal where you implement the generator. |
| **Interpretation** | A Java application loads M1 at runtime and acts on its declarations directly. | `application` creates a `Main.java` where you implement the interpreter. |

For a web service DSL, a compiler can generate an HTTP client class per endpoint. An interpreter can read an endpoint at runtime, build the HTTP request from its method, route, and parameters, and send it directly.

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

## Use case: generate a Java client for a web service

| Where | What you do | What you get |
| --- | --- | --- |
| **Forge** | Define a DSL with endpoints, HTTP methods, paths, and parameters. | A metamodel (M2) that describes what a service can declare. |
| **Studio** | Describe a service using that DSL. | A model (M1) with concrete routes, such as `GET /users/{userId}`. |
| **Build the generator** | Configure Maven with the M2 URL from Quassar and add a client template. | Typed accessors such as `path()` and `parameters()`, plus a Java renderer for the template. |
| **Generate the client** | Load M1 from its Quassar URL and render each endpoint through the template. | Java source such as `GetUserClient.java`. |
| **Call the service** | Compile the generated source and pass the service's base URL and parameter values. | An HTTP response from the running service. |

For example, an endpoint can declare a required path parameter and an optional query parameter:

```tara
Endpoint(method = "GET", path = "/users/{userId}") GetUser
	Parameter(location = "path", valueType = "string", required = true) userId
	Parameter(location = "query", valueType = "boolean", required = false) includeDetails
```

The generator uses the route, HTTP method, and parameter declarations to write the client. Application code can then make the request with the generated class:

```java
var client = new GetUserClient(URI.create("https://api.example.com"), HttpClient.newHttpClient());
var response = client.send(Map.of("userId", "42"), Map.of("includeDetails", "true"));
System.out.println(response.body());
```

Quassar serves M1 and M2 at `https://quassar.io/models/[code]`, using the code for the corresponding model. The downloaded archives already include their manifests.

Follow the [web service client example](docs/maven-plugin/web-service-client.md) to run the full pipeline. The SDK also supports generators for other kinds of DSLs through the same metamodel and template workflow.

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
