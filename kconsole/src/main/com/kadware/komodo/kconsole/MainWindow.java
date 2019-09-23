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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@SuppressWarnings("Duplicates")
class MainWindow {

    final Console _console;

    MainWindow(Console console) { _console = console; }

    Scene createScene() {
        //  We're going to put a tab pane inside a border pane.
        //  Mainly... so we can add more above and below if we decide to do so.
        //  I fully expect that we'll have a status line or something, at a minimum.
        BorderPane border = new BorderPane();

        TabPane tabPane = new TabPane();

        Tab overviewTab = new Tab("Overview", new Label("Hardware overview"));
        overviewTab.setClosable(false);

        Tab consoleTab = new Tab("Console", new Label("Operating system console"));
        consoleTab.setClosable(false);

        Tab iplTab = new Tab("IPL", new Label("Initial Program Load"));
        iplTab.setClosable(false);

        Tab logTab = new Tab("Log", new Label("Hardware log"));
        logTab.setClosable(false);

        tabPane.getTabs().add(overviewTab);
        tabPane.getTabs().add(consoleTab);
        tabPane.getTabs().add(iplTab);
        tabPane.getTabs().add(logTab);
        border.setCenter(tabPane);

        return new Scene(border, 700, 350);
    }
}
