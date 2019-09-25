/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.kadware.komodo.commlib.SecureClient;
import javafx.application.Application;
import javafx.stage.Stage;

@SuppressWarnings("Duplicates")
public class Console extends Application {

    Stage _primaryStage = null;
    ConnectDialog _connectDialog = null;
    MainWindow _mainWindow = null;
    SecureClient _secureClient = null;
    String _systemIdent = "";
    String _systemVersion = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        _connectDialog = new ConnectDialog(this);
        _mainWindow = new MainWindow(this);
    }

    /**
     * Called when the launcher is ready for us to set up our graphical elements
     */
    @Override
    public void start(
        final Stage primaryStage
    ) {
        _primaryStage = primaryStage;
        _primaryStage.setTitle("KOMODO System Console");
        _primaryStage.show();

        primaryStage.setScene(_connectDialog.createScene());
    }
}
