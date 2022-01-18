package heig.osmparser.controllers;

import heig.osmparser.logs.Log;
import javafx.application.Platform;

/**
 * This class allows an object in the model layer to
 * communicate with the main controller associated to the GUI
 */
public class MainControllerHandler {
    private MainController controller;
    public MainControllerHandler() {
        this.controller = null;
    }
    public MainControllerHandler(MainController controller) {
        this.controller = controller;
    }
    public void sendMessageToController(String msg, Log.LogLevels level) {
        if(this.controller != null) {
            Platform.runLater(() -> {
                controller.log(msg, level);
            });
        }
    }
}
