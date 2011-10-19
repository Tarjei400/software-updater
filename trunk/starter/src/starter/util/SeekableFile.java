package starter.util;

import com.nothome.delta.SeekableSource;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SeekableFile implements SeekableSource {

    private byte[] b;
    protected RandomAccessFile file;
    protected final List<Runnable> interruptedTasks;

    public SeekableFile(RandomAccessFile file) {
        b = new byte[1024];
        this.file = file;
        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
    }

    public void addInterruptedTask(Runnable task) {
        interruptedTasks.add(task);
    }

    public void removeInterruptedTask(Runnable task) {
        interruptedTasks.remove(task);
    }

    @Override
    public void seek(long pos) throws IOException {
        checkInterrupted();
        file.seek(pos);
    }

    @Override
    public int read(ByteBuffer bb) throws IOException {
        checkInterrupted();
        int byteRead, byteToRead = 0, cumulatedByteRead = 0;

        while (true) {
            byteToRead = bb.remaining();
            if (byteToRead > b.length) {
                byteToRead = b.length;
            }

            if ((byteRead = file.read(b, 0, byteToRead)) != -1) {
                bb.put(b, 0, byteRead);
                cumulatedByteRead += byteRead;
            } else {
                return cumulatedByteRead == 0 ? -1 : cumulatedByteRead;
            }
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    protected void checkInterrupted() {
        if (Thread.interrupted()) {
            synchronized (interruptedTasks) {
                for (Runnable task : interruptedTasks) {
                    task.run();
                }
            }
            throw new RuntimeException(new InterruptedException());
        }
    }
}
