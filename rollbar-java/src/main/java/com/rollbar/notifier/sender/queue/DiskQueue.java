package com.rollbar.notifier.sender.queue;

import static java.util.Collections.emptyList;

import com.rollbar.api.payload.Payload;
import com.rollbar.notifier.util.ObjectsUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Queue queue} of {@link Payload payloads} persisted on disk.
 */
public class DiskQueue extends AbstractQueue<Payload> {

  private static Logger LOGGER = LoggerFactory.getLogger(DiskQueue.class);

  private static final int UNBOUNDED_QUEUE = 0;

  private static final String QUEUE_FOLDER = ".rollbar-queue";

  private static final String FILENAME_SUFFIX = "payload";

  private static final String FILENAME_NAME_FORMAT = "%s.%s";

  private final File queueFolder;

  private final int maxSize;

  private DiskQueue(Builder builder) {
    this.maxSize = builder.maxSize;
    this.queueFolder = builder.queueFolder;

    /*
    A RuntimeException can cause a silent crash, since the error would not be saved and in the next
    session there would be no payload to send. So we just log the error.
    */
    if (!this.queueFolder.exists()) {
      if (!this.queueFolder.mkdirs()) {
        LOGGER.error("Could not create folder: {}", queueFolder);
      }
    }

    if (!this.queueFolder.canRead() || !this.queueFolder.canWrite()) {
      LOGGER.error("Not enough permissions folder: {}", queueFolder);
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
    ObjectOutputStream objectOut = null;
    File file = new File(queueFolder.getAbsolutePath(), createFilename(payload));
    try {
      objectOut = new ObjectOutputStream(new FileOutputStream(file));
      objectOut.writeObject(payload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      ObjectsUtils.close(objectOut);
    }
  }

  private boolean isFull() {
    if (maxSize <= UNBOUNDED_QUEUE) {
      return false;
    }

    return size() >= maxSize;
  }

  private Payload readFromFile(boolean removeFile) {
    List<File> files = getFiles();

    if (files.size() > 0) {
      File eventFile = getFiles().get(0);

      return read(eventFile, removeFile);
    } else {
      return null;
    }
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
    ObjectInputStream objectInput = null;
    boolean error = false;

    try {
      objectInput = new ObjectInputStream(new FileInputStream(file));
      Object o = objectInput.readObject();

      return (Payload) o;
    } catch (Exception e) {
      error = true;
      throw new RuntimeException(e);
    } finally {
      // If some error happened discard the payload independently of remove flag.
      if (remove || error) {
        if (!file.delete()) {
          LOGGER.error("Can not delete the file: {}", file.getPath());
        }
      }
      ObjectsUtils.close(objectInput);
    }
  }

  /**
   * Builder class for {@link DiskQueue}.
   */
  public static final class Builder {

    private File queueFolder;

    private int maxSize;

    /**
     * Constructor.
     */
    public Builder() {
      this.maxSize = UNBOUNDED_QUEUE;
    }

    /**
     * The max size of the queue.
     * @param maxSize the max size.
     * @return the builder instance.
     */
    public Builder maxSize(int maxSize) {
      this.maxSize = maxSize;
      return this;
    }

    /**
     * The queue folder where persist the payloads.
     * @param queueFolder the queue folder.
     * @return the builder instance.
     */
    public Builder queueFolder(File queueFolder) {
      this.queueFolder = queueFolder;
      return this;
    }

    /**
     * Builds the {@link DiskQueue disk queue}.
     *
     * @return the disk queue.
     */
    public DiskQueue build() {
      if (this.queueFolder == null) {
        this.queueFolder = new File(QUEUE_FOLDER);
      }
      return new DiskQueue(this);
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
