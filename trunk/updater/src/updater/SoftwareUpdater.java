package updater;

import java.awt.Window;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import starter.script.Client;
import updater.RemoteContent.GetCatalogResult;
import updater.RemoteContent.RSAPublicKey;
import updater.script.Catalog;
import updater.script.Catalog.CatalogUpdate;
import updater.util.Util;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareUpdater {

    private SoftwareUpdater() {
    }

    protected static List<CatalogUpdate> getPatches(Catalog catalog, String currentVersion) {
        return getPatches(catalog.getUpdates(), currentVersion);
    }

    protected static List<CatalogUpdate> getPatches(List<CatalogUpdate> allUpdates, String fromVersion) {
        List<CatalogUpdate> returnResult = new ArrayList<CatalogUpdate>();

        String maxVersion = fromVersion;
        for (CatalogUpdate update : allUpdates) {
            if (update.getVersionFrom().equals(fromVersion)) {
                List<CatalogUpdate> tempResult = new ArrayList<CatalogUpdate>();

                List<CatalogUpdate> _allUpdates = getPatches(allUpdates, update.getVersionTo());
                tempResult.add(update);
                if (!_allUpdates.isEmpty()) {
                    tempResult.addAll(_allUpdates);
                }

                CatalogUpdate _maxUpdateThisRound = tempResult.get(tempResult.size() - 1);
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

    protected static long calculateTotalLength(List<CatalogUpdate> allUpdates) {
        long returnResult = 0;
        for (CatalogUpdate _update : allUpdates) {
            returnResult += _update.getPatchLength();
        }
        return returnResult;
    }

    public static void checkForUpdates(Client client, String catalogURL, Window parentWindow) {
        Catalog catalog = null;
        try {
            catalog = getUpdatedCatalog(client, catalogURL);
        } catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(parentWindow, "Catalog URL is not a valid HTTP URL.");
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parentWindow, "Error occurred when getting the update catalog.");
            Logger.getLogger(SoftwareUpdater.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        List<CatalogUpdate> updatePatches = getPatches(catalog, client.getVersion());
        if (updatePatches.isEmpty()) {
            JOptionPane.showMessageDialog(parentWindow, "There are no updates available.");
            return;
        }

        long totalDownloadSize = calculateTotalLength(updatePatches);
        
        // download updates
    }

    public static Catalog getUpdatedCatalog(Client client, String catalogURL) throws MalformedURLException, IOException {
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
            throw new IOException();
        }
        return getCatalogResult.getCatalog();
    }

    public static void main(String[] args) {
    }
}
