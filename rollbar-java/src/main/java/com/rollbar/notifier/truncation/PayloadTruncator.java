package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.sender.json.JsonSerializer;
import com.rollbar.notifier.util.ObjectsUtils;

import java.nio.charset.Charset;

public class PayloadTruncator {
  // We send data to Rollbar in UTF-8, so we use this to calculate payload size
  private static final Charset TRANSPORT_CHARSET = Charset.forName("UTF-8");

  private static final TruncationStrategy[] STRATEGIES = {
      new RollbarThreadStrategy(),
      new FramesStrategy(),
      new TelemetryEventsStrategy(),
      new StringsStrategy(1024),
      new StringsStrategy(512),
      new StringsStrategy(256),
      new MinBodyStrategy(),
  };

  private final JsonSerializer serializer;

  public PayloadTruncator(JsonSerializer serializer) {
    ObjectsUtils.requireNonNull(serializer, "serializer cannot be null");
    this.serializer = serializer;
  }

  /**
   * <p>
   * Attempts to truncate the payload so that its JSON representation, encoded as UTF-8, has size
   * equal or less than the specified maximum size size.
   * </p>
   * @param payload The payload to be truncated.
   * @param maxSizeInBytes The maximum size, in bytes, for the payload.
   * @return The truncated payload.
   */
  public PayloadTruncationResult truncate(Payload payload, int maxSizeInBytes) {
    String json = serializer.toJson(payload);
    int currentSize = sizeInBytes(json);

    for (int j = 0; currentSize > maxSizeInBytes && j < STRATEGIES.length; ++j) {
      TruncationStrategy.TruncationResult<Payload> result = STRATEGIES[j].truncate(payload);
      if (result.wasTruncated) {
        payload = result.value;
        json = serializer.toJson(payload);
        currentSize = sizeInBytes(json);
      }
    }

    // Skip serialization from now on and use a pre-serialized payload.
    return new PayloadTruncationResult(new Payload(json), currentSize);
  }

  /**
   * The size of the JSON string, as UTF-8 bytes.
   * @param payloadJsonString The string to measure.
   * @return The size, in UTF-8 encoded bytes, of the string.
   */
  public static int sizeInBytes(String payloadJsonString) {
    if (payloadJsonString == null) {
      return 0;
    }
    return payloadJsonString.getBytes(TRANSPORT_CHARSET).length;
  }

  public static final class PayloadTruncationResult {
    private final Payload payload;
    public final int finalSize;

    PayloadTruncationResult(Payload payload, int finalSize) {
      this.payload = payload;
      this.finalSize = finalSize;
    }

    public Payload getPayload() {
      return payload;
    }
  }
}
