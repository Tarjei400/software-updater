package updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import updater.script.Catalog;
import updater.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class RemoteContent {

    protected RemoteContent() {
    }

    public static GetCatalogResult getCatalog(String url, long lastUpdateDate, RSAPublicKey key) {
        GetCatalogResult returnResult = new GetCatalogResult(null, false);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        try {
            URL urlObj = new URL(url);

            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL.");
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);
            if (lastUpdateDate != -1) {
                httpConn.setIfModifiedSince(lastUpdateDate);
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
                } catch (Exception ex) {
                    // ignore
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode == 304 && lastUpdateDate != -1) {
                return new GetCatalogResult(null, true);
            } else if (httpStatusCode != 200) {
                returnResult = new GetCatalogResult(null, false);
                throw new Exception("HTTP status not 200.");
            }


            // download
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);
            ByteArrayOutputStream buffer = contentLength == -1 ? new ByteArrayOutputStream() : new ByteArrayOutputStream(contentLength);
            int byteRead;
            byte[] b = new byte[contentLength == -1 ? 32 : Math.min(contentLength, 32)];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                buffer.write(b, 0, byteRead);
            }

            byte[] content = buffer.toByteArray();

            // decrypt
            if (key != null) {
                ByteArrayOutputStream rsaBuffer = new ByteArrayOutputStream(contentLength);

                Cipher decryptCipher = Cipher.getInstance("RSA");
                decryptCipher.init(Cipher.DECRYPT_MODE, key.getKey());

                int maxContentLength = key.getMaxContentLength();
                if (content.length % maxContentLength != 0) {
                    throw new Exception("RSA block size not match.");
                }

                for (int i = 0, iEnd = content.length; i < iEnd; i += maxContentLength) {
                    rsaBuffer.write(decryptCipher.doFinal(content, i, maxContentLength));
                }

                content = rsaBuffer.toByteArray();
            }

            returnResult = new GetCatalogResult(Catalog.read(buffer.toByteArray()), false);
        } catch (Exception ex) {
            Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    protected static void digest(MessageDigest digest, File file) throws Exception {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            int byteRead;
            byte[] b = new byte[1024];
            while ((byteRead = fin.read(b)) != -1) {
                digest.update(b, 0, byteRead);
            }
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
    }

    public static boolean getPatch(GetPatchListener listener, String url, File saveToFile, String fileSHA256, int expectedLength) {
        boolean returnResult = false;

        InputStream in = null;
        HttpURLConnection httpConn = null;
        OutputStream fout = null;
        try {
            if (!fileSHA256.matches("^[0-9a-f]{64}$")) {
                throw new Exception("SHA format invalid.");
            }

            URL urlObj = new URL(url);
            long fileLength = saveToFile.length();

            // check saveToFile with fileLength and expectedLength
            if (fileLength != 0) {
                if ((fileLength == expectedLength && !Util.getSHA256(saveToFile).equals(fileSHA256))
                        || fileLength > expectedLength) {
                    // truncate/delete the file
                    try {
                        new FileOutputStream(saveToFile).close();
                    } catch (Exception ex2) {
                        saveToFile.delete();
                    }
                    fileLength = 0;
                } else if (fileLength == expectedLength) {
                    return true;
                }
            }


            // open connection
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL.");
            }
            httpConn = (HttpURLConnection) conn;

            // connection setting
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);

            // set request header
            if (fileLength != 0) {
                httpConn.setRequestProperty("Range", "bytes=" + fileLength + "-");
            }
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);

            // connect
            httpConn.connect();

            // get header
            int httpStatusCode = httpConn.getResponseCode();
            String contentEncoding = httpConn.getHeaderField("Content-Encoding");
            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                if (fileLength != 0) {
                    Pattern pattern = Pattern.compile("^([0-9]+)-([0-9]+)/([0-9]+)$");
                    String contentRangeString = httpConn.getHeaderField("Content-Range");
                    if (contentRangeString != null) {
                        Matcher matcher = pattern.matcher(contentRangeString.trim());
                        if (matcher.matches()) {
                            int rangeStart = Integer.parseInt(matcher.group(1));
                            int rangeEnd = Integer.parseInt(matcher.group(2));
                            contentLength = Integer.parseInt(matcher.group(3));
                            if (rangeStart != fileLength) {
                                throw new Exception("Request byte range from " + rangeStart + " but respond byte range: " + contentRangeString);
                            }
                            if (contentLength - 1 != rangeEnd) {
                                throw new Exception("Respond byte range end do not match content length.");
                            }
                        }
                    }
                } else {
                    try {
                        contentLength = Integer.parseInt(contentLengthString.trim());
                    } catch (Exception ex) {
                    }
                }
            }
            //</editor-fold>

            // check according to header information
            if (httpStatusCode != 200 && httpStatusCode != 206) {
                throw new Exception("HTTP status is not 200 or 206.");
            }
            if (contentLength != - 1 && contentLength != expectedLength) {
                throw new Exception("Expected length and respond content length not match.");
            }


            // download
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            if (fileLength != 0) {
                digest(digest, saveToFile);
            }
            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);
            fout = new BufferedOutputStream(new FileOutputStream(saveToFile, httpStatusCode == 206));

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[32];
            while ((byteRead = in.read(b)) != -1) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                digest.update(b, 0, byteRead);
                fout.write(b, 0, byteRead);
                cumulateByteRead += byteRead;

                if (listener != null) {
                    listener.byteDownloaded(byteRead);
                }
            }

            // check the downloaded file
            if (cumulateByteRead + fileLength != expectedLength) {
                throw new Exception("Error occurred when reading (cumulated bytes read != expected length).");
            }
            if (!Util.byteArrayToHexString(digest.digest()).equals(fileSHA256)) {
                throw new Exception("Checksum not matched.");
            }

            returnResult = true;
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static interface GetPatchListener {

        void byteDownloaded(int numberOfBytes);
    }

    public static class GetCatalogResult {

        protected Catalog catalog;
        protected boolean notModified;

        public GetCatalogResult(Catalog catalog, boolean notModified) {
            this.catalog = catalog;
            this.notModified = notModified;
        }

        public Catalog getCatalog() {
            return catalog;
        }

        public boolean isNotModified() {
            return notModified;
        }
    }

    public static class RSAPublicKey {

        protected BigInteger mod;
        protected BigInteger exp;

        public RSAPublicKey(BigInteger mod, BigInteger exp) {
            this.mod = mod;
            this.exp = exp;
        }

        public PublicKey getKey() {
            try {
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, exp);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            } catch (Exception ex) {
                Logger.getLogger(RemoteContent.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public int getMaxContentLength() {
            return (mod.bitLength() / 8) - 11;
        }
    }

    public static void main(String[] args) throws Exception {
//        FileInputStream fin = new FileInputStream(new File("N_VirtualBox-4.1.4-74291-Win.exe"));
//        int byteRead;
//        byte[] b = new byte[1024];
//        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        while ((byteRead = fin.read(b)) != -1) {
//            digest.update(b, 0, byteRead);
//        }
//        System.out.println(Util.byteArrayToHexString(digest.digest()));
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println(getPatch(new GetPatchListener() {

                    @Override
                    public void byteDownloaded(int numberOfBytes) {
                    }
                }, "http://download.virtualbox.org/virtualbox/4.1.4/VirtualBox-4.1.4-74291-Win.exe", new File("VirtualBox-4.1.4-74291-Win.exe"), "d90568b90fe4d6b091d2673e996880d47afa9e952fedd0befdec160ee216e468", 91681072));
            }
        });
        thread.start();
        Thread.sleep(10000);
        thread.interrupt();
        Thread.sleep(5000);
        System.out.println("end");
    }
}
