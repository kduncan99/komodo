/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.commlib.HttpMethod;
import com.kadware.komodo.commlib.SecureClient;
import com.kadware.komodo.commlib.SystemProcessorPoll;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    private BorderPane _borderPane;
    private final ConsoleInfo _consoleInfo;

    MainWindow(
        final ConsoleInfo consoleInfo
    ) {
        _consoleInfo = consoleInfo;
    }

    /**
     * Creates the scene for this window
     */
    Scene createScene() {
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

        _borderPane = new BorderPane();
        _borderPane.setTop(new Label(""));
        _borderPane.setCenter(tabPane);

        poll();//TODO wedge this in for now, do it appropriately later
        return new Scene(_borderPane, 700, 350);
    }

    /**
     * Polls the remote system for any updates it might have for us
     */
    private void poll() {
        try {
            SecureClient.SendResult sendResult = _consoleInfo._secureClient.sendGet("/poll");
            System.out.println("FOO");//TODO
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(new String(sendResult._responseStream));//TODO
            SystemProcessorPoll spp = mapper.readValue(sendResult._responseStream, new TypeReference<SystemProcessorPoll>() {});
            System.out.println("FEE");//TODO

            String headerMsg = String.format("Remote System:%s Version:%s",
                                             spp._identifiers._systemIdentifier,
                                             spp._identifiers._versionString);
            System.out.println("FEI " + headerMsg);//TODO
            _borderPane.setTop(new Label(headerMsg));
            System.out.println("FISH");//TODO

            //TODO jumpkey traffic
            //TODO console traffic
            //TODO log traffic
            //TODO system hardware
        } catch (Exception ex) {
            ex.printStackTrace();//TODO
            new Alert(Alert.AlertType.ERROR, String.format("Cannot poll remote system:%s", ex.getMessage())).showAndWait();
        }
    }
}
