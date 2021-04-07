package com.rollbar.reactivestreams.notifier.config;

import com.rollbar.reactivestreams.notifier.sender.Sender;

public interface Config extends com.rollbar.notifier.config.CommonConfig {
  Sender asyncSender();
}
