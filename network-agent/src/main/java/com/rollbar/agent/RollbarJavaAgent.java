package com.rollbar.agent;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class RollbarJavaAgent {
  private static volatile boolean started = false;

  public static synchronized void premain(
    String agentArgs,
    Instrumentation inst
  ) {
    if (started) {
      return;
    }
    started = true;

    System.out.println("Agent started!");

    try {
      // Path to *this* agent jar
      String agentPath =
        RollbarJavaAgent.class
          .getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .getPath();

      inst.appendToBootstrapClassLoaderSearch(
        new JarFile(agentPath)
      );

    } catch (Exception e) {
      e.printStackTrace();
    }

    inst.addTransformer(new NetworkTransformer(), true);
  }
}
