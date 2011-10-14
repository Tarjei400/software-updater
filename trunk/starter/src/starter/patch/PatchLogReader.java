package starter.patch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The log is in human-readable format.
 * <p>
 * <h>Sample (separated by tab):</h><br />
 * [patchPath]	1.0.0 to 1.0.1	start<br />
 * [patchPath]	1.0.0 to 1.0.1	patch	0	start	[oldFilePath] -> [newFilePath]<br />
 * [patchPath]	1.0.0 to 1.0.1	patch	0	finish	[oldFilePath] -> [newFilePath]<br />
 * [patchPath]	1.0.0 to 1.0.1	finish
 * </p>
 * <p><b>Note that '[' and ']' didn't really exist.</b></p>
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogReader {

    private List<String> finishedPatches;
    private UnfinishedPatch unfinishedPatch;

    public PatchLogReader(File file) throws IOException {
        finishedPatches = new ArrayList<String>();

        int lastUnfinishedFileIndex = -1;
        String readLine, patchPathStarted = null;

        // not very strict check, assume IO is correct and in sequence
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        while ((readLine = in.readLine()) != null) {
            String[] params = readLine.split("\t");
            if (isStart(params)) {
                patchPathStarted = params[0];
                lastUnfinishedFileIndex = 0;
            } else if (isFinish(params)) {
                if (finishedPatches != null) {
                    finishedPatches.add(patchPathStarted);
                    patchPathStarted = null;
                }
            } else {
                // is patch
                if (isPatchStart(params)) {
                    lastUnfinishedFileIndex = Integer.parseInt(params[3]);
                } else if (isPatchFinish(params)) {
                    lastUnfinishedFileIndex = -1;
                }
            }
        }

        if (lastUnfinishedFileIndex != -1) {
            unfinishedPatch = new UnfinishedPatch(patchPathStarted, lastUnfinishedFileIndex);
        }

        in.close();
    }

    public List<String> getfinishedPatches() {
        return new ArrayList<String>(finishedPatches);
    }

    public UnfinishedPatch getUnfinishedPatch() {
        return unfinishedPatch;
    }

    protected boolean isStart(String[] params) {
        return params[2].equals("start");
    }

    protected boolean isFinish(String[] params) {
        return params[2].equals("finish");
    }

    protected boolean isPatchStart(String[] params) {
        return params[4].equals("start");
    }

    protected boolean isPatchFinish(String[] params) {
        return params[4].equals("finish");
    }

    public static class UnfinishedPatch {

        private String patchPath;
        private int fileIndex;

        protected UnfinishedPatch(String patchPath, int fileIndex) {
            this.patchPath = patchPath;
            this.fileIndex = fileIndex;
        }

        public String getPatchPath() {
            return patchPath;
        }

        /**
         * The recovery should start from this file index.
         */
        public int getFileIndex() {
            return fileIndex;
        }
    }
}
