package org.asf.connective.php;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;

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
	}

	protected static void initComponent() {
		assign(new ConnectivePHP());
		ConnectivePHP.start();
	}

	@Override
	protected void startModule() {
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> readConfig());
	}

	private void readConfig() {
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

	public static InputStream execPHP(String path, ProviderContext context, HttpResponse response, HttpRequest request,
			ConnectiveHTTPServer server, HashMap<String, String> res, InputStream strm)
			throws IOException, InterruptedException {

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
		if (strm != null)
			strm.transferTo(proc.getOutputStream());
		proc.getOutputStream().close();

		if (strm != null)
			strm.close();

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

}
