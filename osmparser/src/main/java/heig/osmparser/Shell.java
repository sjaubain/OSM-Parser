package heig.osmparser;

import heig.osmparser.controllers.MainController;
import heig.osmparser.utils.logs.Log;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Shell {

    private boolean isWindows;
    private MainController controller;

    public Shell(MainController controller) {
        isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        this.controller = controller;
    }

    public void exec(String command) {
        new Thread(() -> {
            Process process;
            try {
                if (isWindows) {
                    //process = Runtime.getRuntime().exec(String.format("cmd.exe dir %s", homeDirectory));
                    process = Runtime.getRuntime().exec(command);
                } else {
                    //process = Runtime.getRuntime().exec(String.format("sh -c ls %s", homeDirectory));
                    process = Runtime.getRuntime().exec(command);
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        controller.log(finalLine, Log.LogLevels.INFO);
                    });
                }
                process.waitFor();
                reader.close();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    controller.log(e.getMessage(), Log.LogLevels.ERROR);
                });
            }
        }).start();
    }
}
