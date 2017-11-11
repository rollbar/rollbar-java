package com.rollbar.example

import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder

import scala.util.control.NonFatal

object Application {

  def main(args: Array[String]) {
    val config = ConfigBuilder.withAccessToken(sys.env("ROLLBAR_ACCESSTOKEN"))
      .language("scala")
      .codeVersion("1.0.0")
      .environment("development")
      .build();

    val rollbar = Rollbar.init(config)

    try {
      exec()
    } catch {
      case NonFatal(e) => rollbar.error(e)
    }
  }

  def exec() {
    greeting()

    throw new RuntimeException("Execution error after greeting.")
  }

  def greeting() {
    println(s"Hello Rollbar!")
  }
}