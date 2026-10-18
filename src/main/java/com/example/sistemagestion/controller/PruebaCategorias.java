package com.example.sistemagestion.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PruebaCategorias extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/sistemagestion/view/categorias.fxml"
                )
        );

        Scene scene = new Scene(loader.load(), 1000, 650);

        stage.setTitle("Prueba - Gestión de Categorías");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
