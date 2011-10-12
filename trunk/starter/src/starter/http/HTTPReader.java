package starter.http;

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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import starter.script.Catalog;
import starter.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class HTTPReader {

    private HTTPReader() {
    }

    public static GetCatalogResult getCatalog(String url, long lastUpdateDate, RSAPublicKey key) throws MalformedURLException {
        GetCatalogResult returnResult = null;

        URL urlObj = new URL(url);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        try {
            URLConnection conn = urlObj.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                throw new MalformedURLException("It is not a valid http URL.");
            }
            httpConn = (HttpURLConnection) conn;
            httpConn.setRequestProperty("Connection", "close");
            httpConn.setRequestProperty("Accept-Encoding", "gzip");
            httpConn.setRequestProperty("User-Agent", "Software Updater");
            httpConn.setUseCaches(false);
            httpConn.setIfModifiedSince(lastUpdateDate);

            httpConn.connect();

            if (httpConn.getResponseCode() != 200) {
                returnResult = new GetCatalogResult(null, false);
                throw new Exception();
            }
            if (httpConn.getResponseCode() == 304) {
                returnResult = new GetCatalogResult(null, true);
                throw new Exception();
            }

            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (Exception ex) {
                }
            }
            //</editor-fold>

            String contentEncoding = httpConn.getHeaderField("Content-Encoding");

            in = httpConn.getInputStream();
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(in, 8192);
            } else {
                in = new BufferedInputStream(in);
            }

            ByteArrayOutputStream buffer;
            if (contentLength == -1) {
                buffer = new ByteArrayOutputStream();
            } else {
                buffer = new ByteArrayOutputStream(contentLength);
            }

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
                    throw new Exception();
                }

                for (int i = 0, iEnd = content.length; i < iEnd; i += maxContentLength) {
                    rsaBuffer.write(decryptCipher.doFinal(content, i, maxContentLength));
                }

                content = rsaBuffer.toByteArray();
            }

            returnResult = new GetCatalogResult(Catalog.read(buffer.toByteArray()), false);
        } catch (Exception ex) {
            Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException ex) {
                Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static class GetCatalogResult {

        private Catalog catalog;
        private boolean notModified;

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

        private BigInteger mod;
        private BigInteger exp;

        public RSAPublicKey(BigInteger mod, BigInteger exp) {
            this.mod = mod;
            this.exp = exp;
        }

        public PublicKey getKey() {
            try {
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(mod, exp);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public int getMaxContentLength() {
            return (mod.bitLength() / 8) - 11;
        }
    }

    public static boolean getPatch(String url, File file, String sha1, int length) throws MalformedURLException {
        boolean returnResult = false;

        URL urlObj = new URL(url);

        InputStream in = null;
        HttpURLConnection httpConn = null;
        OutputStream fout = null;
        try {
            long fileLength = file.length();

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

            if (httpConn.getResponseCode() != 200) {
                throw new Exception();
            }

            int contentLength = -1;
            //<editor-fold defaultstate="collapsed" desc="content length">
            String contentLengthString = httpConn.getHeaderField("Content-Length");
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (Exception ex) {
                }
                if (length != -1 && contentLength != length) {
                    throw new Exception();
                }
            }
            //</editor-fold>

            String contentEncoding = httpConn.getHeaderField("Content-Encoding");

            MessageDigest digest = null;
            if (sha1 != null && sha1.matches("^[0-9a-f]{40}$")) {
                digest = MessageDigest.getInstance("SHA1");
            }

            in = httpConn.getInputStream();
            if (contentEncoding != null && contentEncoding.equals("gzip")) {
                in = new GZIPInputStream(in, 8192);
            } else {
                in = new BufferedInputStream(in);
            }

            fout = new BufferedOutputStream(new FileOutputStream(file, httpConn.getResponseCode() == 206));

            int byteRead, byteCount = 0;
            byte[] b = new byte[1024];
            while ((byteRead = in.read(b)) != -1) {
                if (digest != null) {
                    digest.update(b, 0, byteRead);
                }
                fout.write(b, 0, byteRead);
                byteCount += byteRead;
            }

            if (length != -1 && byteCount != length) {
                try {
                    fout.close();
                } catch (Exception ex) {
                }
                fout = null;
                try {
                    new FileOutputStream(file).close();
                } catch (Exception ex) {
                    file.delete();
                }
                throw new Exception();
            }
            if (digest != null && !Util.getHexString(digest.digest()).equals(sha1)) {
                throw new Exception();
            }

            returnResult = true;
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(HTTPReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
        RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

        keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        System.out.println(privateKeySpec.getModulus().bitLength());

        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        encryptCipher.update("testtesttestttesttesttestttesttesttestttesttesttestttesttesttestt".getBytes());
        byte[] cipherData = encryptCipher.doFinal();
        System.out.println(cipherData.length);

        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainData = decryptCipher.doFinal(cipherData);
        System.out.println(plainData.length);

        System.out.println(new String(plainData));

//        System.out.println(getPatch("http://www.google.com.hk/images/srpr/logo3w.png", new File("out.png"), "b5da62ee593ecc0c40f470d34cae68911914b3fb"));
    }
}
