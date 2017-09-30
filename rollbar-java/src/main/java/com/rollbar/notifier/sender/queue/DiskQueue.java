package com.rollbar.notifier.sender.queue;

import static java.util.Collections.emptyList;

import com.rollbar.api.payload.Payload;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * A {@link Queue queue} of {@link Payload payloads} persisted on disk.
 */
public class DiskQueue extends AbstractQueue<Payload> {

  private static final int UNBOUNDED_QUEUE = 0;

  private static final String QUEUE_FOLDER = ".rollbar-queue";

  private static final String FILENAME_SUFFIX = "payload";

  private static final String FILENAME_NAME_FORMAT = "%s.%s";

  private final File queueFolder;

  private final int maxSize;

  /**
   * Constructor.
   */
  public DiskQueue() {
    this(UNBOUNDED_QUEUE);
  }

  /**
   * Constructor.
   * @param maxSize the maximum size of the queue.
   */
  public DiskQueue(int maxSize) {
    this(maxSize, new File(QUEUE_FOLDER));
  }

  DiskQueue(int maxSize, File queueFolder) {
    this.maxSize = maxSize;
    this.queueFolder = queueFolder;

    if (!this.queueFolder.exists()) {
      if (this.queueFolder.mkdirs()) {
        throw new RuntimeException("Could not create folder: " + queueFolder);
      }
    }

    if (!this.queueFolder.canRead() || !this.queueFolder.canWrite()) {
      throw new RuntimeException("Not enough permissions folder: " + queueFolder);
    }
  }

  @Override
  public Iterator<Payload> iterator() {
    return new PayloadIterator(getFiles().iterator());
  }

  @Override
  public int size() {
    return getFiles().size();
  }

  @Override
  public boolean offer(Payload payload) {
    if (isFull()) {
      return false;
    }

    writeToFile(payload);
    return true;
  }

  @Override
  public Payload poll() {
    return readFromFile(true);
  }

  @Override
  public Payload peek() {
    return readFromFile(false);
  }

  private void writeToFile(Payload payload) {
    File file = new File(queueFolder.getAbsolutePath(), createFilename(payload));
    try (FileOutputStream fileOut = new FileOutputStream(file);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
      objectOut.writeObject(payload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isFull() {
    if (maxSize <= UNBOUNDED_QUEUE) {
      return false;
    }

    return size() >= maxSize;
  }

  private Payload readFromFile(boolean removeFile) {
    File eventFile = getFiles().get(0);

    return read(eventFile, removeFile);
  }

  private List<File> getFiles() {
    File[] files = this.queueFolder.listFiles();

    if (files == null) {
      return emptyList();
    }

    List<File> payloads = new ArrayList<>();
    for (File file : files) {
      if (file.getName().endsWith(FILENAME_SUFFIX)) {
        payloads.add(file);
      }
    }

    return payloads;
  }

  private static String createFilename(Payload payload) {
    String uuid;
    if (payload.getData() != null && payload.getData().getUuid() != null) {
      uuid = payload.getData().getUuid();
    } else {
      uuid = UUID.randomUUID().toString();
    }
    return String.format(FILENAME_NAME_FORMAT, uuid, FILENAME_SUFFIX);
  }

  private static Payload read(File file, boolean remove) {
    try (FileInputStream fileInput = new FileInputStream(file);
        ObjectInputStream objectInput = new ObjectInputStream(fileInput)) {
      Object o = objectInput.readObject();
      if (remove) {
        if (!file.delete()) {
          // TODO log that the file was not removed
        }
      }
      return (Payload) o;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static final class PayloadIterator implements Iterator<Payload> {

    private final Iterator<File> it;

    public PayloadIterator(Iterator<File> it) {
      this.it = it;
    }

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public Payload next() {
      return read(it.next(), false);
    }

    @Override
    public void remove() {
      it.remove();
    }
  }
}
