package starter.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sample (separated by tab):<br />
 * [patchPath]	1.0.0 to 1.0.1	start<br />
 * [patchPath]	1.0.0 to 1.0.1	patch	0	start	[oldFilePath] -> [newFilePath]<br />
 * [patchPath]	1.0.0 to 1.0.1	patch	0	finish	[oldFilePath] -> [newFilePath]<br />
 * [patchPath]	1.0.0 to 1.0.1	finish
 * <p><b>Note that '[' and ']' didn't really exist.</b></p>
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class PatchLogWriter {

    public static enum PatchAction {

        START, FINISH
    }
    private FileOutputStream out;

    public PatchLogWriter(File file) throws IOException {
        out = new FileOutputStream(file, true);
    }

    public void close() throws IOException {
        out.close();
    }

    public void logStart(String patchPath, String fromVersion, String toVersion) throws IOException {
        StringBuilder sb = new StringBuilder(patchPath.length() + 1 + fromVersion.length() + 4 + toVersion.length() + (1 + 5 + 1));

        sb.append(patchPath);
        sb.append("\t");
        sb.append(fromVersion);
        sb.append(" to ");
        sb.append(toVersion);
        sb.append("\tstart\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void logEnd(String patchPath, String fromVersion, String toVersion) throws IOException {
        StringBuilder sb = new StringBuilder(patchPath.length() + 1 + fromVersion.length() + 4 + toVersion.length() + (1 + 5 + 1));

        sb.append(patchPath);
        sb.append("\t");
        sb.append(fromVersion);
        sb.append(" to ");
        sb.append(toVersion);
        sb.append("\tfinish\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void logPatch(String patchPath, String fromVersion, String toVersion, int fileIndex, PatchAction action, String oldFilePath, String newFilePath) throws IOException {
        String fileIndexString = Integer.toString(fileIndex), actionString = action == PatchAction.START ? "start" : "finish";

        StringBuilder sb = new StringBuilder(patchPath.length() + 1 + fromVersion.length() + 4 + toVersion.length() + (1 + 5 + 1)
                + fileIndexString.length() + 1 + actionString.length() + 1 + oldFilePath.length() + 4 + newFilePath.length() + 1);

        sb.append(patchPath);
        sb.append("\t");
        sb.append(fromVersion);
        sb.append(" to ");
        sb.append(toVersion);
        sb.append("\tpatch\t");

        sb.append(fileIndexString);
        sb.append("\t");
        sb.append(actionString);
        sb.append("\t");
        sb.append(oldFilePath);
        sb.append(" -> ");
        sb.append(newFilePath);
        sb.append("\n");

        out.write(sb.toString().getBytes());
        out.flush();
    }
}
