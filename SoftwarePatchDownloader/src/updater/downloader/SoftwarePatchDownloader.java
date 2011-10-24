package updater.downloader;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import updater.downloader.download.PatchDownloader;
import updater.downloader.util.Util;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwarePatchDownloader {

    protected SoftwarePatchDownloader() {
    }

    public static void main(String[] args) throws IOException {
        Util.setLookAndFeel();
        try {
            GetClientScriptResult result = Util.getClientScript(args.length > 0 ? args[0] : null);
            if (result.getClientScript() != null) {
                PatchDownloader.checkForUpdates(new File(result.getClientScriptPath()), result.getClientScript());
            } else {
                JOptionPane.showMessageDialog(null, "Config file not found, is empty or is invalid.");
            }
        } catch (IOException ex) {
            Logger.getLogger(SoftwarePatchDownloader.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software->icon or root->information->downloader->icon.");
        }
    }
}