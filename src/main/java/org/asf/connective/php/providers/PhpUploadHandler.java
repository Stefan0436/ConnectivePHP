package org.asf.connective.php.providers;

import java.io.InputStream;
import java.net.Socket;

import org.asf.connective.commoncgi.CgiScript.CgiContext;
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
			CgiContext ctx = ConnectivePHP.runPHP(context, getServer(), getRequest(), getResponse(), client, getFolderPath());
			ctx.applyToResponse(getResponse());
			
			InputStream oldBody = getResponse().body;
			InputStream output = ctx.getOutput();
			output = ConnectivePHP.processMemCall(oldBody, output, getResponse(), getRequest());
			
			if (getResponse().body != null) {
				getResponse().body.close();
			}
			
			getResponse().body = output;
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
