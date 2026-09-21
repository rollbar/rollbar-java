# JVMTI Native Agent

This Rust library implements a native agent for interacting with the JVM via the
[JVMTI](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html). Currently this is used for
enriching stack traces with local variables for each frame. This works by interacting with some
corresponding Java code included in the `rollbar-java` SDK.

## Using the agent

How to use the agent depends on how you invoke the JVM to start your application. In order
to use a native agent you need to pass a command line argument to this invocation. The most
basic usage would look like:

```
java -jar foo.jar -agentpath:path/to/librollbar_java_agent.dylib
```

However, if you are using a toolchain, such as Gradle, to manage your application then
adding this command line argument might take a bit more effort to figure out where to add it. For
Gradle the easiest way is to add the following to your `build.gradle` file:

```
applicationDefaultJvmArgs = ["-agentpath:path/to/"+System.mapLibraryName("rollbar_java_agent")]
```

Once you have your Java code using the Native Agent, you need make sure your `rollbar-java` is
configured with appPackages at a minimum. If you also want to handle uncaught exceptions,
make sure the handleUncaughtErrors is set to true.

```
// [appPackages]: Add a list of packages considered to be in your app.  This is used
//   to filter out exceptions that don't match the package name in the stacktrace.

// [handleUncaughtErrors]: Set to true for unhandled exceptions

new Rollbar(withAccessToken("ACCESS-TOKEN")
    .appPackages(Arrays.asList("com.example.app"))
    .handleUncaughtErrors(true)
    .build());
```

Once you have the agent setup and `rollbar-java` configured, `rollbar-java` will attribute the exceptions
using the agent as well as send back unhandled exceptions if configured.

Two things are required, and the agent captures nothing if either is missing:

* **`appPackages` must be configured.** It is not an optional filter — it is the switch that
  turns capture on. With no app packages the agent stays dormant.
* **Your classes must be compiled with debug information** (`javac -g`, which Gradle and Maven
  enable by default for `debug = true`). Local variable names live in the `LocalVariableTable`
  attribute; without `-g` the agent has nothing to read and frames come back with no locals.

Note also that the cache is keyed per thread: locals are only available if the throwable is
reported on the same thread that threw it. Handing an exception to another thread (an executor,
an async error handler) before reporting it loses the locals.

## How the agent activates

The agent does nothing until `com.rollbar.jvmti.ThrowableCache` is loaded. It watches for that
class via a JVMTI `ClassPrepare` callback and only then enables exception capture, so an
application that never initialises Rollbar — and the whole of JVM and framework startup before
Rollbar *is* initialised — pays essentially nothing.

This also means the agent resolves `ThrowableCache` through whichever classloader actually
loaded it. 

Regardless of your JVM language of choice, at some level there will be an invocation of the JVM and
therefore there is a configuration option to pass arguments directly to the JVM.

## Getting the agent library

No pre-built binaries have been published since 1.4.1, and nothing in CI builds this library, so
in practice you should expect to build it yourself. It is a plain Cargo project and is not part
of the Gradle build — `./gradlew build` does not produce it.

Build for the architecture you will run on: an `x86_64` library will not load on an Apple Silicon
JVM (`mach-o file, but is an incompatible architecture`). `build.rs` reads `JAVA_HOME` to locate
the JNI headers, so make sure it points at a real JDK.

### Building Generically

* Install Rust: [https://www.rust-lang.org/tools/install](https://www.rust-lang.org/tools/install)
* `cargo build --release`
* Get library from `target/release/librollbar_java_agent.{so,dll,dylib}`

### Building on a Mac for Linux

In the particular case where you are using a Mac but want to build a shared library that works on
Linux, you have to do a little bit of extra work. Luckily, Rust has a decent cross compilation
story. The first step is adding the right target via `rustup`:

* `rustup target add x86_64-unknown-linux-gnu`

This is not enough because you need a cross compiling toolchain, in particular a linker,
that does the right thing. You can get this via:

* `brew tap SergioBenitez/osxct`
* `brew install x86_64-unknown-linux-gnu`
  - You might have to run `xcode-select --install` first depending on your setup

Once that is setup, you can build for the specified target:

* `cargo build --release --target x86_64-unknown-linux-gnu`

You will find the resulting `.so` located at:

```
target/x86_64-unknown-linux-gnu/release/librollbar_java_agent.so
```

## Debugging

If you want to see additional output from our agent, you can set the environment variable
`ROLLBAR_LOG` to one of `trace`, `debug`, `info`, or `warn`. These will output different levels of
information to standard out where your JVM process is running.

At `info` you should see the agent arm itself exactly once, shortly after Rollbar is constructed:

```
INFO  rollbar_java_agent > rollbar agent: armed, exception capture enabled
```

If that line never appears, `ThrowableCache` was never loaded — check that `rollbar-java` is on
the classpath and that a `Rollbar` instance is actually being created.

If the JVM refuses to start with `agent library failed to init` and `ROLLBAR_LOG=warn` shows
`GetEnv failed: -3`, the JVM is older than the agent expects. The agent asks for JVMTI 1.2, which
every JDK 8 or newer provides; earlier builds asked for the version of the JDK they were compiled
against, so a library built with JDK 21 would refuse to load on JDK 17.
