# Interpret a web service model in Java

Run an HTTP request directly from the endpoint declared in M1. Maven uses M2 to generate the Java accessors. At runtime the application reads M1, builds the request, and sends it to an existing service.

## 1. Prepare the model and project

Create the `WebServices` metamodel and the `GetUser` endpoint in Quassar. Copy their download URLs. In the Maven configuration, use the M2 URL and set `<scaffold>application</scaffold>`. [M2, M1, and POM setup](getting-started.md#1-prepare-m2-and-m1-in-quassar)

Run `mvn generate-sources` to generate the model API and scaffold `Main.java` in an empty Java source directory.

## 2. Build the request from M1

Use this as `src/main/java/example/Main.java`. It takes the M1 download URL and the running service's base URL as arguments. The sample request supplies values for the path and query parameters declared by `GetUser`.

```java
package example;

import example.model.WebServicesModel;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public final class Main {
	public static void main(String[] args) throws Exception {
		var model = WebServicesModel.load(URI.create(args[0]));
		var endpoint = model.endpoints().get(0);
		var values = Map.of("userId", "42", "includeDetails", "true");
		var path = endpoint.path();
		var query = new StringJoiner("&");

		for (var parameter : endpoint.parameters()) {
			var name = parameter.identifier();
			var value = values.get(name);
			if (value == null) {
				if (parameter.required()) throw new IllegalArgumentException("Missing parameter " + name);
				continue;
			}
			switch (parameter.location()) {
				case "path" -> path = path.replace("{" + name + "}", encode(value));
				case "query" -> query.add(encode(name) + "=" + encode(value));
				default -> throw new IllegalArgumentException("Unsupported location " + parameter.location());
			}
		}

		var uri = URI.create(args[1]).resolve(path + (query.length() == 0 ? "" : "?" + query));
		var request = HttpRequest.newBuilder(uri)
			.method(endpoint.method(), HttpRequest.BodyPublishers.noBody())
			.build();
		var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println(response.body());
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
```

## 3. Run the interpreter

Replace `[code]` with the M1 code and `https://api.example.com` with the service's base URL.

```sh
mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
	-Dexec.mainClass=example.Main \
	-Dexec.args='https://quassar.io/models/[code] https://api.example.com'
```

The application sends `GET /users/42?includeDetails=true` and prints the response body. Remove `includeDetails` from the values map to send `GET /users/42`. The sample handles requests without a body and passes parameter values as strings.
