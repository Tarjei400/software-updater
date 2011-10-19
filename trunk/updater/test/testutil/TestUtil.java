package testutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class TestUtil {

    protected TestUtil() {
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
            Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fromFileStream != null) {
                    fromFileStream.close();
                }
                if (toFileStream != null) {
                    toFileStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean writeFile(File file, byte[] content) {
        boolean returnResult = true;

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            fout.write(content);
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static class ObjectReference<T> {

        protected T obj;

        public ObjectReference(T obj) {
            this.obj = obj;
        }

        public T getObj() {
            return obj;
        }

        public void setObj(T obj) {
            this.obj = obj;
        }
    }
}
