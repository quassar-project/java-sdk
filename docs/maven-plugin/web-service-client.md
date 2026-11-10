# Generate a Java client for a web service

Use a DSL to describe an HTTP API and generate a client that can call it. This example generates a client for `GET /users/{userId}` with an optional `includeDetails` query parameter. It assumes the service is already running.

## The pipeline

| Step | Input | Output |
| --- | --- | --- |
| Build the generator with Maven | The `WebServices` metamodel (M2) and `endpoint-client.itr` | The typed Java model API and `EndpointClientTemplate` |
| Run the generator | The service model (M1) | `GetUserClient.java`, with the route and parameter handling from the model |
| Compile and call the client | The generated Java source, a base URL, and parameter values | An HTTP response |

M2 and M1 come from Quassar. The base URL points to the running web service. Once compiled, the generated client uses that base URL and the request values.

## 1. Prepare the generator project

Use the M2, M1, and POM from steps 1 and 2 of the [Maven Plugin quickstart](getting-started.md). Add the following files to that project.

## 2. Write the client template

Save this as `src/main/resources/endpoint-client.itr`. Path and query parameters each have their own rule. The generator substitutes path placeholders, checks required values, and URL-encodes parameter values.

```itr
rule type(Endpoint)
	package example.client;

	import java.io.IOException;
	import java.net.URI;
	import java.net.URLEncoder;
	import java.net.http.HttpClient;
	import java.net.http.HttpRequest;
	import java.net.http.HttpResponse;
	import java.nio.charset.StandardCharsets;
	import java.util.Map;
	import java.util.StringJoiner;

	public record $identifier~Client(URI baseUri, HttpClient http) {
		public HttpResponse<String> send(Map<String, String> pathParameters, Map<String, String> queryParameters)
				throws IOException, InterruptedException {
			String path = $path+JsonString;
			var query = new StringJoiner("&");
			$parameter...[$NL]
			URI uri = baseUri.resolve(path + (query.length() == 0 ? "" : "?" + query));
			var request = HttpRequest.newBuilder(uri)
				.method($method+JsonString, HttpRequest.BodyPublishers.noBody())
				.build();
			return http.send(request, HttpResponse.BodyHandlers.ofString());
		}

		private static String encode(String value) {
			return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
		}
	}

rule type(Parameter) and attribute(location, path)
	{
		var name = $identifier+JsonString;
		var value = pathParameters.get(name);
		if (value == null) throw new IllegalArgumentException("Missing path parameter " + name);
		path = path.replace("{" + name + "}", encode(value));
	}

rule type(Parameter) and attribute(location, query)
	{
		var name = $identifier+JsonString;
		var value = queryParameters.get(name);
		if (value == null && $required) throw new IllegalArgumentException("Missing query parameter " + name);
		if (value != null) query.add(encode(name) + "=" + encode(value));
	}
```

This example generates requests without a body. Both maps take string values, so pass a boolean query parameter as `"true"` or `"false"`.

## 3. Generate Java source from M1

Save this as `src/main/java/example/GenerateClient.java` and replace the example URL with the M1 download URL from Quassar.

```java
package example;

import example.model.WebServicesModel;
import example.templates.EndpointClientTemplate;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GenerateClient {
	public static void main(String[] args) throws IOException {
		var services = WebServicesModel.load(URI.create("https://quassar.io/models/[id]"));
		var output = Path.of("target/generated-clients/example/client");
		Files.createDirectories(output);
		for (var endpoint : services.endpoints()) {
			var source = EndpointClientTemplate.render(endpoint);
			Files.writeString(output.resolve(endpoint.identifier() + "Client.java"), source);
		}
	}
}
```

Compile the generator, run it, and compile the client it writes.

```sh
mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=example.GenerateClient
javac -d target/client-classes target/generated-clients/example/client/GetUserClient.java
```

## 4. Call the web service

Save this as `CallUserService.java` and replace the example base URL with the address of your service.

```java
import example.client.GetUserClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

class CallUserService {
	public static void main(String[] args) throws Exception {
		var client = new GetUserClient(URI.create("https://api.example.com"), HttpClient.newHttpClient());
		var response = client.send(Map.of("userId", "42"), Map.of("includeDetails", "true"));
		System.out.println(response.body());
	}
}
```

```sh
java -cp target/client-classes CallUserService.java
```

This sends `GET /users/42?includeDetails=true`. Pass an empty query map to send `GET /users/42`.
