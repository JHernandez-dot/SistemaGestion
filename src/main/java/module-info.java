
module com.example.sistemagestion {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.sistemagestion to javafx.fxml;
    opens com.example.sistemagestion.controller to javafx.fxml;

    exports com.example.sistemagestion;
}
