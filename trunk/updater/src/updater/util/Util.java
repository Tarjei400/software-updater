package updater.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util {

    protected Util() {
    }
    protected static final byte[] HEX_CHAR_TABLE = {
        (byte) '0', (byte) '1', (byte) '2', (byte) '3',
        (byte) '4', (byte) '5', (byte) '6', (byte) '7',
        (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
        (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public static String getHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }

        String result = null;
        try {
            result = new String(hex, "ASCII");
        } catch (UnsupportedEncodingException ex) {
            // should not happen
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public static String getSHA1(File file) {
        String returnResult = null;

        InputStream fin = null;
        try {
            fin = new FileInputStream(file);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[8096];
            while ((byteRead = fin.read(b)) > 0) {
                messageDigest.update(b, 0, byteRead);
                cumulateByteRead += byteRead;
            }

            if (cumulateByteRead != file.length()) {
                throw new Exception("The total number of bytes read does not match the file size: " + file.getAbsolutePath());
            }
            returnResult = getHexString(messageDigest.digest());
        } catch (Exception ex) {
            returnResult = null;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static long compareVersion(String version1, String version2) {
        String[] version1Parted = version1.split("\\.");
        String[] version2Parted = version2.split("\\.");

        long returnValue = 0;

        for (int i = 0, iEnd = Math.min(version1Parted.length, version2Parted.length); i < iEnd; i++) {
            returnValue += (Integer.parseInt(version1Parted[i]) - Integer.parseInt(version2Parted[i])) * Math.pow(10000, iEnd - i);
        }

        return returnValue;
    }

    public static byte[] readFile(File file) {
        byte[] content = new byte[(int) file.length()];

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);

            int byteRead = 0, cumulateByteRead = 0;
            while ((byteRead = fin.read(content, cumulateByteRead, content.length - cumulateByteRead)) > 0) {
                cumulateByteRead += byteRead;
            }

            if (cumulateByteRead != content.length) {
                throw new Exception("The total number of bytes read does not match the file size: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            content = null;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return content;
    }

    public static byte[] readResourceFile(String path) {
        byte[] returnResult = null;

        int byteRead = 0;
        byte[] b = new byte[256];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;

        try {
            in = Util.class.getResourceAsStream(path);
            if (in == null) {
                throw new Exception("Resources not found: " + path);
            }

            while ((byteRead = in.read(b)) > 0) {
                bout.write(b, 0, byteRead);
            }

            returnResult = bout.toByteArray();
        } catch (Exception ex) {
            returnResult = null;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    /**
     * Set UI look & feel to system look & feel.
     */
    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.INFO, "Failed to set system look and feel.", ex);
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
