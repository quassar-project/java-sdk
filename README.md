# Quassar Java SDK

Create your own domain-specific language (DSL) in [Quassar](https://quassar.io). Use the Java SDK to turn the models people write in that language into applications, reports, or generated source code.

## Define a web service and use it from Java

| Where | What you do | What you get |
| --- | --- | --- |
| **Forge** | Define a DSL with endpoints, HTTP methods, paths, and parameters. | A metamodel (M2) that describes what a service can declare. |
| **Studio** | Describe a service using that DSL. | A model (M1) with concrete routes, such as `GET /users/{userId}`. |
| **Your Java project** | Configure Maven with the M2 download URL and load M1 using its own URL. | Typed Java accessors for the routes and parameters in the model. |

For example, an endpoint can declare a required path parameter and an optional query parameter:

```tara
Endpoint(method = "GET", path = "/users/{userId}") GetUser
	Parameter(location = "path", valueType = "string", required = true) userId
	Parameter(location = "query", valueType = "boolean", required = false) includeDetails
```

For a metamodel named `WebServices`, Maven generates `WebServicesModel`, `Endpoint`, and `Parameter`. Read the model through accessors such as `path()` and `required()`:

```java
var services = WebServicesModel.load(URI.create("https://quassar.io/models/[id]"));
var endpoint = services.endpoints().get(0);

System.out.println(endpoint.method() + " " + endpoint.path());
for (var parameter : endpoint.parameters())
	System.out.println(parameter.identifier() + " required=" + parameter.required());
```

Replace the example URL with your M1 download URL. The [Maven Plugin quickstart](docs/maven-plugin/getting-started.md) defines the metamodel and shows how to render the endpoint with an `.itr` template. You can also write templates to generate client code or server stubs from the same model.

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
