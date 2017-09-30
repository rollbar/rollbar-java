package com.rollbar.example;

import static com.rollbar.notifier.config.ConfigBuilder.withAccessToken;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application example using rollbar-java notifier.
 */
public class Application {

  private Greeting greeting;

  private Rollbar rollbar;

  /**
   * Constructor.
   */
  public Application() {
    this.greeting = new Greeting();
    Config config = withAccessToken(System.getenv("ROLLBAR_ACCESSTOKEN"))
        .environment("development")
        .codeVersion("1.0.0")
        .build();
    this.rollbar = Rollbar.init(config);
  }

  /**
   * Main method.
   * @param args the args.
   */
  public static void main(String[] args) {
    Application app = new Application();
    app.execute();

    throw new RuntimeException("Exception finishing execution.");
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