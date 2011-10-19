package updater.util;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class DownloadProgessUtil {

    protected long downloadedSize;
    protected long totalSize;
    protected int averageTimeSpan;
    protected List<Record> records;
    protected long speed;

    public DownloadProgessUtil() {
        downloadedSize = 0;
        totalSize = 0;
        averageTimeSpan = 5;
        records = new LinkedList<Record>();
        speed = 0;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public synchronized void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
        feed(downloadedSize - this.downloadedSize);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getAverageTimeSpan() {
        return averageTimeSpan;
    }

    public synchronized void setAverageTimeSpan(int averageTimeSpan) {
        this.averageTimeSpan = averageTimeSpan;
        updateSpeed();
    }

    public synchronized void feed(long byteDownloaded) {
        this.downloadedSize += byteDownloaded;
        records.add(new Record(byteDownloaded));
        updateSpeed();
    }

    public long getSpeed() {
        return speed;
    }

    public int getTimeRemaining() {
        return speed == 0 ? 0 : (int) ((double) (totalSize - downloadedSize) / (double) speed);
    }

    protected void updateSpeed() {
        // should be synchronized
        long currentTime = System.currentTimeMillis();
        long minimumTime = currentTime;
        for (int i = 0, iEnd = records.size(); i < iEnd; i++) {
            Record record = records.get(i);
            if (currentTime - record.time > averageTimeSpan * 1000) {
                records.remove(i);
                i--;
                iEnd--;
            } else if (record.time < minimumTime) {
                minimumTime = record.time;
            }
        }

        long bytesDownloadedWithinPeriod = 0;
        for (Record record : records) {
            bytesDownloadedWithinPeriod += record.byteDownloaded;
        }
        speed = currentTime == minimumTime ? 0 : (long) ((double) bytesDownloadedWithinPeriod / ((double) (currentTime - minimumTime) / 1000F));
    }

    protected static class Record {

        protected long time;
        protected long byteDownloaded;

        protected Record(long byteDownloaded) {
            time = System.currentTimeMillis();
            this.byteDownloaded = byteDownloaded;
        }
    }
}
