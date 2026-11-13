package tests;

import io.quassar.templating.TemplateEngine;

import io.quassar.templating.Frames;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentationExamplesTest {
	@Test
	public void rendersOrdersCustomersAndOptionalTracking() throws Exception {
		OrderBatch batch = new OrderBatch("Daily orders", List.of(
			new Order("ORD-1042", 2599, new Customer("Alex Morgan"), "TRK-1042", true),
			new Order("ORD-1043", 1499, new Customer("Sam Rivera"), null, false)));
		String result = render("order-batch", batch);
		assertThat(result).isEqualTo(TestTemplates.expected("order-batch"));
	}

	@Test
	public void selectsTheFirstMatchingRule() throws Exception {
		assertThat(render("availability", order(true))).isEqualTo("ORD-1042: shipped");
		assertThat(render("availability", order(false))).isEqualTo("ORD-1042: pending");
	}

	@Test
	public void rendersAFallbackForAnAbsentSlot() throws Exception {
		assertThat(render("fallback", order(true))).isEqualTo("ORD-1042: Tracking TRK-1042");
		Order order = new Order("ORD-1043", 1499, new Customer("Sam Rivera"), null, false);
		assertThat(render("fallback", order)).isEqualTo("ORD-1043: Tracking unavailable");
	}

	@Test
	public void rendersRecursiveTasks() throws Exception {
		Task task = new Task("Fulfil order", List.of(
			new Task("Pack items", List.of()),
			new Task("Ship order", List.of(new Task("Confirm delivery", List.of())))));
		assertThat(render("tasks", task)).isEqualTo("Fulfil order\n\tPack items\n\tShip order\n\t\tConfirm delivery");
	}

	@Test
	public void generatesAJavaRecord() throws Exception {
		Entity event = new Entity("OrderPlaced", List.of(
			new Property("String", "orderId"),
			new Property("long", "totalInCents")));
		assertThat(render("record", event)).isEqualTo("public record OrderPlaced(String orderId, long totalInCents) {}");
	}

	@Test
	public void configuresAFormatterAndAnAdapter() throws Exception {
		try (InputStream input = template("label")) {
			TemplateEngine engine = TemplateEngine.with(input).add("Label", value -> "[" + value + "]");
			assertThat(engine.render(order(true))).isEqualTo("[ORD-1042]");
		}
		try (InputStream input = template("label")) {
			TemplateEngine engine = TemplateEngine.with(input)
				.add("Label", value -> "[" + value + "]")
				.add(ExternalOrder.class, (source, frame) -> frame.add("Order").add("number", source.label()));
			assertThat(engine.render(new ExternalOrder("ORD-1043"))).isEqualTo("[ORD-1043]");
		}
	}

	@Test
	public void rendersAFrameAndItsContainer() throws Exception {
		var order = Frames.builder("Order").add("number", "ORD-1042").toFrame();
		var batch = Frames.builder("OrderBatch").add("name", "Daily orders").add("orders", order).toFrame();
		assertThat(render("context", batch)).isEqualTo("ORD-1042 (Daily orders)");
	}

	private Order order(boolean shipped) {
		return new Order("ORD-1042", 2599, new Customer("Alex Morgan"), "TRK-1042", shipped);
	}

	private String render(String name, Object object) throws Exception {
		try (InputStream input = template(name)) {
			return TemplateEngine.with(input).render(object);
		}
	}

	private InputStream template(String name) {
		return TestTemplates.input("examples/" + name);
	}

	public record Customer(String name) {
	}

	public record Order(String number, long totalInCents, Customer customer, String trackingCode, boolean shipped) {
	}

	public record OrderBatch(String name, List<Order> orders) {
		public OrderBatch {
			orders = List.copyOf(orders);
		}
	}

	public record Task(String number, List<Task> children) {
		public Task {
			children = List.copyOf(children);
		}
	}

	public record Entity(String name, List<Property> properties) {
		public Entity {
			properties = List.copyOf(properties);
		}
	}

	public record Property(String type, String name) {
	}

	public record ExternalOrder(String label) {
	}
}
