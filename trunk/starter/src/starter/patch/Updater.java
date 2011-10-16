package starter.patch;

import java.awt.Image;
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
import starter.script.Client.Update;
import starter.patch.PatchLogReader.UnfinishedPatch;
import starter.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Updater {

    protected Updater() {
    }

    protected static int getPatchStartIndex(Update patch, PatchLogReader patchLogReader) {
        if (patchLogReader == null) {
            return 0;
        }
        UnfinishedPatch unfinishedPatch = patchLogReader.getUnfinishedPatch();
        if (unfinishedPatch != null) {
            if (new File(patch.getPath()).getAbsolutePath().equals(unfinishedPatch.getPatchPath())) {
                return unfinishedPatch.getFileIndex();
            }
        }
        return 0;
    }

    public static UpdateResult update(File clientScriptFile, Client clientScript, File tempDir, String windowTitle, Image windowIcon, String title, Image icon) {
        UpdateResult returnResult = new UpdateResult(true, false);

        List<Update> updates = clientScript.getUpdates();
        if (updates.isEmpty()) {
            return new UpdateResult(true, true);
        }

        // action log
        File logFile = new File(tempDir + "/action.log");

        // GUI
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
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        PatchLogWriter patchActionLogWriter = null;
        try {
            PatchLogReader patchLogReader = null;
            //<editor-fold defaultstate="collapsed" desc="read log">
            try {
                patchLogReader = new PatchLogReader(logFile);
            } catch (Exception ex) {
                // ignore
            }
            if (patchLogReader != null) {
                boolean rewriteClientXML = false;

                List<String> finishedPatches = patchLogReader.getfinishedPatches();
                for (String finishedPatch : finishedPatches) {
                    Iterator<Update> iterator = updates.iterator();
                    while (iterator.hasNext()) {
                        Update _patch = iterator.next();
                        if (new File(_patch.getPath()).getAbsolutePath().equals(finishedPatch)) {
                            rewriteClientXML = true;
                            iterator.remove();
                        }
                    }
                }

                if (rewriteClientXML) {
                    clientScript.setUpdates(updates);
                    saveClientScript(clientScriptFile, clientScript);
                }

                if (updates.isEmpty()) {
                    return new UpdateResult(true, true);
                }
            }
            //</editor-fold>

            updaterGUI.setProgress(1);
            updaterGUI.setMessage("Check to see if there is another updater running ...");
            // acquire lock
            lockFileOut = new FileOutputStream(tempDir + "/update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new Exception("There is another updater running.");
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
            final float stepSize = 97F / (float) updates.size();
            int count = -1;
            Iterator<Update> iterator = updates.iterator();
            while (iterator.hasNext()) {
                count++;
                Update _update = iterator.next();

                // check if the update if not suitable
                if (Util.compareVersion(clientScript.getVersion(), _update.getVersionFrom()) > 0) {
                    // normally should not reach here
                    iterator.remove();
                    // save the client scirpt
                    clientScript.setUpdates(updates);
                    saveClientScript(clientScriptFile, clientScript);
                    continue;
                }
                if (!_update.getVersionFrom().equals(clientScript.getVersion())) {
                    // the 'version from' of this update dun match with current version
                    continue;
                }

                // temporary storage folder for this patch
                String tempDirPath = tempDir.getAbsolutePath() + "/" + _update.getId();
                if (!Util.makeDir(tempDirPath)) {
                    throw new Exception("Failed to create folder for patches.");
                }
                File tempDirForPatch = new File(tempDirPath);

                // initialize patcher
                final int _count = count;
                Patcher _patcher = new Patcher(new PatcherListener() {

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
                }, patchActionLogWriter, new File(_update.getPath()), tempDirForPatch);

                // patch
                if (!_patcher.doPatch(getPatchStartIndex(_update, patchLogReader))) {
                    throw new Exception("Do patch failed.");
                } else { // patch succeed
                    // remove from patches list
                    iterator.remove();

                    // save the client scirpt
                    clientScript.setVersion(_update.getVersionTo());
                    clientScript.setUpdates(updates);
                    saveClientScript(clientScriptFile, clientScript);

                    Util.truncateFolder(tempDirForPatch);
                    tempDirForPatch.delete();
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(updaterFrame, "An error occurred when updating the software.");

            boolean launchSoftware = false;

            if (updaterGUI.isCancelEnabled()) {
                Object[] options = {"Launch", "Exit"};
                int result = JOptionPane.showOptionDialog(updaterFrame, "Continue to launch the software?", "Continue action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (result == 0) {
                    launchSoftware = true;
                } else {
                    JOptionPane.showMessageDialog(updaterFrame, "You can restart the software to try to update files again.");
                }
            }

            returnResult = new UpdateResult(false, launchSoftware);
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
            if (returnResult.isUpdateSucceed()) {
                // remove log
                logFile.delete();
            }
        }

        return returnResult;
    }

    protected static void saveClientScript(File clientScriptFile, Client clientScript) throws IOException {
        File clientScriptTemp = new File(Util.getFileDirectory(clientScriptFile) + File.separator + clientScriptFile.getName() + ".new");
        if (!Util.writeFile(clientScriptTemp, clientScript.output()) || !clientScriptFile.delete() || !clientScriptTemp.renameTo(clientScriptFile)) {
            throw new IOException("Failed to save to script.");
        }
    }

    public static class UpdateResult {

        protected boolean updateSucceed;
        protected boolean launchSoftware;

        public UpdateResult(boolean updateSucceed, boolean launchSoftware) {
            this.updateSucceed = updateSucceed;
            this.launchSoftware = launchSoftware;
        }

        public boolean isUpdateSucceed() {
            return updateSucceed;
        }

        public boolean isLaunchSoftware() {
            return launchSoftware;
        }
    }
}
