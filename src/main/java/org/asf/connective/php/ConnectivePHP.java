package org.asf.connective.php;

import java.io.ByteArrayInputStream;
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

	public static InputStream execPHP(String path, ProviderContext context, HttpResponse response, HttpRequest request,
			ConnectiveHTTPServer server, HashMap<String, String> res, InputStream strm)
			throws IOException, InterruptedException {

		if (!new File(context.getSourceDirectory(), path).exists()) {
			response.status = 404;
			response.message = "File not found";
			return new ByteArrayInputStream(server.genError(response, request).getBytes());
		}

		ProcessBuilder builder = new ProcessBuilder(configuration.get("php-binary"));
		builder.environment().put("SERVER_NAME", configuration.get("server-name"));
		builder.environment().put("REDIRECT_STATUS", Integer.toString(response.status));
		builder.environment().put("PHP_SELF", path);
		builder.environment().put("SERVER_SOFTWARE", server.getName() + " " + server.getVersion());
		builder.environment().put("SERVER_PROTOCOL", request.version);
		builder.environment().put("REQUEST_METHOD", request.method);
		builder.environment().put("QUERY_STRING", (request.query == null ? "" : request.query));

		String root = new File(context.getSourceDirectory()).getCanonicalPath();
		builder.environment().put("DOCUMENT_ROOT", root);

		if (request.headers.containsKey("Accept"))
			builder.environment().put("HTTP_ACCEPT", request.headers.get("Accept"));
		if (request.headers.containsKey("Accept-Charset"))
			builder.environment().put("HTTP_ACCEPT_CHARSET", request.headers.get("Accept-Charset"));
		if (request.headers.containsKey("Accept-Encoding"))
			builder.environment().put("HTTP_ACCEPT_ENCODING", request.headers.get("Accept-Encoding"));
		if (request.headers.containsKey("Accept-Language"))
			builder.environment().put("HTTP_ACCEPT_LANGUAGE", request.headers.get("Accept-Language"));

		if (request.headers.containsKey("Connection"))
			builder.environment().put("HTTP_CONNECTION", request.headers.get("Connection"));
		if (request.headers.containsKey("Host"))
			builder.environment().put("HTTP_HOST", request.headers.get("Host"));

		if (request.headers.containsKey("User-Agent"))
			builder.environment().put("HTTP_USER_AGENT", request.headers.get("User-Agent"));

		if (server.getClass().getTypeName().contains("HTTPSServer"))
			builder.environment().put("HTTPS", "true");

		if (request.headers.containsKey("Content-Length"))
			builder.environment().put("CONTENT_LENGTH", request.headers.get("Content-Length"));

		if (request.headers.containsKey("Content-Type"))
			builder.environment().put("CONTENT_TYPE", request.headers.get("Content-Type"));

		// TODO: REMOTE_ADDR, REMOTE_HOST, REMOTE_PORT, REMOTE_USER,
		// REDIRECT_REMOTE_USER

		String scriptFile = new File(root, path).getCanonicalPath();
		builder.environment().put("SCRIPT_FILENAME", scriptFile);

		if (request.headers.containsKey("Authorization")) {
			String header = request.headers.get("Authorization");
			String type = header.substring(0, header.indexOf(" "));
			String cred = header.substring(header.indexOf(" ") + 1);

			builder.environment().put("AUTH_TYPE", type);
			if (type.equals("Basic")) {
				cred = new String(Base64.getDecoder().decode(cred));
				String username = cred.substring(0, cred.indexOf(":"));
				String password = cred.substring(cred.indexOf(":") + 1);

				builder.environment().put("PHP_AUTH_USER", username);
				builder.environment().put("PHP_AUTH_PW", password);
			} else if (type.equals("Digest"))
				builder.environment().put("PHP_AUTH_DIGEST", header);
		}

		builder.environment().put("SERVER_PORT", Integer.toString(server.getPort()));

		Process proc = builder.start();
		if (strm != null) {
			if (request.headers.get("Content-Length") != null) {
				long length = Long.valueOf(request.headers.get("Content-Length"));
				int tr = 0;
				for (long i = 0; i < length; i += tr) {
					tr = Integer.MAX_VALUE / 1000;
					if ((length - (long) i) < tr) {
						tr = strm.available();
						if (tr == 0) {
							proc.getOutputStream().write(strm.read());
							i += 1;
						}
						tr = strm.available();
					}
					proc.getOutputStream().write(strm.readNBytes(tr));
				}
			} else {
				strm.transferTo(proc.getOutputStream());
			}
			proc.getOutputStream().close();
		}

		while (true) {
			String line = readStreamLine(proc.getInputStream());
			if (line.isEmpty())
				break;

			String key = null;
			try {
				key = line.substring(0, line.indexOf(":"));
			} catch (Exception ex) {

			}
			if (key == null) {
				break;
			}
			String value = "";
			try {
				value = line.substring(line.indexOf(": ") + 2);
			} catch (Exception ex) {

			}
			res.put(key, value);
		}

		return proc.getInputStream();
	}

	private static String readStreamLine(InputStream strm) throws IOException {
		String buffer = "";
		while (true) {
			char ch = (char) strm.read();
			if (ch == '\n') {
				return buffer;
			} else if (ch != '\r') {
				buffer += ch;
			}
		}
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
