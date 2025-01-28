package org.example.chatapplication;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Wrapper extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader1 = new FXMLLoader(ServerController.class.getResource("client-form.fxml"));
        Scene clientScene = new Scene(fxmlLoader1.load());
        Stage clientStage = new Stage();
        clientStage.setTitle("Client");
        clientStage.setScene(clientScene);
        clientStage.show();

        FXMLLoader fxmlLoader3 = new FXMLLoader(ServerController.class.getResource("client2-form.fxml"));
        Scene client2Scene = new Scene(fxmlLoader3.load());
        Stage client2Stage = new Stage();
        client2Stage.setTitle("Client2");
        client2Stage.setScene(client2Scene);
        client2Stage.show();

        FXMLLoader fxmlLoader = new FXMLLoader(ClientController.class.getResource("server-form.fxml"));
        Scene serverScene = new Scene(fxmlLoader.load());
        Stage serverStage = new Stage();
        serverStage.setTitle("Server");
        serverStage.setScene(serverScene);
        serverStage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}
