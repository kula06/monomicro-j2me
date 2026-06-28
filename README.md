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
├── Dockerfile.j2me-build
├── monomicro-j2me.jad
├── res/
│   └── META-INF/
│       └── MANIFEST.MF
├── scripts/
│   └── build-real.sh
└── src/
    └── com/monomicro/j2me/
        └── RatesMidlet.java
```

- `src/com/monomicro/j2me/RatesMidlet.java` - MIDlet source code.
- `res/META-INF/MANIFEST.MF` - JAR manifest for the packaged MIDlet.
- `monomicro-j2me.jad` - Java Application Descriptor for deployment.
- `build.xml` - Ant build targets for MicroEmulator and real Java ME devices.
- `Dockerfile.j2me-build` - containerized Java ME build environment.
- `scripts/build-real.sh` - Docker wrapper for the real Nokia build.

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
- Docker for real-device builds on macOS
- Java ME Wireless Toolkit or Nokia SDK for manual real-device builds

Java 8 is required because MicroEmulator and old CLDC/MIDP tooling cannot use
modern Java class-file versions. The Docker real-device build includes Java 8,
CLDC 1.1, MIDP 2.0, and `preverify`.

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

## MicroEmulator Build

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

Do not install the `dev-jar` output on a real phone. It is for MicroEmulator
only and is not preverified for CLDC devices.

## Real Nokia Build With Docker

Use this build for Nokia Series 40 and other real CLDC/MIDP devices:

```sh
scripts/build-real.sh
```

The script builds `Dockerfile.j2me-build`, then runs:

```sh
ant clean real-jar
```

inside the container. The Docker image contains:

- Java 8
- CLDC 1.1 API
- MIDP 2.0 API
- `preverify`

Output:

```text
dist/monomicro-j2me.jar
dist/monomicro-j2me.jad
```

The JAR is packaged from `build/preverified` only. This is the build to copy to
a real Nokia phone.

On Apple Silicon Macs the script uses `linux/amd64` by default because the old
WTK tools are 32-bit x86 Linux binaries. Override when needed:

```sh
J2ME_BUILD_PLATFORM=linux/amd64 scripts/build-real.sh
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

## Manual Real Nokia Build

If you already have a Java ME Wireless Toolkit, Nokia SDK, or compatible MIDP
2.0 toolchain installed locally, you can run the real build without Docker:

```sh
export JAVA8_HOME=/path/to/jdk8
export WTK_HOME=/path/to/wtk
ant clean real-jar
```

You can also pass paths directly:

```sh
ant -Dwtk.home=/path/to/wtk \
    -Dreal.javac.executable=/path/to/jdk8/bin/javac \
    clean real-jar
```

For SDKs with a different folder layout, override the individual files:

```sh
ant -Dcldcapi.jar=/path/to/cldcapi11.jar \
    -Dmidpapi.jar=/path/to/midpapi20.jar \
    -Dpreverify=/path/to/preverify \
    -Dreal.javac.executable=/path/to/jdk8/bin/javac \
    clean real-jar
```

The `jar` target is an alias for `real-jar`, but `real-jar` is the recommended
command for phone builds because it makes the intent explicit.

## Deploy to a Real Nokia Phone

The real-device build target:

1. compiles against CLDC 1.1 and MIDP 2.0 APIs
2. runs `preverify`
3. packages only the preverified classes into `dist/monomicro-j2me.jar`
4. writes `dist/monomicro-j2me.jad` with the final JAR size

Copy both files to the phone or serve them from a web server:

```text
dist/monomicro-j2me.jar
dist/monomicro-j2me.jad
```

For OTA installation, make sure the JAD points to the correct hosted JAR URL.

## Screenshots

Screenshots will be added after the first device or emulator verification pass.
