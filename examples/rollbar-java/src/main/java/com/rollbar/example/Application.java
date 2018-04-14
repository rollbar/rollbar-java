package com.rollbar.example;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application example using rollbar-java notifier.
 */
public class Application {

  private static Logger LOGGER = LoggerFactory.getLogger(Application.class);

  private Greeting greeting;

  private Rollbar rollbar;

  /**
   * Constructor.
   */
  public Application() {
    this.greeting = new Greeting();
    LOGGER.info("Configuring Rollbar");
    Config config = ConfigBuilder.withAccessToken(System.getenv("ROLLBAR_ACCESSTOKEN"))
        .environment("development")
        .codeVersion("1.0.0")
        .build();
    LOGGER.info("Initializing Rollbar");
    this.rollbar = Rollbar.init(config);
  }

  /**
   * Main method.
   * @param args the args.
   */
  public static void main(String[] args) {
    LOGGER.info("Starting application");
    Application app = new Application();

    LOGGER.info("Executing application");
    app.execute();

    try {
      app.rollbar.close(true);
    } catch (Exception e) {
      LOGGER.error("Error while closing the notifier", e);
    }

    LOGGER.info("Finished application");
  }

  private void execute() {
    ExecutorService executor = Executors.newFixedThreadPool(3);
    List<Callable<Void>> callableTasks = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      callableTasks.add(new GreetingTask(i + 1, rollbar, greeting));
    }

    try {
      executor.invokeAll(callableTasks);
    } catch (InterruptedException e) {
      rollbar.error(e, "Unexpectedly the execution was interrupted.");
    } finally {
      executor.shutdown();
    }
  }

  private static final class GreetingTask implements Callable<Void> {

    private final int taskNumber;

    private final Rollbar rollbar;

    private final Greeting greeting;

    public GreetingTask(int taskNumber, Rollbar rollbar, Greeting greeting) {
      this.taskNumber = taskNumber;
      this.rollbar = rollbar;
      this.greeting = greeting;
    }

    @Override
    public Void call() throws Exception {
      try {
        printGreeting();
      } catch (Exception e) {
        rollbar.error(e, "Error in task: " + taskNumber);
      }
      return null;
    }

    private void printGreeting() {
      System.out.println(greeting.greeting());
    }
  }
}
