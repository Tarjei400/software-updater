package updater.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The HTTP downloader.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class HTTPDownloader implements Pausable, Interruptible {

    /**
     * Indicate whether it is in debug mode or not.
     */
    protected final static boolean debug;

    static {
        String debugMode = System.getProperty("SoftwareUpdaterDebugMode");
        debug = debugMode == null || !debugMode.equals("true") ? false : true;
    }
    /**
     * List of tasks to be executed after interrupted.
     */
    protected final List<Runnable> interruptedTasks;
    /**
     * Indicate currently is paused or not.
     */
    protected boolean pause;
    /**
     * If not null, downloader will try to download the file with start position according to the downloaded length of {@code resumeFile}. 
     */
    protected File resumeFile;
    /**
     * The content to output to. Note if {@link #resumeFile} is set, the downloader will not output to this stream.
     */
    protected OutputStream outputTo;
    /**
     * The If-Modified-Since header value, -1 means not set.
     */
    protected long ifModifiedSince;
    /**
     * Indicate if currently is downloading a file.
     */
    protected boolean downloading = false;

    /**
     * Constructor.
     */
    public HTTPDownloader() {
        resumeFile = null;
        outputTo = null;
        ifModifiedSince = -1;

        interruptedTasks = Collections.synchronizedList(new ArrayList<Runnable>());
        pause = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterruptedTask(Runnable task) {
        if (task == null) {
            return;
        }
        interruptedTasks.add(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInterruptedTask(Runnable task) {
        if (task == null) {
            return;
        }
        interruptedTasks.remove(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause(boolean pause) {
        synchronized (this) {
            this.pause = pause;
            if (!pause) {
                notifyAll();
            }
        }
    }

    /**
     * Get the file to resume download on.
     * @return the file
     */
    public File getResumeFile() {
        return resumeFile;
    }

    /**
     * Tell the downloader to resume download according to {@link resumeFile}.
     * @param resumeFile the file to resume download on
     */
    public void setResumeFile(File resumeFile) {
        this.resumeFile = resumeFile;
    }

    /**
     * Get the stream to outputthe content to.
     * @return the stream to output to
     */
    public OutputStream getOutputTo() {
        return outputTo;
    }

    /**
     * Set the stream to output to. Note if {@link #resumeFile} is set, the downloader will not output to this stream.
     * @param outputTo the file to save to
     */
    public void setOutputTo(OutputStream outputTo) {
        this.outputTo = outputTo;
    }

    /**
     * Set the If-Modified-Since header.
     * @param time the time in milli second
     */
    public void setIfModifiedSince(long time) {
        ifModifiedSince = time;
    }

    /**
     * Download.
     * @param listener the listener to listen to download progress
     * @param url the URL to download from
     * @param fileSHA256 the expected SHA-256 checksum of the final downloaded file, null means not specified
     * @param expectedLength expected file size in bytes to download, -1 means no specified
     * @return the result
     * @throws MalformedURLException URL is invalid
     * @throws IOException error occurred when reading
     */
    public DownloadResult download(DownloadProgressListener listener, URL url, String fileSHA256, int expectedLength, int retryTimes, int retryDelay) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException("argument 'url' cannot be null");
        }
        if (fileSHA256 != null && !fileSHA256.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("SHA format invalid, expected: ^[0-9a-f]{64}$, checksum: " + fileSHA256);
        }

        DownloadResult retryResult = null;
        boolean truncateResumeFileOnRetry = true;

        HttpURLConnection httpConn = null;
        InputStream in = null;
        OutputStream resumeFileOut = null;
        try {
            downloading = true;

            URLConnection conn = url.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL: " + conn.toString());
            }

            httpConn = (HttpURLConnection) conn;

            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "HTTP Downloader");
            httpConn.setUseCaches(false);

            long startRange = 0;
            if (resumeFile != null && resumeFile.length() > 0) {
                long resumeFileLength = resumeFile.length();

                if (expectedLength >= 0) {
                    if (resumeFileLength == expectedLength) {
                        if (fileSHA256 != null) {
                            if (!CommonUtil.getSHA256String(resumeFile).equals(fileSHA256)) {
                                CommonUtil.truncateFile(resumeFile);
                            } else {
                                // download finished
                                if (listener != null) {
                                    listener.byteStart(resumeFileLength);
                                }
                                return DownloadResult.SUCCEED;
                            }
                        } else {
                            // download finished
                            if (listener != null) {
                                listener.byteStart(resumeFileLength);
                            }
                            return DownloadResult.SUCCEED;
                        }
                    } else if (resumeFileLength > expectedLength) {
                        CommonUtil.truncateFile(resumeFile);
                    } else {
                        startRange = resumeFileLength;
                        httpConn.setRequestProperty("Range", "bytes=" + resumeFileLength + "-");
                    }
                } else {
                    startRange = resumeFileLength;
                    httpConn.setRequestProperty("Range", "bytes=" + resumeFileLength + "-");
                }
            }
            if (ifModifiedSince != -1) {
                httpConn.setIfModifiedSince(ifModifiedSince);
            }

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (NumberFormatException ex) {
                }
            }
            //</editor-fold>
            if (startRange != 0) {
                Pattern pattern = Pattern.compile("^bytes\\s([0-9]+)-([0-9]+)/([0-9]+)$");
                String contentRangeString = httpConn.getHeaderField("Content-Range");
                if (contentRangeString != null) {
                    Matcher matcher = pattern.matcher(contentRangeString.trim());
                    if (matcher.matches()) {
                        int rangeStart = Integer.parseInt(matcher.group(1));
                        int rangeEnd = Integer.parseInt(matcher.group(2));
                        contentLength = Integer.parseInt(matcher.group(3));
                        if (rangeStart != startRange) {
                            throw new RuntimeException(DownloadResult.RESUME_RANGE_FAILED.getValue());
                        }
                        if (contentLength - 1 != rangeEnd) {
                            throw new RuntimeException(DownloadResult.RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH.getValue());
                        }
                    } else {
                        throw new RuntimeException(DownloadResult.RESUME_RANGE_RESPOND_INVALID.getValue());
                    }
                } else {
                    startRange = 0;
                    CommonUtil.truncateFile(resumeFile);
                }
            }

            if (listener != null) {
                listener.byteTotal(contentLength);
            }

            // check according to header information
            if (httpStatusCode == 304 && ifModifiedSince != -1) {
                return DownloadResult.FILE_NOT_MODIFIED;
            } else if (httpStatusCode != 200 && httpStatusCode != 206) {
                throw new RuntimeException(DownloadResult.EXPECTED_LENGTH_NOT_MATCH.getValue());
            }

            if (contentLength != - 1 && expectedLength != -1 && contentLength != expectedLength) {
                throw new RuntimeException(DownloadResult.FAILED.getValue());
            }

            // notify listener the starting byte position
            if (listener != null) {
                listener.byteStart(startRange);
            }

            // download
            MessageDigest digest = null;
            if (fileSHA256 != null) {
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException ex) {
                    return DownloadResult.FAILED;
                }
            }
            if (startRange != 0) {
                digest(digest, resumeFile);
            }
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 32768) : new BufferedInputStream(in, 32768);
            OutputStream outputToOut = null;
            if (resumeFile != null) {
                resumeFileOut = new BufferedOutputStream(new FileOutputStream(resumeFile, startRange != 0), 32768);
            } else if (outputTo != null) {
                outputToOut = outputTo;
            }
            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                try {
                    check();
                } catch (RuntimeException ex) {
                    return DownloadResult.INTERRUPTED;
                }

                if (digest != null) {
                    digest.update(b, 0, byteRead);
                }
                if (resumeFileOut != null) {
                    resumeFileOut.write(b, 0, byteRead);
                }
                if (outputToOut != null) {
                    outputToOut.write(b, 0, byteRead);
                }
                cumulateByteRead += byteRead;

                if (listener != null) {
                    listener.byteDownloaded(byteRead);
                }
            }

            // check the downloaded file
            if (cumulateByteRead + startRange != contentLength) {
                throw new RuntimeException(DownloadResult.FAILED.getValue());
            }
            if (fileSHA256 != null && digest != null && !CommonUtil.byteArrayToHexString(digest.digest()).equals(fileSHA256)) {
                throw new RuntimeException(DownloadResult.CHECKSUM_FAILED.getValue());
            }
        } catch (MalformedURLException ex) {
            throw ex;
        } catch (IOException ex) {
            truncateResumeFileOnRetry = false;
            retryResult = DownloadResult.FAILED;
        } catch (RuntimeException ex) {
            truncateResumeFileOnRetry = true;
            retryResult = DownloadResult.getDownloadResult(ex.getMessage());
        } finally {
            downloading = false;
            CommonUtil.closeQuietly(in);
            CommonUtil.closeQuietly(resumeFileOut);
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        if (retryResult != null) {
            return retry(retryResult, truncateResumeFileOnRetry, listener, url, fileSHA256, expectedLength, retryTimes, retryDelay);
        }

        return DownloadResult.SUCCEED;
    }

    /**
     * Retry the download.
     * @param result the reason to retry
     * @param truncateResumeFileOnRetry whether truncate the {@code resumeFile} on retry or not
     */
    protected DownloadResult retry(DownloadResult result, boolean truncateResumeFileOnRetry, DownloadProgressListener listener, URL url, String fileSHA256, int expectedLength, int retryTimes, int retryDelay) throws MalformedURLException {
        if (retryTimes <= 0) {
            return result;
        }
        listener.downloadRetry(result);
        try {
            Thread.sleep(Math.max(0, retryDelay));
        } catch (InterruptedException ex) {
            if (debug) {
                Logger.getLogger(HTTPDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (truncateResumeFileOnRetry) {
            CommonUtil.truncateFile(resumeFile);
        }
        return download(listener, url, fileSHA256, expectedLength, retryTimes - 1, retryDelay);
    }

    /**
     * Check if paused or interrupted.
     */
    protected void check() {
        synchronized (this) {
            if (pause) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        if (Thread.interrupted()) {
            synchronized (interruptedTasks) {
                for (Runnable task : interruptedTasks) {
                    task.run();
                }
            }
            throw new RuntimeException(new InterruptedException());
        }
    }

    /**
     * Download result for {@link #download(updater.util.DownloadProgressListener, java.net.URL, java.lang.String, int)}.
     */
    public static enum DownloadResult {

        SUCCEED("SUCCEED"),
        FILE_NOT_MODIFIED("FILE_NOT_MODIFIED"),
        EXPECTED_LENGTH_NOT_MATCH("EXPECTED_LENGTH_NOT_MATCH"),
        CHECKSUM_FAILED("CHECKSUM_FAILED"),
        FAILED("FAILED"),
        INTERRUPTED("INTERRUPTED"),
        RESUME_RANGE_FAILED("RESUME_RANGE_FAILED"),
        RESUME_RANGE_RESPOND_INVALID("RESUME_RANGE_RESPOND_INVALID"),
        RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH("RANGE_LENGTH_NOT_MATCH_CONTENT_LENGTH");
        protected final String value;

        DownloadResult(String value) {
            this.value = value;
        }

        /**
         * Get the unique integer representation for this type of compression..
         * @return the integer value
         */
        public String getValue() {
            return value;
        }

        /**
         * Get the {@link updater.util.HTTPDownloader.DownloadResult} by the downloadResults' string value.
         * @param value the string value
         * @return the {@link updater.util.HTTPDownloader.DownloadResult} or null if not correspondent found
         */
        public static DownloadResult getDownloadResult(String value) {
            DownloadResult[] downloadResults = DownloadResult.values();
            for (DownloadResult downloadResult : downloadResults) {
                if (downloadResult.getValue().equals(value)) {
                    return downloadResult;
                }
            }
            return null;
        }
    }

    /**
     * Read the content in the <code>file</code> and put into the <code>digest</code>.
     * @param digest the digest object
     * @param file the file to read the content from
     * @throws IOException error occurred when reading file
     */
    protected static void digest(MessageDigest digest, File file) throws IOException {
        if (digest == null) {
            throw new NullPointerException("argument 'digest' cannot be null");
        }
        if (file == null) {
            throw new NullPointerException("argument 'file' cannot be null");
        }

        FileInputStream fin = null;
        try {
            long fileLength = file.length();
            fin = new FileInputStream(file);

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32768];
            while ((byteRead = fin.read(b)) != -1) {
                digest.update(b, 0, byteRead);

                cumulateByteRead += byteRead;
                if (cumulateByteRead >= fileLength) {
                    break;
                }
            }

            if (cumulateByteRead != fileLength) {
                throw new IOException(String.format("The total number of bytes read does not match the file size. Actual file size: %1$d, bytes read: %2$d, path: %3$s",
                        fileLength, cumulateByteRead, file.getAbsolutePath()));
            }
        } finally {
            CommonUtil.closeQuietly(fin);
        }
    }
}
