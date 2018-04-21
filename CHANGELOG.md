# Change Log

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