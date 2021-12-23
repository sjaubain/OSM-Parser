package heig.osmparser;

import heig.osmparser.model.Graph;
import heig.osmparser.utils.converters.EPSConverter;
import heig.osmparser.utils.parsers.Parser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);
        stage.setTitle("OSM Parser");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest((we -> {
            Platform.exit();
            System.exit(0);
        }));
    }

    public static void main(String[] args) {
        launch();
    }
}