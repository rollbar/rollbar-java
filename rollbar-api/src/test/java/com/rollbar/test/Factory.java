package com.rollbar.test;

import static java.util.Arrays.asList;

import com.rollbar.api.payload.data.Client;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import com.rollbar.api.payload.data.Notifier;
import com.rollbar.api.payload.data.Person;
import com.rollbar.api.payload.data.Request;
import com.rollbar.api.payload.data.Server;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.CodeContext;
import com.rollbar.api.payload.data.body.CrashReport;
import com.rollbar.api.payload.data.body.ExceptionInfo;
import com.rollbar.api.payload.data.body.Frame;
import com.rollbar.api.payload.data.body.Message;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import java.util.HashMap;
import java.util.Map;

public class Factory {

  private Factory() {

  }

  public static CodeContext codeContext() {
    return new CodeContext.Builder()
        .pre(asList())
        .build();
  }

  public static ExceptionInfo exceptionInfo() {
    return new ExceptionInfo.Builder()
        .className(Factory.class.getName())
        .description("Unexpected error.")
        .message("Something went wrong")
        .build();
  }

  public static CrashReport crashReport() {
    return new CrashReport.Builder()
        .raw("Report of the crash.")
        .build();
  }

  public static Frame frame() {
    return new Frame.Builder()
        .filename(Factory.class.getCanonicalName())
        .lineNumber(1)
        .columnNumber(1)
        .method("methodInFrame")
        .context(codeContext())
        .args(asList("arg1", 2))
        .className(Factory.class.getName())
        .build();
  }

  public static Message message() {
    return new Message.Builder()
        .body("Body of the message send by the user")
        .build();
  }

  public static Trace trace() {
    return new Trace.Builder()
        .frames(asList(frame()))
        .exception(exceptionInfo())
        .build();
  }

  public static TraceChain traceChain() {
    return new TraceChain.Builder()
        .traces(asList(trace(), trace()))
        .build();

  }

  public static Body body(Message message) {
    return new Body.Builder()
        .bodyContent(message)
        .build();
  }

  public static Body body(CrashReport crashReport) {
    return new Body.Builder()
        .bodyContent(crashReport)
        .build();
  }

  public static Body body(Trace trace) {
    return new Body.Builder()
        .bodyContent(trace)
        .build();
  }

  public static Body body(TraceChain traceChain) {
    return new Body.Builder()
        .bodyContent(traceChain)
        .build();
  }

  public static Client client() {
    Map<String, String> javascriptProps = new HashMap<>();
    javascriptProps.put("browser", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3)");
    javascriptProps.put("code_version", "b6437f45b7bbbb15f5eddc2eace4c71a8625da8c");

    return new Client.Builder()
        .addClient("javascript", javascriptProps)
        .addClient("flash", "version", "8.10")
        .build();
  }

  public static Notifier notifier() {
    return new Notifier.Builder()
        .name("rollbar")
        .name("0.0.1")
        .build();
  }

  public static Person person() {
    return new Person.Builder()
        .id("test")
        .email("test@example.com")
        .username("user_test")
        .build();
  }

  public static Request request() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html");
    headers.put("Referer", "https://rollbar.com/");

    Map<String, String> get = new HashMap<>();
    get.put("param1", "value1");
    get.put("param2", "value2");

    return new Request.Builder()
        .url("https://rollbar.com/project/1")
        .method("GET")
        .headers(headers)
        .get(get)
        .querystring("param1=value1&param2=value2")
        .userIp("192.168.1.1")
        .build();
  }

  public static Server server() {
    return new Server.Builder()
        .host("web4")
        .root("/var/www")
        .branch("master")
        .codeVersion("b6437f45b7bbbb15f5eddc2eace4c71a8625da8c")
        .build();
  }

  public static Data data() {
    Map<String, Object> custom = new HashMap<>();
    custom.put("var1", "value1");

    return new Data.Builder()
        .environment("production")
        .body(body(traceChain()))
        .level(Level.ERROR)
        .codeVersion("1.2.3")
        .platform("linux")
        .language("java")
        .framework("spring")
        .request(request())
        .person(person())
        .server(server())
        .client(client())
        .custom(custom)
        .notifier(notifier())
        .build();
  }
}
