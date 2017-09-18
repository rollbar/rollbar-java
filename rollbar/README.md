# Rollbar

Rollbar is an official Java notifier for Rollbar. It is a work in progress. It represents a payload to be sent to
Rollbar and ensures that all (applicable) portions of the payload spec'ed in https://rollbar.com/docs/api/items_post/
can be sent correctly.

## Installation

Installation is through Maven:

```xml
<dependency>
    <groupId>com.rollbar</groupId>
    <artifactId>rollbar</artifactId>
</dependency>
```

## Usage

Construct a `Rollbar` and call `handleUncaughtErrors` on it:

```java
public class MainClass {
    public static final Rollbar rollbar = new Rollbar("ACCESS_TOKEN", "production");
    public int main(String[] args) {
        rollbar.handleUncaughtErrors();
        OtherClass.runProgram();
        return 0;
    }
}
```

If you're running a Web application or using a framework that handles errors in a special way you'll need to get an
instance of a `Rollbar` using whatever technique is appropriate for your framework. For Spring, for instance, a properly
configured Rollbar bean along with the following class will do the trick:

```java
@ControllerAdvice
class RollbarExceptionHandler {
    private final Rollbar rollbar;

    public RollbarExceptionHandler(Rollbar rollbar) {
        this.rollbar = rollbar;
    }

    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null)
            throw e;

        rollbar.log(e);

        throw e;
    }
}
```

You might configure your bean with something like this:

```java
package com.yourcompany.product;

import org.springframework.beans.factory.FactoryBean;
import com.rollbar.Rollbar;

public class RollbarBean implements FactoryBean<Rollbar> {
	private Rollbar rollbar = new Rollbar("YOUR_ACCESS_TOKEN_HERE", "production");

	@Override
	public Rollbar getObject() throws Exception {
		return rollbar;
	}

	@Override
	public Class<? extends Rollbar> getObjectType() {
		return rollbar.getClass();
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
}
```

You can, of course, always choose to configure this with XML.

## Custom Usage

Rollbar also provides a set of classes for building and sending error reports to Rollbar.

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
            PayloadSender ps = new PayloadSender();
            try {
                // Here you can filter or transform the payload as needed before sending it
                ps.send(p);
            } catch (ConnectionFailedException e) {
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
                PayloadSender ps = new PayloadSender();
                try {
                    // Here you can filter or transform the payload as needed before sending it
                    ps.send(p);
                } catch (ConnectionFailedException e) {
                    Logger.getLogger(MyClass.class.getName()).severe(p.toJson());
                }
                throw t;
            }
        });
    }
}
```

Note that the above snippet monitors a **single** thread. If your framework starts a thread per request, or uses
different threads for different parts of your UI you'll need to take that into consideration.

## Tips for Optimal Usage

 * If you can construct the `Payload`, it compiles, and does not throw an exception in the process, then it's valid to
   send to Rollbar and will be displayed correctly in the interface.

 * Everything in the `payload` package is immutable, exposing a fluent interface for making small changes to the Payload.

   In practice this means the 'setters' are very expensive (because they create a whole new object every time). Since
   reporting exceptions should be the "exceptional" case, this should not matter in practice. (Please report any serious
   performance issues!). If you are only going to alter one or two fields then using these setters is a great time
   saver, and you should feel free to use them. If you are fully customizing a portion of your payload with lots of
   custom data, however, you should use the constructor that exposes all the fields available to the class.

 * The fluent interface uses `property()` as the getter, and `property(TypeName val)` as the setter, rather than the
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

