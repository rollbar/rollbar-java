<p align="center">
    <a href="https://rollbar.com" target="_blank" align="center">
        <img src="https://rollbar.com/assets/media/rollbar-logo-color-horiz.png" width="280">
    </a>
<br/>
    <h1>Rollbar for Java</h1>
</p>

[![Build Status](https://travis-ci.org/rollbar/rollbar-java.svg?branch=master)](https://travis-ci.org/rollbar/rollbar-java)


## Setup Instructions

1. [Sign up for a Rollbar account](https://rollbar.com/signup)
2. Follow the [Installation](https://docs.rollbar.com/docs/java#section-installation) instructions in our [Java SDK docs](https://docs.rollbar.com/docs/java) to install rollbar-java and configure it for your platform.
 
## Usage

For actual usage, the easiest way to get started is by looking at the examples:

- [rollbar-java](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-java)
- [rollbar-web](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-web)
- [rollbar-android](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-android)
- [rollbar-scala](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-scala)
- [rollbar-spring-webmvc](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-spring-webmvc)
- [rollbar-spring-boot-webmvc](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-spring-boot-webmvc)
- [rollbar-log4j2](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-log4j2)
- [rollbar-logback](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-logback)

## Release History & Changelog

See our [Releases](https://github.com/rollbar/rollbar-java/releases) page for a list of all releases, including changes.
 
## Help / Support

If you run into any issues, please email us at [support@rollbar.com](mailto:support@rollbar.com)

For bug reports, please [open an issue on GitHub](https://github.com/rollbar/rollbar-java/issues/new).

## For Developers

Instructions for developers on how to build, develop, test and contribute to rollbar-java.

### Build
**Dependencies**

Ensure you have the following dependencies installed on your machine:
* Android SDK
* Java 8 SDK
* Java 7 SDK

**Eenvironment variables**

Set the following variables:
* ANDROID_HOME - Point to the android sdk
* JAVA_HOME - Point to the java8 sdk
* JDK7_HOME - Point to java7 sdk

**Build rollbar-java**
```shell script
./gradlew clean build
```

### Develop

**JavaDocs**

For quick reference into the code, you can reference the JavaDocs here: https://javadoc.io/doc/com.rollbar

**Project gradle tasks**

To run individual gradle tasks by project similar to this example with spring-boot.

```shell script
./gradlew rollbar-spring-boot-webmvc:build
````

This is helpful when you develop for a specific component.

**Proxy**

If you need to test and develop within a proxy, you can create a java.net.Proxy object and pass it with the Rollbar configuration builder.

```java
RollbarConfigBuilder.withAccessToken(accessToken)             
            .environment("development")
            .proxy(proxy) // Pass your java.net.Proxy object here
            .build();
```


### Test

When you are ready to test this against Rollbar, you can create a test project and use the access token from that project to send events.

### Contribute

1. [Fork it](https://github.com/rollbar/rollbar-java)
2. Create your feature branch (```git checkout -b my-new-feature```).
3. Commit your changes (```git commit -am 'Added some feature'```)
4. Push to the branch (```git push origin my-new-feature```)
5. Create new Pull Request

## Resources

* [Examples](https://github.com/rollbar/rollbar-java/tree/master/examples)
* [Documentation](https://docs.rollbar.com/docs/java)

## Developer Community

Join our Slack Channel here -> [[Join slack channel]](https://join.slack.com/t/rollbar-developers/shared_invite/zt-f1tvgecw-m9~mtXLsP2wETpQg3vanTQ) 

## P.S. -- Documentation is available at https://docs.rollbar.com/docs/java

<p align="center">
        <a href="https://docs.rollbar.com/docs/java"><img src="https://user-images.githubusercontent.com/398292/83357965-b6d84b80-a324-11ea-8533-fcb01e6ee8f4.png" align="center" /></a>
</p>

## License

Rollbar-java is free software released under the MIT License. See [LICENSE.txt](LICENSE.txt) for details.
