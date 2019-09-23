package com.kadware.komodo.kconsole;

import javafx.application.Platform;
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
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@SuppressWarnings("Duplicates")
class ConnectDialog {

    private final Console _console;
    private Socket _socket = null;

    private TextField _hostNameField = null;
    private NumberField _portNumberField = null;
    private TextField _userNameField = null;
    private TextField _passwordField = null;

    private class CancelPressed implements EventHandler<ActionEvent> {

        public void handle(
            final ActionEvent event
        ) {
            Platform.exit();
        }
    }

    private class ConnectPressed implements EventHandler<ActionEvent> {

        public void handle(
            final ActionEvent event
        ) {
//            try {
                //  open a client socket to the indicated server
//                System.out.println("FOO");
//                _socket = new Socket(InetAddress.getByName(_hostNameField.getText()), 20);
//                System.out.println("FOO");
                _console._primaryStage.setScene(_console._mainWindow.createScene());
//                System.out.println("FOO");
//            } catch (IOException ex) {
//                //TODO
//            }
        }
    }

    private class NumberField extends TextField {

        @Override
        public void replaceText(
            int start,
            int end,
            String text
        ) {
            // If the replaced text would end up being invalid, then simply
            // ignore this call!
            if (text.matches("[0-9]")) {
                super.replaceText(start, end, text);
            }
        }

        @Override
        public void replaceSelection(
            String text
        ) {
            if (text.matches("[0-9]")) {
                super.replaceSelection(text);
            }
        }
    }

    ConnectDialog(Console console) { _console = console; }

    Scene createScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle = new Text("Enter Host Connection Info");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label hostName = new Label("Host:");
        grid.add(hostName, 0, 1);

        _hostNameField = new TextField();
        grid.add(_hostNameField, 1, 1);

        Label portNumber = new Label("Port:");
        grid.add(portNumber, 0, 2);

        _portNumberField = new NumberField();
        grid.add(_portNumberField, 1, 2);

        Label userName = new Label("User Name:");
        grid.add(userName, 0, 3);

        _userNameField = new TextField();
        grid.add(_userNameField, 1, 3);

        Label password = new Label("Password:");
        grid.add(password, 0, 4);

        _passwordField = new PasswordField();
        grid.add(_passwordField, 1, 4);

        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(new CancelPressed());

        Button connectButton = new Button("Connect");
        connectButton.setDefaultButton(true);
        connectButton.setOnAction(new ConnectPressed());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);
        buttonBox.getChildren().add(cancelButton);
        buttonBox.getChildren().add(connectButton);

        grid.add(buttonBox, 1, 5);

        return new Scene(grid, 300, 275);
    }
}
