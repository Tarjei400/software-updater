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
        totalSize = 0;
        downloadedSize = 0;
        averageTimeSpan = 5;
        records = new LinkedList();
        speed = 0;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public synchronized void setDownloadedSize(long downloadedSize) {
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

    public void setAverageTimeSpan(int averageTimeSpan) {
        this.averageTimeSpan = averageTimeSpan;
    }

    public synchronized void feed(long byteDownloaded) {
        records.add(new Record(byteDownloaded));
        updateSpeed();
    }

    public long getSpeed() {
        return speed;
    }

    public int getTimeRemaining() {
        return (int) ((double) (totalSize - downloadedSize) / (double) speed);
    }

    protected void updateSpeed() {
        // should be synchronized
        long currentTime = System.currentTimeMillis();
        long minimumTime = currentTime;
        long _averageTimeSpan = averageTimeSpan * 1000;
        for (int i = 0, iEnd = records.size(); i < iEnd; i++) {
            Record record = records.get(0);
            if (currentTime - record.time > _averageTimeSpan) {
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
        speed = bytesDownloadedWithinPeriod / (currentTime - minimumTime);
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
