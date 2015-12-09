# Rollbar

Rollbar is an official Java notifier for Rollbar. It is a work in progress. It represents a payload to be sent to
Rollbar and ensures that all (applicable) portions of the payload spec'ed in https://rollbar.com/docs/api/items_post/
can be sent correctly.

## Installation

Installation is through Maven or cloning the repo, compiling it, and including it in your project manually.

## Usage

Rollbar provides a set of classes for building and sending error reports to Rollbar.

Since catching the errors to report is so framework specific we've kept this package separate from the packages that
integrate directly into your framework, be it Spring, Swing, Play, or something completely custom.

The simplest way to use this is with a `try/catch` like so:

```java
public class MyClass {
    public static final String SERVER_POST_ACCESS_TOKEN = getAccessToken();
    public static final String ENVIRONMENT = currentEnvironment();

    /*...*/
    public void doSomething() {
        try {
            this.monitoredMethod();
        } catch (Throwable t) {
            Payload p = Payload.fromError(SERVER_POST_ACCESS_TOKEN, ENVIRONMENT, t, null);
            try {
                // Here you can filter or transform the payload as needed before sending it
                p.send();
            } catch (ConnectionFailedExeption e) {
                Logger.getLogger(MyClass.class.getName()).severe(p.toJson());
            }
            // You can obviously choose to do something *other* than re-throw the exception
            throw t;
        }
    }
}
```

Uncaught errors look different depending on the framework you're using. Play, for instance, uses an `HttpErrorHandler`.
For general use you'll want to do something like this:

```java
public class Program {
    public static final String SERVER_POST_ACCESS_TOKEN = getAccessToken();
    public static final String ENVIRONMENT = currentEnvironment();

    /*...*/
    public void main(String[] argv) {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Payload p = Payload.fromError(SERVER_POST_ACCESS_TOKEN, ENVIRONMENT, t, null);
                try {
                    // Here you can filter or transform the payload as needed before sending it
                    p.send();
                } catch (ConnectionFailedExeption e) {
                    Logger.getLogger(MyClass.class.getName()).severe(p.toJson());
                }
                throw t;
            }
        });
    }
}
```

Note that that snippet monitors a **single** thread. If your framework starts a thread per request, or uses different
threads for different parts of your UI you'll need to take that into consideration.

## Tips for Optimal Usage

 * If you can construct the `Payload`, it compiles, and does not throw an exception in the process, then it's valid to
   send to Rollbar and will be displayed correctly in the interface.

 * Everything in the `payload` package is immutable, exposing a fluent interface for making small changes to the Payload.

   In practice this means the 'setters' are very expensive (because they create a whole new object every time). Since
   reporting exceptions should be the "exceptional" case, this should not matter in practice. (Please report any serious
   performance issues!). If you are only going to alter one or two fields then using these setters is a great time
   saver, and you should feel free to use them. If you are fully customizing a portion of your payload with lots of
   custom data, however, you should use the constructor that exposes all the fields available to the class.
 * The fluent interface uses `property()` as the getter, and `proerty(TypeName val)` as the setter, rather than the
   typical Java `getProperty()` and `setProperty()` convention. This should help remind the user that the setter isn't
   doing a simple set operation, and results in an attractive (to the Author) fluent interface:

   ```java
   Server s = new Server()
       .host("www.rollbar.com")
       .branch("master")
       .codeVersion("b01ff9e")
       .put("TAttUQoLtUaE", 42);
   ```

 * Every class in `payload` has two constructors: one that contains the required fields, and one that contains all the
   fields that the class offers. Prefer the latter ones to using the fluent setters with the smaller constructor.
 * The `Extensible` class represents the various portions of the payload that can have arbitrary additional data sent.

   This class contains two additional methods: `get(String key)` and `put(String key, Object value)`. `get` simply
   retrieves the value at that key. It can be used for built-in and custom keys. `put` checks the key against the
   built-in properties and will throw an `IllegalArgumentException` if it is one of the known values. This allows the
   built-in property setters to validate the properties when they are set.
 * The `send` method, replace `Sender` if you need something asynchronous you will not be able to use `Payload.send`.
   Build a similar static method to what you find in `PayloadSender` and use that instead! This will also allow you to
   use a library (like Apache or Google's HttpClient library).
 * If you integrate your library into a library for which there is no sub-library on Maven, consider creating a package
   so others can benefit from your expertise!

## Contributing

This library was written by someone who knows C# much better than Java. Feel free to issue stylistic PRs, or offer
suggestions on how we can improve this library.

1. [Fork it](https://github.com/rollbar/rollbar-java)
2. Create your feature branch (```git checkout -b my-new-feature```).
3. Commit your changes (```git commit -am 'Added some feature'```)
4. Push to the branch (```git push origin my-new-feature```)
5. Create new Pull Request
