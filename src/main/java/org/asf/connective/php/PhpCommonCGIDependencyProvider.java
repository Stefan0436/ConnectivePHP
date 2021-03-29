package org.asf.connective.php;

import org.asf.connective.standalone.IModuleMavenDependencyProvider;

public class PhpCommonCGIDependencyProvider implements IModuleMavenDependencyProvider {

	@Override
	public String group() {
		return "org.asf.connective.commoncgi";
	}

	@Override
	public String name() {
		return "CommonCGI";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

}
