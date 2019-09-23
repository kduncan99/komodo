/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kts;

import javafx.application.Application;
import javafx.stage.Stage;

@SuppressWarnings("Duplicates")
public class Terminal extends Application {

    Stage _primaryStage = null;

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
        _primaryStage.setTitle("KOMODO Terminal System - Host and Credentials");
        _primaryStage.show();

        primaryStage.setScene(new ConnectDialog(this).createScene());
    }

    /**
     * Called when the application is ready to go away
     */
    @Override
    public void stop() {
        //TODO
        System.out.println("STOPPING");
    }
}
