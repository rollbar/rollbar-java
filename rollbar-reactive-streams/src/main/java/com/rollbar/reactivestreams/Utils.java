package com.rollbar.reactivestreams;

import com.rollbar.api.annotations.Unstable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for basic {@link Publisher} operations.
 */
@Unstable
public class Utils {
  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  /**
   * Maps between two publisher element types.
   *
   * @param from The {@link Publisher} to map from
   * @param converter The converter to map the publisher's elements
   * @param <F> The element type to map from
   * @param <T> The element type to map to
   * @return A new {@link Publisher} where each element will be mapped using the provided converter
   */
  public static <F, T> Publisher<T> map(final Publisher<F> from,
                                        final Converter<? super F, ? extends T> converter) {
    return new Publisher<T>() {
      @Override
      public void subscribe(final Subscriber<? super T> subscriber) {
        from.subscribe(new Subscriber<F>() {
          @Override
          public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(s);
          }

          @Override
          public void onNext(F f) {
            T converted;
            try {
              converted = converter.convert(f);
            } catch (Throwable e) {
              onError(e);
              return;
            }

            subscriber.onNext(converted);
          }

          @Override
          public void onError(Throwable t) {
            subscriber.onError(t);
          }

          @Override
          public void onComplete() {
            subscriber.onComplete();
          }
        });
      }
    };
  }

  /**
   * Creates a mono {@link Publisher} with the provided value.
   *
   * @param value The value of the publisher's single element
   * @param <T> The type of the element
   * @return A mono {@link Publisher} with the provided value
   */
  public static <T> Publisher<T> just(final T value) {
    return new Publisher<T>() {
      @Override
      public void subscribe(final Subscriber<? super T> s) {
        s.onSubscribe(new CancellableSubscription() {
          @Override
          public void request(long n) {
            if (n > 0 && done.compareAndSet(false, true)) {
              s.onNext(value);
              s.onComplete();
            }
          }
        });
      }
    };
  }

  /**
   * Creates an empty {@link Publisher}.
   *
   * @param <T> The type of the element
   * @return An empty {@link Publisher}
   */
  public static <T> Publisher<T> empty() {
    return new Publisher<T>() {
      @Override
      public void subscribe(final Subscriber<? super T> s) {
        s.onSubscribe(new CancellableSubscription() {
          @Override
          public void request(long n) {
            if (n > 0 && done.compareAndSet(false, true)) {
              s.onComplete();
            }
          }
        });
      }
    };
  }

  /**
   * Represents a conversion function between two types.
   *
   * @param <F> Type to convert from
   * @param <T> Type to convert to
   */
  public interface Converter<F, T> {
    T convert(F from);
  }

  private abstract static class CancellableSubscription implements Subscription {
    final AtomicBoolean done = new AtomicBoolean(false);

    @Override
    public void cancel() {
      done.set(true);
    }
  }
}
