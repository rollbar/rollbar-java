package com.rollbar.notifier.filter;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Filter interface.
 */
public interface Filter {

  /**
   * Pre-filter hook to decide before gathering information and transforming the data if it is
   * susceptible of it.
   *
   * @param level the level
   * @param error the error.
   * @param custom the custom data.
   * @param description the description.
   * @return true if filtered otherwise false.
   */
  boolean preProcess(Level level, @Nullable Throwable error, @Nullable Map<String,
          Object> custom, @Nullable String description);

  /**
   * Post-filter hook to decide once the final payload is ready if it should be send to Rollbar.
   *
   * @param data the data.
   * @return true if filtered otherwise false.
   */
  boolean postProcess(Data data);
}
