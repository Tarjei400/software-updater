package starter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            int byteRead;
            byte[] b = new byte[8096];
            while ((byteRead = fin.read(b)) != -1) {
                messageDigest.update(b, 0, byteRead);
            }

            returnResult = getHexString(messageDigest.digest());
        } catch (Exception ex) {
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
            while ((byteRead = fin.read(content, cumulateByteRead, content.length - cumulateByteRead)) != -1) {
                cumulateByteRead += byteRead;
            }

            if (cumulateByteRead != content.length) {
                return null;
            }
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            content = null;
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

    public static boolean writeFile(File file, String content) {
        boolean returnResult = true;

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(content.getBytes("UTF-8"));
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static boolean copyFile(File fromFile, File toFile) {
        boolean returnResult = true;

        FileInputStream fromFileStream = null;
        FileOutputStream toFileStream = null;
        try {
            fromFileStream = new FileInputStream(fromFile);
            toFileStream = new FileOutputStream(toFile);

            int byteRead = 0;
            byte[] buf = new byte[32768];
            while ((byteRead = fromFileStream.read(buf)) != -1) {
                toFileStream.write(buf, 0, byteRead);
            }
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            returnResult = false;
        } finally {
            try {
                if (fromFileStream != null) {
                    fromFileStream.close();
                }
                if (toFileStream != null) {
                    toFileStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static boolean truncateFolder(File directory) {
        try {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!truncateFolderRecursively(file)) {
                        return false;
                    }
                } else {
                    file.delete();
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    protected static boolean truncateFolderRecursively(File directory) {
        try {
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!truncateFolderRecursively(file)) {
                            return false;
                        }
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static boolean makeDir(String directoryPath) {
        try {
            File file = new File(directoryPath);
            if (!file.isDirectory()) {
                if (!file.mkdir()) {
                    return false;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * If the file is a directory, return the directory path; if the file is not a directory, return the directory path that contain the file.
     * @param file the file
     * @return the file parent path or null if error occurred
     */
    public static String getFileDirectory(File file) {
        String returnResult = null;
        try {
            if (file.isDirectory()) {
                return file.getAbsolutePath();
            } else {
                return getFileDirectory(file.getAbsolutePath());
            }
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnResult;
    }

    /**
     * Assume the filePath is a file path not a directory path.
     * @param filePath the file path
     * @return the file parent path
     */
    public static String getFileDirectory(String filePath) {
        int pos = filePath.replace((CharSequence) "\\", (CharSequence) "/").lastIndexOf('/');
        if (pos != -1) {
            return filePath.substring(0, pos);
        }
        return filePath;
    }
}
