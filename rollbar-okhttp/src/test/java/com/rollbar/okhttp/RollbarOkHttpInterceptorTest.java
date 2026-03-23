package com.rollbar.okhttp;

import com.rollbar.api.payload.data.Level;
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
import static org.mockito.Mockito.*;

class RollbarOkHttpInterceptorTest {

    private MockWebServer server;
    private NetworkTelemetryRecorder recorder;
    private OkHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        recorder = mock(NetworkTelemetryRecorder.class);

        client = new OkHttpClient.Builder()
                .addInterceptor(new RollbarOkHttpInterceptor(recorder))
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void successfulResponse_doesNotRecordEvent() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200));

        Request request = new Request.Builder().url(server.url("/ok")).build();
        Response response = client.newCall(request).execute();
        response.close();

        assertEquals(200, response.code());
        verifyNoInteractions(recorder);
    }

    @Test
    void redirectResponse_doesNotRecordEvent() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(301).addHeader("Location", "/other"));

        OkHttpClient noFollowClient = client.newBuilder().followRedirects(false).build();
        Request request = new Request.Builder().url(server.url("/redirect")).build();
        Response response = noFollowClient.newCall(request).execute();
        response.close();

        assertEquals(301, response.code());
        verifyNoInteractions(recorder);
    }

    @Test
    void clientErrorResponse_recordsNetworkEvent() throws IOException {
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
    void serverErrorResponse_recordsNetworkEvent() throws IOException {
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
    void connectionFailure_recordsErrorEvent() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        Request request = new Request.Builder().url(server.url("/fail")).build();

        assertThrows(IOException.class, () -> client.newCall(request).execute());

        verify(recorder).recordErrorEvent(any(IOException.class));
        verify(recorder, never()).recordNetworkEvent(any(), any(), any(), any());
    }

    @Test
    void postRequest_recordsCorrectMethod() throws IOException {
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
    void nullRecorder_errorResponse_doesNotThrowNPE() throws IOException {
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
    void nullRecorder_connectionFailure_doesNotThrow() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        OkHttpClient nullRecorderClient = new OkHttpClient.Builder()
                .addInterceptor(new RollbarOkHttpInterceptor(null))
                .build();

        Request request = new Request.Builder().url(server.url("/fail")).build();

        assertThrows(IOException.class, () -> nullRecorderClient.newCall(request).execute());
    }
}
