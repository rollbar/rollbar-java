package com.rollbar.notifier.sender.json;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.result.Result;

/**
 * Serializer to be used by the {@Sender sender}. the payload to send to Rollbar to json and get
 * {@link Result result} from a Rollbar response.
 */
public interface JsonSerializer {

  /**
   * Parses to a json the payload to be sent.
   *
   * @param payload the payload to sent.
   * @return the payload serialized to json format.
   */
  String toJson(Payload payload);

  /**
   * Parses the response from Rollbar to a {@link Result result}.
   *
   * @param code the response code.
   * @param response the response.
   * @return the result.
   */
  Result resultFrom(int code, String response);
}
