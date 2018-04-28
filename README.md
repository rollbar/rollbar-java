# Rollbar for Java and Android

[![Build Status](https://travis-ci.org/rollbar/rollbar-java.svg?branch=master)](https://travis-ci.org/rollbar/rollbar-java)

The current library has undergone a major overhaul between versions 0.5.4 and 1.0.0.
We recommend upgrading from prior versions of `rollbar-java`, but that process may require some
work on your end for the more complex use cases of the old library.

The code is documented with javadoc and therefore should be usable from viewing
the documentation in the source. There are examples in the `examples` directory showing different
use cases for consuming these libraries.

There are currently four libraries in this repository:

* `rollbar-api`
* `rollbar-java`
* `rollbar-web`
* `rollbar-android`
* `rollbar-log4j2`
* `rollbar-logback`

`rollbar-api` is a set of data objects representing structures that make up the payload
the backend API understands.

`rollbar-java` has the core functionality for taking input from your code and transmitting
it to our API. `rollbar-java` depends on `rollbar-api` and provides many points of
customizing its behavior.

`rollbar-web` is a higher level abstraction built on `rollbar-java` which intended to be
integrated into web servers based on the Servlet API.

`rollbar-android` is a library for use in an Android environment built on `rollbar-java`.

The example directory contains examples using `rollbar-java` directly as well as using
`rollbar-web` and `rollbar-android`.

## Feedback

To report problems or ask a question please [create an issue](https://github.com/rollbar/rollbar-java/issues/new).

## Installation

For the most basic Java applications use:

```groovy
compile('com.rollbar:rollbar-java:1.1.1')
```

If you require direct access to the underlying API objects include `rollbar-api` as a dependency.
For Android include `rollbar-android:1.1.1@aar`. For web projects include `rollbar-web`.

## Upgrading from 0.5.4 or earlier to 1.0.0+

This package used to be divided into five modules

* `rollbar-utilities`
* `rollbar-testing`
* `rollbar-sender`
* `rollbar-payload`
* `rollbar`

As of 1.0.0 we have changed the project structure to these modules

* `rollbar-api`
* `rollbar-java`
* `rollbar-web`
* `rollbar-android`

`rollbar-api` contains roughly the same objects as `rollbar-payload` previously did. The main
difference being that the objects are now constructed via builders rather than a new allocation in
every setter. Therefore, any usage of `rollbar-payload` objects is still possible, but the style is
slightly changed. For example, one of the examples for specifying the server information from the
old documentation
https://github.com/rollbar/rollbar-java/tree/496eb59edea7203a246f207986e332ee28d1916c/rollbar
stated:

```java
Server s = new Server()
    .host("www.rollbar.com")
    .branch("master")
    .codeVersion("b01ff9e")
    .put("TAttUQoLtUaE", 42);
```

The equivalent is now:

```java
Server s = new Server.Builder()
    .host("www.rollbar.com")
    .branch("master")
    .codeVersion("b01ff9e")
    .build();
```

The Extensible base class from `rollbar-utilities` is no longer used in favor of sticking more
closely to the spec.

The other use cases from those old docs was calling `send` directly on a `Payload` object:

```java
// Throwable t
Payload p = Payload.fromError(SERVER_POST_ACCESS_TOKEN, ENVIRONMENT, t, null);
p.send();
```

This is no longer directly supported. The equivalent is to use `Rollbar` directly, either by
constructing a new instance (`new Rollbar(config)`) or by using the library managed singleton
(`Rollbar.init(config)`):

```java
// Throwable t
Config config = withAccessToken(SERVER_POST_ACCESS_TOKEN)
        .environment("development")
        .build();
Rollbar rollbar = Rollbar.init(config);
rollbar.error(t);
```

There is a shim that has the same basic API as the old library located in the `rollbar-java`
package at `com.rollbar.Rollbar`. This class is marked as deprecated as it is only intended to
make upgrading slightly more convenient. This old example code should still work thanks to this shim class:

```java
import com.rollbar.Rollbar;
public class MainClass {
    public static final Rollbar rollbar = new Rollbar("ACCESS_TOKEN", "production");
    public int main(String[] args) {
        rollbar.handleUncaughtErrors();
        OtherClass.runProgram();
        return 0;
    }
}
```

However, we strongly advise upgrading to at least this equivalent using the new library:

```java
 import com.rollbar.notifier.Rollbar;
 public class MainClass {
     public static final Rollbar rollbar = new Rollbar(
         withAccessToken("ACCESS_TOKEN")
             .environment("production")
             .handleUncaughtErrors(true)
             .build());
     public int main(String[] args) {
         OtherClass.runProgram();
         return 0;
     }
 }
```

## Installing

You can, of course, build it yourself and depend on the .jar manually,
however, the modules are up on maven central and can be installed in
most tool chains.

### Maven

All these can be installed as Maven projects. Simply add the
dependency to your pom file:

```xml
<dependencies>
<dependency>
  <groupId>com.rollbar</groupId>
   <artifactId>rollbar-java</artifactId>
   <version>1.1.1</version>
</dependency>
</dependencies>
```

### Gradle

```groovy
compile('com.rollbar:rollbar-java:1.1.1')
```

### Android

As described above, this library is split into different components that build upon each other.
There is an Android specific part of the library. Therefore for Android you should use that
interface which requires you to add this dependency to your build process. For example,

```groovy
compile('com.rollbar:rollbar-java:1.1.1')
compile('com.rollbar:rollbar-android:1.1.1@aar')
```

## How payloads are sent

The actual notifier configuration builds a notifier that uses a BufferedSender to send the items
to Rollbar. That sender is built using an unbound memory queue and a scheduled thread to send
the events from the queue. 
The queue as well as the frequency of the scheduled thread can be customized
when building the buffered sender and it can be replaced by passing a custom configuration when 
creating the notifier or initializing it.
 
## Usage

For actual usage, the easiest way to get started is by looking at the examples:

- [rollbar-java](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-java)
- [rollbar-web](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-web)
- [rollbar-android](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-android)
- [rollbar-scala](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-scala)
- [rollbar-log4j2](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-log4j2)
- [rollbar-logback](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-logback)


### Spring 

Check out [this blog post](https://rollbar.com/blog/spring-mvc-exception-handling/) for more information on how to use rollbar-java in your Spring app. 

## How to build it
To build the notifier there are some system environment variables that are needed.

- ANDROID_HOME. Pointing to the android sdk.
- JAVA_HOME. Pointing to the java8 sdk.
- JDK7_HOME. Pointing to the java7 sdk.


```
./gradlew clean build
```
 
## Contributing

1. [Fork it](https://github.com/rollbar/rollbar-java)
2. Create your feature branch (```git checkout -b my-new-feature```).
3. Commit your changes (```git commit -am 'Added some feature'```)
4. Push to the branch (```git push origin my-new-feature```)
5. Create new Pull Request
