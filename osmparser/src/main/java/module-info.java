module heig.osmparser {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.xml;
    requires javafx.graphics;
    requires com.google.gson;

    opens heig.osmparser to javafx.fxml;
    exports heig.osmparser;
    exports heig.osmparser.controllers;
    opens heig.osmparser.controllers to javafx.fxml;
    exports heig.osmparser.net;
    exports heig.osmparser.utils.logs;
    opens heig.osmparser.net to javafx.fxml;
}