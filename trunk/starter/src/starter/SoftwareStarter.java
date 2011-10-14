package starter;

import java.util.logging.Level;
import java.util.logging.Logger;
import starter.util.Util;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import starter.script.Client;
import starter.patch.Updater;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareStarter {

    private Client client;
    private String executablePath = "LanguageFilesTool.jar";
    private String mainClass = "langfiles.Main";
    private String pathToStoreUpdates = "update/";
    private String updateCatalogURL = "";

    public SoftwareStarter(String[] args) throws IOException {
        File clientScript = new File("updater.xml");
        client = Client.read(Util.readFile(clientScript));

        if (Updater.update(clientScript, client, new File(pathToStoreUpdates), "Language Files Tool", "/starter/logo.png", "Software Updater", "/starter/gui/images/UpdaterFrame/titleIcon.png")) {
            Util.truncateFolder(new File(pathToStoreUpdates));
            Util.writeFile(clientScript, client.output());
            try {
                startSoftware(args);
            } catch (Exception ex) {
                Logger.getLogger(SoftwareStarter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void startSoftware(String[] args) throws Exception {
        ClassLoader loader = URLClassLoader.newInstance(new URL[]{new File(executablePath).toURI().toURL()}, getClass().getClassLoader());
        Class<?> clazz = Class.forName(mainClass, true, loader);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("main")) {
                method.invoke(null, (Object) (args));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new SoftwareStarter(args);
        System.out.println(Math.round(11.5));
        System.out.println(Math.round(-11.5));
    }
}
