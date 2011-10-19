package starter.util;

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
import starter.script.Client;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class CommonUtil {

    protected CommonUtil() {
    }
    protected static final byte[] HEX_CHAR_TABLE = {
        (byte) '0', (byte) '1', (byte) '2', (byte) '3',
        (byte) '4', (byte) '5', (byte) '6', (byte) '7',
        (byte) '8', (byte) '9', (byte) 'a', (byte) 'b',
        (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    public static String byteArrayToHexString(byte[] raw) {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;

        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index] = HEX_CHAR_TABLE[v >>> 4];
            index++;
            hex[index] = HEX_CHAR_TABLE[v & 0xF];
            index++;
        }

        String result = null;
        try {
            result = new String(hex, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            // should not happen
            Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public static String getSHA256(File file) {
        String returnResult = null;

        InputStream fin = null;
        try {
            fin = new FileInputStream(file);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            int byteRead, cumulateByteRead = 0;
            byte[] b = new byte[8096];
            while ((byteRead = fin.read(b)) > 0) {
                messageDigest.update(b, 0, byteRead);
                cumulateByteRead += byteRead;
            }

            if (cumulateByteRead != file.length()) {
                throw new Exception("The total number of bytes read does not match the file size: " + file.getAbsolutePath());
            }
            returnResult = byteArrayToHexString(messageDigest.digest());
        } catch (Exception ex) {
            returnResult = null;
            Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(CommonUtil.class.getName()).log(Level.INFO, "Failed to set system look and feel.", ex);
        }
    }

    /**
     * If the file is a directory, return the directory path; if the file is not a directory, return the directory path that contain the file.
     * @param file the file
     * @return the file parent path or null if error occurred
     */
    public static String getFileDirectory(File file) {
        String returnResult = null;
        try {
            returnResult = file.isDirectory() ? file.getAbsolutePath() : getFileDirectory(file.getAbsolutePath());
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
        int pos = filePath.replace((CharSequence) File.separator, (CharSequence) "/").lastIndexOf('/');
        return pos != -1 ? filePath.substring(0, pos) : filePath;
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
                throw new Exception("The total number of bytes read does not match the file size: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            content = null;
            Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
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
            in = CommonUtil.class.getResourceAsStream(path);
            if (in == null) {
                throw new Exception("Resources not found: " + path);
            }

            while ((byteRead = in.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }

            returnResult = bout.toByteArray();
        } catch (Exception ex) {
            returnResult = null;
            Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    public static long compareVersion(String version1, String version2) {
        String[] version1Parted = version1.split("\\.");
        String[] version2Parted = version2.split("\\.");

        long returnValue = 0;

        for (int i = 0, iEnd = Math.min(version1Parted.length, version2Parted.length); i < iEnd; i++) {
            try {
                returnValue += (Integer.parseInt(version1Parted[i]) - Integer.parseInt(version2Parted[i])) * Math.pow(10000, iEnd - i);
            } catch (Exception ex) {
            }
        }

        return returnValue;
    }

    public static GetClientScriptResult getClientScript(String inputPath) {
        Client clientScript = null;
        String clientScriptPath = null;

        try {
            if (inputPath != null) {
                File inputFile = new File(inputPath);
                if (inputFile.exists()) {
                    byte[] inputFileData = readFile(inputFile);
                    if (inputFileData != null && (clientScript = Client.read(inputFileData)) != null) {
                        return new GetClientScriptResult(clientScript, inputFile.getAbsolutePath());
                    }
                } else {
                    throw new Exception("Client script file not found: " + inputPath);
                }
            }


            byte[] configPathByte = readResourceFile("/config");
            if (configPathByte == null || configPathByte.length == 0) {
                throw new Exception("'/config' not found in the jar.");
            }

            String configPath = new String(configPathByte, "US-ASCII");
            configPath.replace("{home}", System.getProperty("user.home") + File.separator).replace("{tmp}", System.getProperty("java.io.tmpdir") + File.separator);

            File configFile = new File(configPath);
            File newConfigFile = new File(getFileDirectory(configFile) + File.separator + configFile.getName() + ".new");

            if (configFile.exists()) {
                byte[] configFileData = readFile(configFile);
                if (configFileData != null) {
                    clientScript = Client.read(configFileData);
                }

                clientScriptPath = configFile.getAbsolutePath();
                if (newConfigFile.exists()) {
                    if (clientScript != null) {
                        Client newConfigClientScript = null;

                        byte[] _newConfigFileData = readFile(newConfigFile);
                        if (_newConfigFileData != null) {
                            newConfigClientScript = Client.read(_newConfigFileData);
                        }

                        if (newConfigClientScript != null) {
                            if (compareVersion(newConfigClientScript.getVersion(), clientScript.getVersion()) > 0) {
                                configFile.delete();
                                newConfigFile.renameTo(configFile);

                                clientScript = newConfigClientScript;
                                clientScriptPath = newConfigFile.getAbsolutePath();
                            }
                        }
                    } else {
                        configFile.delete();
                        newConfigFile.renameTo(configFile);

                        byte[] _newConfigFileData = readFile(configFile);
                        if (_newConfigFileData != null) {
                            clientScript = Client.read(_newConfigFileData);
                        }
                    }
                }
            } else {
                if (newConfigFile.exists()) {
                    newConfigFile.renameTo(configFile);

                    byte[] _newConfigFileData = readFile(configFile);
                    if (_newConfigFileData != null) {
                        clientScript = Client.read(_newConfigFileData);
                    }
                }
            }

            if (clientScript == null) {
                throw new Exception("Config file not found");
            }
        } catch (Exception ex) {
            Logger.getLogger(CommonUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new GetClientScriptResult(clientScript, clientScriptPath);
    }

    public static class GetClientScriptResult {

        protected Client clientScript;
        protected String clientScriptPath;

        protected GetClientScriptResult(Client clientScript, String clientScriptPath) {
            this.clientScript = clientScript;
            this.clientScriptPath = clientScriptPath;
        }

        public Client getClientScript() {
            return clientScript;
        }

        public String getClientScriptPath() {
            return clientScriptPath;
        }
    }
}
