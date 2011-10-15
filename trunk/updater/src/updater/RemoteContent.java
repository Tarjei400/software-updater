package updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL.");
            }
            httpConn = (HttpURLConnection) conn;

            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);
            if (lastUpdateDate != -1) {
                httpConn.setIfModifiedSince(lastUpdateDate);
            }

            httpConn.connect();

            if (httpConn.getResponseCode() == 304) {
                return new GetCatalogResult(null, true);
            } else if (httpConn.getResponseCode() != 200) {
                returnResult = new GetCatalogResult(null, false);
                throw new Exception("HTTP status not 200.");
            }

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

            String contentEncoding = httpConn.getHeaderField("Content-Encoding");

            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);
            ByteArrayOutputStream buffer = contentLength == -1 ? new ByteArrayOutputStream() : new ByteArrayOutputStream(contentLength);
            int byteRead;
            byte[] b = new byte[contentLength == -1 ? 1024 : Math.min(contentLength, 1024)];
            while ((byteRead = in.read(b)) != -1) {
                buffer.write(b, 0, byteRead);
            }

            byte[] content = buffer.toByteArray();
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

    public static boolean getPatch(GetPatchListener listener, String url, File saveToFile, String fileSHA1, int expectedLength) {
        boolean returnResult = false;

        InputStream in = null;
        HttpURLConnection httpConn = null;
        OutputStream fout = null;
        try {
            URL urlObj = new URL(url);
            long fileLength = saveToFile.length();

            if (fileLength != 0 && expectedLength != -1) {
                if ((fileLength == expectedLength && !Util.getSHA1(saveToFile).equals(fileSHA1))
                        || fileLength > expectedLength) {
                    try {
                        new FileOutputStream(saveToFile).close();
                    } catch (Exception ex2) {
                        saveToFile.delete();
                    }
                }
            }

            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL.");
            }
            httpConn = (HttpURLConnection) conn;

            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            if (fileLength != 0) {
                httpConn.setRequestProperty("Range", "bytes=" + fileLength + "-");
            }
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);

            httpConn.connect();

            int httpStatusCode = httpConn.getResponseCode();
            if (httpStatusCode != 200 && httpStatusCode != 206) {
                throw new Exception("HTTP status is not 200 or 206.");
            }

            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (Exception ex) {
                }
                if (expectedLength != -1 && contentLength != expectedLength) {
                    throw new Exception("Length not matched.");
                }
            }
            //</editor-fold>

            String contentEncoding = httpConn.getHeaderField("Content-Encoding");

            MessageDigest digest = null;
            if (fileSHA1 != null && fileSHA1.matches("^[0-9a-f]{40}$")) {
                digest = MessageDigest.getInstance("SHA1");
            }

            in = httpConn.getInputStream();
            in = (contentEncoding != null && contentEncoding.equals("gzip")) ? new GZIPInputStream(in, 8192) : new BufferedInputStream(in);

            fout = new BufferedOutputStream(new FileOutputStream(saveToFile, httpStatusCode == 206));

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[1024];
            while ((byteRead = in.read(b)) != -1) {
                if (digest != null) {
                    digest.update(b, 0, byteRead);
                }
                fout.write(b, 0, byteRead);
                cumulateByteRead += byteRead;

                if (cumulateByteRead > expectedLength) {
                    throw new Exception("Error occurred when reading.");
                }
                if (listener != null) {
                    listener.byteDownloaded(byteRead);
                }
            }

            if (expectedLength != -1 && cumulateByteRead != expectedLength) {
                // truncate 'saveToFile'
                try {
                    fout.close();
                } catch (Exception ex) {
                }
                fout = null;
                try {
                    new FileOutputStream(saveToFile).close();
                } catch (Exception ex) {
                    saveToFile.delete();
                }
                throw new Exception("Length not matched.");
            }
            if (digest != null && !Util.getHexString(digest.digest()).equals(fileSHA1)) {
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
        System.out.println(getPatch(new GetPatchListener() {

            @Override
            public void byteDownloaded(int numberOfBytes) {
            }
        }, "http://www.google.com.hk/images/srpr/logo3w.png", new File("out.png"), null, -1));
    }
}
