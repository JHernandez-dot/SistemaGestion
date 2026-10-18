package com.example.sistemagestion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/example/sistemagestion/view/login.fxml"
                )
        );

        Scene scene = new Scene(fxmlLoader.load(), 900, 550);

        stage.setTitle("Sistema de Gestión");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

}
