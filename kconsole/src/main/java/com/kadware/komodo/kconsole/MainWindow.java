/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.commlib.SecureClient;
import com.kadware.komodo.commlib.SystemProcessorPoll;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

@SuppressWarnings("Duplicates")
class MainWindow {

    private class PollThread extends Thread {
        private boolean _terminate = false;

        public void run() {
            while (!_terminate) {
                try {
                    SecureClient.SendResult sendResult = _consoleInfo._secureClient.sendGet("/poll");
                    ObjectMapper mapper = new ObjectMapper();
                    SystemProcessorPoll spp =
                        mapper.readValue(sendResult._responseStream, new TypeReference<SystemProcessorPoll>() {});
                    if (spp != null) {
                        _pollResult = spp;
                        Platform.runLater(_refreshTask);
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                              "Communications with remote lost:" + ex.getMessage()).showAndWait();
                    Platform.exit();
                }
            }
        }

        public void terminate() { _terminate = true; }
    }

    //  Refreshes the GUI elements
    private class RefreshTask implements Runnable {

        public void run() {
            if (_pollResult != null) {
                if (_pollResult._identifiers != null) {
                    String headerMsg = String.format("Remote System:%s Version:%s",
                                                     _pollResult._identifiers._systemIdentifier,
                                                     _pollResult._identifiers._versionString);
                    _borderPane.setTop(new Label(headerMsg));
                }

                if (_pollResult._jumpKeys != null) {
                    _consoleInfo._jumpKeyPane.update(_pollResult._jumpKeys);
                }

                //TODO  Check other elements of the poll result
            }
        }
    }


    private BorderPane _borderPane;
    private final ConsoleInfo _consoleInfo;
    private final PollThread _pollThread = new PollThread();
    private SystemProcessorPoll _pollResult = null;
    private final RefreshTask _refreshTask = new RefreshTask();


    MainWindow(ConsoleInfo consoleInfo) { _consoleInfo = consoleInfo; }


    /**
     * Creates the scene for this window
     */
    Scene createScene() {
        _consoleInfo._jumpKeyPane = JumpKeyPane.create(_consoleInfo);

        Tab overviewTab = new Tab("Overview", new Label("Hardware overview"));
        overviewTab.setClosable(false);

        Tab consoleTab = new Tab("Console", new Label("Operating system console"));
        consoleTab.setClosable(false);

        Tab iplTab = new Tab("IPL", new Label("Initial Program Load"));
        iplTab.setClosable(false);
        //TODO for now, just put the JK's on the pane, we'll add more stuff later, in some kind of other pane
        iplTab.setContent(_consoleInfo._jumpKeyPane);

        Tab logTab = new Tab("Log", new Label("Hardware log"));
        logTab.setClosable(false);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(overviewTab);
        tabPane.getTabs().add(consoleTab);
        tabPane.getTabs().add(iplTab);
        tabPane.getTabs().add(logTab);

        _borderPane = new BorderPane();
        _borderPane.setTop(new Label(""));
        _borderPane.setCenter(tabPane);

        _pollThread.start();
        return new Scene(_borderPane, 700, 350);
    }

    /**
     * Notification that the application is shutting down
     */
    void terminate() {
        _pollThread.terminate();
        while (_pollThread.isAlive()) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                //  do nothing
            }
        }
    }
}
