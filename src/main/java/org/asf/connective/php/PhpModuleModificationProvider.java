package org.asf.connective.php;

import java.io.IOException;

import org.asf.rats.http.IAutoContextModificationProvider;
import org.asf.rats.http.ProviderContextFactory;

// This class allows RaTs! to use the module components, it allows
// for modifying any context created by the IAutoContextBuilders used by RaTs.
public class PhpModuleModificationProvider implements IAutoContextModificationProvider {

	@Override
	public void accept(ProviderContextFactory arg0) {
		if (!PhpModificationManager.hasBeenPrepared()) {
			try {
				PhpModificationManager.prepareModifications();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		PhpModificationManager.appy(arg0);
	}

}
