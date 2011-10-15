package starter.patch;

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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.XZInputStream;
import starter.script.InvalidFormatException;
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

    protected PatcherListener listener;
    protected PatchLogWriter log;
    protected File patchFile;
    protected File tempDir;
    private byte[] buf;
    protected float progress;

    public Patcher(PatcherListener listener, PatchLogWriter log, File patchFile, File tempDir) throws IOException {
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

    protected void checkHeader(InputStream in) throws IOException {
        if (in.read(buf, 0, 5) != 5) {
            throw new IOException("Reach the end of stream.");
        }
        if (buf[0] != 'P' || buf[1] != 'A' || buf[2] != 'T' || buf[3] != 'C' || buf[4] != 'H') {
            throw new IOException("Invalid patch header.");
        }
    }

    protected InputStream getCompressionMethod(InputStream in) throws IOException {
        if (in.read(buf, 0, 3) != 3) {
            throw new IOException("Reach the end of stream.");
        }
        int compressionMode = ((buf[0] & 0xff) << 16) | ((buf[1] & 0xff) << 8) | (buf[2] & 0xff);
        switch (compressionMode) {
            case 0: //gzip
                return new GZIPInputStream(in);
            case 1: // XZ/LZMA2
                return new XZInputStream(in);
            default:
                throw new IOException("Compression method not supported/not exist");
        }
    }

    protected Patch getXML(InputStream in) throws IOException, InvalidFormatException {
        if (in.read(buf, 0, 2) != 2) {
            throw new IOException("Reach the end of stream.");
        }
        int xmlLength = ((buf[1] & 0xff) << 8) | (buf[2] & 0xff);
        byte[] xmlData = new byte[xmlLength];
        if (in.read(xmlData) != xmlLength) {
            throw new IOException("Reach the end of stream.");
        }
        return Patch.read(xmlData);
    }

    protected void doOperation(Operation operation, InputStream patchIn, File tempNewFile) throws Exception {
        if (operation.getType().equals("remove")) {
            // doOperation will not change/remove all existing 'old files'
            return;
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
                throw new Exception("Checksum or length does not match (old file): " + operation.getOldFilePath());
            }
        }

        // check if it is patched and waiting for move already
        if (tempNewFile.exists() && Util.getSHA1(tempNewFile).equals(operation.getNewFileChecksum()) && tempNewFile.length() == operation.getNewFileLength()) {
            return;
        }

        InterruptibleOutputStream tempNewFileOut = null;
        InterruptibleInputStream interruptiblePatchIn = null;
        RandomAccessFile randomAccessOldFile = null;
        try {
            tempNewFileOut = new InterruptibleOutputStream(new BufferedOutputStream(new FileOutputStream(tempNewFile)));
            interruptiblePatchIn = new InterruptibleInputStream(patchIn, operation.getPatchLength());

            if (operation.getType().equals("patch")) {
                GDiffPatcher diffPatcher = new GDiffPatcher();
                randomAccessOldFile = new RandomAccessFile(oldFile, "r");
                SeekableFile seekableRandomAccessOldFile = new SeekableFile(randomAccessOldFile);

                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
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
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);
                seekableRandomAccessOldFile.addInterruptedTask(interruptedTask);

                diffPatcher.patch(seekableRandomAccessOldFile, interruptiblePatchIn, tempNewFileOut);
            } else {
                final OutputStream _tempNewFileOut = tempNewFileOut;
                final InterruptibleInputStream _interruptiblePatchIn = interruptiblePatchIn;
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
                tempNewFileOut.addInterruptedTask(interruptedTask);
                interruptiblePatchIn.addInterruptedTask(interruptedTask);

                // replace or new
                int byteRead, remaining = operation.getPatchLength();
                while (true) {
                    if (remaining <= 0) {
                        break;
                    }

                    int lengthToRead = buf.length > remaining ? remaining : buf.length;
                    byteRead = interruptiblePatchIn.read(buf, 0, lengthToRead);
                    if (byteRead <= 0) {
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
            throw new Exception("Checksum or length does not match (new file): " + tempNewFile.getAbsolutePath());
        }
    }

    protected void tryAcquireExclusiveLocks(List<Operation> operations, int startFromFileIndex) throws IOException {
        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            if (_operation.getOldFilePath() != null) {
                if (!Util.tryLock(new File(_operation.getOldFilePath()))) {
                    throw new IOException("Failed to acquire lock on (old file): " + _operation.getOldFilePath());
                }
            }

            if (_operation.getNewFilePath() != null) {
                if (!Util.tryLock(new File(_operation.getNewFilePath()))) {
                    throw new IOException("Failed to acquire lock on (new file): " + _operation.getNewFilePath());
                }
            }
        }
    }

    protected void doReplacement(List<Operation> operations, int startFromFileIndex, String patchFileAbsolutePath, Patch patch) throws IOException {
        float progressStep = 4.0F / (float) operations.size();
        progress += startFromFileIndex * progressStep;

        for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
            Operation _operation = operations.get(i);

            log.logPatch(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo(), i, PatchLogWriter.Action.START, _operation.getOldFilePath(), _operation.getNewFilePath());

            if (_operation.getType().equals("remove")) {
                listener.patchProgress((int) progress, "Removing " + _operation.getOldFilePath() + " ...");
                new File(_operation.getOldFilePath()).delete();
            } else if (_operation.getType().equals("new")) {
                listener.patchProgress((int) progress, "Copying new file to " + _operation.getNewFilePath() + " ...");
                new File(_operation.getNewFilePath()).delete();
                new File(tempDir + File.separator + i).renameTo(new File(_operation.getNewFilePath()));
            } else {
                // patch or replace
                listener.patchProgress((int) progress, "Copying from " + _operation.getOldFilePath() + " to " + _operation.getNewFilePath() + " ...");
                new File(_operation.getNewFilePath()).delete();
                new File(tempDir + File.separator + i).renameTo(new File(_operation.getNewFilePath()));
            }

            log.logPatch(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo(), i, PatchLogWriter.Action.FINISH, _operation.getOldFilePath(), _operation.getNewFilePath());
            progress += progressStep;
        }
    }

    public boolean doPatch(int startFromFileIndex) {
        boolean returnResult = true;

        InputStream patchIn = null;
        try {
            String patchFileAbsolutePath = patchFile.getAbsolutePath();
            patchIn = new BufferedInputStream(new FileInputStream(patchFile));

            progress = 0;
            listener.patchProgress((int) progress, "Preparing new patch ...");
            listener.patchEnableCancel(false);
            // header
            checkHeader(patchIn); // 'P' 'A' 'T' 'C' 'H'
            InputStream decompressedPatchIn = getCompressionMethod(patchIn); // compression method
            Patch patch = getXML(decompressedPatchIn); // xml

            List<Operation> operations = patch.getOperations();

            // start log
            log.logStart(patchFileAbsolutePath, patch.getVersionFrom(), patch.getVersionTo());

            progress = 5;
            listener.patchProgress((int) progress, "Updating ...");
            listener.patchEnableCancel(true);
            // start patch - patch files and store to temporary directory first
            float progressStep = 90.0F / (float) operations.size();
            progress += startFromFileIndex * progressStep;
            for (int i = startFromFileIndex, iEnd = operations.size(); i < iEnd; i++) {
                Operation _operation = operations.get(i);
                doOperation(_operation, decompressedPatchIn, new File(tempDir + File.separator + i));
                progress += progressStep;
            }

            progress = 95;
            listener.patchProgress((int) progress, "Checking the accessibility of all files ...");
            // try acquire locks on all files
            tryAcquireExclusiveLocks(operations, startFromFileIndex);

            progress = 96;
            listener.patchProgress((int) progress, "Replacing old files with new files ...");
            listener.patchEnableCancel(false);
            // all files has patched to temporary directory, replace old files with the new one
            doReplacement(operations, startFromFileIndex, patchFileAbsolutePath, patch);

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
}
