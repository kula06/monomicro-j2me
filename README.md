# monomicro-j2me

`monomicro-j2me` is a small Java ME MIDlet for old Nokia phones and other
CLDC 1.1 / MIDP 2.0 devices. It downloads a plain-text currency rate feed,
parses lines in `CODE|BUY|SELL` format, and displays the rates in a simple
MIDP list with Refresh and Exit commands.

The code intentionally avoids modern Java APIs and language features so it can
run on constrained Java ME devices.

## Project Structure

```text
.
├── build.xml
├── monomicro-j2me.jad
├── res/
│   └── META-INF/
│       └── MANIFEST.MF
└── src/
    └── com/monomicro/j2me/
        └── RatesMidlet.java
```

- `src/com/monomicro/j2me/RatesMidlet.java` - MIDlet source code.
- `res/META-INF/MANIFEST.MF` - JAR manifest for the packaged MIDlet.
- `monomicro-j2me.jad` - Java Application Descriptor for deployment.
- `build.xml` - Ant build targets for MicroEmulator and real Java ME devices.

Generated files are written to `build/` and `dist/`; both folders are ignored by
Git.

## Server Response Format

The server URL is configured with the `Rates-URL` application property in both
`monomicro-j2me.jad` and `res/META-INF/MANIFEST.MF`.

Expected response:

```text
USD|40.10|40.75
EUR|43.20|44.00
GBP|50.00|51.20
```

Rules:

- one rate per line
- fields are separated with `|`
- field order is `CODE|BUY|SELL`
- empty lines are ignored

The current default URL is:

```text
http://monomicro.pluxa.cc/rates.txt
```

## Requirements

- Apache Ant
- MicroEmulator JAR for local development
- Java 8 JDK for MicroEmulator builds
- Java ME Wireless Toolkit or Nokia SDK for real device builds

Java 8 is required for the development build because MicroEmulator can fail on
modern Java class-file versions with ASM `ClassReader` errors. The Ant build
uses `source=1.3` and `target=1.4` for `dev-jar`.

## MicroEmulator Setup

Put `microemulator.jar` in the project root:

```text
microemulator.jar
```

Or pass the path when running Ant:

```sh
ant -Dmicroemulator.jar=/path/to/microemulator.jar dev-jar
```

Set `JAVA8_HOME` when Java 8 is not the default JDK:

```sh
export JAVA8_HOME=/path/to/jdk8
```

On macOS with Temurin 8 installed in the standard location, `build.xml` can use:

```text
/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home
```

You can also pass the compiler path directly:

```sh
ant -Ddev.javac.executable=/path/to/jdk8/bin/javac dev-jar
```

## Build

Build the MicroEmulator development JAR:

```sh
ant clean dev-jar
```

Output:

```text
dist/monomicro-j2me.jar
dist/monomicro-j2me.jad
```

The default Ant target is `dev-jar`, so this also works:

```sh
ant
```

## Run

Run the built JAR directly in MicroEmulator:

```sh
ant run-microemu-jar
```

This passes a `file://` URL for the built JAR and can start the single MIDlet
directly when MicroEmulator supports it.

Run through the JAD when you need to test JAD properties such as `Rates-URL`:

```sh
ant run-microemu-jad
```

`run-microemu` is an alias for `run-microemu-jar`.

## Deploy to a Real Nokia Phone

Real devices need preverified Java ME bytecode. Use a Java ME Wireless Toolkit,
Nokia SDK, or compatible MIDP 2.0 toolchain:

```sh
ant -Dwtk.home=/path/to/wtk jar
```

The real-device build target:

1. compiles against CLDC 1.1 and MIDP 2.0 APIs
2. runs `preverify`
3. packages `dist/monomicro-j2me.jar`
4. writes `dist/monomicro-j2me.jad` with the final JAR size

Copy both files to the phone or serve them from a web server:

```text
dist/monomicro-j2me.jar
dist/monomicro-j2me.jad
```

For OTA installation, make sure the JAD points to the correct hosted JAR URL.

## Screenshots

Screenshots will be added after the first device or emulator verification pass.
