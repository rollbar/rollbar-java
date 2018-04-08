package com.rollbar.logback.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger("com.example.rollbar.logback");

  public static void main(String[] args) {
    LOGGER.info("Starting the application! Current time: {}", System.currentTimeMillis());

    MDC.put("my_custom_key", "custom_value");

    execute();

    LOGGER.info("Finishing the application! Current time: {}", System.currentTimeMillis());
  }

  private static void execute() {
    ExecutorService executor = Executors.newFixedThreadPool(3);
    List<Callable<Void>> callableTasks = new ArrayList<>();

    Greeting greeting = new Greeting();

    for (int i = 0; i < 20; i++) {
      callableTasks.add(new GreetingTask(i + 1, greeting));
    }

    try {
      executor.invokeAll(callableTasks);
    } catch (InterruptedException e) {
      LOGGER.error("Unexpectedly the execution was interrupted.", e);
    } finally {
      executor.shutdown();
    }
  }

  private static final class GreetingTask implements Callable<Void> {

    private final int taskNumber;

    private final Greeting greeting;

    public GreetingTask(int taskNumber, Greeting greeting) {
      this.taskNumber = taskNumber;
      this.greeting = greeting;
    }

    @Override
    public Void call() throws Exception {
      MDC.put("greeting_number", String.valueOf(this.taskNumber));

      try {
        printGreeting();
      } catch (Exception e) {
        LOGGER.error("Error in task: {}", taskNumber, e);
      }
      return null;
    }

    private void printGreeting() {
      System.out.println(greeting.greeting());
    }
  }
}
