/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import com.bearsnake.komodo.kutelib.Terminal;
import com.bearsnake.komodo.kutelib.TerminalStack;
import com.bearsnake.komodo.kutelib.keypads.ControlKeyPad;
import com.bearsnake.komodo.kutelib.keypads.CursorKeyPad;
import com.bearsnake.komodo.kutelib.keypads.FunctionKeyPad;
import com.bearsnake.komodo.kutelib.keypads.MiscKeyPad;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Implements a terminal emulator for Komodo
 * We do NOT implement the conventional protocol in terms of RID/SID/DID, nor do we do checksum calculations.
 * We do not implement KANJI.
 * We *might* not implement transferring to virtual diskette
 * We probably will implement printing... probably
 * We support terminal sizes from 12x64 through 256x256 per convention
 */
public class Kute extends Application {

    private static Kute _instance;

    private TerminalStack _terminalStack;
    private Scene _scene;

    public static void main(String[] args) {
        launch(args);
    }

    private boolean confirmExit() {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quit Kute");
        alert.setHeaderText("Shutting down Kute");
        alert.setContentText("Do you really want to close all sessions and shut down?");
        var result = alert.showAndWait();
        return result.isPresent() && (result.get() == ButtonType.OK);
    }

    @Override
    public void start(Stage primaryStage) {

        var root = new VBox();
        var menuBar = createMenuBar();
        var content = createContentPane();
        root.getChildren().addAll(menuBar, content);
        _scene = new Scene(root);

        primaryStage.setTitle("Kute - Komodo UTS Terminal Emulator");
        primaryStage.setScene(_scene);
        primaryStage.sizeToScene();

        primaryStage.setOnCloseRequest(event -> {
            if (!confirmExit()) {
                event.consume();
            }
        });

        primaryStage.show();

        _instance = this;
    }

    public void cycleTabs() {
        _terminalStack.cycleTabs();
    }

    public static Kute getInstance() {
        return _instance;
    }

    public Scene getScene() {
        return _scene;
    }

    public Terminal getActiveTerminal() {
        return _terminalStack.getActiveTerminal();
    }

    @Override
    public void stop() throws Exception {
        _terminalStack.closeAll();
    }

    // ---------------------------------------------------------------------------------------------
    // Graphical content
    // ---------------------------------------------------------------------------------------------

    private BorderPane createContentPane() {
        var content = new BorderPane();

        var ctlPad = new ControlKeyPad();
        BorderPane.setMargin(ctlPad, new Insets(5)); // optional
        content.setTop(ctlPad);

        var fkPad = new FunctionKeyPad();
        BorderPane.setMargin(fkPad, new Insets(5)); // optional
        content.setLeft(fkPad);

        var cursorPad = new CursorKeyPad();
        var miscKeyPad = new MiscKeyPad();
        var cursorMiscSet = new VBox();
        cursorMiscSet.setSpacing(10.0);
        cursorMiscSet.getChildren().addAll(miscKeyPad, cursorPad);
        BorderPane.setMargin(cursorMiscSet, new Insets(5));
        content.setRight(cursorMiscSet);

        _terminalStack = new TerminalStack();
        _terminalStack.registerKeyPad(ctlPad);
        _terminalStack.registerKeyPad(fkPad);
        _terminalStack.registerKeyPad(cursorPad);
        _terminalStack.registerKeyPad(miscKeyPad);
        BorderPane.setMargin(_terminalStack, new Insets(5)); // optional
        content.setCenter(_terminalStack);

        return content;
    }

    // ---------------------------------------------------------------------------------------------
    // Menu bar
    // ---------------------------------------------------------------------------------------------

    public Menu createFileMenu() {
        var menu = new Menu("File");

        var exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            if (confirmExit()) {
                Platform.exit();
            }
        });

        menu.getItems().addAll(exitItem);
        return menu;
    }

    public Menu createSessionMenu() {
        var menu = new Menu("Session");

        var connectItem = new MenuItem("Connect");
        connectItem.setOnAction(e -> _terminalStack.getActiveTerminal().connect());

        var disconnectItem = new MenuItem("Disconnect");
        disconnectItem.setOnAction(e -> _terminalStack.getActiveTerminal().disconnect(true));

        menu.getItems().addAll(connectItem, disconnectItem);
        menu.setOnShowing(e -> {
            connectItem.setDisable(_terminalStack.getActiveTerminal().isConnected());
            disconnectItem.setDisable(!_terminalStack.getActiveTerminal().isConnected());
        });
        return menu;
    }

    public Menu createTraceMenu() {
        var menu = new Menu("Trace");

        var startItem = new MenuItem("Start");
        startItem.setOnAction(e -> _terminalStack.getActiveTerminal().startNetworkTrace());

        var stopItem = new MenuItem("Stop");
        stopItem.setOnAction(e -> _terminalStack.getActiveTerminal().stopNetworkTrace());

        var pauseItem = new MenuItem("Pause");
        pauseItem.setOnAction(e -> _terminalStack.getActiveTerminal().pauseNetworkTrace());

        menu.getItems().addAll(startItem, stopItem, pauseItem);
        menu.setOnShowing(e -> {
            var term = _terminalStack.getActiveTerminal();
            var canStart = false;
            var canStop = false;
            var canPause = false;
            if (term.isConnected()) {
                canStart = !term.isTraceActive();
                canPause = term.isTraceActive();
                canStop = term.isTraceActive() || term.isTracePaused();
            }
            startItem.setDisable(!canStart);
            stopItem.setDisable(!canStop);
            pauseItem.setDisable(!canPause);
        });
        return menu;
    }

    public Menu createHelpMenu() {
        var menu = new Menu("Help");
        menu.getItems().add(new MenuItem("About"));
        return menu;
    }

    public MenuBar createMenuBar() {

        var menuBar = new MenuBar();
        menuBar.getMenus().add(createFileMenu());
        menuBar.getMenus().add(createSessionMenu());
        menuBar.getMenus().add(createTraceMenu());
        menuBar.getMenus().add(createHelpMenu());
        menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }
}
