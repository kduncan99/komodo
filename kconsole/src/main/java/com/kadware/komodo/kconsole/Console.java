/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import javafx.application.Application;
import javafx.stage.Stage;

@SuppressWarnings("Duplicates")
public class Console extends Application {

    private final ConsoleInfo _consoleInfo = new ConsoleInfo();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        _consoleInfo._connectDialog = new ConnectDialog(_consoleInfo);
        _consoleInfo._mainWindow = new MainWindow(_consoleInfo);
    }

    /**
     * Called when the launcher is ready for us to set up our graphical elements
     */
    @Override
    public void start(
        final Stage primaryStage
    ) {
        _consoleInfo._primaryStage = primaryStage;
        _consoleInfo._primaryStage.setTitle("KOMODO System Console");
        _consoleInfo._primaryStage.show();
        primaryStage.setScene(_consoleInfo._connectDialog.createScene());
    }

    /**
     * Called when the application goes away.
     * If there's a main window, tell it we're going away.
     */
    @Override
    public void stop() {
        if (_consoleInfo._mainWindow != null) {
            _consoleInfo._mainWindow.terminate();
        }
    }
}
