package com.example.administracion_restaurante;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//C:\Users\alvarod\IdeaProjects\CartasJavaFX\target
//C:\Users\alvarod\IdeaProjects\CartasJavaFX\target\classes
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("menu.fxml"));
        Scene scene = new Scene(root, 600, 400);

       // scene.getStylesheets().add(getClass().getResource("menuCSS.css").toExternalForm());
        primaryStage.setResizable(false);
        primaryStage.setTitle("Restaurante");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}