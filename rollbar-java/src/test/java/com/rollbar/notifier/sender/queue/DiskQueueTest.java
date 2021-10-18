package com.rollbar.notifier.sender.queue;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

import com.rollbar.api.payload.Payload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

import com.rollbar.api.payload.data.Data;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DiskQueueTest {

  @Rule
  public TemporaryFolder folder= new TemporaryFolder();

  File queueFolder;

  DiskQueue sut;

  @Before
  public void setUp() throws Exception {
    queueFolder = folder.newFolder();
    sut = new DiskQueue.Builder()
        .queueFolder(queueFolder)
        .build();
  }

  @Test
  public void shouldReturnTheSize() {
    Payload payload1 = new Payload.Builder().build();
    Payload payload2 = new Payload.Builder().build();

    sut.add(payload1);
    sut.add(payload2);

    assertThat(sut.size(), is(2));
  }

  @Test
  public void shouldReturnIteratorOfPayloads() {
    Payload payload1 = new Payload.Builder().build();
    Payload payload2 = new Payload.Builder().build();

    sut.add(payload1);
    sut.add(payload2);

    Iterator<Payload> it = sut.iterator();

    assertThat(it.hasNext(), is(true));
    assertThat(it.next(), anyOf(equalTo(payload1), equalTo(payload2)));
    assertThat(it.hasNext(), is(true));
    assertThat(it.next(), anyOf(equalTo(payload1), equalTo(payload2)));
  }

  @Test
  public void shouldOfferEnqueueIfNotFull() {
    Payload payload = new Payload.Builder().build();

    assertThat(sut.offer(payload), is(true));
    assertThat(sut.size(), is(1));
  }

  @Test
  public void shouldOfferNotEnqueueIfFull() {
    DiskQueue sut = new DiskQueue.Builder()
        .maxSize(1)
        .queueFolder(queueFolder)
        .build();

    Payload payload1 = new Payload.Builder().build();
    Payload payload2 = new Payload.Builder().build();

    assertThat(sut.offer(payload1), is(true));
    assertThat(sut.offer(payload2), is(false));
    assertThat(sut.size(), is(1));
  }

  @Test
  public void shouldPollDequeueRemovingIt() {
    Payload payload = new Payload.Builder().build();

    sut.offer(payload);

    Payload result = sut.poll();

    assertThat(result, is(payload));
    assertThat(sut.size(), is(0));
  }

  @Test
  public void shouldPeekDequeueNotRemovingIt() {
    Payload payload = new Payload.Builder().build();

    sut.offer(payload);

    Payload result = sut.peek();

    assertThat(result, is(payload));
    assertThat(sut.size(), is(1));
  }

  @Test
  public void shouldPollEmptyQueue() {
    Payload result = sut.peek();

    assertThat(result, is(nullValue()));
    assertThat(sut.size(), is(0));
  }

  @Test(expected = RuntimeException.class)
  public void shouldDiscardInvalidPayloadsWithPeek() throws Exception {
    // Create an old serialized payload file in the disk queue.
    File queueFile = new File(queueFolder.getAbsolutePath(),
            String.format("%s.%s", UUID.randomUUID().toString(), "payload"));

    InputStream input = this.getClass().getClassLoader().getResource("invalid.payload").openStream();
    Files.copy(input, queueFile.toPath());

    Payload result = sut.peek();

    assertThat(result, is(nullValue()));
    assertFalse(new File(queueFile.getPath()).exists());
  }

  @Test(expected = RuntimeException.class)
  public void shouldDiscardInvalidPayloadsWithPoll() throws Exception {
    // Create an old serialized payload file in the disk queue.
    File queueFile = new File(queueFolder.getAbsolutePath(),
        String.format("%s.%s", UUID.randomUUID().toString(), "payload"));

    InputStream input = this.getClass().getClassLoader().getResource("invalid.payload").openStream();
    Files.copy(input, queueFile.toPath());

    Payload result = sut.poll();

    assertThat(result, is(nullValue()));
    assertFalse(new File(queueFile.getPath()).exists());
  }

  @Test
  public void shouldStoreAndRetrieveRetryCount() {
    Payload payload1 = new Payload.Builder().data(
            new Data.Builder().uuid("a1").build()
    ).build();

    payload1.incrementSendAttemptCount();

    Payload payload2 = new Payload.Builder().data(
            new Data.Builder().uuid("a2").build()
    ).build();

    payload2.incrementSendAttemptCount();
    payload2.incrementSendAttemptCount();

    sut.add(payload1);
    sut.add(payload2);

    assertThat(sut.size(), is(2));

    // The queue is written as files in a directory, so we are not guaranteed their order here.
    ArrayList<Payload> payloads = new ArrayList<>();
    payloads.add(sut.poll());
    payloads.add(sut.poll());
    payloads.sort(Comparator.comparing(o -> o.getData().getUuid()));

    Payload retrieved1 = payloads.get(0);
    assertThat(retrieved1.getData().getUuid(), equalTo("a1"));
    // Ensure they're not the same object, otherwise the test isn't testing anything
    assertNotSame(retrieved1, payload1);
    assertThat(retrieved1.getSendAttemptCount(), equalTo(1));

    Payload retrieved2 = payloads.get(1);
    assertThat(retrieved2.getData().getUuid(), equalTo("a2"));
    assertNotSame(retrieved2, payload2);
    assertThat(retrieved2.getSendAttemptCount(), equalTo(2));
  }
}
