# Rollbar for Java Spring Boot 

[![javadoc](https://javadoc.io/badge2/com.rollbar/rollbar-spring-boot-webmvc/javadoc.svg?style=for-the-badge)](https://javadoc.io/doc/com.rollbar/rollbar-spring-boot-webmvc)

Simple Rollbar integration for Spring Boot.

## Installation

Add the following into your gradle dependency:

```java
compile('com.rollbar:rollbar-spring-boot-webmvc:1.7.2')
```

## Configuration

Configure a Rollbar Bean with your package and ACCESS_TOKEN like the following:

```java
@Configuration()
@EnableWebMvc
@ComponentScan({
    // ADD YOUR PROJECT PACKAGE HERE
    "com.rollbar.spring"
})
public class RollbarConfig {

  /**
   * Register a Rollbar bean to configure App with Rollbar.
   */
  @Bean
  public Rollbar rollbar() {
    return new Rollbar(getRollbarConfigs("<ACCESS_TOKEN>"));
  }

  private Config getRollbarConfigs(String accessToken) {

    // Reference ConfigBuilder.java for all the properties you can set for Rollbar
    return RollbarSpringConfigBuilder.withAccessToken(accessToken)
            .environment("development")
            .build();
  }
}
```

## Usage

Once you have the configuration implemented, Rollbar will now catch all exceptions raised by Java Spring. If you need to manually send exceptions, you can do so like the following. 

```java
try {
   String test = null;
   test.toString();
} catch(Exception e) {
   rollbar.error(e,"This is a null pointer exception");
}
```

For a full project example reference the project example under https://github.com/rollbar/rollbar-java/tree/master/examples/rollbar-spring-boot-webmvc.
 

