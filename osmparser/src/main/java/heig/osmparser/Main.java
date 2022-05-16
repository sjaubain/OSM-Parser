package heig.osmparser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {

 /*
        // THIS PART IS TO TRY TO COMPRESS VERY LARGE OSM FILES
        try {
            String input = "./input/wayss.osm";
            String output = "./input/wayss_new.osm";
            try {

                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                BufferedReader reader = new BufferedReader(new FileReader(input));

                String line; String[] splitLine;
                while((line = reader.readLine()) != null) {
                    splitLine = line.split(" ");
                    if(splitLine.length > 3) {
                        if(splitLine[2].equals("<node")) {
                            line = "  " + splitLine[2] + " " + splitLine[3] + " " + splitLine[8] + " " + splitLine[9];
                        } else if(splitLine[2].equals("<way")) {
                            line = "  <way>";
                        }
                    }
                    writer.write(line + "\n");
                    writer.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

 */


        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);
        stage.setTitle("OSM Parser");
        //stage.setResizable(false);
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