# READ THIS FIRST

[![Build Status](https://travis-ci.org/rollbar/rollbar-java.svg?branch=master)](https://travis-ci.org/rollbar/rollbar-java)

The current library has undergone a major overhaul and is released as an alpha version.
We do not recommend upgrading yet from any prior version of `rollbar-java` yet. This
disclaimer will be removed and a non-alpha version will be released when we are confident
in general consumption of this library.

The documentation below has yet to be updated to match the new code in the alpha release.
However, the code is documented with javadoc and therefore should be usable from viewing
the documentation in the source.

There are currently four libraries in this repository:

* `rollbar-api`
* `rollbar-java`
* `rollbar-web`
* `rollbar-android`

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

# Feedback

To report problems or ask a question about the alpha release, please [create an issue](https://github.com/rollbar/rollbar-java/issues/new) and apply the label `1.0.0-alpha` so our team can follow up with you.

## Installation

```groovy
compile('com.rollbar:rollbar-java:1.0.0-alpha-1')
```

## See below for the old library documentation

## Overview


`java-rollbar` is a set of maven modules that allows reporting issues to
Rollbar in anything that can use Java.

It's still under development, and many of the design decisions may still
be altered. If you have an opinion voice it in the issues!

The library is split into small modules to enable re-use as much as
possible. If you want to change a single piece of how it works it should
be relatively straightforward.

* `rollbar-utilities` contains code shared by the other modules.
* `rollbar-testing` contains shared test code.
* `rollbar-sender` implements sending occurrences to Rollbar. No external
dependencies make this lightweight, but a good candidate for an
upgrade.
* `rollbar-payload` implements a Payload object that can be serialized to
JSON. It does so with no external dependencies.
* `rollbar` brings together all the pieces from above to make it easy to
install and start recording errors.

## Installing

You can, of course, build it yourself and depend on the .jar manually,
however, the modules are up on maven central and can be installed in
most tool chains pretty trivially.

### Maven

All these can be installed as Maven projects. Simply add the
dependency to your pom file:

```xml
<dependencies>
<dependency>
  <groupId>com.rollbar</groupId>
   <artifactId>rollbar</artifactId>
   <version>0.5.4</version>
</dependency>
</dependencies>
```

### Gradle

```groovy
compile('com.rollbar:rollbar:0.5.4')
```

## Usage

For actual usage, the easiest way to get started is with the `rollbar`
package. See the [documentation there](https://github.com/rollbar/rollbar-java/tree/master/rollbar).

## Contributing

This library was written by someone who knows C# much better than Java. Feel free to issue stylistic PRs, or offer
suggestions on how we can improve this library.

1. [Fork it](https://github.com/rollbar/rollbar-java)
2. Create your feature branch (```git checkout -b my-new-feature```).
3. Commit your changes (```git commit -am 'Added some feature'```)
4. Push to the branch (```git push origin my-new-feature```)
5. Create new Pull Request
