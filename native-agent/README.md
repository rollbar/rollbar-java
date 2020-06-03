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

Regardless of your JVM language of choice, at some level there will be an invocation of the JVM and
therefore there is a configuration option to pass arguments directly to the JVM.

## Getting the agent library

We will attempt to distribute via the releases page pre-built versions of the agent library for
various architectures. However, if you are running in an environment where one of these libraries
does not work, then you can build your own as long as you can install the Rust toolchain.

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
