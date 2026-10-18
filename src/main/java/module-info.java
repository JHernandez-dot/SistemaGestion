
module com.example.sistemagestion {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;

    opens com.example.sistemagestion to javafx.fxml;
    opens com.example.sistemagestion.controller to javafx.fxml;
    opens com.example.sistemagestion.controller.config to javafx.fxml;

    exports com.example.sistemagestion;
    exports com.example.sistemagestion.controller;
    exports com.example.sistemagestion.controller.config;
}
