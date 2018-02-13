package com.rollbar.notifier.filter;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.notifier.transformer.Transformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterPipeline implements Filter {

  private final List<Filter> pipeline;

  /**
   * Constructor.
   */
  public FilterPipeline() {
    this(null);
  }

  /**
   * Constructor.
   * @param pipeline the list of filers.
   */
  public FilterPipeline(List<Filter> pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  public boolean preProcess(Level level, Throwable error, Map<String, Object> custom,
      String description) {
    if (usePipeline()) {
      return pipeline(level, error, custom, description);
    }

    return false;
  }

  @Override
  public boolean postProcess(Data data) {
    if (usePipeline()) {
      return pipeline(data);
    }

    return false;
  }

  private boolean usePipeline() {
    return pipeline != null && !pipeline.isEmpty();
  }

  private boolean pipeline(Level level, Throwable error, Map<String, Object> custom,
      String description) {
    for (Filter filter : pipeline) {
      boolean result = filter.preProcess(level, error, custom, description);
      if (result) {
        return true;
      }
    }
    return false;
  }

  private boolean pipeline(Data data) {
    for (Filter filter : pipeline) {
      boolean result = filter.postProcess(data);
      if (result) {
        return true;
      }
    }
    return false;
  }
}
