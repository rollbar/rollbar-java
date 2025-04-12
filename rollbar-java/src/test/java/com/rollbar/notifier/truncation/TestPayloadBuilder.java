package com.rollbar.notifier.truncation;

import com.rollbar.api.payload.Payload;
import com.rollbar.api.payload.data.*;
import com.rollbar.api.payload.data.body.*;

import java.util.*;
import java.util.stream.Collectors;

public class TestPayloadBuilder {
  private final int stringLength;
  private final int customLength;

  public TestPayloadBuilder() {
    this(-1);
  }

  public TestPayloadBuilder(int stringLength) {
    this(stringLength, 10);
  }

  public TestPayloadBuilder(int stringLength, int customLength) {
    this.stringLength = stringLength;
    this.customLength = customLength;
  }

  public Payload createTestPayload() {
    return createTestPayloadSingleTrace(new ArrayList<>());
  }

  public Payload createTestPayloadSingleTrace(int frameCount) {
    return createTestPayloadSingleTrace(createFrames(frameCount));
  }

  public Payload createTestPayloadSingleTraceWithRollbarThreads(int frameCount) {
    return createTestPayload(Collections.singletonList(createFrames(frameCount)), true);
  }

  public Payload createTestPayloadSingleTrace(Trace trace) {
    return createTestPayload(new Body.Builder().bodyContent(trace).build());
  }

  public Payload createTestPayloadSingleTrace(List<Frame> frameList) {
    return createTestPayload(Collections.singletonList(frameList));
  }

  public Payload createTestPayload(List<List<Frame>> frameLists) {
    return createTestPayload(frameLists, false);
  }

  public Payload createTestPayload(List<List<Frame>> frameLists, boolean addRollbarThreads) {
    List<Trace> traces = frameLists.stream().map(frameList -> new Trace.Builder()
      .exception(
        new ExceptionInfo.Builder()
          .message(makeString("Error"))
          .description(makeString("some error"))
          .className(makeString("com.example.TestException"))
          .build()
      )
      .frames(frameList)
      .build()).collect(Collectors.toList());

    BodyContent bodyContent;
    if (traces.size() == 1) {
      bodyContent = traces.get(0);
    } else {
      bodyContent = new TraceChain.Builder().traces(traces).build();
    }

    ArrayList<RollbarThread> rollbarThreads = null;
    if (addRollbarThreads) {
      rollbarThreads = new ArrayList<>();
      TraceChain traceChain = new TraceChain.Builder().traces(traces).build();
      Group group = new Group(traceChain);
      RollbarThread rollbarThread = new RollbarThread(Thread.currentThread(), group);
      rollbarThreads.add(rollbarThread);
    }

    return createTestPayload(new Body.Builder().bodyContent(bodyContent).rollbarThreads(rollbarThreads).build());
  }

  public Payload createTestPayload(Body body) {
    return new Payload.Builder().data(
        new Data.Builder()
            .client(
                new Client.Builder()
                    .addClient("client", "some_prop", 42)
                    .addClient("client_b", "prop_b", makeString("test"))
                    .build()
            )
            .codeVersion(makeString("9.999942"))
            .environment(makeString("unit_testing"))
            .framework(makeString("junit"))
            .person(
                new Person.Builder()
                    .username(makeString("some username"))
                    .metadata(makeMap(5))
                    .email(makeString("test@test.com"))
                    .build()
            )
            .custom(makeMap(customLength))
            .level(Level.WARNING)
            .context(makeString("ABC"))
            .language(makeString("java"))
            .platform(makeString("BeOS"))
            .person(new Person.Builder().username(makeString("test-user")).build())
            .server(new Server.Builder().root(makeString("/server/root")).build())
            .client(
                new Client.Builder()
                    .addClient("a", "b", makeString("some value"))
                    .addTopLevel("c", makeString("another value"))
                    .build()
            )
            .body(body)
            .build()
    ).build();
  }

  private Map<String, Object> makeMap(int length) {
    HashMap<String, Object> map = new HashMap<>();
    map.put("non-string", 20);
    for (int j = 0; j < length; ++j) {
      map.put("loop-key-" + j, makeString("value" + j));
    }
    return map;
  }

  public List<Frame> createFrames(int frameCount) {
    ArrayList<Frame> frames = new ArrayList<>();
    for (int j = 0; j < frameCount; ++j) {
      frames.add(
          new Frame.Builder()
              .filename(makeString("/src/test/input" + j))
              .className(makeString("com.rollbar.test.Class" + j))
              // Use frame index a line and column number, which can be used to verify truncation
              .lineNumber(j)
              .columnNumber(j)
              .method(makeString("CallMethod" + j))
              .build()
      );
    }

    return frames;
  }

  public String makeString(String baseString) {
    return makeString(baseString, this.stringLength);
  }

  public static String makeString(String baseString, int stringLength) {
    if (stringLength < 0) {
      return baseString;
    } else {
      StringBuilder sb = new StringBuilder();
      int baseLength = baseString.length();
      int remaining = stringLength;
      while (remaining > 0) {
        if (baseLength <= remaining) {
          sb.append(baseString);
          remaining -= baseLength;
        } else {
          sb.append(baseString, 0, remaining);
          remaining = 0;
        }
      }

      return sb.toString();
    }
  }
}
