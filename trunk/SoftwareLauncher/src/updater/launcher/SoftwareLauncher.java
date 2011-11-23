package updater.launcher;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import updater.gui.UpdaterWindow;
import updater.launcher.BatchPatcher.UpdateResult;
import updater.patch.Patcher.Replacement;
import updater.patch.PatcherListener;
import updater.script.Client;
import updater.script.Client.Information;
import updater.script.InvalidFormatException;
import updater.util.CommonUtil.GetClientScriptResult;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class SoftwareLauncher {

    static {
        // set debug mode
        System.setProperty("SoftwareUpdaterDebugMode", "true");
    }

    protected SoftwareLauncher() {
    }

    /**
     * Start update (if any) and launch/start the software.
     * @param clientScriptPath the path of the client script file
     * @param args the arguments to pass-in to launch/start the software
     * @throws IOException possible failure: failed to copy self-updater to desire destination, failed to write to replacement file or failed to execute the command
     * @throws InvalidFormatException the format of the client script is invalid
     * @throws LaunchFailedException launch failed, possible jar not found, class not found or main method not found
     */
    public static void start(String clientScriptPath, String[] args) throws IOException, InvalidFormatException, LaunchFailedException {
        File clientScript = new File(clientScriptPath);
        Client client = Client.read(Util.readFile(clientScript));
        if (client != null) {
            start(clientScript, client, args);
        }
    }

    /**
     * Start update (if any) and launch/start the software.
     * @param clientScriptFile the client script file
     * @param client the client script
     * @param args the arguments to pass-in to launch/start the software
     * @throws IOException possible failure: failed to copy self-updater to desire destination, failed to write to replacement file or failed to execute the command
     * @throws LaunchFailedException launch failed, possible jar not found, class not found or main method not found
     */
    public static void start(File clientScriptFile, Client client, String[] args) throws IOException, LaunchFailedException {
        boolean launchSoftware = false;
        UpdateResult updateResult = null;
        if (!client.getPatches().isEmpty()) {
            String storagePath = client.getStoragePath();
            Information clientInfo = client.getInformation();

            Image softwareIcon = null;
            Image updaterIcon = null;
            //<editor-fold defaultstate="collapsed" desc="get icons">
            if (clientInfo != null) {
                if (clientInfo.getSoftwareIconLocation() != null) {
                    if (clientInfo.getSoftwareIconLocation().equals("jar")) {
                        URL resourceURL = SoftwareLauncher.class.getResource(clientInfo.getSoftwareIconPath());
                        if (resourceURL != null) {
                            softwareIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                        } else {
                            throw new IOException("Resource not found: " + clientInfo.getSoftwareIconPath());
                        }
                    } else {
                        softwareIcon = ImageIO.read(new File(clientInfo.getSoftwareIconPath()));
                    }
                }
                if (clientInfo.getLauncherIconLocation() != null) {
                    if (clientInfo.getLauncherIconLocation().equals("jar")) {
                        URL resourceURL = SoftwareLauncher.class.getResource(clientInfo.getLauncherIconPath());
                        if (resourceURL != null) {
                            updaterIcon = Toolkit.getDefaultToolkit().getImage(resourceURL);
                        } else {
                            throw new IOException("Resource not found: " + clientInfo.getLauncherIconPath());
                        }
                    } else {
                        updaterIcon = ImageIO.read(new File(clientInfo.getLauncherIconPath()));
                    }
                }
            }
            if (softwareIcon == null) {
                softwareIcon = Toolkit.getDefaultToolkit().getImage(SoftwareLauncher.class.getResource("/software_icon.png"));
            }
            if (updaterIcon == null) {
                updaterIcon = Toolkit.getDefaultToolkit().getImage(SoftwareLauncher.class.getResource("/updater_icon.png"));
            }
            //</editor-fold>

            final BatchPatcher batchPatcher = new BatchPatcher();

            String softwareName = clientInfo != null && clientInfo.getSoftwareName() != null ? clientInfo.getSoftwareName() : "Software Updater";
            String launcherName = clientInfo != null && clientInfo.getLauncherTitle() != null ? clientInfo.getLauncherTitle() : "Software Updater";

            // GUI
            final Thread currentThread = Thread.currentThread();
            final UpdaterWindow updaterGUI = new UpdaterWindow(softwareName, softwareIcon, launcherName, updaterIcon);
            updaterGUI.addListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    batchPatcher.pause(true);

                    Object[] options = {"Yes", "No"};
                    int result = JOptionPane.showOptionDialog(null, "Are you sure to cancel update?", "Canel Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                    if (result == 0) {
                        updaterGUI.setCancelEnabled(false);
                        currentThread.interrupt();
                    }

                    batchPatcher.pause(false);
                }
            });
            updaterGUI.setProgress(0);
            updaterGUI.setMessage("Preparing ...");
            JFrame updaterFrame = updaterGUI.getGUI();
            updaterFrame.setVisible(true);

            // update
            try {
                updateResult = batchPatcher.update(clientScriptFile, client, new File(storagePath), new PatcherListener() {

                    @Override
                    public void patchProgress(int percentage, String message) {
                        updaterGUI.setProgress(percentage);
                        updaterGUI.setMessage(message);
                    }

                    @Override
                    public void patchFinished() {
                    }

                    @Override
                    public void patchEnableCancel(boolean enable) {
                        updaterGUI.setCancelEnabled(enable);
                    }
                });

                // check if there is any replacement failed and do the replacement with the self updater
                handleReplacement(client, updateResult.getReplacementList(), args);
            } catch (Exception ex) {
                Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);

                JOptionPane.showMessageDialog(updaterFrame, "Error occurred when updating the software.");

                if (updaterGUI.isCancelEnabled()) {
                    Object[] options = {"Launch", "Exit"};
                    int result = JOptionPane.showOptionDialog(updaterFrame, "Continue to launch the software?", "Continue Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (result == 0) {
                        launchSoftware = true;
                    } else {
                        JOptionPane.showMessageDialog(updaterFrame, "You can restart the software to try to update software again.");
                    }
                }
            } finally {
                updaterFrame.setVisible(false);
                updaterFrame.dispose();
            }
        }

        // launch/start the software
        if (updateResult == null || (updateResult.isUpdateSucceed() || launchSoftware)) {
            String launchType = client.getLaunchType();
            String afterLaunchOperation = client.getLaunchAfterLaunch();
            String jarPath = client.getLaunchJarPath();
            String mainClass = client.getLaunchMainClass();
            List<String> launchCommands = client.getLaunchCommands();

            if (launchType.equals("jar")) {
                startSoftware(jarPath, mainClass, args);
            } else {
                ProcessBuilder builder = new ProcessBuilder(launchCommands);
                try {
                    builder.start();
                } catch (Exception ex) {
                    throw new LaunchFailedException(ex);
                }
                if (afterLaunchOperation != null && afterLaunchOperation.equals("exit")) {
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Write the replacement list into the file. (destination, then new file path, line by line)
     * @param file the file to write into
     * @param replacementList the replacement list
     * @throws IOException error occurred when writing to the file
     */
    protected static void writeReplacement(File file, List<Replacement> replacementList) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(file));

            for (Replacement _replacement : replacementList) {
                writer.println(_replacement.getDestination());
                writer.println(_replacement.getNewFilePath());
            }
        } finally {
            Util.closeQuietly(writer);
        }
    }

    public static void handleReplacement(Client clientScript, List<Replacement> replacementList, String[] launchArgs) throws IOException {
        if (replacementList == null || replacementList.isEmpty()) {
            return;
        }
        if (clientScript == null) {
            throw new NullPointerException("argument 'clientScript' cannot be null");
        }
        if (launchArgs == null) {
            throw new NullPointerException("argument 'launchArgs' cannot be null");
        }

        // copy the self updater to the storage path from inside the jar
        Util.writeFile(new File(clientScript.getStoragePath() + File.separator + "SoftwareSelfUpdater.jar"), Util.readResourceFile("/SoftwareSelfUpdater.jar"));

        // prepare the replacement file for the self updater
        File replacementFile = new File(clientScript.getStoragePath() + File.separator + "replacement.txt");
        writeReplacement(replacementFile, replacementList);

        // prepare the command to execute the self updater
        List<String> commands = new ArrayList<String>();

        String javaBinary = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        // the physical/file path of this launcher jar
        String launcherPath = null;
        try {
            launcherPath = SoftwareLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
        } catch (URISyntaxException ex) {
            JOptionPane.showMessageDialog(null, "Fatal error occurred: jar path detected of this launcher is invalid.");
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        commands.add(javaBinary);
        commands.add("-jar");
        commands.add(clientScript.getStoragePath() + File.separator + "SoftwareSelfUpdater.jar");
        commands.add(clientScript.getStoragePath() + File.separator + "update.lck");
        commands.add(replacementFile.getAbsolutePath());

        if (clientScript.getLaunchType().equals("jar")) {
            commands.add(javaBinary);
            commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            commands.add("-jar");
            commands.add(launcherPath);
            commands.addAll(Arrays.asList(launchArgs));
        } else {
            for (String _command : clientScript.getLaunchCommands()) {
                commands.add(_command.replace("{java}", javaBinary));
            }
        }

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.start();
        System.exit(0);
    }

    /**
     * Load the jar and execute the main method.
     * @param jarPath the path of the jar file
     * @param mainClass the class that contain the main method
     * @param args arguments to pass-in to the main method
     * @throws LaunchFailedException launch failed, possible jar not found, class not found or main method not found
     */
    protected static void startSoftware(String jarPath, String mainClass, String[] args) throws LaunchFailedException {
        try {
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{new File(jarPath).toURI().toURL()}, SoftwareLauncher.class.getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, loader);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("main")) {
                    method.invoke(null, (Object) (args));
                }
            }
        } catch (Exception ex) {
            throw new LaunchFailedException(ex);
        }
    }

    public static void main(String[] args) {
        try {
            Util.setLookAndFeel();
        } catch (Exception ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }

        GetClientScriptResult result = null;
        try {
            result = Util.getClientScript(args.length > 0 ? args[0] : null);
        } catch (IOException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to load config file.");
            return;
        } catch (InvalidFormatException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Config file format invalid.");
            return;
        }

        try {
            SoftwareLauncher.start(new File(result.getClientScriptPath()), result.getClientScript(), args);
        } catch (IOException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Fail to read images stated in the config file: root->information->software-icon or root->information->updater-icon.");
            return;
        } catch (LaunchFailedException ex) {
            Logger.getLogger(SoftwareLauncher.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Failed to launch the software.");
            return;
        }
    }
}