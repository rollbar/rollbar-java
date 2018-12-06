package com.rollbar.example

import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object Application {

  def main(args: Array[String]) {
    val appPackages = List("com.rollbar.example")
    val config = ConfigBuilder.withAccessToken(sys.env("ROLLBAR_ACCESSTOKEN"))
      .language("scala")
      .codeVersion("1.0.0")
      .environment("development")
      .appPackages(appPackages.asJava)
      .build();

    val rollbar = new Rollbar(config)

    val whoa = 92
    try {
      exec()
    } catch {
      case NonFatal(e) => rollbar.error(e)
    }

    try {
      rollbar.close(true);
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

  def exec() {
    val x = 42
    val y = x + 99
    greeting()

    throw new RuntimeException("Execution error after greeting.")
  }

  def greeting() {
    val zz = s"Hello Rollbar!"
    println(zz)
  }
}
