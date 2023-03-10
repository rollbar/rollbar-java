# Change Log

# 1.10.0

- Add support for Spring 6.x and Spring Boot 3.x. [#302](https://github.com/rollbar/rollbar-java/pull/302)

# 1.9.0

- Enable maven publishing for rollbar-reactive-streams and rollbar-reactive-streams-reactor artifacts. [#297](https://github.com/rollbar/rollbar-java/pull/297)

# 1.8.1

- Update log4j dependencies to v2.17.0 to fix CVE-2021-45105 [#291](https://github.com/rollbar/rollbar-java/pull/291)

# 1.8.0

- Update log4j dependencies to v 2.16.0 (#287)
- log4j 2.15.0 - fixes security vulnerability CVE-2021-44228 (#285) (#286)
  This change removes Java 7 compatibility for `rollbar-log4j2`. See [rollbar-log4j2/README.md](rollbar-log4j2/README.md) for more details.
- Update PR template (#284)

# 1.7.10

- Add option to truncate payloads before sending them to Rollbar.
- Disable vmlens tests when running in Java 11, since it occasionally generates invalid bytecode.

# 1.7.9

- Add rollbar-android option to detect when the network is unavailable and suspend sending occurrences.
- Add [reactive-streams](http://www.reactive-streams.org/) Notifier implementation with optional [Reactor](https://projectreactor.io/) support.

# 1.7.8

- Fix serialization of objects containing quotes in custom section of payload.
- Set enabled to true by default in log4j2 appender. [#270](https://github.com/rollbar/rollbar-java/issues/270)

# 1.7.7

-  Don't require an access token in log4j appender when configuration class is provided.
-  Add revapi binary and source backwards compatibility check to build.
-  Use generated class instead of jar manifest to populate notifier version in payload.

# 1.7.6

- Add struts2 integration with examples.
- Add all request parameters for post request method.
- Move all Travis build steps to Github action.
- Fix build on JDK 11 and add JDK 11 to GH action matrix.
- Catch Throwable in payload send task, to ensure it doesn't stop being scheduled. [#246](https://github.com/rollbar/rollbar-java/issues/246)

# 1.7.5

rollbar-logback
--------------
- Adds new param for Argument Array.

# 1.7.4

rollbar-log4j2
--------------
- Fix [#230](https://github.com/rollbar/rollbar-java/issues/230)

# 1.7.3

Build system
------------
- Upgrade to use gradle 6.3. [#221](https://github.com/rollbar/rollbar-java/pull/221)

rollbar-api
-----------
- Fix not present `method` property when json serialization. [#222](https://github.com/rollbar/rollbar-java/pull/222)

# 1.7.2

New integrations
----------------
- rollbar-spring-webmvc
- rollbar-spring-boot-webmvc

Update to existing integrations
-------------------------------
- \[rollbar-android\] Remove VERSION constant in rollbar-android and load from metadata. [#214](https://github.com/rollbar/rollbar-java/pull/214)

# 1.7.1

rollbar-android
---------------
- Fix jacoco error that breaks the application.
- Add proguard rules to keep classes need for Serialization.
- Make `DiskQueue` to delete payload files error when reading.

# 1.7.0
NOTE: **This build was broken when publishing to maven. Please do not use.** [#202](https://github.com/rollbar/rollbar-java/issues/202)

rollbar-android
---------------
- Use by default `DiskQueue` instead of `ConcurrentLinkedQueue` in sender. [#200](https://github.com/rollbar/rollbar-java/pull/200)
- Fix IndexOutOfBoundsException in DiskQueue.poll(). [#200](https://github.com/rollbar/rollbar-java/pull/200)
- Use setDefaultUncaughtExceptionHandler to capture all uncaught exceptions. [#200](https://github.com/rollbar/rollbar-java/pull/200)

rollbar-java
------------
- Fix for not expected server responses. [#198](https://github.com/rollbar/rollbar-java/pull/198)

# 1.6.0
- Add codeVersion as an option to Log4j2 appender. [#196](https://github.com/rollbar/rollbar-java/pull/196)
- Allow configuring more options via logback xml. [#194](https://github.com/rollbar/rollbar-java/pull/194)
- Fix pre filter error NullPointerException. [#190](https://github.com/rollbar/rollbar-java/pull/190)
- Add enabled as an option to log4j2 appender. [#188](https://github.com/rollbar/rollbar-java/pull/188)

# 1.5.2
- Make `RollbarAppender(Rollbar rollbar)` constructor public and allow `start()` to skip `Rollbar` 
  instantiation so `RollbarAppender` can be constructed and started programmatically. [#184](https://github.com/rollbar/rollbar-java/issues/184)

# 1.5.1
- Make RollbarFilter dependency-injectable for rollbar-web. [#182](https://github.com/rollbar/rollbar-java/issues/182)

# 1.5.0
- Enable sending complete JSON payloads, as proxy for other SDKs. [#180](https://github.com/rollbar/rollbar-java/pull/180)

# 1.4.1
- Add configuration to determine default item level. [#179](https://github.com/rollbar/rollbar-java/pull/179)
- Allow the maximum number of logcat lines to be configured. [#178](https://github.com/rollbar/rollbar-java/pull/178)
- Provide convenience setter for JsonSerializer. [#177](https://github.com/rollbar/rollbar-java/pull/177)

# 1.4.0
- Capture local variables in stack frames with a native agent. [#169](https://github.com/rollbar/rollbar-java/pull/169)
- Only create the default sender if a custom one is not present. [#168](https://github.com/rollbar/rollbar-java/pull/168)

# 1.3.1
- Remove use of java.util.Objects as is not available in android sdk version lower than 19. [#162](https://github.com/rollbar/rollbar-java/pull/162)

# 1.3.0
- Fix rollbar-log4j2 appender by overriding stop methods and close the Rollbar client. [#156](https://github.com/rollbar/rollbar-java/pull/156)
- Add configuration options to the rollbar-log4j2 to match the ones of the rollbar-logback. [#157](https://github.com/rollbar/rollbar-java/pull/157)
- Fix rollbar-android publication by setting in the pom.xml the packaging value as `arr`. [#158](https://github.com/rollbar/rollbar-java/pull/158)
- Fix rollbar-android dependencies to include the one declared as api. [#159](https://github.com/rollbar/rollbar-java/pull/159)
- Add feature to set up a proxy to be used by to send the payloads. [#154](https://github.com/rollbar/rollbar-java/pull/154)

## 1.2.1
- Fix NPE when not passing default values in rollbar-logback and override stop method to stop the appender [#147](https://github.com/rollbar/rollbar-java/pull/147)

## 1.2.0
- Added configuration options for `rollbar-web` and `rollbar-android` to specify how IP addresses
  are captured. For `rollbar-web` this is configured via the `capture_ip` filter init parameter.
  This accepts the values: `"full"`, `"anonymize"`, and `"none"`. `"full"` is the default and means
  we capture the full IP address from a request. `"anonymize"` implies that we take the full IP
  address and mask out the least significant bits. `"none"` means we do not capture the IP address
  at all.
  For `rollbar-android` one of the three string values: `"full"`, `"anonymize"`, or `"none"` can be
  passed to the `Rollbar` initializer. Here this refers to how the backend attempts to capture the
  IP address of the client when an item is submitted. `"full"` again is the default and the current
  behaviour where the full IP address of the client is gathered. `"anonymize"` takes the same IP
  address and masks out the least significant bits. `"none"` implies that the client IP is not
  captured.

  See [#144](https://github.com/rollbar/rollbar-java/pull/144)

- Better support for configuring the `rollbar-logback` integration as well as the ability to easily
  set the endpoint for submitting items directly on the Configuration object.
  See [#143](https://github.com/rollbar/rollbar-java/pull/143)

## 1.1.0
- Android integration compatible with api level 16 [#123](https://github.com/rollbar/rollbar-java/pull/123)
- Add logback integration [#122](https://github.com/rollbar/rollbar-java/pull/122)
- Add close method with wait flag to sender [#121](https://github.com/rollbar/rollbar-java/pull/121)
- Use buffered sender by default [#119](https://github.com/rollbar/rollbar-java/pull/119)
- Add log4j2 integration [#114](https://github.com/rollbar/rollbar-java/pull/114)
- Add enabled flag to configuration [#113](https://github.com/rollbar/rollbar-java/pull/113)

## 1.0.1
- Allow arbitrary extra data in API objects that support this loose format: Client, Person, Request, Message and Server [#104](https://github.com/rollbar/rollbar-java/pull/104)
- Add support to know if an exception was uncaught [#102](https://github.com/rollbar/rollbar-java/pull/102)
- Query param list of one element are send as the element instead of list [#92](https://github.com/rollbar/rollbar-java/pull/92)
- Timestamp sent in seconds to Rollbar [#91](https://github.com/rollbar/rollbar-java/pull/91)
- Make public BodyContent interface [#90](https://github.com/rollbar/rollbar-java/pull/90)

## 1.0.0
- Breaking change from the previous versions of the notifier
- New notifier package naming and modules structure
- Change build process to gradle
