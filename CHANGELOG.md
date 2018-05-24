# Change Log

The change log has moved to this repo's [GitHub Releases Page](https://github.com/rollbar/rollbar-java/releases).

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
