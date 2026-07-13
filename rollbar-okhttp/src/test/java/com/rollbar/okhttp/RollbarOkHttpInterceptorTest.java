package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class RollbarOkHttpInterceptorTest {

  private MockWebServer server;
  private NetworkTelemetryRecorder recorder;
  private OkHttpClient client;

  @BeforeEach
  public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    recorder = mock(NetworkTelemetryRecorder.class);

    client = new OkHttpClient.Builder()
        .addInterceptor(new RollbarOkHttpInterceptor(recorder))
        .build();
  }

  @AfterEach
  public void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void successfulResponse_doesNotRecordEvent() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(200));

    Request request = new Request.Builder().url(server.url("/ok")).build();
    Response response = client.newCall(request).execute();
    response.close();

    assertEquals(200, response.code());
    verifyNoInteractions(recorder);
  }

  @Test
  public void redirectResponse_doesNotRecordEvent() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(301).addHeader("Location", "/other"));

    OkHttpClient noFollowClient = client.newBuilder().followRedirects(false).build();
    Request request = new Request.Builder().url(server.url("/redirect")).build();
    Response response = noFollowClient.newCall(request).execute();
    response.close();

    assertEquals(301, response.code());
    verifyNoInteractions(recorder);
  }

  @Test
  public void clientErrorResponse_recordsNetworkEvent() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404));

    Request request = new Request.Builder().url(server.url("/not-found")).build();
    Response response = client.newCall(request).execute();
    response.close();

    assertEquals(404, response.code());
    verify(recorder).recordNetworkEvent(
        eq(Level.CRITICAL), eq("GET"), contains("/not-found"), eq("404"));
    verify(recorder, never()).recordErrorEvent(any());
  }

  @Test
  public void serverErrorResponse_recordsNetworkEvent() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    Request request = new Request.Builder().url(server.url("/error")).build();
    Response response = client.newCall(request).execute();
    response.close();

    assertEquals(500, response.code());
    verify(recorder).recordNetworkEvent(
        eq(Level.CRITICAL), eq("GET"), contains("/error"), eq("500"));
    verify(recorder, never()).recordErrorEvent(any());
  }

  @Test
  public void connectionFailure_recordsErrorEvent() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    Request request = new Request.Builder().url(server.url("/fail")).build();

    assertThrows(IOException.class, () -> client.newCall(request).execute());

    verify(recorder).recordErrorEvent(any(IOException.class));
    verify(recorder, never()).recordNetworkEvent(any(), any(), any(), any());
  }

  @Test
  public void postRequest_recordsCorrectMethod() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    Request request = new Request.Builder()
        .url(server.url("/post"))
        .post(okhttp3.RequestBody.create("body", okhttp3.MediaType.parse("text/plain")))
        .build();
    Response response = client.newCall(request).execute();
    response.close();

    verify(recorder).recordNetworkEvent(eq(Level.CRITICAL), eq("POST"), any(), eq("500"));
  }

  @Test
  public void nullRecorder_errorResponse_doesNotThrowNPE() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    OkHttpClient nullRecorderClient = new OkHttpClient.Builder()
        .addInterceptor(new RollbarOkHttpInterceptor(null))
        .build();

    Request request = new Request.Builder().url(server.url("/error")).build();
    Response response = nullRecorderClient.newCall(request).execute();
    response.close();

    assertEquals(500, response.code());
  }

  @Test
  public void nullRecorder_connectionFailure_doesNotThrow() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    OkHttpClient nullRecorderClient = new OkHttpClient.Builder()
        .addInterceptor(new RollbarOkHttpInterceptor(null))
        .build();

    Request request = new Request.Builder().url(server.url("/fail")).build();

    assertThrows(IOException.class, () -> nullRecorderClient.newCall(request).execute());
  }

  @Test
  public void recorderThrowsOnErrorResponse_responseStillReturned() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    doThrow(new RuntimeException("recorder boom"))
        .when(recorder)
        .recordNetworkEvent(any(), any(), any(), any());

    Request request = new Request.Builder().url(server.url("/error")).build();
    Response response = client.newCall(request).execute();
    response.close();

    assertEquals(500, response.code());
  }

  @Test
  public void recorderThrowsOnConnectionFailure_originalIOExceptionPropagates() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

    doThrow(new RuntimeException("recorder boom"))
        .when(recorder)
        .recordErrorEvent(any());

    Request request = new Request.Builder().url(server.url("/fail")).build();

    assertThrows(IOException.class, () -> client.newCall(request).execute());
    verify(recorder).recordErrorEvent(any(IOException.class));
  }

  @Test
  public void defaultSanitizer_stripsQueryParamsFromUrl() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    Request request = new Request.Builder()
        .url(server.url("/sensitive?token=sk_live_secret&email=user@example.com"))
        .build();
    Response response = client.newCall(request).execute();
    response.close();

    verify(recorder).recordNetworkEvent(
        eq(Level.CRITICAL), eq("GET"),
        argThat(url -> url.contains("/sensitive") && !url.contains("secret") && !url.contains("email")),
        eq("500"));
  }

  @Test
  public void defaultSanitizer_stripsCredentialsAndFragment() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    HttpUrl urlWithCredentials = server.url("/charge")
        .newBuilder()
        .username("anyUser")
        .password("anyPassword")
        .addQueryParameter("token", "abc")
        .fragment("section")
        .build();

    Request request = new Request.Builder().url(urlWithCredentials).build();
    Response response = client.newCall(request).execute();
    response.close();

    verify(recorder).recordNetworkEvent(
        eq(Level.CRITICAL), eq("GET"),
        argThat(url -> url.contains("/charge")
            && !url.contains("anyUser")
            && !url.contains("anyPassword")
            && !url.contains("token")
            && !url.contains("section")),
        eq("500"));
  }

  @Test
  public void customSanitizerThrows_responseStillReturnedAndRecorderNotCalled() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    OkHttpClient throwingClient = new OkHttpClient.Builder()
        .addInterceptor(new RollbarOkHttpInterceptor(recorder,
            (UrlSanitizer) url -> { throw new IllegalStateException("bad url"); }))
        .build();

    Request request = new Request.Builder().url(server.url("/error")).build();
    Response response = throwingClient.newCall(request).execute();
    response.close();

    assertEquals(500, response.code());
    verify(recorder, never()).recordNetworkEvent(any(), any(), any(), any());
  }

  @Test
  public void customSanitizer_isAppliedToUrl() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(500));

    OkHttpClient customClient = new OkHttpClient.Builder()
        .addInterceptor(new RollbarOkHttpInterceptor(recorder, (UrlSanitizer) url -> "Updated String"))
        .build();

    Request request = new Request.Builder()
        .url(server.url("/path?secret=abc"))
        .build();
    Response response = customClient.newCall(request).execute();
    response.close();

    verify(recorder).recordNetworkEvent(
        eq(Level.CRITICAL), eq("GET"), eq("Updated String"), eq("500"));
  }
}
