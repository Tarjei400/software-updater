package updater;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.script.Client;
import updater.RemoteContent.GetCatalogResult;
import updater.RemoteContent.GetPatchListener;
import updater.RemoteContent.RSAPublicKey;
import updater.gui.UpdaterWindow;
import updater.script.Catalog;
import updater.script.Catalog.Update;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareUpdater {

    protected SoftwareUpdater() {
    }

    public static void checkForUpdates(String clientScriptPath) throws InvalidFormatException, IOException {
        Client client = Client.read(Util.readFile(new File(clientScriptPath)));

        Information clientInfo = client.getInformation();
        Image softwareIcon = clientInfo.getSoftwareIconLocation().equals("jar") ? Toolkit.getDefaultToolkit().getImage(SoftwareUpdater.class.getResource(clientInfo.getSoftwareIconPath())) : ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
        Image updaterIcon = clientInfo.getUpdaterIconLocation().equals("jar") ? Toolkit.getDefaultToolkit().getImage(SoftwareUpdater.class.getResource(clientInfo.getUpdaterIconPath())) : ImageIO.read(new File(clientInfo.getUpdaterIconPath()));

        final UpdaterWindow updaterGUI = new UpdaterWindow(clientInfo.getSoftwareName(), softwareIcon, clientInfo.getUpdaterTitle(), updaterIcon);
        updaterGUI.addListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updaterGUI.setEnableCancel(false);
            }
        });
        updaterGUI.setProgress(0);
        updaterGUI.setMessage("Getting patches catalog ...");
        JFrame updaterFrame = updaterGUI.getGUI();
        updaterFrame.setVisible(true);

        Catalog catalog = null;
        try {
            catalog = getUpdatedCatalog(client);
            if (catalog == null) {
                JOptionPane.showMessageDialog(updaterFrame, "There are no updates available.");
                disposeWindow(updaterFrame);
                return;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(updaterFrame, "Error occurred when getting the patches catalog.");
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            disposeWindow(updaterFrame);
            return;
        }

        List<Update> updatePatches = getPatches(catalog, client.getVersion());
        if (updatePatches.isEmpty()) {
            JOptionPane.showMessageDialog(updaterFrame, "There are no updates available.");
            disposeWindow(updaterFrame);
            return;
        }

        final IntegerReference downloadedSize = new IntegerReference(0);
        final long totalDownloadSize = calculateTotalLength(updatePatches);
        GetPatchListener listener = new GetPatchListener() {

            @Override
            public void byteDownloaded(int numberOfBytes) {
                int newValue = downloadedSize.getValue() + numberOfBytes;
                downloadedSize.setValue(newValue);
                updaterGUI.setProgress((int) ((float) downloadedSize.getValue() / (float) totalDownloadSize));
                updaterGUI.setMessage("Downloading patches, " + Util.humanReadableByteCount(downloadedSize.getValue(), false) + "/" + Util.humanReadableByteCount(totalDownloadSize, false) + " downloaded ...");
            }
        };

        // update storage path
        for (Update update : updatePatches) {
            boolean updateResult = RemoteContent.getPatch(listener, update.getPatchUrl(), new File(client.getStoragePath() + ""), update.getPatchChecksum(), update.getPatchLength());
            if (!updateResult) {
                JOptionPane.showMessageDialog(updaterFrame, "Error occurred when getting the update patch.");
                disposeWindow(updaterFrame);
                return;
            }
        }

        updaterGUI.setProgress(100);
        updaterGUI.setMessage("You have to restart the application to make the update take effect.");
        disposeWindow(updaterFrame);
    }

    protected static void disposeWindow(JFrame frame) {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    protected static List<Update> getPatches(Catalog catalog, String currentVersion) {
        return getPatches(catalog.getUpdates(), currentVersion);
    }

    protected static List<Update> getPatches(List<Update> allUpdates, String fromVersion) {
        List<Update> returnResult = new ArrayList<Update>();

        String maxVersion = fromVersion;
        for (Update update : allUpdates) {
            if (update.getVersionFrom().equals(fromVersion)) {
                List<Update> tempResult = new ArrayList<Update>();

                List<Update> _allUpdates = getPatches(allUpdates, update.getVersionTo());
                tempResult.add(update);
                if (!_allUpdates.isEmpty()) {
                    tempResult.addAll(_allUpdates);
                }

                Update _maxUpdateThisRound = tempResult.get(tempResult.size() - 1);
                long compareResult = Util.compareVersion(_maxUpdateThisRound.getVersionTo(), maxVersion);
                if (compareResult > 0) {
                    maxVersion = _maxUpdateThisRound.getVersionTo();
                    returnResult = tempResult;
                } else if (compareResult == 0) {
                    long tempResultCost = calculateTotalLength(tempResult);
                    long returnResultCost = calculateTotalLength(returnResult);
                    if (tempResultCost < returnResultCost
                            || (tempResultCost == returnResultCost && tempResult.size() < returnResult.size())) {
                        returnResult = tempResult;
                    }
                }
            }
        }

        return returnResult;
    }

    protected static long calculateTotalLength(List<Update> allUpdates) {
        long returnResult = 0;
        for (Update _update : allUpdates) {
            returnResult += _update.getPatchLength();
        }
        return returnResult;
    }

    public static Catalog getUpdatedCatalog(String clientScriptPath) throws IOException, InvalidFormatException {
        Client client = Client.read(Util.readFile(new File(clientScriptPath)));
        return getUpdatedCatalog(client);
    }

    protected static Catalog getUpdatedCatalog(Client client) throws IOException {
        String catalogURL = client.getCatalogUrl();

        RSAPublicKey publicKey = null;
        if (client.getPublicKey() != null) {
            String publicKeyString = client.getPublicKey();
            int pos = publicKeyString.indexOf(';');
            if (pos != -1) {
                publicKey = new RSAPublicKey(new BigInteger(publicKeyString.substring(0, pos), 16), new BigInteger(publicKeyString.substring(pos + 1, publicKeyString.length()), 16));
            }
        }

        GetCatalogResult getCatalogResult = RemoteContent.getCatalog(catalogURL, client.getLastUpdated(), publicKey);
        if (getCatalogResult.isNotModified()) {
            return null;
        }
        if (getCatalogResult.getCatalog() == null) {
            throw new IOException("Error occurred when getting the catalog.");
        }
        return getCatalogResult.getCatalog();
    }

    public static class IntegerReference {

        protected int value;

        public IntegerReference(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static void main(String[] args) {
        Util.setLookAndFeel();
        try {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(new Dimension(400, 300));
            frame.setLocationByPlatform(true);
            frame.setVisible(true);

            if (args.length > 0) {
                checkForUpdates(args[0]);
            } else {
                byte[] configPathByte = Util.readResourceFile("/config");
                if (configPathByte != null && configPathByte.length != 0) {
                    checkForUpdates(new String(configPathByte, "US-ASCII"));
                } else {
                    JOptionPane.showMessageDialog(null, "Config path not set and not found");
                }
            }
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
        } catch (IOException ex) {
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
        }
    }
}
