package starter;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;
import starter.script.InvalidFormatException;
import starter.util.Util;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import starter.script.Client;
import starter.patch.Updater;
import starter.patch.Updater.UpdateResult;
import starter.script.Client.Information;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareStarter {

    protected String[] args;
    protected Client client;
    protected String jarPath;
    protected String mainClass;
    protected String storagePath;

    public SoftwareStarter(String[] args) {
        this.args = args;
    }

    public void start(String clientScriptPath) throws IOException, InvalidFormatException, LaunchFailedException {
        File clientScript = new File(clientScriptPath);
        client = Client.read(Util.readFile(clientScript));

        jarPath = client.getJarPath();
        mainClass = client.getMainClass();
        storagePath = client.getStoragePath();

        Information clientInfo = client.getInformation();
        Image softwareIcon = clientInfo.getSoftwareIconLocation().equals("jar") ? Toolkit.getDefaultToolkit().getImage(SoftwareStarter.class.getResource(clientInfo.getSoftwareIconPath())) : ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
        Image updaterIcon = clientInfo.getUpdaterIconLocation().equals("jar") ? Toolkit.getDefaultToolkit().getImage(SoftwareStarter.class.getResource(clientInfo.getUpdaterIconPath())) : ImageIO.read(new File(clientInfo.getUpdaterIconPath()));

        UpdateResult updateResult = Updater.update(clientScript, client, new File(storagePath), clientInfo.getSoftwareName(), softwareIcon, clientInfo.getUpdaterTitle(), updaterIcon);
        if (updateResult.isUpdateSucceed() || updateResult.isLaunchSoftware()) {
            startSoftware();
        }
    }

    protected void startSoftware() throws LaunchFailedException {
        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{new File(jarPath).toURI().toURL()}, getClass().getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, loader);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("main")) {
                    method.invoke(null, (Object) (args));
                }
            }
        } catch (Exception ex) {
            throw new LaunchFailedException();
        }
    }

    public static void main(String[] args) {
        Util.setLookAndFeel();
        try {
            SoftwareStarter softwareStarter = new SoftwareStarter(args);
            if (args.length > 0) {
                softwareStarter.start(args[0]);
            } else {
                byte[] configPathByte = Util.readResourceFile("/config");
                if (configPathByte != null && configPathByte.length != 0) {
                    softwareStarter.start(new String(configPathByte, "US-ASCII"));
                } else {
                    JOptionPane.showMessageDialog(null, "Config file not found or is empty.");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
        } catch (LaunchFailedException ex) {
            Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
        }
    }
}
