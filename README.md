<p align="center">
  <img alt="rollbar-logo" src="https://user-images.githubusercontent.com/3300063/207964480-54eda665-d6fe-4527-ba51-b0ab3f41f10b.png" />
</p>

<h1 align="center">Rollbar Java SDK</h1>

<p align="center">
  <strong>Proactively discover, predict, and resolve errors in real-time with <a href="https://rollbar.com">Rollbar’s</a> error monitoring platform. <a href="https://rollbar.com/signup/">Start tracking errors today</a>!</strong>
</p>

---


[![Build Status](https://github.com/rollbar/rollbar-java/workflows/rollbar-java%20CI/badge.svg?branch=master)](https://github.com/rollbar/rollbar-java/actions/workflows/ci.yml?query=branch%3Amaster)

The current library has undergone a major overhaul between versions 0.5.4 and 1.0.0.
We recommend upgrading from prior versions of `rollbar-java`, but that process may require some
work on your end for the more complex use cases of the old library.

The code is documented with javadoc and therefore should be usable from viewing
the documentation in the source. There are examples in the `examples` directory showing different
use cases for consuming these libraries.

## Key benefits of using Rollbar.js are:
- **Cross platform:** Rollbar Java SDK supports both server-side and mobile Java applications, including plaforms such as <a href="https://docs.rollbar.com/docs/android">Android</a>, <a href="https://docs.rollbar.com/docs/scala">Scala</a>, <a href="https://docs.rollbar.com/docs/spring">Spring</a>, <a href="https://docs.rollbar.com/docs/web">Web</a> and more!
- **Automatic error grouping:** Rollbar aggregates Occurrences caused by the same error into Items that represent application issues. <a href="https://docs.rollbar.com/docs/grouping-occurrences">Learn more about reducing log noise</a>.
- **Advanced search:** Filter items by many different properties. <a href="https://docs.rollbar.com/docs/search-items">Learn more about search</a>.
- **Customizable notifications:** Rollbar supports several messaging and incident management tools where your team can get notified about errors and important events by real-time alerts. <a href="https://docs.rollbar.com/docs/notifications">Learn more about Rollbar notifications</a>.


## Setup Instructions
1. [Sign up for a Rollbar account](https://rollbar.com/signup)
2. Follow the [Installation](https://docs.rollbar.com/docs/java#section-installation) instructions in our [Java SDK docs](https://docs.rollbar.com/docs/java) to install rollbar-java and configure it for your platform.

## Usage

For actual usage, the easiest way to get started is by looking at the examples:

- [rollbar-java](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-java)
- [rollbar-web](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-web)
- [rollbar-android](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-android)
- [rollbar-scala](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-scala)
- [rollbar-log4j2](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-log4j2)
- [rollbar-logback](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-logback)
- [rollbar-spring-webmvc](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-spring-webmvc)
- [rollbar-spring-boot-webmvc](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-spring-boot-webmvc)
- [rollbar-reactive-streams-reactor](https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-reactive-streams-reactor)

## Release History & Changelog

See our [Releases](https://github.com/rollbar/rollbar-java/releases) page for a list of all releases, including changes.

## Help / Support

If you run into any issues, please email us at [support@rollbar.com](mailto:support@rollbar.com)

For bug reports, please [open an issue on GitHub](https://github.com/rollbar/rollbar-java/issues/new).

## Contributing

1. [Fork it](https://github.com/rollbar/rollbar-java)
2. Create your feature branch (```git checkout -b my-new-feature```).
3. Commit your changes (```git commit -am 'Added some feature'```)
4. Push to the branch (```git push origin my-new-feature```)
5. Create new Pull Request

## License
Rollbar-java is free software released under the MIT License. See [LICENSE.txt](LICENSE.txt) for details.
