/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.commlib.SecureClient;
import com.kadware.komodo.commlib.PollResult;
import javafx.application.Platform;
import javafx.scene.Scene;
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
                    //  This has a delay built-in, by virtual of the call to /poll which blocks for a period of time
                    //  until there is something to report, or until the /poll endpoint gets tired of us hanging around.
                    SecureClient.ResultFromSend sendResult = _consoleInfo._secureClient.sendGet("/poll");
                    ObjectMapper mapper = new ObjectMapper();
                    PollResult spp =
                        mapper.readValue(sendResult._responseStream, new TypeReference<PollResult>() {});
                    if (spp != null) {
                        _pollResult = spp;
                        Platform.runLater(_refreshTask);
                    }
                } catch (Exception ex) {
                    //  TODO following - fails, not on FX application thread; does Platform.ext() also die?
//                    new Alert(Alert.AlertType.ERROR,
//                              "Communications with remote lost:" + ex.getMessage()).showAndWait();
                    Platform.exit();
                }
            }
        }

        void terminate() { _terminate = true; }
    }

    //  Refreshes the GUI elements
    private class RefreshTask implements Runnable {

        public void run() {
            if (_pollResult != null) {
                if (_pollResult._jumpKeySettings != null) {
                    _consoleInfo._jumpKeyPane.update(_pollResult._jumpKeySettings);
                }

                if (_pollResult._newOutputMessages != null) {
                    _consoleInfo._consolePane.update(_pollResult._newOutputMessages);
                }

                if (_pollResult._newLogEntries != null) {
                    _consoleInfo._logPane.update(_pollResult._newLogEntries);
                }

                //TODO check for system configuration changes
                //TODO check for hardware configuration changes
            }
        }
    }

    private final ConsoleInfo _consoleInfo;
    private final PollThread _pollThread = new PollThread();
    private PollResult _pollResult = null;
    private final RefreshTask _refreshTask = new RefreshTask();

    MainWindow(ConsoleInfo consoleInfo) { _consoleInfo = consoleInfo; }

    /**
     * Creates the scene for this window
     */
    Scene createScene() {
        _consoleInfo._consolePane = ConsolePane.create(_consoleInfo);
        _consoleInfo._jumpKeyPane = JumpKeyPane.create(_consoleInfo);
        _consoleInfo._logPane = LogPane.create(_consoleInfo);

        Tab iplTab = new Tab("IPL", new Label("Initial Program Load"));
        iplTab.setClosable(false);
        iplTab.setContent(_consoleInfo._jumpKeyPane);

        Tab consoleTab = new Tab("Console", new Label("Operating System Console"));
        consoleTab.setClosable(false);
        consoleTab.setContent(_consoleInfo._consolePane);

        Tab hardwareConfigTab = new Tab("Components", new Label("Hardware Configuration"));
        hardwareConfigTab.setClosable(false);

        Tab systemConfigTab = new Tab("Configuration", new Label("System Settings"));
        systemConfigTab.setClosable(false);

        Tab logTab = new Tab("Log", new Label("System Log"));
        logTab.setClosable(false);
        logTab.setContent(_consoleInfo._logPane);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(iplTab);
        tabPane.getTabs().add(consoleTab);
        tabPane.getTabs().add(hardwareConfigTab);
        tabPane.getTabs().add(systemConfigTab);
        tabPane.getTabs().add(logTab);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Label(""));
        borderPane.setCenter(tabPane);

        _pollThread.start();
        return new Scene(borderPane, 700, 350);
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
