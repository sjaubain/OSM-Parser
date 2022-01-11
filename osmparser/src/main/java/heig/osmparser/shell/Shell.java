package heig.osmparser.shell;

import heig.osmparser.controllers.MainController;
import heig.osmparser.logs.Log;
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
            if(!command.isEmpty()) {
                Process process;
                try {
                    System.out.println(command);
                    Platform.runLater(() -> {
                        controller.log(command, Log.LogLevels.INFO);
                    });
                    if (isWindows) {
                        ProcessBuilder builder = new ProcessBuilder("CMD", "/C", command);
                        builder.redirectErrorStream(true);
                        process = builder.start();
                    } else {
                        ProcessBuilder builder = new ProcessBuilder("sh", "-c", command);
                        builder.redirectErrorStream(true);
                        process = builder.start();
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        Platform.runLater(() -> {
                            controller.log(finalLine, Log.LogLevels.INFO);
                        });
                    }
                    reader.close();
                    process.waitFor();
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        controller.log(e.getMessage(), Log.LogLevels.ERROR);
                    });
                }
            }
        }).start();
    }
}
