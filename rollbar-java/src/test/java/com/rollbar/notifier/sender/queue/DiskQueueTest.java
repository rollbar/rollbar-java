package com.rollbar.notifier.sender.queue;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.rollbar.api.payload.Payload;
import java.io.File;
import java.util.Iterator;
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
}