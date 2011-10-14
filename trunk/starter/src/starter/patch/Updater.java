package starter.patch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import starter.gui.UpdaterWindow;
import starter.script.Client;
import starter.script.Client.Patch;
import starter.patch.PatchLogReader.UnfinishedPatch;
import starter.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Updater {

    private Updater() {
    }

    public static boolean update(File clientScriptFile, Client clientScript, File tempDir, String windowTitle, String windowIcon, String title, String icon) {
        boolean returnResult = true;

        List<Patch> patches = clientScript.getPatches();
        if (patches.isEmpty()) {
            return true;
        }

        // action log
        File logFile = new File(tempDir + "/action.log");

        // read log history
        PatchLogReader patchActionLogReader = null;
        try {
            patchActionLogReader = new PatchLogReader(logFile);
        } catch (Exception ex) {
        }
        if (patchActionLogReader != null) {
            try {
                boolean rewriteClientXML = false;

                List<String> finishedPatches = patchActionLogReader.getfinishedPatches();
                for (String finishedPatch : finishedPatches) {
                    Iterator<Patch> iterator = patches.iterator();
                    while (iterator.hasNext()) {
                        Patch _patch = iterator.next();
                        if (new File(_patch.getPath()).getAbsolutePath().equals(finishedPatch)) {
                            rewriteClientXML = true;
                            iterator.remove();
                        }
                    }
                }

                if (rewriteClientXML) {
                    clientScript.setPatches(patches);
                    if (!saveClientScript(clientScriptFile, clientScript, tempDir)) {
                        return false;
                    }
                }

                if (patches.isEmpty()) {
                    return true;
                }
            } catch (Exception ex) {
                return false;
            }
        }

        // frame
        final Thread currentThread = Thread.currentThread();
        final UpdaterWindow updaterGUI = new UpdaterWindow(windowTitle, windowIcon, title, icon);
        updaterGUI.addListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updaterGUI.setEnableCancel(false);
                currentThread.interrupt();
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Preparing ...");
        JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        // patch
        boolean canLaunchSoftware = true;
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        PatchLogWriter patchActionLogWriter = null;
        try {
            updaterGUI.setProgress(1);
            updaterGUI.setMessage("Check to see if there is another updater running ...");

            // acquire lock
            lockFileOut = new FileOutputStream(tempDir + "/update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new Exception();
            }

            updaterGUI.setProgress(2);
            updaterGUI.setMessage("Clear log ...");

            // truncate log file
            new FileOutputStream(logFile).close();
            // open log file
            patchActionLogWriter = new PatchLogWriter(logFile);

            updaterGUI.setProgress(3);
            updaterGUI.setMessage("Starting ...");

            // iterate patches and do patch
            final float stepSize = 97F / (float) patches.size();
            int count = -1;
            Iterator<Patch> iterator = patches.iterator();
            while (iterator.hasNext()) {
                count++;
                Patch _patch = iterator.next();

                // temporary storage folder for this patch
                if (!Util.makeDir(tempDir.getAbsolutePath() + "/" + count)) {
                    throw new Exception();
                }
                File tempDirForPatch = new File(tempDir.getAbsolutePath() + "/" + count);

                // initialize patcher
                final int _count = count;
                final Patcher _patcher = new Patcher(new PatcherListener() {

                    @Override
                    public void patchProgress(int percentage, String message) {
                        float base = 3F + (stepSize * (float) _count);
                        float addition = ((float) percentage / 100F) * stepSize;
                        updaterGUI.setProgress((int) (base + addition));
                        updaterGUI.setMessage(message);
                    }

                    @Override
                    public void patchFinished(boolean succeed) {
                    }

                    @Override
                    public void patchEnableCancel(boolean enable) {
                        updaterGUI.setEnableCancel(enable);
                    }
                }, patchActionLogWriter, new File(_patch.getPath()), tempDirForPatch);

                // get file index
                int startFromFileIndex = 0;
                UnfinishedPatch unfinishedPatch = patchActionLogReader.getUnfinishedPatch();
                if (unfinishedPatch != null) {
                    if (new File(_patch.getPath()).getAbsolutePath().equals(unfinishedPatch.getPatchPath())) {
                        startFromFileIndex = unfinishedPatch.getFileIndex();
                    }
                }

                // patch
                if (!_patcher.doPatch(startFromFileIndex)) {
                    throw new Exception();
                } else {
                    // patch succeed
                    // remove from patches list
                    iterator.remove();
                    // save the client scirpt
                    clientScript.setPatches(patches);
                    if (!saveClientScript(clientScriptFile, clientScript, tempDir)) {
                        throw new Exception();
                    }
                    Util.truncateFolder(tempDirForPatch);
                    tempDirForPatch.delete();
                }
            }
        } catch (Exception ex) {
            returnResult = false;

            JOptionPane.showMessageDialog(updaterFrame, "An error occurred when updating the software.");
            if (updaterGUI.isCancelEnabled()) {
                Object[] options = {"Launch", "Exit"};
                int result = JOptionPane.showOptionDialog(null, "Continue to launch the software?", "Continue action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (result == 0) {
                    returnResult = true;
                }
            } else {
                JOptionPane.showMessageDialog(updaterFrame, "You can restart the software to try to update files again.");
            }

            Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            updaterFrame.setVisible(false);
            updaterFrame.dispose();
            try {
                if (lock != null) {
                    lock.release();
                }
                if (lockFileOut != null) {
                    lockFileOut.close();
                }
                if (patchActionLogWriter != null) {
                    patchActionLogWriter.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return returnResult;
    }

    protected static boolean saveClientScript(File clientScriptFile, Client clientScript, File tempDir) {
        File clientTemp = new File(tempDir + "/client.xml");
        if (!Util.writeFile(clientTemp, clientScript.output()) || !clientScriptFile.delete() || !clientTemp.renameTo(clientScriptFile)) {
            return false;
        }
        return true;
    }
}
