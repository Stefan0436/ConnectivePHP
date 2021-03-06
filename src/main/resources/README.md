# ConnectiveHTTP PHP Module
${description}

<br />

# Building the ${pretty_name}
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
cd build\\libs
```

<br />

# Installing the ${pretty_name}
The installation depends on your server software.<br />
Though it is not too different for each server type.

## Installing for the standalone server
${install.standalone.desc}

$moduleConfigAdditionsStandalone

## Installing for ASF RaTs! (Remote Advanced Testing Suite)
${install.rats.desc}<br />
After which, add the following line to the `${install.rats.config.key}` key of the `components.ccfg` file:

```
# File: components.ccfg
# ...
${install.rats.config.key}> {

    # ...

    ${install.rats.config.entry}> '${name}-${version}.jar'
${otherclasses.rats}
    # ...

}
# ...
```

### Module dependencies
This module depends on the 'CommonCGI' library, it is automatically downloaded on the standalone server, but needs to be installed manually on RaTs!<br/>
<br/>
You can get the latest version [here](https://aerialworks.ddns.net/maven/org/asf/connective/commoncgi/CommonCGI/) (select the jarfile that does not end with sources or javadoc)<br/>
After downloading, drop the jar in the main directory of RaTs!

# Version Notice:
This module was build targeting ASF Connective version ${connectiveversion},
it may not work on newer or older versions.

# Copyright Notice:
This project is licensed under the ${license} license.<br />
Copyright(c) ${buildyear} ${authorname}.<br />
${copyrightsuffix}.<br />
<br />
This project uses the ConnectiveHTTP libraries.<br />
Copyright(c) 2021 AerialWorks Software Foundation.
