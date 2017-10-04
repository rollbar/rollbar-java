package com.rollbar.notifier.transformer;

import com.rollbar.api.payload.data.Data;
import java.util.List;

/**
 * Utility to create a pipeline of {@link Transformer transformers} to transform
 * the {@link Data data}.
 */
public class TransformerPipeline implements Transformer {

  private final List<Transformer> pipeline;

  /**
   * Constructor.
   */
  public TransformerPipeline() {
    this(null);
  }

  /**
   * Constructor.
   * @param pipeline the list of transformers.
   */
  public TransformerPipeline(List<Transformer> pipeline) {
    this.pipeline = pipeline;
  }


  @Override
  public Data transform(Data data) {
    if (usePipeline()) {
      return pipeline(data);
    }

    return data;
  }

  private boolean usePipeline() {
    return pipeline != null && !pipeline.isEmpty();
  }

  private Data pipeline(Data data) {
    for (Transformer transformer : pipeline) {
      data = transformer.transform(data);
    }
    return data;
  }
}

