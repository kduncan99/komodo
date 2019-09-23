package com.kadwaare.komodo.kts;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

@SuppressWarnings("Duplicates")
public class Terminal extends Application {

    private Stage _primaryStage = null;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Invoked first - any necessary initialization occurs here
     */
    @Override
    public void init() {
        //TODO
        System.out.println("INITIALIZING");
    }

    /**
     * Called when the launcher is ready for us to set up our graphical elements
     */
    @Override
    public void start(
        final Stage primaryStage
    ) {
        _primaryStage = primaryStage;
        _primaryStage.setTitle("KOMODO Terminal System - Login");
        _primaryStage.show();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 300, 275);
        primaryStage.setScene(scene);

        Text scenetitle = new Text("Destination and Credentials:");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(scenetitle, 0, 0, 2, 1);

        Label hostName = new Label("Host:");
        grid.add(hostName, 0, 1);

        TextField hostNameField = new TextField();
        grid.add(hostNameField, 1, 1);

        Label portNumber = new Label("Port:");
        grid.add(portNumber, 0, 2);

        TextField portNumberField = new TextField();
        grid.add(portNumberField, 1, 2);

        Label userName = new Label("User Name:");
        grid.add(userName, 0, 3);

        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 3);

        Label pw = new Label("Password:");
        grid.add(pw, 0, 4);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 4);

        Button btn = new Button("Connect");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 5);

        btn.setOnAction(new LoginPressed());
//        btn.setOnAction(new EventHandler<ActionEvent>() {
//
//            @Override
//            public void handle(ActionEvent e) {
//                primaryStage.close();
//            }
//        });
    }

    /**
     * Called when the application is ready to go away
     */
    @Override
    public void stop() {
        //TODO
        System.out.println("STOPPING");
    }

    private class LoginPressed implements EventHandler<ActionEvent> {

        public void handle(ActionEvent e) {
            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(25, 25, 25, 25));

            Scene scene = new Scene(grid, 300, 275);
            Text scenetitle = new Text("You are now logged in.");
            scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
            grid.add(scenetitle, 0, 0, 2, 1);

            _primaryStage.setScene(scene);
        }
    }
}
