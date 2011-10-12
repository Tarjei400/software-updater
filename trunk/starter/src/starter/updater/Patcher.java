package starter.updater;

import com.nothome.delta.GDiffPatcher;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;
import starter.script.Patch;
import starter.script.Patch.Operation;
import starter.util.InterruptibleInputStream;
import starter.util.InterruptibleOutputStream;
import starter.util.SeekableFile;
import starter.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Patcher {

    private PatcherListener listener;
    private PatchActionLogWriter log;
    private File patchFile;
    private File tempDir;
    private byte[] buf;
    private float progress;

    public Patcher(PatcherListener listener, PatchActionLogWriter log, File patchFile, File tempDir) throws IOException {
        this.listener = listener;
        this.log = log;

        this.patchFile = patchFile;
        if (!patchFile.exists() || patchFile.isDirectory()) {
            throw new IOException("patch file not exist or not a file");
        }

        this.tempDir = tempDir;
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException("temporary directory not exist or not a directory");
        }

        buf = new byte[32768];
        progress = 0;
    }

    public boolean doPatch(int startFromFileIndex) {
        boolean returnResult = true;

        InputStream patchIn = null;
        InputStream decompressedPatchIn = null;
        Patch patch = null;
        try {
            progress = 0;
            listener.patchProgress((int) progress, "Preparing ...");

            patchIn = new BufferedInputStream(new FileInputStream(patchFile));

            // 'P' 'A' 'T' 'C' 'H'
            if (patchIn.read(buf, 0, 5) != 5) {
                throw new Exception();
            }

            // compression method
            if (patchIn.read(buf, 0, 1) != 1) {
                throw new Exception();
            }
            int compressionMode = (buf[0] & 0xff);
            switch (compressionMode) {
                case 0: //gzip
                    decompressedPatchIn = new GZIPInputStream(patchIn);
                    break;
                case 1: // XZ/LZMA2
                    decompressedPatchIn = new XZInputStream(patchIn);
                    break;
                default:
                    throw new Exception();
            }

            // xml
            if (decompressedPatchIn.read(buf, 0, 2) != 2) {
                throw new Exception();
            }
            int xmlLength = ((buf[1] & 0xff) << 8) | (buf[2] & 0xff);
            byte[] xmlData = new byte[xmlLength];
            if (decompressedPatchIn.read(xmlData) != xmlLength) {
                throw new Exception();
            }
            patch = Patch.read(xmlData);
            if (patch == null) {
                throw new Exception();
            }


            String patchFileAbsolutePath = patchFile.getAbsolutePath();
            log.logStart(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo());


            progress = 5;
            listener.patchProgress((int) progress, "Updating ...");

            // start patch - patch files and store to temporary directory first
            List<Operation> operations = patch.getOperations();

            float progressStep = 90.0F / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);
                if (!doOperation(_operation, decompressedPatchIn, new File(tempDir + "/" + i))) {
                    throw new Exception();
                }
                progress += progressStep;
            }


            progress = 95;
            listener.patchProgress((int) progress, "Checking the accessibility of all files ...");

            // try acquire locks on all files
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);

                if (_operation.getOldFilePath() != null) {
                    if (!tryLock(new File(_operation.getOldFilePath()))) {
                        throw new Exception();
                    }
                }

                if (_operation.getNewFilePath() != null) {
                    if (!tryLock(new File(_operation.getNewFilePath()))) {
                        throw new Exception();
                    }
                }
            }


            progress = 96;
            listener.patchProgress((int) progress, "Replacing old files with new files ...");

            // all files patched to temporary directory, replace old files with the new one
            progressStep = 4.0F / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);

                log.logPatch(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo(), i, PatchActionLogWriter.PatchAction.START, _operation.getOldFilePath(), _operation.getNewFilePath());

                if (_operation.getType().equals("remove")) {
                    listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                    new File(_operation.getOldFilePath()).delete();
                } else if (_operation.getType().equals("new")) {
                    listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                    new File(_operation.getNewFilePath()).delete();
                    new File(tempDir + "/" + i).renameTo(new File(_operation.getNewFilePath()));
                } else {
                    // patch or replace
                    listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                    new File(_operation.getNewFilePath()).delete();
                    new File(tempDir + "/" + i).renameTo(new File(_operation.getNewFilePath()));
                }


                log.logPatch(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo(), i, PatchActionLogWriter.PatchAction.FINISH, _operation.getOldFilePath(), _operation.getNewFilePath());
                progress += progressStep;
            }

            log.logEnd(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo());
        } catch (Exception ex) {
            returnResult = false;
            Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (patchIn != null) {
                    patchIn.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        listener.patchFinished(returnResult);

        return returnResult;
    }

    protected boolean doOperation(Operation operation, InputStream patchIn, File tempNewFile) {
        if (operation.getType().equals("remove")) {
            return true;
        } else if (operation.getType().equals("new")) {
            listener.patchProgress((int) progress, "Creating new file " + operation.getNewFilePath() + " ...");
        } else {
            // replace or patch
            listener.patchProgress((int) progress, "Patching " + operation.getOldFilePath() + " ...");
        }

        File oldFile = null;
        if (operation.getOldFilePath() != null) {
            // check old file checksum and length
            oldFile = new File(operation.getOldFilePath());
            if (!Util.getSHA1(oldFile).equals(operation.getOldFileChecksum()) || oldFile.length() != operation.getOldFileLength()) {
                return false;
            }
        }

        // check if it is patched and waiting for move already
        if (!tempNewFile.exists() || !Util.getSHA1(tempNewFile).equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
            OutputStream tempNewFileOut = null;
            InterruptibleInputStream interruptiblePatchIn = null;
            RandomAccessFile randomAccessOldFile = null;
            try {
                tempNewFileOut = new BufferedOutputStream(new InterruptibleOutputStream(new FileOutputStream(tempNewFile)));
                interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;

                if (operation.getType().equals("patch")) {
                    GDiffPatcher diffPatcher = new GDiffPatcher();
                    randomAccessOldFile = new RandomAccessFile(oldFile, "r");
                    SeekableFile seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

                    final RandomAccessFile _randomAccessOldFile = randomAccessOldFile;

                    Runnable interruptedTask = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                _tempNewFileOut.close();
                                _interruptiblePatchIn.close();
                                _randomAccessOldFile.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    };
                    ((InterruptibleOutputStream) tempNewFileOut).addInterruptedTask(interruptedTask);
                    ((InterruptibleInputStream) interruptiblePatchIn).addInterruptedTask(interruptedTask);
                    seekableRandomAccessOldFile.addInterruptedTask(interruptedTask);

                    diffPatcher.patch(seekableRandomAccessOldFile, interruptiblePatchIn, tempNewFileOut);
                } else {
                    Runnable interruptedTask = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                _tempNewFileOut.close();
                                _interruptiblePatchIn.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    };
                    ((InterruptibleOutputStream) tempNewFileOut).addInterruptedTask(interruptedTask);
                    ((InterruptibleInputStream) interruptiblePatchIn).addInterruptedTask(interruptedTask);

                    // replace or new
                    int byteRead, remaining = operation.getPatchLength();
                    while (true) {
                        if (remaining <= 0) {
                            break;
                        }

                        int lengthToRead = buf.length > remaining ? remaining : buf.length;
                        byteRead = interruptiblePatchIn.read(buf, 0, lengthToRead);
                        if (byteRead == -1) {
                            break;
                        }
                        tempNewFileOut.write(buf, 0, byteRead);
                        remaining -= byteRead;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (randomAccessOldFile != null) {
                        randomAccessOldFile.close();
                    }
                    if (interruptiblePatchIn != null) {
                        patchIn.skip(interruptiblePatchIn.remaining());
                    }
                    if (tempNewFileOut != null) {
                        tempNewFileOut.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Patcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // check new file checksum and length
            if (!Util.getSHA1(tempNewFile).equals(operation.getNewFileChecksum()) || tempNewFile.length() != operation.getNewFileLength()) {
                return false;
            }
        }

        return true;
    }

    public static boolean tryLock(File file) {
        boolean returnResult = false;

        FileInputStream fin = null;
        FileLock lock = null;
        try {
            fin = new FileInputStream(file);
            lock = fin.getChannel().tryLock();
            if (lock == null) {
                throw new IOException();
            }
            lock.release();
            fin.close();
            returnResult = true;
        } catch (IOException ex) {
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
