package org.asf.connective.php.providers;

import java.net.Socket;

import org.asf.connective.php.ConnectivePHP;
import org.asf.rats.HttpRequest;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.FileUploadHandler;
import org.asf.rats.http.providers.IContextProviderExtension;

public class PhpUploadHandler extends FileUploadHandler implements IContextProviderExtension {

	private ProviderContext context;

	@Override
	protected FileUploadHandler newInstance() {
		return new PhpUploadHandler();
	}

	@Override
	public boolean match(HttpRequest request, String path) {
		return path.endsWith(".php");
	}

	@Override
	public boolean process(String contentType, Socket client, String method) {
		try {
			ConnectivePHP.runPHP(context, getServer(), getRequest(), getResponse(), client, getFolderPath())
					.applyFullCGI(getResponse());
		} catch (Exception e) {
			ConnectivePHP.errorMsg("Exception in PHP generation", e);
			getResponse().status = 503;
			getResponse().message = "Internal server error";
			this.setBody("text/html", getServer().genError(getResponse(), getRequest()));
		}

		return true;
	}

	@Override
	public void provide(ProviderContext context) {
		this.context = context;
	}

}
