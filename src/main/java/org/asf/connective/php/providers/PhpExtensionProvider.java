package org.asf.connective.php.providers;

import java.io.ByteArrayInputStream;
import java.net.Socket;

import org.asf.connective.commoncgi.CgiScript.CgiContext;
import org.asf.connective.php.ConnectivePHP;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.FileContext;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.IClientSocketProvider;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IFileExtensionProvider;
import org.asf.rats.http.providers.IPathProviderExtension;
import org.asf.rats.http.providers.IServerProviderExtension;

public class PhpExtensionProvider implements IFileExtensionProvider, IClientSocketProvider, IContextProviderExtension,
		IServerProviderExtension, IPathProviderExtension {

	private ConnectiveHTTPServer server;
	private String path;
	private ProviderContext context;
	private Socket client;

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
	public void provide(Socket client) {
		this.client = client;
	}

	@Override
	public IFileExtensionProvider newInstance() {
		return new PhpExtensionProvider();
	}

	@Override
	public String fileExtension() {
		return ".php";
	}

	@Override
	public FileContext rewrite(HttpResponse input, HttpRequest request) {
		try {
			CgiContext ctx = ConnectivePHP.runPHP(context, server, request, input, client, path);
			ctx.applyToResponse(input);			
			return FileContext.create(input, "text/html", ctx.getOutput());
		} catch (Exception e) {
			ConnectivePHP.errorMsg("Exception in PHP generation", e);

			input.status = 503;
			input.message = "Internal server error";
			return FileContext.create(input, "text/html",
					new ByteArrayInputStream(server.genError(input, request).getBytes()));
		}
	}

}
