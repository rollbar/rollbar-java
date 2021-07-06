package com.rollbar.reactivestreams;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;

public class UtilsTest {
  @Test
  public static class EmptyTckTest extends PublisherVerification<Void> {
    public EmptyTckTest() {
      super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
      return 0;
    }

    @Override
    public Publisher<Void> createPublisher(long elements) {
      return Utils.empty();
    }

    @Override
    public Publisher<Void> createFailedPublisher() {
      // Our publisher cannot fail
      return null;
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void optional_spec104_mustSignalOnErrorWhenFails() {
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() {
    }
  }

  @Test
  public static class JustTckTest extends PublisherVerification<Integer> {
    public JustTckTest() {
      super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
      return 1;
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
      return Utils.just(42);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
      // Our publisher cannot fail
      return null;
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void optional_spec104_mustSignalOnErrorWhenFails() {
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() {
    }
  }

  @Test
  public static class MapTest extends PublisherVerification<Double> {
    public MapTest() {
      super(new TestEnvironment());
    }

    @Override
    public long maxElementsFromPublisher() {
      return 1000;
    }

    @Override
    public Publisher<Double> createPublisher(long elements) {
      Stream<Integer> range = IntStream.range(0, (int) elements).boxed();
      return Utils.map(Flux.fromStream(range), Double::valueOf);
    }

    @Override
    public Publisher<Double> createFailedPublisher() {
      // Our publisher cannot fail
      return null;
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void optional_spec104_mustSignalOnErrorWhenFails() {
    }

    @Test
    @Ignore("Our publisher cannot fail")
    public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() {
    }
  }
}
