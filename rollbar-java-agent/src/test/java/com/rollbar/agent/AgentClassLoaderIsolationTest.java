package com.rollbar.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Guards the classloader contract the agent depends on.
 *
 * <p>{@code -javaagent:} appends the agent jar to the <em>system</em> class path, while an
 * application normally keeps the Rollbar SDK in a child classloader ({@code BOOT-INF/lib} in a
 * Spring Boot fat jar, {@code WEB-INF/lib} in a WAR). An SDK type named from agent code therefore
 * resolves against the system classloader and is not found — and in a signature of the
 * {@code Premain-Class} that kills the JVM before {@code main} runs, because the JVM calls
 * {@code getDeclaredMethods()} on it to locate {@code premain}.
 */
public class AgentClassLoaderIsolationTest {

  private static final List<String> FORBIDDEN_REFERENCES =
      Arrays.asList("com/rollbar/api/", "com/rollbar/notifier/");

  @Test
  public void agentJarClasses_doNotReferenceRollbarSdkTypes() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (JarFile jar = new JarFile(agentJar())) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (!entry.getName().endsWith(".class")) {
          continue;
        }
        byte[] bytecode = readAll(jar, entry);
        for (String forbidden : FORBIDDEN_REFERENCES) {
          if (contains(bytecode, forbidden)) {
            offenders.add(entry.getName() + " -> " + forbidden);
          }
        }
      }
    }

    assertTrue(offenders.isEmpty(),
        "the agent jar loads in the system classloader, which in a Spring Boot fat jar or a WAR "
            + "cannot see the application's SDK; move anything that needs an SDK type into "
            + "rollbar-java (see AgentTelemetryEventTracker). Offending classes: " + offenders);
  }

  @Test
  public void premain_startsAndRecordsWithoutTheSdkOnTheClasspath() throws Exception {
    // The reported failure in full: a JVM whose classpath has no Rollbar SDK at all. Before the
    // split this aborted with "FATAL ERROR in native method: processing of -javaagent failed".
    Path java = Path.of(System.getProperty("java.home"), "bin", "java");
    Process process = new ProcessBuilder(
        java.toString(),
        "-javaagent:" + agentJar().getAbsolutePath(),
        "-cp", probeClasspath(),
        PremainProbe.class.getName())
        .redirectErrorStream(true)
        .start();

    String output = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
    assertTrue(process.waitFor(60, TimeUnit.SECONDS), "probe JVM did not exit: " + output);
    assertEquals(0, process.exitValue(), "probe JVM failed to start under -javaagent:\n" + output);
    assertTrue(output.contains("probe-ok events=1"),
        "agent must still record without the SDK present, got:\n" + output);
  }

  @Test
  public void sdkInChildClassLoader_readsEventsFromTheSystemClassLoaderStore() throws Exception {
    // Simulates the Spring Boot / WAR layout: the agent's store in the system classloader, the SDK
    // in a child of it. The SDK-side tracker has to reach *up* for the events, which is the only
    // direction that works.
    Class<?> store = systemClassLoaderStore();
    store.getMethod("resetForTesting").invoke(null);

    try (URLClassLoader applicationLoader = new ApplicationClassLoader(sdkUrls())) {
      Class<?> trackerClass = applicationLoader
          .loadClass("com.rollbar.notifier.telemetry.AgentTelemetryEventTracker");
      assertEquals(applicationLoader, trackerClass.getClassLoader(),
          "the tracker must come from the child loader, not from the system class path");

      // Recorded on a thread belonging to that application, as a servlet container would.
      recordAs(applicationLoader, store, "https://api.example.com/charge", "503");

      Object tracker = trackerClass.getDeclaredConstructor().newInstance();
      List<?> events = (List<?>) trackerClass.getMethod("getAll").invoke(tracker);

      assertEquals(1, events.size(), "the SDK must see the event the agent recorded");
      String event = events.get(0).toString();
      assertTrue(event.contains("status_code=503"), event);
      assertTrue(event.contains("https://api.example.com/charge"), event);
    } finally {
      store.getMethod("resetForTesting").invoke(null);
    }
  }

  @Test
  public void twoApplicationsInOneJvm_doNotSeeEachOthersEvents() throws Exception {
    // Two WARs in one container, each with its own copy of the SDK and its own access token. The
    // agent is loaded once for the whole JVM, so without partitioning one deployment's internal
    // hostnames and paths would be attached to the other deployment's Rollbar reports.
    Class<?> store = systemClassLoaderStore();
    store.getMethod("resetForTesting").invoke(null);

    try (URLClassLoader firstApp = new ApplicationClassLoader(sdkUrls());
        URLClassLoader secondApp = new ApplicationClassLoader(sdkUrls())) {
      Object firstTracker = newTracker(firstApp);
      Object secondTracker = newTracker(secondApp);

      recordAs(firstApp, store, "https://first.internal/charge", "500");
      recordAs(secondApp, store, "https://second.internal/refund", "503");

      String firstSees = telemetryOf(firstTracker).toString();
      String secondSees = telemetryOf(secondTracker).toString();

      assertEquals(1, telemetryOf(firstTracker).size(), firstSees);
      assertTrue(firstSees.contains("https://first.internal/charge"), firstSees);
      assertFalse(firstSees.contains("second.internal"),
          "the other deployment's URLs must not reach this one's report: " + firstSees);

      assertEquals(1, telemetryOf(secondTracker).size(), secondSees);
      assertTrue(secondSees.contains("https://second.internal/refund"), secondSees);
      assertFalse(secondSees.contains("first.internal"),
          "the other deployment's URLs must not reach this one's report: " + secondSees);
    } finally {
      store.getMethod("resetForTesting").invoke(null);
    }
  }

  @Test
  public void callOnAThreadOwnedByNoApplication_isStillAttributedToTheCallingApplication()
      throws Exception {
    // A ForkJoinPool.commonPool worker carries the container's classloader, not the application's,
    // so the thread alone cannot say whose call this is — and an event nobody can claim is an
    // event nobody is shown. The stack still names the caller, which is what keeps telemetry from
    // quietly disappearing whenever an application calls out from a shared pool.
    Class<?> store = systemClassLoaderStore();
    store.getMethod("resetForTesting").invoke(null);

    try (URLClassLoader app = new ApplicationClassLoader(applicationUrls())) {
      Class<?> applicationCode = app.loadClass(CallingApplication.class.getName());
      assertEquals(app, applicationCode.getClassLoader(),
          "the calling class must belong to the application, not to the test");

      ClassLoader previous = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
      try {
        applicationCode.getMethod("makeCall", String.class)
            .invoke(null, "https://first.internal/charge");
      } finally {
        Thread.currentThread().setContextClassLoader(previous);
      }

      String seen = telemetryOf(newTracker(app)).toString();
      assertTrue(seen.contains("https://first.internal/charge"),
          "the application must still be shown the call its own code made: " + seen);
    } finally {
      store.getMethod("resetForTesting").invoke(null);
    }
  }

  private static Class<?> systemClassLoaderStore() throws ClassNotFoundException {
    return ClassLoader.getSystemClassLoader().loadClass(AgentTelemetryStore.class.getName());
  }

  private static Object newTracker(ClassLoader application) throws Exception {
    return application.loadClass("com.rollbar.notifier.telemetry.AgentTelemetryEventTracker")
        .getDeclaredConstructor().newInstance();
  }

  private static List<?> telemetryOf(Object tracker) throws Exception {
    return (List<?>) tracker.getClass().getMethod("getAll").invoke(tracker);
  }

  /** Records an event the way an HTTP call made by that application's code would. */
  private static void recordAs(ClassLoader application, Class<?> store, String url,
      String statusCode) throws Exception {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(application);
    try {
      store.getMethod("recordNetworkEvent", String.class, String.class, String.class)
          .invoke(null, "GET", url, statusCode);
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  /** Classes the child loader must load itself: the SDK, plus the logging API it needs. */
  private static URL[] sdkUrls() {
    return new URL[] {
        codeSourceOf("com.rollbar.notifier.telemetry.TelemetryEventTracker"),
        codeSourceOf("com.rollbar.api.payload.data.TelemetryEvent"),
        codeSourceOf("org.slf4j.Logger"),
    };
  }

  /** The SDK, plus this test's own classes, so the application can have its own calling code. */
  private static URL[] applicationUrls() {
    List<URL> urls = new ArrayList<>(Arrays.asList(sdkUrls()));
    urls.add(CallingApplication.class.getProtectionDomain().getCodeSource().getLocation());
    return urls.toArray(new URL[0]);
  }

  private static URL codeSourceOf(String className) {
    try {
      return Class.forName(className).getProtectionDomain().getCodeSource().getLocation();
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("not on the test classpath: " + className, e);
    }
  }

  private static File agentJar() {
    String path = System.getProperty("rollbar.agent.jar");
    if (path == null) {
      fail("the rollbar.agent.jar system property is set by the test task; run through Gradle");
    }
    File jar = new File(path);
    assertTrue(jar.isFile(), "agent jar not built: " + path);
    return jar;
  }

  private static String probeClasspath() {
    return new File(PremainProbe.class.getProtectionDomain().getCodeSource().getLocation()
        .getPath()).getAbsolutePath();
  }

  private static byte[] readAll(JarFile jar, JarEntry entry) throws IOException {
    try (InputStream in = jar.getInputStream(entry)) {
      return readAll(in);
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
    return out.toByteArray();
  }

  // Class names live in the constant pool as modified UTF-8, so a raw byte scan finds every
  // reference a class file can carry — field and method descriptors, signatures, and the names of
  // types it merely mentions.
  private static boolean contains(byte[] bytecode, String text) {
    byte[] needle = text.getBytes(StandardCharsets.UTF_8);
    outer:
    for (int i = 0; i <= bytecode.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (bytecode[i + j] != needle[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Loads the SDK itself instead of delegating for it, so the application's copy is invisible to
   * the parent — the asymmetry a Spring Boot fat jar or a WAR creates, whatever the test harness
   * happens to put on the system class path.
   */
  private static final class ApplicationClassLoader extends URLClassLoader {

    private static final List<String> OWNED_PACKAGES = Arrays.asList(
        "com.rollbar.notifier.",
        "com.rollbar.api.",
        "org.slf4j.",
        // The application's own calling code. Named class by class, because the rest of
        // com.rollbar.agent is the agent itself and must stay shared with the parent — one store
        // for the JVM is the whole point.
        CallingApplication.class.getName());

    ApplicationClassLoader(URL[] urls) {
      super(urls, ClassLoader.getSystemClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Class<?> loaded = findLoadedClass(name);
        if (loaded == null && owns(name)) {
          loaded = findClass(name);
        }
        if (loaded == null) {
          return super.loadClass(name, resolve);
        }
        if (resolve) {
          resolveClass(loaded);
        }
        return loaded;
      }
    }

    private static boolean owns(String name) {
      for (String owned : OWNED_PACKAGES) {
        if (name.startsWith(owned)) {
          return true;
        }
      }
      return false;
    }
  }
}
