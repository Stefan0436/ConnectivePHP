# ConnectiveHTTP PHP Module
PHP Support for Connective.

<br />

# Building the ConnectivePHP Module
This project is build using gradle, but it supplies the wrapper so you dont need to install it.<br />
What you do need is Java JDK, if you have it installed, proceed with the following commands depending
on your operating system.

## Building for linux
Run the following commands:

```bash
chmod +x gradlew createlocalserver.sh
./createlocalserver.sh
./gradlew createEclipseLaunches build
cd build/libs
```

## Building for windows
Run the following commands:

```batch
gradlew.bat createEclipseLaunches build
cd build\libs
```

<br />

# Installing the ConnectivePHP Module
The installation depends on your server software.<br />
Though it is not too different for each server type.

## Installing for the standalone server
You can install the module by placing the jar in the `modules` directory of the server.

## Installing for ASF RaTs! (Remote Advanced Testing Suite)
First, drop the module in the `main` folder of your RaTs! installation.<br />
After which, add the following line to the `classes` key of the `components.ccfg` file:

```
# File: components.ccfg
# ...
classes> {

    # ...

    org.asf.connective.php.ConnectivePHP> 'ConnectivePHP-1.0.0.A1.jar'
    org.asf.connective.php.PhpModuleModificationProvider> 'ConnectivePHP-1.0.0.A1.jar'

    # ...

}
# ...
```

## Enabling the modifications on the standalone server
To enable the modifications, one will need to add the following to their server configuration:

```
# File: server.ccfg
# ...
context> {
  # ...

    # We use root, but you can add the instructions to any of your contextfiles
    root> '
    # ...

    extension "class:org.asf.connective.php.PhpFileExtensionProvider"

    posthandler "class:org.asf.connective.php.PhpFileExtensionProvider"

    # ...
    '

  # ...
}
# ...

```

<br />

# Version Notice:
This module was build targeting ASF Connective version 1.0.0.A2,
it may not work on newer or older versions.

# Copyright Notice:
This project is licensed under the LGPL 3.0 license.<br />
Copyright(c) 2021 AerialWorks Software Foundation.<br />
Free software, read LGPL 3.0 license document for more information.<br />
<br />
This project uses the ConnectiveHTTP libraries.<br />
Copyright(c) 2021 AerialWorks Software Foundation.
