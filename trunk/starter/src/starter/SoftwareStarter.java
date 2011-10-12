package starter;

import java.io.BufferedReader;
import starter.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import starter.script.Client;
import starter.script.Patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareStarter {

    private Client client;
    private String executablePath;
    private String mainClass;
    private String pathToStoreUpdates = "update/";
    private String updateCatalogURL = "";

    public SoftwareStarter(String[] args) throws IOException {
        File clientScript = new File("updater.xml");
        client = Client.read(Util.readFile(clientScript));

        boolean softwareUpdated = false;

        Patch update;
//        while ((update = client.getAndRemoveUpdate(client.getVersion())) != null) {
//            String newVersion = updateSoftware(update);
//            client.setVersion(newVersion);
//            softwareUpdated = true;
//        }

        if (softwareUpdated) {
            Util.truncateFolder(new File(pathToStoreUpdates));
            Util.writeFile(clientScript, client.output());
        }
    }

//    public String updateSoftware(Update update) throws IOException {
//        List<UpdateFile> files = update.getFiles();
//        for (UpdateFile _updateFile : files) {
//            File replaceDir = new File(Util.getFileDirectory(new File(_updateFile.getReplace())));
//            if (!replaceDir.isDirectory()) {
//                replaceDir.mkdirs();
//            }
//            Util.copyFile(new File(pathToStoreUpdates + _updateFile.getStore()), new File(_updateFile.getReplace()));
//        }
//
//        return update.getVersionTo();
//    }
    public void startSoftware(String[] args) throws Exception {
        ClassLoader loader = URLClassLoader.newInstance(new URL[]{new URL("file://" + executablePath)}, getClass().getClassLoader());
        Class<?> clazz = Class.forName(mainClass, true, loader);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("main")) {
                method.invoke(null, args == null ? new Object[1] : args);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        URL oracle = new URL("http://www.yahoo.com/");
        HttpURLConnection yc = (HttpURLConnection) oracle.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                yc.getInputStream()));
        String inputLine;

        System.out.println(yc.getHeaderFields());
        
        while ((inputLine = in.readLine()) != null) 
            System.out.println(inputLine);
        in.close();
    }
//    public static void main(String[] args) throws IOException {
//        new SoftwareStarter(args);
//
//    }
}
