# Change Log

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