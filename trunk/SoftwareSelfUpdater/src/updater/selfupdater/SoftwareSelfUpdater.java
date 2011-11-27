package updater.selfupdater;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * This is a self updater used by software launcher.
 * When the launcher encounter any files that it can't do a replacement due to file locking, then this will be used.
 * The launcher will launch this and exit (to release the file lock on the itself), then this self updater will do the replacement 
 * according to a list the launcher give it.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareSelfUpdater {

    /**
     * Indicate whether it is in test mode or not.
     */
    protected final static boolean test;

    static {
        String testMode = System.getProperty("SoftwareSelfUpdaterTestMode");
        test = testMode == null || !testMode.equals("true") ? false : true;
    }
    /**
     * The maximum execution time allowed (in ms).
     * When the self updater failed, it will not give up immediately, because the launcher may not exit so quickly after launching this.
     * In this case, the program will keep trying until this maximum execution time is reached.
     * This is configurable by replacing/editing '/config.xml' inside the jar, for more information, see the code below.
     */
    protected static long maxExecutionTime = 15000;

    protected SoftwareSelfUpdater() {
    }

    /**
     * The format of the replacement file:
     * <p>
     * One row for destination file path (0), one row for new file path (1), one row for the path to place/move the destination file (2).<br />
     * Flow: 0->2, 1->0
     * <p>
     * Example:
     * <p>
     * C:\software\dest.txt<br />
     * C:\tmp\1.tmp<br />
     * C:\tmp\1.old<br />
     * C:\software\dest.jar<br />
     * C:\tmp\2.tmp<br />
     * C:\tmp\2.old<br />
     * (a new line character here)
     * </p>
     * @param args 0: lock file path, 1: replacement file path, start from 2: command and arguments to launch the software.
     */
    public static void main(String[] args) {
        // set look & feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // leave it
        }

        // check if the length of args meet the minimum requirement
        if (args.length < 3) {
            StringBuilder sb = new StringBuilder();
            sb.append("argument length should >= 3, current length: ");
            sb.append(args.length);
            sb.append(", args: ");
            for (String arg : args) {
                sb.append("\"");
                sb.append(arg.replace("\"", "\\\""));
                sb.append("\"");
                sb.append(' ');
            }
            JOptionPane.showMessageDialog(null, sb.toString()); // this error message normally would not be shown to user
            return;
        }

        // read the maximum execution time from /config.xml inside the jar if there is any
        updateMaxExecutionTime();

        // the time to use to determine whether reached maxExecutionTime or not
        long startTime = System.currentTimeMillis();

        // acquire lock on the lock file to make sure there is no other launcher/downloader/self-updater running
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        while (true) {
            try {
                lockFileOut = new FileOutputStream(args[0], true);
                lock = lockFileOut.getChannel().tryLock();
                if (lock != null) {
                    // acquire lock succeed
                    break;
                } else {
                    // acquire lock failed, retry
                    throw new Exception();
                }
            } catch (Exception ex) {
                // check if maxExecutionTime reached
                if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                    JOptionPane.showMessageDialog(null, "There is another update process running.");
                    return;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex1) {
                    // currently don't allow cancel so this should not be interrupted
                }

                releaseQuietly(lock);
                closeQuietly(lockFileOut);
            }
        }


        // read the replacement file and delete & move file
        File replacementFile = new File(args[1]);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(replacementFile)));

            while (true) {
                String destinationFilePath = reader.readLine();
                String newFilePath = reader.readLine();
                String destinationMoveToPath = reader.readLine();

                if (destinationFilePath != null && newFilePath != null && destinationMoveToPath != null) {
                    File destinationFile = new File(destinationFilePath);
                    File newFile = new File(newFilePath);
                    File destinationMoveToFile = new File(destinationMoveToPath);

                    destinationFile.renameTo(destinationMoveToFile);
                    while (!newFile.renameTo(destinationFile)) {
                        if (System.currentTimeMillis() - startTime > maxExecutionTime) {
                            JOptionPane.showMessageDialog(null, String.format("Failed to move file from %1$s to %2$s", newFilePath, destinationFilePath));
                            Object[] options = {"Recover", "Exit & Restart manually"};
                            int result = JOptionPane.showOptionDialog(null, "Recover back to original version or exit & restart manually?", "Update Failed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                            if (result == 0) {
                                // launch the launcher to do recoverery
                                break;
                            } else {
                                return;
                            }
                        } else {
                            // retry the move
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                // currently don't allow cancel so this should not be interrupted
                            }
                        }
                        // since we didn't check if the movement on destinationFile succeed or not, so we move it here again (in case the previous movement was failed due to the program not terminated yet)
                        if (!destinationMoveToFile.exists()) {
                            destinationFile.renameTo(destinationMoveToFile);
                        }
                    }
                } else {
                    // if not three lines was read, finish the process
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error occurred when reading replacement file.");
            return;
        } finally {
            closeQuietly(reader);
            releaseQuietly(lock);
            closeQuietly(lockFileOut);
        }
        replacementFile.delete();


        // launch the software
        String[] launchCommands = new String[args.length - 2];
        System.arraycopy(args, 2, launchCommands, 0, args.length - 2);
        try {
            new ProcessBuilder(Arrays.asList(launchCommands)).start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to launch the software, you can try to launch it again after a while.");
            return;
        }
        if (!test) {
            System.exit(0);
        }
    }

    /**
     * Get and update the maximum execution time from /config.xml inside the jar if there is any.
     */
    protected static void updateMaxExecutionTime() {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(readResourceFile("/config.xml"));
            Properties config = new Properties();
            config.loadFromXML(in);
            maxExecutionTime = Integer.parseInt(config.getProperty("max_execution_time"));
        } catch (Exception ex) {
            // ignore
        }
    }

    /**
     * Release the file lock quietly without throwing any exception
     * @param fileLock the file lock to release
     */
    public static void releaseQuietly(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Close the closeable quietly without throwing any exception
     * @param closeable the object with closable
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Read the resource file from the jar.
     * @param path the resource path
     * @return the content of the resource file in byte array
     * @throws IOException error occurred when reading the content from the file
     */
    public static byte[] readResourceFile(String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("argument 'path' cannot be null");
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;
        try {
            in = SoftwareSelfUpdater.class.getResourceAsStream(path);
            if (in == null) {
                throw new IOException("Resources not found: " + path);
            }

            int byteRead = 0;
            byte[] b = new byte[8096];

            while ((byteRead = in.read(b)) != -1) {
                bout.write(b, 0, byteRead);
            }
        } finally {
            closeQuietly(in);
        }

        return bout.toByteArray();
    }
}