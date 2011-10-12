package starter.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import starter.script.Client;
import starter.script.Client.Patch;
import starter.updater.PatchActionLogReader.UnfinishedPatch;
import starter.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Updater {

    private Updater() {
    }

    public static boolean update(File clientScriptFile, Client clientScript, File tempDir) {
        boolean returnResult = true;

        List<Patch> patches = clientScript.getPatches();
        if (patches.isEmpty()) {
            return true;
        }

        // action log
        File logFile = new File(tempDir + "/action.log");

        // read log history
        PatchActionLogReader patchActionLogReader = null;
        try {
            boolean rewriteClientXML = false;

            patchActionLogReader = new PatchActionLogReader(logFile);
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

        // patch
        FileOutputStream lockFileOut = null;
        FileLock lock = null;
        PatchActionLogWriter patchActionLogWriter = null;
        try {
            // acquire lock
            lockFileOut = new FileOutputStream("update.lck");
            lock = lockFileOut.getChannel().tryLock();
            if (lock == null) {
                throw new Exception();
            }

            // truncate log file
            new FileOutputStream(logFile).close();
            // open log file
            patchActionLogWriter = new PatchActionLogWriter(logFile);

            // iterate patches and do patch
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
                final Patcher _patcher = new Patcher(new PatcherListener() {

                    @Override
                    public void patchProgress(int percentage, String message) {
                        // update progress
                    }

                    @Override
                    public void patchFinished(boolean succeed) {
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
            Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
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
