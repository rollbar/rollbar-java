package com.rollbar.notifier.filter;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import java.util.Map;

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
  boolean preProcess(Level level, Throwable error, Map<String, Object> custom, String description);

  /**
   * Pre-filter hook to decide before gathering information and transforming the data if it is
   * susceptible of it. This includes whether or not this data comes from an uncaught exception.
   * By default this just calls preProcess method that ignores the isUncaught parameter.
   * Only this method is called and therefore if you override the default implementation, the other
   * preProcess method will never be called by this library. The existence of the two methods is for
   * legacy reasons, but we encourage implementing this method as the less specific method may be
   * dropped in a future release.
   *
   * @param level the level
   * @param error the error.
   * @param custom the custom data.
   * @param description the description.
   * @param isUncaught whether or not this set of data originates from an uncaught exception.
   * @return true if filtered otherwise false.
   */
  default boolean preProcess(Level level, Throwable error, Map<String, Object> custom, String description,
      boolean isUncaught) {
    return preProcess(level, error, custom, description);
  }

  /**
   * Post-filter hook to decide once the final payload is ready if it should be send to Rollbar.
   *
   * @param data the data.
   * @return true if filtered otherwise false.
   */
  boolean postProcess(Data data);
}
