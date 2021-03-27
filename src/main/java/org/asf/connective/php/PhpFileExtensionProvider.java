package org.asf.connective.php;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;

import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.http.FileContext;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.FilePostHandler;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IFileExtensionProvider;
import org.asf.rats.http.providers.IPathProviderExtension;
import org.asf.rats.http.providers.IServerProviderExtension;

public class PhpFileExtensionProvider extends FilePostHandler
		implements IFileExtensionProvider, IContextProviderExtension, IServerProviderExtension, IPathProviderExtension {

	private ConnectiveHTTPServer server;
	private String path;
	private ProviderContext context;

	public PhpFileExtensionProvider() {
	}

	public PhpFileExtensionProvider(ProviderContext context) {
		this.context = context;
	}

	@Override
	public String fileExtension() {
		return ".php";
	}

	@Override
	public FileContext rewrite(HttpResponse input, HttpRequest request) {
		try {
			HashMap<String, String> newHeaders = new HashMap<String, String>(input.headers);
			InputStream strm = ConnectivePHP.execPHP(path, context, input, request, server, newHeaders,
					request.getBodyStream());
			if (newHeaders.containsKey("Status")) {
				String status = newHeaders.get("Status");
				input.status = Integer.valueOf(status.substring(0, status.indexOf(" ")));
				input.message = status.substring(status.indexOf(" ") + 1);
				newHeaders.remove("Status");
			}
			input.headers = newHeaders;
			return FileContext.create(input, "text/html", strm);
		} catch (Exception e) {
			ConnectivePHP.errorMsg("Exception in PHP generation", e);

			input.status = 503;
			input.message = "Internal server error";
			return FileContext.create(input, "text/html",
					new ByteArrayInputStream(server.genError(input, request).getBytes()));
		}
	}

	@Override
	public void provide(ConnectiveHTTPServer arg0) {
		server = arg0;
	}

	@Override
	public void provide(ProviderContext arg0) {
		context = arg0;
	}

	@Override
	public void provide(String arg0) {
		path = arg0;
	}

	@Override
	protected FilePostHandler newInstance() {
		return new PhpFileExtensionProvider(context);
	}

	@Override
	public boolean match(HttpRequest request, String path) {
		return path.endsWith(".php");
	}

	@Override
	public void process(String contentType, Socket client) {
		try {
			server = getServer();

			HashMap<String, String> newHeaders = new HashMap<String, String>(getResponse().headers);
			InputStream strm = ConnectivePHP.execPHP(path, context, getResponse(), getRequest(), server, newHeaders,
					getRequest().getBodyStream());
			getResponse().headers.put("Content-Type", "text/html");
			if (newHeaders.containsKey("Status")) {
				String status = newHeaders.get("Status");
				getResponse().status = Integer.valueOf(status.substring(0, status.indexOf(" ")));
				getResponse().message = status.substring(status.indexOf(" ") + 1);
				newHeaders.remove("Status");
			}
			getResponse().headers = newHeaders;
			setBody(strm.readAllBytes());
		} catch (Exception e) {
			ConnectivePHP.errorMsg("Exception in PHP generation", e);
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			this.setBody("text/html", server.genError(getResponse(), getRequest()));
		}
	}

}
