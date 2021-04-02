package org.asf.connective.php;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;

@CYAN_COMPONENT
public class TestComponent {
	protected static void initComponent() {
		Memory.getInstance().getOrCreate("php.test.memcalltest.one").<Runnable>append(() -> {
			System.out.println("Hello from memcall one!");
		});
		Memory.getInstance().getOrCreate("php.test.memcalltest.two").<Consumer<String>>append((query) -> {
			System.out.println("Hello from memcall two! Query: " + query);
		});
		Memory.getInstance().getOrCreate("php.test.memcalltest.two")
				.<BiConsumer<HttpRequest, String>>append((request, query) -> {
					System.out.println(
							"Hello from memcall two! Query: " + query + ", request headers: " + request.headers.size());
				});
		Memory.getInstance().getOrCreate("php.test.memcalltest.three")
				.<TriConsumer<HttpRequest, HttpResponse, String>>append((request, response, query) -> {
					System.out.println("Hello from memcall three! Query: " + query + ", request headers: "
							+ request.headers.size() + ", response headers: " + response.headers.size());

					response.setHeader("X-ConnectivePHP-Test-Memcall-3-Query", query, true);
					try {
						String body = new String(response.body.readAllBytes());
						body += "<h2>Memcall Java, hello World, Memcall query three: " + query + "</h2>";
						response.setContent("text/html", body);
					} catch (IOException e) {
					}

					System.out.println("Added header with query to response, appended to HTML too.");
				});
	}
}
