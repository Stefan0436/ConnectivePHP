package org.asf.connective.php;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;
import org.asf.rats.http.IAutoContextModificationProvider;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.ProviderContextFactory;

@CYAN_COMPONENT
public class PhpSupportModule extends CyanComponent implements IAutoContextModificationProvider {

	private static String php = "";

	public static InputStream execPHP(String path, ProviderContext context, HttpResponse response, HttpRequest request,
			ConnectiveHTTPServer server, HashMap<String, String> res, InputStream strm)
			throws IOException, InterruptedException {

		ProcessBuilder builder = new ProcessBuilder(php);
		builder.environment().put("SERVER_NAME", server.getName());
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
		strm.transferTo(proc.getOutputStream());
		proc.getOutputStream().close();
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

	protected static void initComponent() {
		Memory.getInstance().getOrCreate("bootstrap.call").append(new Runnable() {

			@Override
			public void run() {
				ModuleBasedConfiguration<?> config = Memory.getInstance().get("memory.modules.shared.config")
						.getValue(ModuleBasedConfiguration.class);

				if (!config.modules.containsKey("php-binary")) {
					config.modules.put("php-binary", "/usr/bin/php-cgi");
					try {
						config.writeAll();
					} catch (IOException e) {
					}
				}

				php = config.modules.get("php-binary");
			}

		});
	}

	@Override
	public void accept(ProviderContextFactory factory) {
		factory.addExtension(new PHPExtensionProvider()); // TODO: specific server support
	}

}
