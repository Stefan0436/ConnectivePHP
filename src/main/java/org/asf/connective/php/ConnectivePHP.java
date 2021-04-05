package org.asf.connective.php;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.asf.connective.commoncgi.CgiScript;
import org.asf.connective.commoncgi.CgiScript.CgiContext;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;
import org.asf.rats.http.ProviderContext;

@CYAN_COMPONENT
public class ConnectivePHP extends PhpModificationManager {

	private static HashMap<String, String> configuration = new HashMap<String, String>();
	private static boolean hasConfigChanged = false;

	@Override
	protected String moduleId() {
		return "ConnectivePHP";
	}

	static {
		configuration.put("php-binary", "/usr/bin/php-cgi");
		configuration.put("server-name", "ASF Connective");
		configuration.put("enable-memcall", "true");
	}

	protected static void initComponent() {
		assign(new ConnectivePHP());
		ConnectivePHP.start();
	}

	@Override
	protected void startModule() {
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> {
			readConfig();
		});
		Memory.getInstance().getOrCreate("bootstrap.reload").<Runnable>append(() -> {
			readConfig();
		});
	}

	private void readConfig() {
		hasConfigChanged = false;
		ModuleBasedConfiguration<?> config = Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class);

		HashMap<String, String> ourConfigCategory = config.modules.getOrDefault(moduleId(),
				new HashMap<String, String>());

		if (!config.modules.containsKey(moduleId())) {
			ourConfigCategory.putAll(configuration);
			hasConfigChanged = true;
		} else {
			configuration.forEach((key, value) -> {
				if (!ourConfigCategory.containsKey(key)) {
					hasConfigChanged = true;
					ourConfigCategory.put(key, value);
				} else {
					configuration.put(key, ourConfigCategory.get(key));
				}
			});
		}

		config.modules.put(moduleId(), ourConfigCategory);
		if (hasConfigChanged) {
			try {
				config.writeAll();
			} catch (IOException e) {
				error("Config saving failed!", e);
			}
		}
	}

	public static boolean isMemCallEnabled() {
		return configuration.get("enable-memcall").equalsIgnoreCase("true");
	}

	public static CgiContext runPHP(ProviderContext context, ConnectiveHTTPServer server, HttpRequest request,
			HttpResponse response, Socket client, String path) throws IOException {

		CgiScript php = CgiScript.create(server, configuration.get("php-binary"));
		php.setDefaultVariables(configuration.get("server-name"), request, client);

		// Assign PHP variables
		php.setVariable("REDIRECT_STATUS", Integer.toString(response.status));
		php.setVariable("PHP_SELF", path);

		// Assing document root
		String root = new File(context.getSourceDirectory()).getCanonicalPath();
		php.setVariable("DOCUMENT_ROOT", root);

		// Assign script path
		String scriptFile = new File(root, path).getCanonicalPath();
		php.setVariable("SCRIPT_FILENAME", scriptFile);

		// Check if the server runs with HTTPS, FIXME: bad practice method
		if (server.getClass().getTypeName().contains("HTTPSServer"))
			php.setVariable("HTTPS", "true");

		// Assign authorization information
		if (request.headers.containsKey("Authorization")) {
			String header = request.headers.get("Authorization");
			String type = header.substring(0, header.indexOf(" "));
			String cred = header.substring(header.indexOf(" ") + 1);

			if (type.equals("Basic")) {

				cred = new String(Base64.getDecoder().decode(cred));
				String username = cred.substring(0, cred.indexOf(":"));
				String password = cred.substring(cred.indexOf(":") + 1);

				php.setVariable("PHP_AUTH_USER", username);
				php.setVariable("PHP_AUTH_PW", password);

			} else if (type.equals("Digest")) {

				php.setVariable("PHP_AUTH_DIGEST", header);

			}
		}

		php.addContentProvider(request);
		return php.run();
	}
	
	public static void errorMsg(String string, Exception e) {
		error(string, e);
	}

	@SuppressWarnings("unchecked")
	public static InputStream processMemCall(InputStream oldBody, InputStream output, HttpResponse input,
			HttpRequest request) {

		if (input.getHeader("X-Connective-Memcall").length != 0) {
			ArrayList<String> calls = new ArrayList<String>();
			for (String call : input.getHeader("X-Connective-Memcall")) {
				calls.add(call);
			}
			input.headers.forEach((k, v) -> {
				if (k.startsWith("X-Connective-Memcall-")) {
					calls.add(v);
				}
			});

			for (String header : new ArrayList<String>(input.headers.keySet())) {
				if (header.equals("X-Connective-Memcall") || header.startsWith("X-Connective-Memcall#")
						|| header.startsWith("X-Connective-Memcall-")) {
					input.headers.remove(header);
				}
			}

			if (isMemCallEnabled()) {
				for (String call : calls) {
					String entry = call;
					String query = "";
					if (entry.contains("; ")) {
						query = entry.substring(entry.indexOf("; ") + 2);
						entry = entry.substring(0, entry.indexOf("; "));
					}

					try {
						try {
							for (Runnable run : Memory.getInstance().get(entry).getValues(Runnable.class)) {
								run.run();
							}
						} catch (Exception ex) {
							ConnectivePHP.errorMsg(
									"Failed to call Runnable: " + ex.getClass().getTypeName() + ": " + ex.getMessage(),
									ex);
						}
						try {
							for (Consumer<String> con : Memory.getInstance().get(entry).getValues(Consumer.class)) {
								con.accept(query);
							}
						} catch (Exception ex) {
							ConnectivePHP.errorMsg(
									"Failed to call Consumer: " + ex.getClass().getTypeName() + ": " + ex.getMessage(),
									ex);
						}
						try {
							for (BiConsumer<HttpRequest, String> con : Memory.getInstance().get(entry)
									.getValues(BiConsumer.class)) {
								con.accept(request, query);
							}
						} catch (Exception ex) {
							ConnectivePHP.errorMsg("Failed to call BiConsumer: " + ex.getClass().getTypeName() + ": "
									+ ex.getMessage(), ex);
						}
						try {
							input.body = output;
							for (TriConsumer<HttpRequest, HttpResponse, String> con : Memory.getInstance().get(entry)
									.getValues(TriConsumer.class)) {
								con.accept(request, input, query);
							}
							if (input.body == output) {
								input.body = oldBody;
							} else {
								output = input.body;
							}
						} catch (Exception ex) {
							ConnectivePHP.errorMsg("Failed to call TriConsumer: " + ex.getClass().getTypeName() + ": "
									+ ex.getMessage(), ex);
						}
					} catch (Exception e) {
					}
				}
			}
		}
		return output;
	}
}
