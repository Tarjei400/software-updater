package starter.util;

import java.io.File;
import java.io.FileInputStream;
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
            if (file.exists()) {
                return file.isDirectory();
            } else {
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
