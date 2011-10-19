package starter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util extends CommonUtil {

    protected Util() {
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean writeFile(File file, String content) {
        byte[] byteContent = null;
        try {
            byteContent = content.getBytes("UTF-8");
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return writeFile(file, byteContent);
    }

    public static boolean writeFile(File file, byte[] content) {
        boolean returnResult = true;

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(content);
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

            int byteRead = 0, cumulateByteRead = 0;
            byte[] buf = new byte[32768];
            while ((byteRead = fromFileStream.read(buf)) != -1) {
                toFileStream.write(buf, 0, byteRead);
                cumulateByteRead += byteRead;
            }

            if (cumulateByteRead != fromFile.length()) {
                throw new Exception();
            }
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
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
        boolean returnResult = false;

        try {
            File file = new File(directoryPath);
            if (!file.isDirectory()) {
                if (!file.mkdir()) {
                    throw new Exception("Failed to create folder: " + directoryPath);
                }
            }
            returnResult = true;
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }

        return returnResult;
    }

    public static boolean tryLock(File file) {
        boolean returnResult = false;

        FileInputStream fin = null;
        FileLock lock = null;
        try {
            fin = new FileInputStream(file);
            lock = fin.getChannel().tryLock();
            if (lock == null) {
                throw new Exception("Failed to acquire an exclusive lock on file: " + file.getAbsolutePath());
            }
            returnResult = true;
        } catch (Exception ex) {
            returnResult = false;
        } finally {
            try {
                if (lock != null) {
                    lock.release();
                }
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex) {
            }
        }

        return returnResult;
    }
}
